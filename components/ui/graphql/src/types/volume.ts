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
    sharedWith: [SharedWith!]!
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
    sharedWith: [SharedWith!]!
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
    sharedWith: [SharedWith!]!
    owningResourceId: ID
    rootVolumeName: String!
  }
  
  type SharedWith {
    id: ID!
    name: String!
    type: PrincipalType!
    allowedActions: [String!]!
  }

  input SharedWithInput {
    id: ID!
    name: String!
    type: PrincipalType!
    allowedActions: [String!]!
  }

  enum PrincipalType {
    USER
    GROUP
  }

  type JobUserVolume {
    id: ID!
    userVolumeId: ID!
    needsWriteAccess: Boolean!
    fullPath: String!
  }

  # Storage usage against a quota. Either a per-user quota on a root volume
  # (username set) or a quota on one user volume (userVolumeId set).
  type FileUsage {
    rootVolumeId: ID
    userVolumeId: ID
    username: String
    type: String
    numberOfBytesUsed: Float!
    numberOfBytesQuota: Float!
  }

  type Query {
    getVolumes: FileService
    getFileUsage: [FileUsage!]!
  }

  type Mutation {
    createUserVolume(rootVolumeName: String!, owner: String!, name: String!, description: String): Boolean!
    updateUserVolume(rootVolumeName: String!, owner: String!, name: String!, newName: String!, description: String): Boolean!
    deleteUserVolume(rootVolumeName: String!, owner: String!, name: String!): Boolean!
    # Replaces the sharing settings of a user volume. To revoke access, send the
    # principal with an empty allowedActions list.
    shareUserVolume(rootVolumeName: String!, owner: String!, name: String!, sharedWith: [SharedWithInput!]!): Boolean!
  }

  enum VolumeType {
    DATAVOLUME
    USERVOLUME
  }
`;
