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
