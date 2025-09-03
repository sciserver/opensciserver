// eslint-disable-next-line import/no-cycle
import { MutationResolvers, QueryResolvers } from '../generated/typings';

export const queryResolvers: QueryResolvers = {
  // eslint-disable-next-line no-empty-pattern
  getJobs: async (_, { filters, top }, { dataSources }) => {
    return dataSources.jobsAPI.getJobs(filters, top || undefined);
  },
  getJobDetails: async (_, { jobId }, { dataSources }) => {
    return dataSources.jobsAPI.getJobDetails(jobId);
  }
};

export const mutationResolvers: MutationResolvers = {
  createJob: async (_, { createJobParams }, { dataSources }) => {
    const created = await dataSources.jobsAPI.createJob(createJobParams);
    return created;
  }
};