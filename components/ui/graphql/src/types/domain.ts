import { gql } from 'graphql-tag';

export const typeDefs = gql`
  type Domain {
    id: ID!
    name: String!
    publisherDID: String!
    racmUUID: UUID!
    apiEndpoint: URL!
    description: String!
    images: [Image!]!
    dataVolumes: [DataVolume!]!
    userVolumes: [UserVolume!]!
  }

  type Image {
    id: ID!
    publisherDID: String!
    racmUUID: String!
    name: String!
    description: String!
  }

  type Query {
    getDomains: [Domain!]!
    getDomainByID(id: ID!): Domain
  }
`;