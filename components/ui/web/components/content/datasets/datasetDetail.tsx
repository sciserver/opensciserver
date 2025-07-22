// eslint-disable-next-line eslint-comments/disable-enable-pair
/* eslint-disable import/no-extraneous-dependencies */
// import/no-extraneous-dependencies
import { FC, useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/router';
import {
  Avatar,
  Chip,
  CircularProgress,
  Divider,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemSecondaryAction,
  ListItemText
} from '@mui/material';
import { useLazyQuery } from '@apollo/client';
import { OpenInNew as OpenInNewIcon, ArrowBackIos as ArrowBackIcon } from '@mui/icons-material';
import styled from 'styled-components';
import { plural, singular } from 'pluralize';
import DOMPurify, { sanitize } from 'dompurify';
import ReactMarkdown from 'react-markdown';
import rehypeRaw from 'rehype-raw';
import Swal from 'sweetalert2';

import { Dataset, Resource } from 'src/graphql/typings';
import { DATASET_QUERY } from 'src/graphql/datasets';

import { SearchBar } from 'components/common/search';
import { CustomizedTabs } from 'components/common/tabs';

const Styled = styled.div`
  margin-right: 20px;
  margin-bottom: 30px;

  .dataset-info {
    display: flex;
    align-items: flex-start;
    margin: 40px 15px 30px 5px;
    
    .title {

      display: flex;

      h1{
        margin: 0px;
      }
      
      .MuiAvatar-root {
        margin-right: 20px;
      }
    }
    max-width: 900px;
  }
  
  .html-description{
    padding: 5px 50px 40px 20px;
  }

  .tags {
    margin-top: 15px;
    display: flex;
    overflow-x: auto;
    height: 45px;
    .tag{
      margin-right: 10px;
    }
  }

  .list {

    max-width: 600px;
    
    .MuiListItem-root{
      border-right: 1px solid rgba(0,0,0,0.12);
      border-left: 1px solid rgba(0,0,0,0.12);
    }
    
    .resource-list-item{
      display: flex;
      flex-direction: column;

      .resource-title {
        
      }
    }
  }

  .back-button-not-found {
    margin-top: 30px;
  }
  .not-found-container {
    margin-top: 100px;
    display: flex;
    flex-direction: column;
    align-items: center;
    span{
      font-size: 200px;
    }
  }
`;

type Props = {
  dataset?: Dataset;
}

export const DatasetDetail: FC<Props> = ({ }) => {

  //Config
  const router = useRouter();
  const query = router.query;

  //GRAPHQL Query
  const [getDataset, { data, loading, error }] = useLazyQuery(DATASET_QUERY);

  // ON MOUNT: Check if URL params make sense. If not, redirect to /datasets
  useEffect(() => {
    // There's 2 url params. If slug is empty, this method should wait until it gets populated.
    if (!router.isReady) {
      return;
    }

    let { volumeID, name, source, catalog } = query;

    volumeID = sanitize(volumeID as string);
    name = sanitize(name as string);
    source = sanitize(source as string);
    catalog = sanitize(catalog as string);

    getDataset(
      {
        variables:
        {
          params: {
            volumeID,
            name,
            source,
            catalog
          }
        }
      }
    );

  }, [router]);

  const dataset = useMemo<Dataset | null>(() => {
    return data?.getDataset as Dataset;
  }, [data]);

  useEffect(() => {
    if (error) {
      Swal.fire({
        icon: 'error',
        title: 'Dataset Error',
        text: error.message
      });
    }
  }, [error]);


  const [valueTab, setValueTab] = useState<number>(0);

  const notFound = useMemo<boolean>(() => {
    return !(dataset || loading);
  }, [dataset, loading]);

  const tabs = useMemo<string[]>(() => {
    const ts = ['About'];
    if (dataset && dataset.resources) {
      for (const r of dataset.resources) {
        const kind = plural(r.kind);
        if (!ts.includes(kind)) {
          ts.push(kind);
        }
      }
    }
    return ts;
  }, [dataset]);

  const resources = useMemo<Resource[]>(() => {
    if (dataset) {
      return dataset.resources.filter(r => singular(r.kind.toLowerCase()) === singular(tabs[valueTab]).toLowerCase());
    }
    return [];
  }, [valueTab]);

  const handleReturnToList = (e: { preventDefault: () => void; }) => {
    e.preventDefault();
    router.push('/datasets');
  };

  return <Styled>
    <SearchBar className="search-bar" />

    {dataset &&
      <div>
        <div className="dataset-info">
          <IconButton onClick={handleReturnToList}><ArrowBackIcon color="inherit" /></IconButton>
          <div >

            <div className="title">
              <Avatar alt={`${dataset.name.replaceAll(' ', '-')}-logo`} src={dataset.logo || 'logo-sm.png'} />
              <h1>{dataset.name}</h1>
            </div>
            <p>{dataset.summary}</p>
            <div className="tags">
              {dataset!.tags && dataset!.tags.map(t => <Chip onClick={() => router.push(`/datasets?tag=${t}`)} className="tag" label={t} variant="outlined" />)}
            </div>
          </div>
        </div>

        <CustomizedTabs tabs={tabs} value={valueTab} setValue={setValueTab} />
        {valueTab === 0 ?
          <>
            {dataset.description &&
              <ReactMarkdown className="html-description" rehypePlugins={[rehypeRaw]}>{DOMPurify.sanitize(dataset!.description)}</ReactMarkdown>
            }
          </>
          :
          <List className="list">
            <Divider />
            {resources.map(r =>
              <>
                <ListItem key={r.name} disablePadding onClick={() => window.open(r.link, '_blank')}>
                  <ListItemButton>
                    <div className="resource-list-item">
                      <ListItemText className="resource-title" primary={r.name} />
                      <ListItemText>{r.description}</ListItemText>
                    </div>
                  </ListItemButton>
                  <ListItemSecondaryAction >
                    <IconButton href={r.link} target="_blank">
                      <OpenInNewIcon color="primary" />
                    </IconButton>
                  </ListItemSecondaryAction>
                </ListItem>
                <Divider />
              </>
            )}
          </List>
        }
      </div>
    }
    {loading &&
      <CircularProgress />
    }
    {notFound &&
      <>
        <IconButton className="back-button-not-found" onClick={handleReturnToList}><ArrowBackIcon color="inherit" /></IconButton>
        <div className="not-found-container">
          <span>404</span>
          <h3>Seems like you don't have access to this dataset or it doesn't exist.</h3>
          <IconButton onClick={handleReturnToList} >Take me back!</IconButton>
        </div>
      </>
    }

  </Styled>;
};
