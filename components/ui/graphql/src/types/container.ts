import { gql } from 'graphql-tag';

export const typeDefs = gql`
  type Container {
    id: ID!
    name: String!
    displayName: String!
    nodeName: String!
    status: ContainerStatus!
    imageName: String!
    domainName: String!
    domainID: Int!
    createdAt: DateTime!
    accessedAt: DateTime!
    maxSecs: Int!
    userVolumes: [ID!]!
    dataVolumes: [ComputeDataVolume!]!
    description: String
  }

  type ContainerDetail {
    id: ID!
    dataVolumes: [DataVolume!]!
    userVolumes: [UserVolume!]!
  }

  enum ContainerStatus {
    CREATED
    NONE
  }

  input ContainerParams {
    imageName: String!
    domainName: String!
    dataVolumeIds: [ID!]!
    userVolumeIds: [ID!]!
  }

  input ContainerDetailParams {
    domainId: ID!
    dataVolumeIds: [ID!]!
    userVolumeIds: [ID!]!
  }
  
  type Query {
    getContainers: [Container!]!
    getContainerDetail(containerDetailParams: ContainerDetailParams!): ContainerDetail!
    getContainerID(containerParams: ContainerParams!): ID!
    pingContainer(containerId: ID!): Boolean
  }

  type Mutation {
    deleteContainer(domainId: ID!, containerId: ID!): Boolean!
  }
`;