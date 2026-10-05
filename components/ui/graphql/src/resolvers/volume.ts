// eslint-disable-next-line import/no-cycle
import { QueryResolvers, MutationResolvers } from '../generated/typings';

export const queryResolvers: QueryResolvers = {
  // eslint-disable-next-line no-empty-pattern
  getVolumes: (_, { }, { dataSources }) => {
    return dataSources.volumesAPI.getVolumes();
  },
  // eslint-disable-next-line no-empty-pattern
  getFileUsage: (_, { }, { dataSources }) => {
    return dataSources.volumesAPI.getFileUsage();
  }
};

export const mutationResolvers: MutationResolvers = {
  shareUserVolume: (_, { rootVolumeName, owner, name, sharedWith }, { dataSources }) => {
    return dataSources.volumesAPI.shareUserVolume(rootVolumeName, owner, name, sharedWith);
  },
  createUserVolume: (_, { rootVolumeName, owner, name, description }, { dataSources }) => {
    return dataSources.volumesAPI.createUserVolume(rootVolumeName, owner, name, description);
  },
  updateUserVolume: (_, { rootVolumeName, owner, name, newName, description }, { dataSources }) => {
    return dataSources.volumesAPI.updateUserVolume(rootVolumeName, owner, name, newName, description);
  },
  deleteUserVolume: (_, { rootVolumeName, owner, name }, { dataSources }) => {
    return dataSources.volumesAPI.deleteUserVolume(rootVolumeName, owner, name);
  }
};
