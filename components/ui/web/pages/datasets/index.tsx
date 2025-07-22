import Head from 'next/head';
import { Layout } from 'components/common/layout';
import { DatasetList } from 'components/content/datasets/datasetList';

export default function Datasets() {
  return (
    <>
      <Head>
        <link rel="shortcut icon" href="/favicon.ico" />
      </Head>
      <Layout>
        <DatasetList />
      </Layout>
    </>
  );
}
