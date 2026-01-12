import { FC, useMemo, useState } from 'react';
import { useQuery } from '@apollo/client';
import { IconButton } from '@mui/material';
import { ArrowBackIos as ArrowBackIcon } from '@mui/icons-material';
import { SimpleTreeView, TreeItem } from '@mui/x-tree-view';
import styled from 'styled-components';

import { Container, DataVolume, UserVolume } from 'src/graphql/typings';
import { VOLUMES_CONTAINER_DETAIL_VIEW } from 'src/graphql/containers';
import { LoadingAnimation } from 'components/common/loadingAnimation';
import { CustomizedTabs } from 'components/common/tabs';
import { getExpireTime } from 'src/utils/dates';

type Props = {
  row: Container;
  back: () => void;
}

const Styled = styled.div`
  .header {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-bottom: 1.5rem;
    }
    
  .label {
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }
      
  .info {
    display: flex;
    flex-direction: column;
    margin-right: 1rem;
  }
        
  .tree-view {
    margin-top: 1.5rem;
    width: 60%;
  }
`;


export const ContainerDetail: FC<Props> = ({ row, back }) => {

  const [error, setError] = useState<boolean>(false);
  const [tabValue, setTabValue] = useState<number>(0);

  const { loading, data } = useQuery(VOLUMES_CONTAINER_DETAIL_VIEW,
    {
      onError: () => setError(true),
      variables: {
        containerDetailParams: {
          domainId: row.domainID,
          dataVolumeIds: row.dataVolumes.map(dv => dv.publisherDID),
          userVolumeIds: row.userVolumes
        }
      }
    }
  );

  const dataVolumes = useMemo<DataVolume[]>(() => {
    if (data && data.getContainerDetail) {
      console.log(data.getContainerDetail.dataVolumes);

      return data.getContainerDetail.dataVolumes;
    }
    return [];
  }, [data]);

  const userVolumes = useMemo<UserVolume[]>(() => {
    if (data && data.getContainerDetail) {
      console.log(data.getContainerDetail.userVolumes);

      return data.getContainerDetail.userVolumes;
    }
    return [];
  }, [data]);

  return (
    <Styled>
      <div className="header">
        <IconButton onClick={back} >
          <ArrowBackIcon />
        </IconButton>
        <div className="info">
          <div className="label">
            <h3>Domain:</h3>
            <p>{row.domainName}</p>
          </div>
          <div className="label">
            <h3>Image:</h3>
            <p>{row.imageName}</p>
          </div>
        </div>
        <div className="info">
          <div className="label">
            <h3>Last active:</h3>
            <p>{new Date(row.accessedAt).toLocaleDateString()}</p>
          </div>
          <div className="label">
            <h3>Expires at:</h3>
            <p>{getExpireTime(new Date(row.createdAt), row.maxSecs)}</p>
          </div>
        </div>
      </div>
      {loading &&
        <LoadingAnimation backDropIsOpen={loading} />
      }
      {error ?
        <>
          <h2>There was an error loading the details for this container.</h2>
          <p>{error}</p>
        </>
        :
        <>
          <CustomizedTabs tabs={['Data Volumes', 'User Volumes']} value={tabValue} setValue={setTabValue} />
          <div>
            {tabValue === 0 ?
              <SimpleTreeView className="tree-view">
                {dataVolumes.map(dv =>
                  <TreeItem itemId={dv.id} label={dv.name}>
                    <TreeItem itemId={`${dv.id}-files`} label="For more details, click here." onClick={() => window.location.href = process.env.NEXT_PUBLIC_FILES_URL || ''} />
                  </TreeItem>
                )}
              </SimpleTreeView>
              :
              <SimpleTreeView className="tree-view">
                {userVolumes.map(uv =>
                  <TreeItem itemId={`${uv.owner}/${uv.name}`} label={`${uv.name} (${uv.owner})`}>
                    <TreeItem itemId={`${uv.id}-files`} label="For more details, click here." onClick={() => window.location.href = process.env.NEXT_PUBLIC_FILES_URL || ''} />
                  </TreeItem>
                )}
              </SimpleTreeView>
            }
          </div>
        </>
      }
    </Styled>
  );
};