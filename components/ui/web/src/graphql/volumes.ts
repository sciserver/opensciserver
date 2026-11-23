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

/** Loaded when the sharing dialog opens: the full sharing list is too big to fetch for every volume up front. */
export const SHARING_DETAILS = gql`
  query sharingDetails {
    getVolumes {
      rootVolumes {
        userVolumes {
          id
          name
          owner
          rootVolumeName
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

export const SHARE_USER_VOLUME = gql`
  mutation shareUserVolume($rootVolumeName: String!, $owner: String!, $name: String!, $sharedWith: [SharedWithInput!]!) {
    shareUserVolume(rootVolumeName: $rootVolumeName, owner: $owner, name: $name, sharedWith: $sharedWith)
  }
`;

/** Loaded when the quotas dialog opens. The volume ids are only there to name each quota. */
export const FILE_QUOTAS = gql`
  query fileQuotas {
    getFileUsage {
      rootVolumeId
      userVolumeId
      username
      type
      numberOfBytesUsed
      numberOfBytesQuota
    }
    getVolumes {
      rootVolumes {
        id
        name
        userVolumes {
          id
          name
        }
      }
    }
  }
`;
