// eslint-disable-next-line import/no-cycle
import { MutationResolvers, QueryResolvers } from '../generated/typings';

export const queryResolvers: QueryResolvers = {
  // eslint-disable-next-line no-empty-pattern
  getJobs: async (_, { filters }, { dataSources }) => {
    return dataSources.jobsAPI.getJobs(filters);
  }
};

export const mutationResolvers: MutationResolvers = {
  createJob: async (_, { createJobParams }, { dataSources }) => {
    const created = await dataSources.jobsAPI.createJob(createJobParams);
    return created;
  }
};