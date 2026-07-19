[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$required = @(
    'GEMINI_API_KEY',
    'GOOGLE_CLIENT_ID',
    'SMOKE_GOOGLE_ID_TOKEN',
    'SMOKE_JOB_URL',
    'ADMIN_EMAIL',
    'ADMIN_PASSWORD'
)
$missing = @($required | Where-Object { [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_)) })
if ($missing.Count -gt 0) {
    [Console]::Error.WriteLine("NOT EXECUTED: missing required acceptance variables: $($missing -join ', ')")
    exit 2
}

$root = Split-Path -Parent $PSScriptRoot
$frontend = Join-Path $root 'frontend'
$envFile = Join-Path $root '.env.example'
$stateFile = Join-Path $frontend 'test-results\acceptance-state.json'
$project = "jobcopilot-acceptance-$([Guid]::NewGuid().ToString('N').Substring(0, 8))"
$compose = @('compose', '--env-file', $envFile, '-p', $project)
$runtimeServices = @(
    'frontend',
    'backend',
    'ai-service',
    'ai-worker',
    'postgres',
    'rabbitmq',
    'redis',
    'minio'
)
$postgresUser = if ($env:POSTGRES_USER) { $env:POSTGRES_USER } else { 'resume_user' }
$postgresDb = if ($env:POSTGRES_DB) { $env:POSTGRES_DB } else { 'resume_assistant' }

# The backend verifies Google through tokeninfo; the frontend build consumes the client ID.
$env:VITE_GOOGLE_CLIENT_ID = $env:GOOGLE_CLIENT_ID
$env:SMOKE_BASE_URL = 'http://host.docker.internal'

try {
    & docker @compose up -d --build
    if ($LASTEXITCODE -ne 0) { throw 'Compose build/start failed' }

    $deadline = (Get-Date).AddMinutes(5)
    do {
        Start-Sleep -Seconds 5
        $rows = @(& docker @compose ps --format json | ForEach-Object { $_ | ConvertFrom-Json })
        $runtimeRows = @($rows | Where-Object { $_.Service -in $runtimeServices })
        $healthy = @($runtimeRows | Where-Object { $_.Health -eq 'healthy' }).Count
        $running = @($runtimeRows | Where-Object { $_.State -eq 'running' }).Count
        $migration = @($rows | Where-Object { $_.Service -eq 'db-migrate' })
        $migrationCompleted = $migration.Count -eq 1 `
            -and $migration[0].State -eq 'exited' `
            -and [int]$migration[0].ExitCode -eq 0
    } while (($runtimeRows.Count -ne 8 -or $healthy -ne 8 -or $running -ne 8 -or -not $migrationCompleted) -and (Get-Date) -lt $deadline)

    if ($runtimeRows.Count -ne 8 -or $healthy -ne 8 -or $running -ne 8 -or -not $migrationCompleted) {
        & docker @compose ps
        throw "Compose did not reach 8/8 runtime healthy with a successful database migration within five minutes"
    }

    $dockerArgs = @(
        'run', '--rm',
        '--add-host=host.docker.internal:host-gateway',
        '-v', "${frontend}:/work",
        '-v', '/work/node_modules',
        '-w', '/work'
    )
    foreach ($name in $required + @('SMOKE_BASE_URL')) {
        $dockerArgs += @('-e', $name)
    }
    $dockerArgs += @(
        'mcr.microsoft.com/playwright:v1.61.1-noble',
        'bash', '-lc',
        'npm ci && npm run test:e2e:acceptance'
    )
    & docker @dockerArgs
    if ($LASTEXITCODE -ne 0) { throw 'Playwright acceptance failed' }

    if (-not (Test-Path -LiteralPath $stateFile)) {
        throw 'Acceptance state file was not produced'
    }
    $conversationId = (Get-Content -Raw -LiteralPath $stateFile | ConvertFrom-Json).conversationId
    if ($conversationId -notmatch '^[0-9a-fA-F-]{36}$') {
        throw 'Acceptance produced an invalid conversation ID'
    }

    $sql = "SELECT CASE WHEN context_summary IS NOT NULL AND context_summary <> '' AND compacted_through_sequence > 0 AND context_tokens > 0 THEN 1 ELSE 0 END FROM conversations WHERE id = '$conversationId';"
    $dbDeadline = (Get-Date).AddMinutes(3)
    do {
        $compacted = (& docker @compose exec -T postgres psql -U $postgresUser -d $postgresDb -Atc $sql).Trim()
        if ($compacted -eq '1') { break }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $dbDeadline)
    if ($compacted -ne '1') {
        throw 'Conversation compaction fields were not persisted before timeout'
    }

    Write-Host 'ACCEPTANCE PASSED: migration completed, 8/8 runtime healthy, real providers, core API flows, deep links, and compaction persistence.'
}
finally {
    & docker @compose down -v --remove-orphans
}
