import Head from 'next/head';
import { Layout } from 'components/common/layout';
import { JobsList } from 'components/content/jobs/list/jobsList';

export default function ComputePage() {
  return (
    <>
      <Head>
        <link rel="shortcut icon" href="/favicon.ico" />
        <title>Jobs - SciServer</title>
      </Head>
      <Layout>
        <JobsList />
      </Layout>
    </>
  );
}
