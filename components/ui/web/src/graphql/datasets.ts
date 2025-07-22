import { gql } from '@apollo/client';

export const DATASETS = gql`
  query datasets($volumeType: VolumeType!) {
    getDatasets(volumeType: $volumeType){
        name
        summary
        source
        logo
        tags
        catalog
        volumeID
    }
  }  
`;

export const DATASET_QUERY = gql`
  query getDataset($params: DatasetDetailInput!) {
    getDataset(params: $params){
        name
        summary
        source
        logo
        description
        tags
        catalog
        volumeID
        resources{
          name
          kind
          link  
          description
        }
    }
  }  
`;
