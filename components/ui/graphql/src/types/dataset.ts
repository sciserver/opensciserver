import { gql } from 'graphql-tag';

export const typeDefs = gql`
  type Dataset {
    name: String!
    summary: String!
    source: String!
    volumeID: ID!
    description: String
    catalog: String
    tags: [String!]!
    readme: String
    logo: URL
    resources: [Resource!]!
  }

  type Resource {
    name: String!
    kind: String!
    link: String!
    description: String
  }

  input DatasetDetailInput {
    name: String!
    volumeID: ID! 
    source: String! 
    catalog: String
  }

  type Query {
    getDatasets(volumeType: VolumeType!): [Dataset!]!
    getDataset(params: DatasetDetailInput!): Dataset
  }
`;