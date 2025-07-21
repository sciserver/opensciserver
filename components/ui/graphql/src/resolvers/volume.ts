// eslint-disable-next-line import/no-cycle
import { QueryResolvers } from '../generated/typings';

export const queryResolvers: QueryResolvers = {
  // eslint-disable-next-line no-empty-pattern
  getVolumes: (_, { }, { dataSources }) => {
    return dataSources.volumesAPI.getVolumes();
  },
  getJsonTree: (_, { volumeName }, { dataSources }) => {
    return dataSources.volumesAPI.getFilesByVolume(volumeName);
  }
};