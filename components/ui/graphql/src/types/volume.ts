import { gql } from 'graphql-tag';

export const typeDefs = gql`
  
  type FileService {
    identifier: ID!
    name: String
    description: String
    apiEndpoint: URL
    rootVolumes: [RootVolume!]
    dataVolumes: [DataVolume!]
  }

  type DataVolume {
    id: ID!
    publisherDID: String!
    racmUUID: String!
    name: String!
    description: String!
    writable: Boolean!
    resourceUUID: ID
    displayName: String
    pathOnFileSystem: String
    url: URL
    allowedActions: [String]!
    sharedWith:[String]!
    owningResourceId: ID
  }

  type ComputeDataVolume{
    publisherDID: String!
    writable: Boolean!
  }

  type RootVolume {
    id: ID!
    resourceUUID: ID!
    name: String
    description: String
    pathOnFileSystem: String
    containsSharedVolumes: Boolean!
    userVolumes: [UserVolume!]!
    allowedActions: [String]!
    sharedWith:[String]!
    owningResourceId: ID
  }
  
  type UserVolume {
    id: ID!
    resourceUUID: ID!
    name: String!
    description: String
    relativePath: String
    owner: String!
    allowedActions: [String]!
    sharedWith:[String]!
    owningResourceId: ID
    rootVolumeName: String!
  }
  
  type JobUserVolume {
    id: ID!
    userVolumeId: ID!
    needsWriteAccess: Boolean!
    fullPath: String!
  }

  type JSONTree {
    root: Root!
    queryPath: String!
  }

  type Root {
    name: String!
    lastModified: DateTime!
    creationTime: DateTime!
    folders: [Folder!]!
    files: [File!]!
  }

  type Folder {
    name: String!
    lastModified: DateTime!
    creationTime: DateTime!
  }
  
  type File {
    name: String!
    size: Float!
    lastModified: DateTime!
    creationTime: DateTime!
  }

  type Query {
    getVolumes: FileService
    getJsonTree(volumeName: String!): JSONTree!
  }

  enum VolumeType {
    DATAVOLUME
    USERVOLUME
  }
`;
