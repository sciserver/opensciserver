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
    submitterDID: String!
    submissionTime: DateTime!
    startTime: DateTime!
    endTime: DateTime!
    duration: Float!
    messages: [JobMessage!]!
    status: JobStatus!
    resultsFolderURI: String!
    type: String!
    userVolumes: [JobUserVolume!]!
    dataVolumes: [DataVolume!]!
    command: String!
    dockerComputeEndpoint: String!
    dockerImageName: String!
    fullDockerCommand: [String!]!
    runByUUID: String
    timeout: Int
    dockerComputeResourceContextUUID: UUID
    submitterTrustId: String
    username: String
    executorDID: String
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