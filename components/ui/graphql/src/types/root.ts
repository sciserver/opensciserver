import { gql } from 'graphql-tag';

import { typeDefs as Container } from './container';
import { typeDefs as Dataset } from './dataset';
import { typeDefs as Domain } from './domain';
import { typeDefs as Account } from './account';
import { typeDefs as Volume } from './volume';
import { typeDefs as Job } from './jobs';


const Root = gql`
  type Query {
    root: String
  }

  type Mutation {
    root: String
  }

  type Subscription {
    root: String
  }

  scalar DateTime

  scalar JSONObject

  scalar URL

  scalar UUID
`;

export const typeDefs = [
  Root,
  Account,
  Container,
  Dataset,
  Domain,
  Job,
  Volume
];
