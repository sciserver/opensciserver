{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "sciserver.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "sciserver.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "sciserver.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create the URL that this service will be accessible from.
Expects a dict with svc and Value keys
*/}}
{{- define "sciserver.url" -}}
{{ include "sciserver.domainurl" . }}/{{ include "sciserver.prefix" . }}{{ .svc }}
{{- end -}}

{{/*
URL path prefix to be used for this installation. See documentation on
prefix in values.yaml.
*/}}
{{- define "sciserver.prefix" -}}
{{- if .Values.prefix -}}
{{- printf "%s/" .Values.prefix -}}
{{- end -}}
{{- end -}}

{{/*
Tomcat will formulate a context path from names by replacing single #
characters with a / for hosted files. This function creates filenames
based on the provided prefix.
*/}}
{{- define "sciserver.tomcatprefix" -}}
{{- if .Values.prefix -}}
{{- printf "%s#" .Values.prefix -}}
{{- end -}}
{{- end -}}

{{/*
Create the url that this instance of sciserver will be accessible from
*/}}
{{- define "sciserver.domainurl" -}}
{{- if not .Values.baseDomain -}}
{{- fail "Need baseDomain for installation!" -}}
{{- end -}}
{{- if .Values.port -}}
{{- printf "%s://%s:%d" .Values.protocol .Values.baseDomain .Values.port -}}
{{- else -}}
{{- printf "%s://%s" .Values.protocol .Values.baseDomain -}}
{{- end -}}
{{- end -}}

{{- define "sciserver.casjobs_url" -}}
{{- if .Values.casjobs.enable -}}
{{- if .Values.casjobs.url -}}
{{- .Values.casjobs.url -}}
{{- else -}}
{{- include "sciserver.url" (dict "Values" .Values "svc" "CasJobs") -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{- define "sciserver.casjobs_ui_url" -}}
{{- if .Values.casjobs.enable -}}
{{- include "sciserver.casjobs_url" . }}/login.aspx
{{- end -}}
{{- end -}}

{{- define "sciserver.rabbitmq_host" -}}
{{- if .Values.logging.rabbitmq.host -}}
{{- .Values.logging.rabbitmq.host -}}
{{ else -}}
{{- include "sciserver.fullname" . }}-logging-rabbitmq
{{- end -}}
{{- end -}}
