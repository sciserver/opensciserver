import { FC, useMemo, useState } from 'react';
import { useQuery } from '@apollo/client';
import { IconButton, Typography } from '@mui/material';
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
            <Typography variant="h5">Domain:</Typography>
            <Typography variant="body1">{row.domainName}</Typography>
          </div>
          <div className="label">
            <Typography variant="h5">Image:</Typography>
            <Typography variant="body1">{row.imageName}</Typography>
          </div>
        </div>
        <div className="info">
          <div className="label">
            <Typography variant="h5">Last active:</Typography>
            <Typography variant="body1">{new Date(row.accessedAt).toLocaleDateString()}</Typography>
          </div>
          <div className="label">
            <Typography variant="h5">Expires at:</Typography>
            <Typography variant="body1">{getExpireTime(new Date(row.createdAt), row.maxSecs)}</Typography>
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