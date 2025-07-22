// eslint-disable-next-line import/no-extraneous-dependencies
import 'dotenv/config';

let apolloIntrospection = true;
let apolloPlayground = true;
const apolloPort = 4001;
const apolloCors = [
  'https://kubetest.sciserver.org',
  'http://localhost:3000',
  'http://localhost:4001'
];;

if (process.env.IDIES_ENV === 'prod') {
  apolloIntrospection = false;
  apolloPlayground = false;
}

interface Environment {
  apollo: {
    introspection: boolean;
    playground: boolean;
    port: number | string;
    cors: string[];
  };
  files: {
    baseUrl: string;
  };
  compute: {
    baseUrl: string;
  };
  racm: {
    baseUrl: string;
    jobsUrl: string;
    userUrl: string;
  };
  loginPortal: {
    baseUrl: string;
  }
}

export const environment: Environment = {
  apollo: {
    introspection: apolloIntrospection,
    playground: apolloPlayground,
    port: apolloPort,
    cors: apolloCors
  },
  files: { baseUrl: process.env.FILES_BASE_URL || '' },
  compute: { baseUrl: process.env.COMPUTE_BASE_URL || '' },
  racm: {
    baseUrl: process.env.RACM_BASE_URL || '',
    jobsUrl: process.env.RACM_BASE_URL ? `${process.env.RACM_BASE_URL}jobm/rest/` : '',
    userUrl: process.env.RACM_BASE_URL ? `${process.env.RACM_BASE_URL}ugm/rest/` : ''
  },
  loginPortal: { baseUrl: process.env.LOGIN_PORTAL_BASE_URL || '' }
};
