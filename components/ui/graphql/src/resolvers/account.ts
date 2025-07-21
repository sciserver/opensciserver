// eslint-disable-next-line import/no-cycle
import { MutationResolvers, QueryResolvers } from '../generated/typings';

export const queryResolvers: QueryResolvers = {
  // eslint-disable-next-line no-empty-pattern
  getUser: async (_, { }, { dataSources }) => {
    return dataSources.accountsAPI.getUser();
  }
};

export const mutationResolvers: MutationResolvers = {
  login: async (_, { username, password }, { dataSources }) => {
    const token = await dataSources.accountsAPI.login(username, password);
    return token;
  }
};