import { gql } from 'graphql-tag';

export const typeDefs = gql`
  type User {
    id: ID!
    userName: String!
    email: String!
    visibility: String!
  }

  # A user or group a volume can be shared with
  type SharePrincipal {
    id: ID!
    name: String!
    type: PrincipalType!
  }

  type Query {
    getUser: User!
    getPublicUsersAndGroups: [SharePrincipal!]!
  }

  type Mutation {
    login(username: String!, password: String!): String!
  }
`;