import { FC, useEffect, useState } from 'react';
import { useMutation } from '@apollo/client';
import { useRouter } from 'next/router';
import Image from 'next/image';
import Link from 'next/link';
import styled from 'styled-components';
import { Box, Button, CircularProgress, TextField } from '@mui/material';

import { LOGIN } from 'src/graphql/accounts';
import { AuthService } from 'src/services/AuthService';

import { ParticlesComp } from 'components/common/particles';

import logo from 'public/sciserver-logo.png';

type StyleProps = {
  height: number
}

const Styled = styled.div`
  height: ${(props: StyleProps) => props.height}px;
  background-size: cover;
  background-position: center;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 1em;

  .error {
    color: ${({ theme }) => theme.palette.error.main};
  }
  
  .login-form {
    width: 100%;
    z-index: 1;
    max-width: 30rem;
    height: auto;
    background-color: white;
    border-radius: 6px;
    padding: 2em;
    display: flex;
    flex-direction: column;
    justify-content: center;

    .caption {
      align-self: center;
      font-size: 0.9rem;
    }

    .logo {
      align-self: center;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.5em;
      margin-bottom: 1em;
    }
    .form { 
      display: flex;
      flex-direction: column;
      gap: 1.5em;
    }

    a {
      font-weight: bold;
    }
  }

  @media (max-width: 600px) {
    .login-form {
      padding: 1.5em;
    }

    .login-form h1 {
      font-size: 1.5rem;
    }
  }
`;

export const Login: FC = () => {

  const router = useRouter();

  const [callback, setCallback] = useState<string>('');

  const [height, setHeight] = useState<number>(0);
  const [width, setWidth] = useState<number>(0);
  const [username, setUsername] = useState<string>('');
  const [password, setPassword] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);

  const [login, { data, error }] = useMutation(LOGIN, { onError: () => setLoading(false) });


  const handleWindowResize = () => {
    setHeight(window.innerHeight);
    setWidth(window.innerWidth);
  };

  // ON MOUNT: UI config
  useEffect(() => {
    window.addEventListener('resize', handleWindowResize);
    setHeight(window.innerHeight);
    setWidth(window.innerWidth);
  }, []);

  useEffect(() => {
    if (!router.isReady) {
      return;
    }

    const query = router.query;

    const { callbackURL } = query;

    if (callbackURL) {
      setCallback(decodeURIComponent((callbackURL as string).replace(/\+/g, ' ')));
    }

  }, [router]);

  const submit = async (e: { preventDefault: () => void; }) => {
    e.preventDefault();
    if (username && password) {
      setLoading(true);
      login({ variables: { username, password } });
    }
  };

  useEffect(() => {

    const handleLogin = async () => {
      await AuthService.login(data.login);

      if (callback.length) {
        router.push(callback);
        return;
      }

      // If no callback URL is found, redirect to configurable landing page
      router.push(process.env.NEXT_PUBLIC_LANDING_ROUTE || '/');
    };

    if (data) {
      handleLogin();
    }
  }, [data]);

  return <Styled {...{ height }}>
    <div className="login-form">
      <div className="logo">
        <Image src={logo} alt="SciServer logo" />
        <span className="caption">Data, Compute, Collaboration</span>
      </div>
      <h1>Sign in</h1>
      <Box
        className="form"
        component="form"
      >
        <TextField
          required
          id="username"
          label="Username"
          color="secondary"
          autoComplete="current-username"
          value={username}
          onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
            setUsername(event.target.value);
          }}
        />
        <TextField
          required
          id="password"
          label="Password"
          color="secondary"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
            setPassword(event.target.value);
          }}
        />
        {error &&
          <span className="caption error">
            {error.message}
          </span>
        }
        <Button type="submit" onClick={submit} variant="contained">
          {loading ?
            <CircularProgress color="secondary" />
            :
            'LOG IN'
          }
        </Button>
        <span className="caption">New to Sciserver? <Link href={process.env.NEXT_PUBLIC_LOGIN_PORTAL_URL || ''}>Create an account</Link></span>
      </Box>
    </div>
    <ParticlesComp />
  </Styled>;
};
