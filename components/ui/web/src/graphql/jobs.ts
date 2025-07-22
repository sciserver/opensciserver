import { gql } from '@apollo/client';

export const GET_JOBS = gql`
  query GetJobs {
    getJobs {
      id
      scriptURI
      resultsFolderURI
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