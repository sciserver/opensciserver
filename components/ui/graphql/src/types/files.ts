import { gql } from 'graphql-tag';

export const typeDefs = gql`
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

  # Identifies a volume. User volumes are addressed by rootVolumeName/owner/volumeName,
  # data volumes by volumeName only.
  input VolumeRefInput {
    volumeType: VolumeType!
    volumeName: String!
    rootVolumeName: String
    owner: String
  }

  # A folder inside a volume. path is '' (or '/') for the volume root, otherwise '/a/b'.
  input FileLocationInput {
    volume: VolumeRefInput!
    path: String!
  }

  type Query {
    getJsonTree(volume: VolumeRefInput!, path: String): JSONTree!
  }

  type Mutation {
    createFolder(volume: VolumeRefInput!, path: String!, name: String!): Boolean!
    deleteFile(volume: VolumeRefInput!, path: String!, name: String!): Boolean!
    renameFile(volume: VolumeRefInput!, path: String!, name: String!, newName: String!): Boolean!
    moveFile(source: FileLocationInput!, name: String!, destination: FileLocationInput!, newName: String): Boolean!
    copyFile(source: FileLocationInput!, name: String!, destination: FileLocationInput!, newName: String): Boolean!
  }
`;
