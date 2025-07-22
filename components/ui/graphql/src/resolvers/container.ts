// eslint-disable-next-line import/no-cycle
import { MutationResolvers, QueryResolvers } from '../generated/typings';

export const queryResolvers: QueryResolvers = {
  // eslint-disable-next-line no-empty-pattern
  getContainers: async (_, { }, { dataSources }) => {
    return dataSources.containersAPI.getContainers();
  },
  getContainerID: async (_, { containerParams }, { dataSources }) => {
    return dataSources.containersAPI.getContainerID(containerParams);
  },
  pingContainer: async (_, { containerId }, { dataSources }) => {
    return dataSources.containersAPI.pingContainer(containerId);
  },
  getContainerDetail: async (_, { containerDetailParams }, { dataSources }) => {
    return dataSources.containersAPI.getContainerDetail(containerDetailParams);
  }
};


export const mutationResolvers: MutationResolvers = {
  deleteContainer: async (_, { domainId, containerId }, { dataSources }) => {
    const deleted = await dataSources.containersAPI.deleteContainer(domainId, containerId);
    return deleted;
  }
};