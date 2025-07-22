/* eslint-disable import/no-cycle */
import {
  JSONObjectResolver,
  URLResolver
} from 'graphql-scalars';


import { queryResolvers as AccountsQuery, mutationResolvers as AccountMutation } from './account';
import { queryResolvers as ContainersQuery, mutationResolvers as ContainersMutation } from './container';
import { queryResolvers as DatasetsQuery } from './dataset';
import { queryResolvers as DomainsQuery } from './domain';
import { queryResolvers as VolumeQuery } from './volume';
import { queryResolvers as JobQuery, mutationResolvers as JobsMutation } from './job';

export const resolvers: any = {
  Query: {
    ...AccountsQuery,
    ...ContainersQuery,
    ...DatasetsQuery,
    ...DomainsQuery,
    ...JobQuery,
    ...VolumeQuery
  },
  Mutation: {
    ...AccountMutation,
    ...ContainersMutation,
    ...JobsMutation
  },
  JSONObject: JSONObjectResolver,
  URL: URLResolver,
  // RACM/JOBM Job Status mapping can be found here:
  // https://github.com/sciserver/resource-management/blob/81c7a4b62092b792da4ba50514b105011eed613b/src/main/java/org/sciserver/racm/jobm/model/JobStatus.java#L14
  JobStatus: {
    PENDING: 1,
    QUEUED: 2,
    ACCEPTED: 4,
    STARTED: 8,
    FINISHED: 16,
    SUCCESS: 32,
    ERROR: 64,
    CANCELED: 128
  }
};
