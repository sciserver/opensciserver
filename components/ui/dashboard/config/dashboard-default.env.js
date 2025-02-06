'use strict'
module.exports = {
  // Service and component URLs
  loginportal: '"https://scitest12.pha.jhu.edu/login-portal/"',
  racm: '"https://scitest12.pha.jhu.edu/racm/"',
  courseware: '"https://scitest12.pha.jhu.edu/courseware/courseware"',
  loggingAPI: '"https://scitest12.pha.jhu.edu/sciserver-logging-api"',
  casJobs: '"https://skyserver.sdss.org/CasJobs/login.aspx"',
  compute: '"https://scitest12.pha.jhu.edu/compute/"',
  sciquery: '"https://scitest12.pha.jhu.edu/sciquery-ui"',
  scidrive: '"https://www.scidrive.org/scidrive/scidrive.html"',
  skyserver: '"https://skyserver.sdss.org/dr14/en/home.aspx"',
  skyquery: '"https://www.voservices.net/skyquery/"',
  datasets: '"https://scitest12.pha.jhu.edu/web/datasets"',
  // User support URLs
  userguides: '"http://www.sciserver.org/support/help/"',
  apidocumentation: '"http://www.sciserver.org/support/documentation/"',
  helpdesk: '"mailto:sciserver-helpdesk@jhu.edu"',
  bugreportform: '"http://www.sciserver.org/support/bug-report-and-suggestion-form/"',
  filemanagement: '"https://www.sciserver.org/support/how-to-use-sciserver/#filemanagement"',
  createuv: '"https://www.sciserver.org/support/how-to-use-sciserver/#createvolume"',
  shareuv: '"https://www.sciserver.org/support/how-to-use-sciserver/#sharevolume"',
  unshareuv: '"https://www.sciserver.org/support/how-to-use-sciserver/#unshare"',
  groupsmanagement: '"https://www.sciserver.org/support/how-to-use-sciserver/#groups"',
  creategroup: '"https://www.sciserver.org/support/how-to-use-sciserver/#groupscreate"',
  inviteuser: '"https://www.sciserver.org/support/how-to-use-sciserver/#groupsinvite"',
  shareresource: '"https://www.sciserver.org/support/how-to-use-sciserver/#shareview"',
  activitylog: '"https://www.sciserver.org/support/how-to-use-sciserver/#recent"',
  changePassword: '"https://www.sciserver.org/support/how-to-use-sciserver/#change"',
  // must be an empty string to not show an alert
  alertMessage: '""',
  // one of the classes in http://getbootstrap.com/docs/3.3/components/#alerts
  alertType: '"danger"',
  sciserverVersion: '"2.1.0"',
  // context path (should begin and end in a slash)
  contextPath: '"/dashboard/"',
  // Application theme and customization
  navbarColor: '"#003466"', // Change the value here, can be either word or hexa
  fontFamily: '""',
  useIconsForActivities: true,
  showApplicationAppRow: true,
  applicationName: '"SciServer"',
  applicationTagline: '"Data, Collaboration, Compute"',
  applicationHomeUrl: '"https://www.sciserver.org/"',
  displaySciserverLogin: true,
  // the path for the default oneclick notebook. Format: {volumetype}/{volid}/{path} where volumetype is datavolumes
  // or uservolumes and path is that under the volume (as seen in fileservice) and omits .ipynb (extension required on
  // file)
  oneclickNotebookPath: '"datavolumes/3/Template"',
  nbconvUrl: null,
}
