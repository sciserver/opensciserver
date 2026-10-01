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
        description
        allowedActions
        userVolumes {
          id
          name
          owner
          description
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

export const CREATE_USER_VOLUME = gql`
  mutation createUserVolume($rootVolumeName: String!, $owner: String!, $name: String!, $description: String) {
    createUserVolume(rootVolumeName: $rootVolumeName, owner: $owner, name: $name, description: $description)
  }
`;

export const UPDATE_USER_VOLUME = gql`
  mutation updateUserVolume($rootVolumeName: String!, $owner: String!, $name: String!, $newName: String!, $description: String) {
    updateUserVolume(rootVolumeName: $rootVolumeName, owner: $owner, name: $name, newName: $newName, description: $description)
  }
`;

export const DELETE_USER_VOLUME = gql`
  mutation deleteUserVolume($rootVolumeName: String!, $owner: String!, $name: String!) {
    deleteUserVolume(rootVolumeName: $rootVolumeName, owner: $owner, name: $name)
  }
`;
