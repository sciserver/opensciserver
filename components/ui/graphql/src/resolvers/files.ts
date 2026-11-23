// eslint-disable-next-line import/no-cycle
import { QueryResolvers, MutationResolvers } from '../generated/typings';

export const queryResolvers: QueryResolvers = {
  getJsonTree: (_, { volume, path }, { dataSources }) => {
    return dataSources.filesAPI.getJsonTree(volume, path);
  }
};

export const mutationResolvers: MutationResolvers = {
  createFolder: (_, { volume, path, name }, { dataSources }) => {
    return dataSources.filesAPI.createFolder(volume, path, name);
  },
  deleteFile: (_, { volume, path, name }, { dataSources }) => {
    return dataSources.filesAPI.deleteFile(volume, path, name);
  },
  renameFile: (_, { volume, path, name, newName }, { dataSources }) => {
    return dataSources.filesAPI.renameFile(volume, path, name, newName);
  },
  moveFile: (_, { source, name, destination, newName }, { dataSources }) => {
    return dataSources.filesAPI.moveFile(source, name, destination, newName);
  },
  copyFile: (_, { source, name, destination, newName }, { dataSources }) => {
    return dataSources.filesAPI.copyFile(source, name, destination, newName);
  }
};
