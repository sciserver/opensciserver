'use strict'
module.exports = {
    // Service and component URLs
    loginportal: '"{{ include "sciserver.url"  (dict "Values" .Values "svc" "login-portal") }}/"',
    racm: '"{{ include "sciserver.url" (dict "Values" .Values "svc" "racm") }}/"',
    courseware: '"{{ include "sciserver.url" (dict "Values" .Values "svc" "courseware/courseware") }}"',
    loggingAPI: '"{{ include "sciserver.url" (dict "Values" .Values "svc" "logging-api") }}"',
    casJobs: '"{{ include "sciserver.casjobs_url" . }}"',
    compute: '"{{ include "sciserver.url" (dict "Values" .Values "svc" "compute") }}"',
    sciquery: '"{{ include "sciserver.url" (dict "Values" .Values "svc" "sciquery-ui") }}"',
    {{ if .Values.dashboard.datasets -}}
    datasets: '"{{ include "sciserver.url"  (dict "Values" .Values "svc" "web") }}/datasets"',
    {{ else -}}
    datasets: '""',
    {{ end -}}
    scidrive: '"{{ .Values.optionalApps.sciDrive }}"',
    skyserver: '"{{ .Values.optionalApps.skyServer }}"',
    skyquery: '"{{ .Values.optionalApps.skyQuery }}"',
    // User support URLs
    userguides: '"{{ .Values.support.userguides }}"',
    apidocumentation: '"{{ .Values.support.apidocumentation }}"', 
    helpdesk: '"{{ .Values.support.helpdesk }}"',
    bugreportform: '"{{ .Values.support.bugreportform }}"',
    filemanagement: '"{{ .Values.support.filemanagement }}"',
    createuv: '"{{ .Values.support.createuv }}"',
    shareuv: '"{{ .Values.support.shareuv }}"',
    unshareuv: '"{{ .Values.support.unshareuv }}"',
    groupsmanagement: '"{{ .Values.support.groupsmanagement }}"',
    creategroup: '"{{ .Values.support.creategroup }}"',
    inviteuser: '"{{ .Values.support.inviteuser }}"',
    shareresource: '"{{ .Values.support.shareresource }}"',
    activitylog: '"{{ .Values.support.activitylog }}"',
    changePassword: '"{{ .Values.support.changePassword }}"',
    policies: '"{{ .Values.support.policies }}"',
    // must be an empty string to not show an alert
    alertMessage: '"{{ .Values.dashboard.alertMessage }}"',
    // one of the classes in http://getbootstrap.com/docs/3.3/components/#alerts
    alertType: '"danger"',
    sciserverVersion: '"{{ .Chart.AppVersion }}"',
    // context path (should begin and end in a slash)
    contextPath: '"/{{ include "sciserver.prefix" . }}dashboard/"',
    // Application theme and customization
    navbarColor: '"{{ .Values.theme.navbarColor }}"',
    fontFamily: '"{{ .Values.theme.fontFamily }}"',
    useIconsForActivities: '"{{ .Values.theme.useIconsForActivities }}"',
    showApplicationAppRow: '"{{ .Values.theme.showApplicationAppRow }}"',
    applicationName: '"{{ .Values.theme.applicationName }}"',
    applicationTagline: '"{{ .Values.theme.applicationTagline }}"',
    applicationHomeUrl: '"{{ .Values.theme.applicationHomeUrl }}"',
    displaySciserverLogin: '"{{ .Values.loginPortal.displaySciserverLogin }}"',
    // notebook-first config. See dashboard repo for more
    oneclickNotebookPath: '"{{ .Values.oneclickNotebookPath }}"',
    // dashboard "apps" config
    appTiles: {{ mustToJson .Values.dashboard.appTiles | squote }},
    addAppTiles: {{ mustToJson .Values.dashboard.addAppTiles | squote }},
    {{ if .Values.rendersvc.enable -}}
    nbconvUrl: '"/{{ include "sciserver.prefix" . }}render/notebook/convert"'
    {{ else -}}
    nbconvUrl: null
    {{ end -}}
}
