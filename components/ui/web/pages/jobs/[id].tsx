import Head from 'next/head';
import { Layout } from 'components/common/layout';
import { JobFullDetail } from 'components/content/jobs/detail/jobFullDetail';

export default function ComputePage() {
  return (
    <>
      <Head>
        <link rel="shortcut icon" href="/favicon.ico" />
      </Head>
      <Layout>
        <JobFullDetail />
      </Layout>
    </>
  );
}
