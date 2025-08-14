import { gql } from '@apollo/client';

export const GET_JOBS = gql`
  query GetJobs($filters: [JobFilters!]) {
    getJobs(filters: $filters) {
      id
      scriptURI
      resultsFolderURI
      command
      submitterDID
      submissionTime
      startTime
      endTime
      status
      dockerImageName
      dockerComputeEndpoint
      dataVolumes {
        publisherDID
      }
      userVolumes {
        id
      }
    }
  }
`;

export const JOB_DETAIL_VIEW = gql`
  query GetJobDetails($jobDetailParams: JobDetailParams!) {
    getJobDetails(jobDetailParams: $jobDetailParams) {
      id
      summary
      files {
        name
        size
        lastModified
        creationTime
      }
    }
  }
`;

export const CREATE_JOB = gql`
  mutation CreateJob($createJobParams: CreateJobParams!) {
    createJob(createJobParams: $createJobParams){
      resultsFolderURI
      scriptURI
      command
      submitterDID
      submissionTime
      status
      dockerImageName
      dockerComputeEndpoint
      dataVolumes {
        publisherDID
      }
      userVolumes {
        id
      }
    }
  }
`;