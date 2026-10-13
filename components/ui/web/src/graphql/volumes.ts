import { gql } from '@apollo/client';

export const VOLUMES = gql`
  query volumes{
    getVolumes{
      dataVolumes{
        name
      }
      rootVolumes{
        userVolumes{
          name
          owner
          datasets{
            name
            logo
            description
            tags
            catalog
            summary
            source
            resources{
              name
              kind
              link  
            }
          }
        }
      }
    }
  }
`;

export const FILE_VOLUMES = gql`
  query fileVolumes {
    getVolumes {
      dataVolumes {
        id
        name
        displayName
        description
        writable
        allowedActions
      }
      rootVolumes {
        name
        userVolumes {
          id
          name
          owner
          rootVolumeName
          allowedActions
          sharedWith {
            id
            name
            type
            allowedActions
          }
        }
      }
    }
  }
`;
