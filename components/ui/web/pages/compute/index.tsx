import Head from 'next/head';
import { Layout } from 'components/common/layout';
import { SessionManagement } from 'components/content/compute/sessionManagement/sessionManagement';

export default function ComputePage() {
  return (
    <>
      <Head>
        <link rel="shortcut icon" href="/favicon.ico" />
      </Head>
      <Layout>
        <SessionManagement />
      </Layout>
    </>
  );
}
