import Head from 'next/head';
import { Layout } from 'components/common/layout';
import { ContainerRun } from 'components/content/compute/containerRun';

export default function ComputePage() {
  return (
    <>
      <Head>
        <link rel="shortcut icon" href="/favicon.ico" />
      </Head>
      <Layout>
        <ContainerRun />
      </Layout>
    </>
  );
}
