import { gql } from '@apollo/client';

export const JSON_TREE = gql`
  query jsonTree($volume: VolumeRefInput!, $path: String) {
    getJsonTree(volume: $volume, path: $path) {
      queryPath
      root {
        name
        lastModified
        creationTime
        folders {
          name
          lastModified
          creationTime
        }
        files {
          name
          size
          lastModified
          creationTime
        }
      }
    }
  }
`;

export const CREATE_FOLDER = gql`
  mutation createFolder($volume: VolumeRefInput!, $path: String!, $name: String!) {
    createFolder(volume: $volume, path: $path, name: $name)
  }
`;

export const DELETE_FILE = gql`
  mutation deleteFile($volume: VolumeRefInput!, $path: String!, $name: String!) {
    deleteFile(volume: $volume, path: $path, name: $name)
  }
`;

export const RENAME_FILE = gql`
  mutation renameFile($volume: VolumeRefInput!, $path: String!, $name: String!, $newName: String!) {
    renameFile(volume: $volume, path: $path, name: $name, newName: $newName)
  }
`;

export const MOVE_FILE = gql`
  mutation moveFile($source: FileLocationInput!, $name: String!, $destination: FileLocationInput!, $newName: String) {
    moveFile(source: $source, name: $name, destination: $destination, newName: $newName)
  }
`;

export const COPY_FILE = gql`
  mutation copyFile($source: FileLocationInput!, $name: String!, $destination: FileLocationInput!, $newName: String) {
    copyFile(source: $source, name: $name, destination: $destination, newName: $newName)
  }
`;
