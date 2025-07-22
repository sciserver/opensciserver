/* eslint-disable eslint-comments/disable-enable-pair */
/* eslint-disable @next/next/no-page-custom-font */
/* eslint-disable @next/next/google-font-display */
import type { AppProps } from 'next/app';
import Script from 'next/script';
import Head from 'next/head';

// Graphql
import { ApolloProvider } from '@apollo/client';
import client from 'apollo-client';

// Styles
import { createTheme, ThemeProvider } from '@mui/material';
import { createGlobalStyle, ThemeProvider as StyledThemeProvider } from 'styled-components';

import themeJSON from 'theme.json';
import 'styles/globals.css';

// Wrappers
import { ContextWrapper } from 'components/wrappers/Context';

export default function App({ Component, pageProps }: AppProps) {

  const GlobalStyle = createGlobalStyle`
  html,
  body {
    padding: 0;
    margin: 0;
    font-family: -apple-system, Roboto, Oxygen,
      Ubuntu, Cantarell, Fira Sans, Droid Sans, Helvetica Neue, sans-serif;
  }

  h1, h2, h3, h4, h5 {
    font-family: "Roboto Slab", serif;
    font-style: normal;
    font-optical-sizing: auto;
  }
    
  h1, h2, h3 {
    font-weight: 600;    
  }

  h4, h5 {
    font-weight: 400;    
  }

  a {
    color: #A9327A;
    text-decoration: none;
  }

  .caption {
    font-family: "Noto Sans", sans-serif;
    font-style: normal;
    font-size: 12px;
  }

  * {
    box-sizing: border-box;
  }

  main {
    margin-top: 20px;
  }
`;

  const theme = createTheme({
    typography: {
      h5: { fontFamily: '\"Roboto Slab\", \"serif\"' },
      h4: { fontFamily: '\"Roboto Slab\", \"serif\"' },
      h3: { fontFamily: '\"Roboto Slab\", \"serif\"' }
    },
    ...themeJSON,
    // disable performance-sucking transitions
    transitions: { create: () => 'none' }
  });

  return (
    <>
      <Head>
        <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" />
      </Head>
      <Script
        id="G-TAG"
        dangerouslySetInnerHTML={{
          __html: `
              window.dataLayer = window.dataLayer || [];
              function gtag(){dataLayer.push(arguments);}
              gtag('js', new Date());

              gtag('config', '${process.env.NEXT_PUBLIC_G_TAG}', {
                page_path: window.location.pathname,
              });
            `
        }}
      />
      <Script
        strategy="afterInteractive"
        src={`https://www.googletagmanager.com/gtag/js?id=${process.env.NEXT_PUBLIC_G_TAG}`}
      />
      <GlobalStyle />
      <ApolloProvider client={client}>
        <ContextWrapper>
          <ThemeProvider theme={theme}>
            <StyledThemeProvider theme={theme}>
              <Component {...pageProps} />
            </StyledThemeProvider>
          </ThemeProvider>
        </ContextWrapper>
      </ApolloProvider>
    </>
  );
}
