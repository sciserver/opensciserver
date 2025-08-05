import Head from 'next/head';
import { Layout } from 'components/common/layout';
import { JobsManagement } from 'components/content/jobs/jobsManagement';

export default function ComputePage() {
  return (
    <>
      <Head>
        <link rel="shortcut icon" href="/favicon.ico" />
      </Head>
      <Layout>
        <JobsManagement />
      </Layout>
    </>
  );
}
