import Head from 'next/head';
import { Layout } from 'components/common/layout';
import { NewSession } from 'components/content/compute/sessionManagement/newSession';

export default function ComputePage() {
  return (
    <>
      <Head>
        <link rel="shortcut icon" href="/favicon.ico" />
      </Head>
      <Layout>
        <NewSession />
      </Layout>
    </>
  );
}
