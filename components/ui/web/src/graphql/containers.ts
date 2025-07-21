import { gql } from '@apollo/client';

export const GET_CONTAINER_ID = gql`
  query GetContainer($containerParams: ContainerParams!) {
    getContainerID(containerParams: $containerParams)
  }  
`;

export const PING_CONTAINER = gql`
  query PingContainer($containerId: ID!) {
    pingContainer(containerId: $containerId)
  }  
`;

export const GET_CONTAINERS = gql`
  query GetContainers {
    getContainers {
      accessedAt
      createdAt
      maxSecs
      domainName
      domainID
      imageName
      name
      id
      status
      dataVolumes {
        publisherDID
      }
      userVolumes
    }
  }
`;
export const VOLUMES_CONTAINER_DETAIL_VIEW = gql`
  query GetContainerDetail($containerDetailParams: ContainerDetailParams!) {
    getContainerDetail(containerDetailParams: $containerDetailParams) {
      dataVolumes {
        id
        name
        publisherDID
      }
      id
      userVolumes {
        id
        name
        owner
      }
    }
  }
`;