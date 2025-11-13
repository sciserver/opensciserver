import { gql } from '@apollo/client';

export const GET_DOMAINS = gql`
  query GetDomains($jobs: Boolean) {
    getDomains(jobs: $jobs) {
      name
      id
      apiEndpoint
      description
      images {
        id
        name
        description
      }
      userVolumes {
        id
        name
        owner
        description
        rootVolumeName
      }
      dataVolumes {
        name
        id
        description
        publisherDID
      }
    }
  } 
`;