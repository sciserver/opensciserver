import Head from 'next/head';
import { Layout } from 'components/common/layout';
import { NewSession } from 'components/content/compute/sessionManagement/newSession';

export default function ComputePage() {
  return (
    <>
      <Head>
        <link rel="shortcut icon" href="/favicon.ico" />
        <title>New Compute Session - SciServer</title>
      </Head>
      <Layout>
        <NewSession />
      </Layout>
    </>
  );
}
