import { mapState } from 'vuex';
import querystring from 'querystring';

const casjobsImgLink = require('./assets/casjobs.png');
const computeImgLink = require('./assets/sciserver_compute.png');
const jobsImgLink = require('./assets/sciserver_compute_jobs.png');
const sciDriveImgLink = require('./assets/scidrive.png');
const skyQueryImgLink = require('./assets/skyquery.png');
const skyServerImgLink = require('./assets/skyserver.png');

const _ = require('lodash');
const urljoin = require('url-join');

export default {
  name: 'applicationConfiguration',
  components: {},
  computed: {
    casJobs_URL() {
      return urljoin(LOGIN_PORTAL_URL,
        `Account/Login?${querystring.stringify(
          {
            callbackUrl: urljoin(CASJOBS_URL, 'login.aspx?nexturl=SubmitJob.aspx'),
          },
        )}`);
    },
    datasets_URL() {
      // eslint-disable-next-line no-undef
      return DATASETS_URL;
    },
    skyquery_URL() {
      return SKYQUERY_URL;
    },
    skyserver_URL() {
      return SKYSERVER_URL;
    },
    scidrive_URL() {
      return SCIDRIVE_URL;
    },
    sciquery_URL() {
      return SCIQUERY_URL;
    },
    compute_URL() {
      return urljoin(COMPUTE_URL,
        `login?${querystring.stringify({ callbackUrl: COMPUTE_URL })}`);
    },
    computeJobs_URL() {
      return urljoin(COMPUTE_URL,
        `login?${querystring.stringify({ callbackUrl: urljoin(COMPUTE_URL, 'jobs') })}`);
    },
    appTiles() {
      if (_.size(APPTILES) > 0) {
        return APPTILES;
      }
      const tiles = [];
      if (CASJOBS_URL) {
        tiles.push(
          {
            name: 'CasJobs',
            description: 'Search online big relational databases collection, store the results online, and share them.',
            serviceUrl: this.casJobs_URL,
            staticIcon: 'casjobs',
          });
      }
      if (COMPUTE_URL) {
        tiles.push(
          {
            name: 'Compute',
            description: 'Analyze data with interactive Jupyter notebooks in Python, R and MATLAB.',
            serviceUrl: this.compute_URL,
            staticIcon: 'compute',
          });
        tiles.push(
          {
            name: 'Compute Jobs',
            description: 'Asychronously run Jupyter notebooks in Python, R and MATLAB or commands.',
            serviceUrl: this.computeJobs_URL,
            staticIcon: 'jobs',
          });
      }
      if (SCIDRIVE_URL) {
        tiles.push(
          {
            name: 'SciDrive',
            description: 'Drag-and-drop file hosting and sharing services.',
            serviceUrl: this.scidrive_URL,
            staticIcon: 'sciDrive',
          });
      }
      if (SKYSERVER_URL) {
        tiles.push(
          {
            name: 'SkyServer',
            description: 'Access the Sloan Digital Sky Survey data, tutorials and educational materials.',
            serviceUrl: this.skyserver_URL,
            staticIcon: 'skyServer',
          });
      }
      if (SKYQUERY_URL) {
        tiles.push(
          {
            name: 'SkyQuery',
            description: 'A scalable database system for cross-matching astronomical source catalogs.',
            serviceUrl: this.skyquery_URL,
            staticIcon: 'skyQuery',
          });
      }
      return tiles.concat(ADDAPPTILES || []);
    },
    ...mapState(['token']),
  },
  data: () => ({
    alertClasses: `alert alert-${ALERT_TYPE}`,
    alertMessage: `${ALERT_MESSAGE}`,
    version: `${SCISERVER_VERSION}`,
    sciserver_Version: `${SCISERVER_VERSION}`,
    useIconsForActivities: `${USE_ICONS_FOR_ACTIVITIES}` === 'true',
    showApplicationAppRow: `${SHOW_APPLICATION_APP_ROW}` === 'true',
    applicationName: `${APPLICATION_NAME}`,
    applicationTagline: `${APPLICATION_TAGLINE}`,
    applicationHomeUrl: `${APPLICATION_HOME_URL}`,
    displaySciserverLogin: `${DISPLAY_SCISERVER_LOGIN}` === 'true',
    oneclickNotebookPath: ONECLICK_NOTEBOOK_PATH,
    staticImages: {
      casjobs: casjobsImgLink,
      compute: computeImgLink,
      jobs: jobsImgLink,
      sciDrive: sciDriveImgLink,
      skyServer: skyServerImgLink,
      skyQuery: skyQueryImgLink,
    },
  }),
  methods: {
    assignCSS() {
      if (`${NAVBAR_COLOR}`) {
        document.querySelector(':root').style.setProperty('--navColor', `${NAVBAR_COLOR}`);
      }
      if (`${FONT_FAMILY}`) {
        document.querySelector('body').style.setProperty('font-family', `${FONT_FAMILY}`);
      }
      if (!this.showApplicationAppRow) {
        const thumbnails = document.querySelectorAll('div.thumbnail.dashboard');
        thumbnails.forEach((thumbnail) => {
          thumbnail.querySelector('h4').style.setProperty('font-weight', 'bold');
          thumbnail.querySelector('h4').style.setProperty('font-size', '20px');
          thumbnail.querySelector('p:first-of-type').style.setProperty('font-weight', 'bold');
          return thumbnail;
        });
      }
    },
  },
};
