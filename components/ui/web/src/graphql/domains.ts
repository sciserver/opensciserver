import { gql } from '@apollo/client';

export const GET_DOMAINS = gql`
  query GetDomains {
    getDomains {
      name
      id
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