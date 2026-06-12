# Coding Conventions

## General

- **Language**: Code comments and docs in English; user-facing text in en/zh-Hans/zh-Hant
- **Git**: Conventional Commits (`feat(scope):`, `fix(scope):`, etc.)
- **Branching**: Trunk-Based — `main` is development, `feature/*`, `fix/*`, `release/v*`

## Backend (Java)

### Package Naming
- Base: `io.jobcopilot.resumeassistant`
- Domain: `...domain.{bounded_context}.{entity|repository|service}`
- Infrastructure: `...infrastructure.{persistence|messaging|storage|ai}.{adapter|config}`
- API: `...api.{bounded_context}.{dto|command|query}`
- Trigger: `...trigger.{rest|websocket|listener}`

### Key Rules
- Domain layer: ZERO Spring/Framework imports. Only Java stdlib + types module.
- Domain entities use plain Java (no `@Entity`). JPA entities in infrastructure.
- App layer owns `@Transactional` boundaries.
- Use Lombok (`@RequiredArgsConstructor`, `@Slf4j`, `@Getter`) — but NOT `@Data`.
- Use MapStruct for DTO ↔ Entity mapping.
- Port interfaces in domain end with `*Port` or `*Repository`.
- Adapter implementations in infrastructure end with `*Adapter` or `*JpaRepository`.

### Testing
- Domain tests: Pure JUnit 5 + AssertJ, no Spring context.
- App tests: Mock ports with Mockito.
- Infrastructure tests: `@DataJpaTest`, `@SpringBootTest` with Testcontainers where needed.
- Architecture tests: ArchUnit in `app/src/test`.

### Anti-Patterns (NEVER DO)
- `rabbitTemplate.convertAndSend()` inside `@Transactional`
- Importing `org.springframework.*` in domain module
- Direct `@Autowired` of infrastructure beans in app layer (use Port interfaces)

## Frontend (TypeScript/React)

### File Structure
- Pages: `pages/{feature}/` — one file per route
- Components: `components/{feature}/` — shared/reusable
- Services: `services/{feature}Service.ts` — API wrappers
- Store: `store/{feature}.store.ts` — Zustand stores
- Hooks: `hooks/use{Name}.ts` — custom hooks

### Key Rules
- Use TypeScript strict mode.
- Zustand over Redux for state.
- Services are plain exported functions, not classes.
- API calls go through `apiClient` from `services/api.ts` (has interceptors).
- Use `createAbortableRequest()` for requests that may be superseded.
- shadcn/ui components imported from `@/components/ui/`.
- Tailwind for all styling; avoid inline styles.

### Testing
- Test files co-located: `Component.test.tsx` next to `Component.tsx`.
- Use Vitest + Testing Library.
- Mock API calls at the service level, not Axios level.

## AI Service (Python)

### Key Rules
- All config from environment variables, loaded in `config.py`.
- LiteLLM is the ONLY way to call LLMs.
- Embedding dimensions MUST match between backend and AI service.
- Validate INTERNAL_API_KEY in non-dev environments.
- Use structlog for structured logging.

### Testing
- Pytest with `pytest-asyncio` for async tests.
- Mock external calls (LiteLLM, backend client, Redis).
- Use Ruff for linting (no flake8/black).

## Infrastructure (Docker)

- `.env` is gitignored; `.env.example` is committed and documented.
- All secrets in `.env`, never in `docker-compose.yml`.
- Only frontend port exposed to host in production.
- Health checks on all services with proper `depends_on` conditions.
- Named volumes for all persistent data.

## Code Review Standards

- Architecture compliance: Check domain layer purity, port/adapter patterns.
- Security: No hardcoded secrets, proper auth checks, input validation.
- Testing: New features must include tests covering happy path + edge cases.
- Coverage: Backend ≥60% line coverage (enforced by JaCoCo).
