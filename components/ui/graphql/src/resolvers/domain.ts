// eslint-disable-next-line import/no-cycle
import { QueryResolvers } from '../generated/typings';

export const queryResolvers: QueryResolvers = {
  // eslint-disable-next-line no-empty-pattern
  getDomains: async (_, { }, { dataSources }) => {
    return dataSources.domainsAPI.getDomains();
  },
  getDomainByID: async (_, { id }, { dataSources }) => {
    return dataSources.domainsAPI.getDomainByID(id);
  }
};