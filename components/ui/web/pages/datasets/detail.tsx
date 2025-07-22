import Head from 'next/head';
import { Layout } from 'components/common/layout';
import { DatasetDetail } from 'components/content/datasets/datasetDetail';

export default function DatasetDetailPage() {
  return (
    <>
      <Head>
        <link rel="shortcut icon" href="/favicon.ico" />
      </Head>
      <Layout>
        <DatasetDetail />
      </Layout>
    </>
  );
}
