import { Card, CardHeader, Avatar, Button, CardContent, CardActions, Chip } from '@mui/material';
import router from 'next/router';
import { FC } from 'react';
import styled from 'styled-components';

import { Dataset, VolumeType } from 'src/graphql/typings';

type Props = {
  type: VolumeType
  dataset: Dataset
  selectedTags: string[]
  setSelectedTags: (tag: string[]) => void
}

const StyledCard = styled(Card)`
  display: flex;
  justify-content: space-between;
  flex-direction: column;
  min-width: 450px;
  max-width: 550px;
  margin-right: 30px;
  margin-bottom: 30px;

  .tags {
    display: flex;
    flex-wrap: wrap;
    min-height: 45px;
    .tag{
      margin-right: 10px;
    }
  }
  
  .actions {
    display: flex;
    justify-content: flex-end;
  }


  &:hover {
    cursor: pointer;
    box-shadow: rgba(0, 0, 0, 0.2) 0px 5px 5px 0px, rgba(0, 0, 0, 0.14) 0px 8px 10px 0px, rgba(0, 0, 0, 0.12) 0px 3px 14px 0px;
  }
`;

export const DatasetCard: FC<Props> = ({ dataset, selectedTags, setSelectedTags }) => {

  const redirectToDatasetURL = () => {
    router.push(`/datasets/detail?volumeID=${dataset.volumeID}&catalog=${dataset.catalog || '-'}&name=${dataset.name}&source=${dataset.source}`);
  };

  return <StyledCard>
    <CardHeader
      avatar={
        <Avatar alt={`${dataset.name.replaceAll(' ', '-')}-logo`} src={dataset.logo || 'logo-sm.png'} />
      }
      title={dataset.name}
      subheader={dataset.catalog || 'No Catalog'}
      onClick={redirectToDatasetURL}
      action={<Button onClick={redirectToDatasetURL} >Learn more</Button>}
    />
    <CardContent onClick={redirectToDatasetURL}>
      <p>
        {dataset.summary}
      </p>
    </CardContent>
    <CardActions>
      <div className="tags">
        {dataset.tags && dataset.tags.map(t => (
          <Button onClick={() => setSelectedTags([...selectedTags, t])}>
            <Chip size="small" className="tag" label={t} variant="outlined" />
          </Button>
        ))}
      </div>
    </CardActions>
  </StyledCard>;
};