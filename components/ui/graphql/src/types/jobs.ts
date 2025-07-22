import { gql } from 'graphql-tag';

export const typeDefs = gql`
  type JobMessage {
    id: ID!
    content: String!
    label: String!
  }

  enum JobStatus {
    PENDING
    QUEUED
    ACCEPTED
    STARTED
    FINISHED
    SUCCESS
    ERROR
    CANCELED
  }

  type Job {
    id: ID!
    executorDID: String!
    submitterDID: String!
    submitterTrustId: String!
    runByUUID: String!
    submissionTime: DateTime!
    startTime: DateTime!
    endTime: DateTime!
    duration: Float!
    timeout: Int!
    messages: [JobMessage!]!
    status: JobStatus!
    resultsFolderURI: String!
    type: String!
    userVolumes: [JobUserVolume!]!
    username: String!
    command: String!
    dockerComputeEndpoint: URL!
    dockerComputeResourceContextUUID: UUID!
    fullDockerCommand: [String!]!
    dockerImageName: String!
    dataVolumes: [DataVolume!]!
    scriptURI: String
  }

  input JobFilters {
    field: String!
    value: String!
  }

  input CreateJobParams {
    volumeContainers: [ID!]!
    userVolumes: [ID!]!
    command: String!
    resultsFolderURI: String!
    dockerComputeEndpoint: String!
    dockerImageName: String!
    submitterDID: String!
    scriptURI: String!
  }

  type Query {
    getJobs(filters: [JobFilters!]): [Job!]!
  }

  type Mutation {
    createJob(createJobParams: CreateJobParams!): Job!
  }
`;