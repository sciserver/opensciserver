import { gql } from '@apollo/client';

export const GET_JOBS = gql`
  query GetJobs($filters: [JobFilters!]) {
    getJobs(filters: $filters) {
      id
      dockerImageName
      command
      startTime
      endTime
      status
      submitterDID
      submissionTime
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
  query GetJobDetails($jobId: ID!) {
    getJobDetails(jobId: $jobId) {
      job {
        id
        dockerImageName
        startTime
        scriptURI
        resultsFolderURI
        endTime
        command
        submitterDID
        dockerComputeEndpoint
        dataVolumes {
          publisherDID
        }
        userVolumes {
          id
        }
      }
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