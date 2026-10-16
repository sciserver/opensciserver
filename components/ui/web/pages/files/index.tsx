import Head from 'next/head';
import { Layout } from 'components/common/layout';
import { FilesPage } from 'components/content/files/filesPage';

export default function FilesRoute() {
  return (
    <>
      <Head>
        <link rel="shortcut icon" href="/favicon.ico" />
        <title>Files - SciServer</title>
      </Head>
      <Layout>
        <FilesPage />
      </Layout>
    </>
  );
}
