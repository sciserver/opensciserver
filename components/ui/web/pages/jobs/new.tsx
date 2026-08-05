import Head from 'next/head';
import { Layout } from 'components/common/layout';
import { NewJob } from 'components/content/jobs/new/newJob';

export default function ComputePage() {
  return (
    <>
      <Head>
        <link rel="shortcut icon" href="/favicon.ico" />
        <title>New Job - SciServer</title>
      </Head>
      <Layout>
        <NewJob />
      </Layout>
    </>
  );
}
