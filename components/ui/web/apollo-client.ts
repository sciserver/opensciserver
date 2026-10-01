/* eslint-disable object-curly-newline */
import { ApolloClient, createHttpLink, InMemoryCache } from '@apollo/client';
import { setContext } from '@apollo/client/link/context';
import Cookies from 'js-cookie';

import { ACCESS_TOKEN_KEY } from './src/utils/keys';


const httpLink = createHttpLink({ uri: process.env.NEXT_PUBLIC_GRAPHQL_URL });

const authLink = setContext((_, { headers }) => {
  const token = Cookies.get(ACCESS_TOKEN_KEY);
  return {
    headers: {
      ...headers,
      authorization: token || ''
    }
  };
});


const client = new ApolloClient({
  // eslint-disable-next-line unicorn/prefer-spread
  link: authLink.concat(httpLink),
  cache: new InMemoryCache(
    {
      typePolicies: {
        Dataset: { keyFields: ['name', 'source'] },
        Container: { keyFields: ['id'] },
        // User and group ids can be equal, so these are never cached by id alone.
        SharedWith: { keyFields: false },
        SharePrincipal: { keyFields: ['type', 'id'] },
        UserVolume: { fields: { sharedWith: { merge: false } } }
      }
    }
  ),
  connectToDevTools: true
});

export default client;