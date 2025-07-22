// eslint-disable-next-line import/no-cycle
import { QueryResolvers } from '../generated/typings';

export const queryResolvers: QueryResolvers = {
  // eslint-disable-next-line no-empty-pattern
  getDatasets: async (_, { volumeType }, { dataSources }) => {
    const volumes = await dataSources.volumesAPI.getVolumes();
    return dataSources.datasetsAPI.getDatasets(volumes, volumeType);
  },
  getDataset: async (_, { params }, { dataSources }) => {
    return dataSources.datasetsAPI.getDataset(params);
  }
};