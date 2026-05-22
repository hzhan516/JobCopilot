{{/*
Resume Assistant — Common Helm helper templates
智能求职助手 — 通用 Helm 辅助模板
*/}}

{{/*
Expand the name of the chart.
*/}}
{{- define "jobcopilot.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "jobcopilot.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "jobcopilot.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "jobcopilot.labels" -}}
helm.sh/chart: {{ include "jobcopilot.chart" . }}
{{ include "jobcopilot.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "jobcopilot.selectorLabels" -}}
app.kubernetes.io/name: {{ include "jobcopilot.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "jobcopilot.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "jobcopilot.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Secret name reference
*/}}
{{- define "jobcopilot.secretName" -}}
{{- if .Values.secrets.existingSecret }}
{{- .Values.secrets.existingSecret }}
{{- else }}
{{- include "jobcopilot.fullname" . }}-secrets
{{- end }}
{{- end }}

{{/*
Namespace helper
*/}}
{{- define "jobcopilot.namespace" -}}
{{- .Values.namespace.name | default .Release.Namespace }}
{{- end }}

{{/*
PostgreSQL labels
*/}}
{{- define "jobcopilot.postgres.labels" -}}
{{ include "jobcopilot.labels" . }}
app.kubernetes.io/component: postgres
{{- end }}

{{/*
RabbitMQ labels
*/}}
{{- define "jobcopilot.rabbitmq.labels" -}}
{{ include "jobcopilot.labels" . }}
app.kubernetes.io/component: rabbitmq
{{- end }}

{{/*
Redis labels
*/}}
{{- define "jobcopilot.redis.labels" -}}
{{ include "jobcopilot.labels" . }}
app.kubernetes.io/component: redis
{{- end }}

{{/*
Backend labels
*/}}
{{- define "jobcopilot.backend.labels" -}}
{{ include "jobcopilot.labels" . }}
app.kubernetes.io/component: backend
{{- end }}

{{/*
AI Service labels
*/}}
{{- define "jobcopilot.ai-service.labels" -}}
{{ include "jobcopilot.labels" . }}
app.kubernetes.io/component: ai-service
{{- end }}

{{/*
Frontend labels
*/}}
{{- define "jobcopilot.frontend.labels" -}}
{{ include "jobcopilot.labels" . }}
app.kubernetes.io/component: frontend
{{- end }}
