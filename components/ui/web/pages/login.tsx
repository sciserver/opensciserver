import { Login } from 'components/content/login/login';
import Head from 'next/head';

export default function LoginPage() {
  return <>
    <Head>
      <link rel="shortcut icon" href="/favicon.ico" />
      <title>Login - SciServer</title>
    </Head>
    <Login />
  </>;
}
