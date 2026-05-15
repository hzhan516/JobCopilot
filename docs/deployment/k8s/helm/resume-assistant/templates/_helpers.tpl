{{/*
Resume Assistant — Common Helm helper templates
智能求职助手 — 通用 Helm 辅助模板
*/}}

{{/*
Expand the name of the chart.
*/}}
{{- define "resume-assistant.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "resume-assistant.fullname" -}}
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
{{- define "resume-assistant.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "resume-assistant.labels" -}}
helm.sh/chart: {{ include "resume-assistant.chart" . }}
{{ include "resume-assistant.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "resume-assistant.selectorLabels" -}}
app.kubernetes.io/name: {{ include "resume-assistant.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "resume-assistant.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "resume-assistant.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Secret name reference
*/}}
{{- define "resume-assistant.secretName" -}}
{{- if .Values.secrets.existingSecret }}
{{- .Values.secrets.existingSecret }}
{{- else }}
{{- include "resume-assistant.fullname" . }}-secrets
{{- end }}
{{- end }}

{{/*
Namespace helper
*/}}
{{- define "resume-assistant.namespace" -}}
{{- .Values.namespace.name | default .Release.Namespace }}
{{- end }}

{{/*
PostgreSQL labels
*/}}
{{- define "resume-assistant.postgres.labels" -}}
{{ include "resume-assistant.labels" . }}
app.kubernetes.io/component: postgres
{{- end }}

{{/*
RabbitMQ labels
*/}}
{{- define "resume-assistant.rabbitmq.labels" -}}
{{ include "resume-assistant.labels" . }}
app.kubernetes.io/component: rabbitmq
{{- end }}

{{/*
Redis labels
*/}}
{{- define "resume-assistant.redis.labels" -}}
{{ include "resume-assistant.labels" . }}
app.kubernetes.io/component: redis
{{- end }}

{{/*
Backend labels
*/}}
{{- define "resume-assistant.backend.labels" -}}
{{ include "resume-assistant.labels" . }}
app.kubernetes.io/component: backend
{{- end }}

{{/*
AI Service labels
*/}}
{{- define "resume-assistant.ai-service.labels" -}}
{{ include "resume-assistant.labels" . }}
app.kubernetes.io/component: ai-service
{{- end }}

{{/*
Frontend labels
*/}}
{{- define "resume-assistant.frontend.labels" -}}
{{ include "resume-assistant.labels" . }}
app.kubernetes.io/component: frontend
{{- end }}
