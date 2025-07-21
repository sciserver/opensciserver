import { gql } from 'graphql-tag';

export const typeDefs = gql`
  type User {
    id: ID!
    userName: String!
    email: String!
    visibility: String!
  }

  type Query {
    getUser: User!
  }

  type Mutation {
    login(username: String!, password: String!): String!
  }
`;