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
    startTime: DateTime
    endTime: DateTime
    runByUUID: String
    timeout: Int
    dockerComputeResourceContextUUID: UUID
    submitterTrustId: String
    username: String
    executorDID: String
    scriptURI: String
  }
  
  type JobsResponse {
    jobs: [Job!]!
    totalJobs: Int!
  }
  
  type JobDetails {
    job: Job!
    summary: String!
    files: [File!]!
  }

  input JobFilters {
    field: String!
    value: String!
  }

  input DataVolInput {
    name: String!
  }
  
  input UserVolInput {
    userVolumeId: ID!
    needsWriteAccess: Boolean!
  }

  input CreateJobParams {
    volumeContainers: [DataVolInput!]!
    userVolumes: [UserVolInput!]!
    command: String!
    resultsFolderURI: String!
    dockerComputeEndpoint: String!
    dockerImageName: String!
    submitterDID: String!
    scriptURI: String!
    name: String
  }

  type Query {
    getJobs(filters: [JobFilters!], top: Int, end: DateTime): JobsResponse!
    getJobDetails(jobId: ID!): JobDetails!
  }

  type Mutation {
    createJob(createJobParams: CreateJobParams!): Job!
    cancelJob(jobId: ID!): Boolean!
  }
`;