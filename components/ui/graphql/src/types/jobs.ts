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
  
  type JobDetails {
    id: ID!
    summary: String!
    files: [File!]!
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
  
  input JobDetailParams {
    jobID: ID!
    resultsFolderURI: String!
  }

  type Query {
    getJobs(filters: [JobFilters!]): [Job!]!
    getJobDetails(jobDetailParams: JobDetailParams!): JobDetails!
  }

  type Mutation {
    createJob(createJobParams: CreateJobParams!): Job!
  }
`;