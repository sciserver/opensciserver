// eslint-disable-next-line eslint-comments/disable-enable-pair
/* eslint-disable import/no-extraneous-dependencies */
// eslint-disable-next-line eslint-comments/disable-enable-pair
/* eslint-disable import/no-cycle */
import { ApolloServer } from '@apollo/server';
import { expressMiddleware } from '@apollo/server/express4';
import { ApolloServerPluginLandingPageLocalDefault } from '@apollo/server/plugin/landingPage/default';
import express from 'express';
import http from 'http';
import cors from 'cors';
import { json } from 'body-parser';

import { typeDefs } from './types/root';
import { resolvers } from './resolvers/root';
import { ContainersAPI } from './data/containers';
import { DomainsAPI } from './data/domains';
import { DatasetsAPI } from './data/datasets';
import { AccountsAPI } from './data/accounts';
import { VolumesAPI } from './data/volumes';
import { JobsAPI } from './data/jobs';

// We need to build a context type so the generated
// typings contains the correct context for resolvers,
// otherwise it defaults to any(not good)

export type Context = {
  token?: string
  dataSources: {
    accountsAPI: AccountsAPI
    containersAPI: ContainersAPI
    datasetsAPI: DatasetsAPI
    domainsAPI: DomainsAPI
    jobsAPI: JobsAPI
    volumesAPI: VolumesAPI
  };
};

const app = express();
const httpServer = http.createServer(app);

const server = new ApolloServer<Context>({
  typeDefs,
  resolvers,
  plugins: process.env.NODE_ENV !== 'production' ?
    [
      // eslint-disable-next-line new-cap
      ApolloServerPluginLandingPageLocalDefault({ footer: false })
    ]
    : [],
  formatError: (err: any) => {
    // Don't give the specific errors to the client.
    // eslint-disable-next-line no-console
    console.log(err);

    if (err.message.startsWith('Database Error: ')) {

      return new Error('Internal server error');
    }

    // Otherwise return the original error. The error can also
    // be manipulated in other ways, as long as it's returned.
    return err;
  }
});

server.start().then(() => {
  app.use(
    '',
    json(),
    cors<cors.CorsRequest>(),
    expressMiddleware(server, {
      context: async ({ req }: { req: any }) => {
        const token = req.headers.authorization || '';
        const { cache } = server;
        const volumesAPI = new VolumesAPI({ cache, token });
        const accountsAPI = new AccountsAPI({ cache, token });
        const jobsAPI = new JobsAPI({ cache, token, volumesAPI });
        const domainsAPI = new DomainsAPI({ cache, token, volumesAPI });
        const containersAPI = new ContainersAPI({ cache, token, volumesAPI, domainsAPI, accountsAPI });

        return {
          token,
          dataSources: {
            accountsAPI,
            containersAPI,
            datasetsAPI: new DatasetsAPI({ cache, token }),
            domainsAPI,
            jobsAPI,
            volumesAPI
          }
        };
      }
    })
  );
}).catch(() => { });

new Promise<void>((resolve) => httpServer.listen({ port: 4000 }, resolve)).then(() => {
  // eslint-disable-next-line no-console
  console.log(`🚀 Server ready at http://localhost:4000/graphql`);
}).catch(() => {

});

app.get('/healthz', (_, res) => {
  res.status(200).send('Okay!');
});



