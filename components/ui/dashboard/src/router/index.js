import Vue from 'vue';
import Router from 'vue-router';
import NotFound from '@/components/sharedComponents/NotFound';
import userProfileGetter from '@/user-profile-getter';
import Groups from '@/components/GroupsTab';
import Sciserver from '@/components/SciserverTab';
import ActivityLog from '@/components/UserActivityTab';
import Resource from '@/components/ResourcesTab';
import Files from '@/components/FilesTab';
import ComputeAdmin from '@/components/admin/ComputeAdmin';
import ScienceDomains from '@/components/ScienceTab';
import Notebook from '@/components/NotebookTab';
import NotebookDash from '@/components/notebook/NotebookDash';
import Editor from '@/components/Editor';
import ComputeApp from '@/components/compute/ComputeApp';
import redirectionToLoginPortal from '../redirectionToLoginPortal';


Vue.use(Router);

const router = new Router({
  mode: 'history',
  base: process.env.NODE_ENV !== 'production' ? '/' : DASHBOARD_CONTEXT_PATH,
  routes: [
    {
      path: '/',
      name: 'Dashboard',
      component: Sciserver,
      alias: ['/dashboard'],
    },
    {
      path: '/groups/:groupId?',
      props: true,
      component: Groups,
    },
    {
      path: '/resource',
      component: Resource,
    },
    {
      path: '/files/:rootVolume?/:userVolumeId?/:path*',
      name: 'Files Tab',
      component: Files,
    },
    {
      path: '/activity_log',
      component: ActivityLog,
    },
    {
      path: '/admin/compute',
      component: ComputeAdmin,
    },
    {
      path: '/notebook/:volType/:volId/:path+',
      component: Notebook,
    },
    {
      path: '/notebook',
      component: NotebookDash,
    },
    {
      path: '/editor',
      component: Editor,
    },
    {
      path: '/science/:scienceId?',
      component: ScienceDomains,
    },
      {
          path: '/apps',
          component: ComputeApp,
      },
    {
      path: '*',
      name: 'fallback',
      component: NotFound,
    },
  ],
});

router.beforeEach((to, from, next) => {
  if (
    // don't authenticate if logged in
    to.name === 'fallback' ||
    // or already logged in
    router.app.$store.state.token
  ) {
    next();
    return;
  }
  userProfileGetter().then(({ userProfile, token }) => {
    router.app.$store.commit('setUserProfile', userProfile);
    router.app.$store.commit('setToken', token);
    router.app.$store.dispatch('loadConfig');
    router.app.$store.dispatch('loadPublicGroupsAndUsers');
    router.app.$store.dispatch('loadCollaborations');
    router.app.$store.dispatch('loadJobs');
    router.app.$store.dispatch('loadAllUserVolumes').then(() =>
      router.app.$store.dispatch('loadAllQuotas'),
      );
    next();
  }).catch(() => {
    redirectionToLoginPortal();
  });
});

export default router;
