import { FC, useEffect, useState } from 'react';
import { useMutation } from '@apollo/client';
import { useRouter } from 'next/router';
import Image from 'next/image';
import Link from 'next/link';
import styled from 'styled-components';
import { Box, Button, CircularProgress, TextField } from '@mui/material';

import { LOGIN } from 'src/graphql/accounts';
import { AuthService } from 'src/services/AuthService';

import dataImg from 'public/dataImages/milleniumG4.png';

type StyleProps = {
  height: number
}

const Styled = styled.div`
  display: grid;
  grid-template-columns: 50% 50%;
  grid-template-rows: 100%;
  height:  ${(props: StyleProps) => props.height}px; 

  .error {
    color: ${({ theme }) => theme.palette.error.main};
  }
  
  .login-form {
    justify-self: center;
    align-self: center;

    .form { 
      display: flex;
      flex-direction: column;
      gap: 1.5em;
    }

    .caption {
      align-self: center;
    }

    a {
      font-weight: bold;
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
      <h1>Welcome to Sciserver</h1>
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
            'LOGIN'
          }
        </Button>
        <span className="caption">New to Sciserver? <Link href={process.env.NEXT_PUBLIC_LOGIN_PORTAL_URL || ''}>Create an account</Link></span>
      </Box>
    </div>
    <div>
      <Image src={dataImg} alt="Sciserver related images" height={height} width={width / 2} />
    </div>
  </Styled>;
};