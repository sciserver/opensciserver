import { FC, useContext, useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/router';
import { CircularProgress, Grid } from '@mui/material';
import { ApolloError, useLazyQuery } from '@apollo/client';
import styled from 'styled-components';
import { compact } from 'lodash';
import { sanitize } from 'dompurify';

import { AppContext } from 'context';
import { Dataset, VolumeType } from 'src/graphql/typings';
import { DATASETS } from 'src/graphql/datasets';
import { textSearch } from 'src/utils/search';

import { SearchBar } from 'components/common/search';
import { AutoCompleteFilter } from 'components/common/autocomplete';
import { DatasetCard } from 'components/content/datasets/datasetCard';

const Styled = styled.div`
  margin: 50px 20px 30px 0;
  
  h1 {
    color: ${({ theme }) => theme.palette.text.title};
  }

  .search-bar {
    margin-bottom: 40px;
  }

  .card-list {
    display: flex;  
    flex-wrap: wrap;

    margin-top: 20px;
  }

  .description {
    padding: 20px;
  }

`;

export const DatasetList: FC = () => {

  //Config
  const router = useRouter();


  // Component state
  const [dataVolumeDatasets, setDataVolumeDatasets] = useState<Dataset[]>([]);
  const [dataVolumeDatasetsUnfiltered, setDataVolumeDatasetsUnfiltered] = useState<Dataset[]>([]);
  const [userVolumeDatasets, setUserVolumeDatasets] = useState<Dataset[]>([]);
  const [userVolumeDatasetsUnfiltered, setUserVolumeDatasetsUnfiltered] = useState<Dataset[]>([]);
  const [selectedTags, setSelectedTags] = useState<string[]>([]);

  const query = router.query;

  // Context
  const { setMenuOption, setShowAppBar } = useContext(AppContext);

  // GRAPHQL Queries
  const errorHandling = (error: ApolloError) => {
    if (error.message.includes('Unauthorized')) {
      router.push('/login?callbackURL=/datasets');
    }
  };

  const [getDataVolumeDatasets, { loading: loadingDVs, data: dataDVs }] =
    useLazyQuery(
      DATASETS,
      {
        variables: { volumeType: VolumeType.Datavolume },
        onError: errorHandling
      }
    );
  const [getUserVolumeDatasets, { loading: loadingUVs, data: dataUVs }] =
    useLazyQuery(
      DATASETS,
      {
        variables: { volumeType: VolumeType.Uservolume },
        onError: errorHandling
      }
    );


  // ON MOUNT: UI config & Data fetching
  useEffect(() => {
    setMenuOption('datasets');
    setShowAppBar(true);

    getDataVolumeDatasets();
    getUserVolumeDatasets();
  }, []);

  // Handle Datavolume datasets query data 
  useEffect(() => {
    const dsDV = (dataDVs?.getDatasets as Dataset[]);
    if (dsDV) {
      setDataVolumeDatasets(dsDV);
      setDataVolumeDatasetsUnfiltered(dsDV);
    }

  }, [dataDVs]);

  // Handle Uservolume datasets query data 
  useEffect(() => {
    const dsUV = (dataUVs?.getDatasets as Dataset[]);
    if (dsUV) {
      setUserVolumeDatasets(dsUV);
      setUserVolumeDatasetsUnfiltered(dsUV);
    }
  }, [dataUVs]);

  // Filtering and Search handling
  const tags = useMemo<string[]>(() => {
    let ts: string[] = [];
    for (const ds of [...dataVolumeDatasets, ...userVolumeDatasets]) {
      ts = [...ts, ...ds.tags];
    }
    return ts;
  }, [dataVolumeDatasets, userVolumeDatasets]);

  const handleTagFilter = (selected: string[]) => {
    setSelectedTags(selected);
    if (selected.length) {
      const filteredDataVolumeDatasets = dataVolumeDatasetsUnfiltered.filter(d => d.tags.some(t => selected.includes(t)));
      const filteredUserVolumeDatasets = userVolumeDatasetsUnfiltered.filter(d => d.tags.some(t => selected.includes(t)));
      setDataVolumeDatasets(filteredDataVolumeDatasets);
      setUserVolumeDatasets(filteredUserVolumeDatasets);
      return;
    }

    setDataVolumeDatasets(dataVolumeDatasetsUnfiltered);
    setUserVolumeDatasets(userVolumeDatasetsUnfiltered);
  };

  // ON MOUNT: Check if URL params make sense. If not, redirect to /datasets
  useEffect(() => {
    if (!router.isReady) {
      return;
    }

    let { tag } = query;
    tag = compact(sanitize((tag as string))?.split(',') || []);

    handleTagFilter(tag);
  }, [router]);

  useEffect(() => {
    handleTagFilter(selectedTags);
  }, [selectedTags]);

  const searchDatasetParams = (dataset: Dataset, input: string) => {
    return textSearch(dataset.description || '', input)
      || textSearch(dataset.name || '', input)
      || textSearch(dataset.catalog || '', input)
      || textSearch(dataset.summary, input)
      || dataset.tags.some(t => t.toLowerCase().includes(input));
  };

  const onSearch = (input: string) => {
    const filterDataVolumeDatasets = dataVolumeDatasetsUnfiltered.filter(d => searchDatasetParams(d, input));
    const filterUserVolumeDatasets = userVolumeDatasetsUnfiltered.filter(d => searchDatasetParams(d, input));
    setDataVolumeDatasets(filterDataVolumeDatasets);
    setUserVolumeDatasets(filterUserVolumeDatasets);
  };

  return <Styled>
    <h1>Datasets</h1>
    <SearchBar placeholder={'Search datasets'} className="search-bar" onChangeParam={onSearch} />
    <AutoCompleteFilter options={tags} handleFilter={handleTagFilter} value={selectedTags} placeholder="Filter by Tags" />
    {dataVolumeDatasets.length > 0 && !loadingDVs &&
      <>
        <h3>Official</h3>
        <Grid container alignItems="stretch">
          {dataVolumeDatasets.map(d => (
            <Grid key={d.name} item className="card-list">
              <DatasetCard type={VolumeType.Datavolume} dataset={d} selectedTags={selectedTags} setSelectedTags={setSelectedTags} />
            </Grid>
          ))}
        </Grid>
      </>
    }
    {userVolumeDatasets.length > 0 && !loadingUVs &&
      <>
        <h3>Contributed</h3>
        <Grid container alignItems="stretch">
          {userVolumeDatasets.map(d => (
            <Grid key={d.name} item className="card-list">
              <DatasetCard type={VolumeType.Uservolume} dataset={d} selectedTags={selectedTags} setSelectedTags={setSelectedTags} />
            </Grid>
          ))}
        </Grid>
      </>
    }
    {(loadingDVs || loadingUVs) &&
      <CircularProgress />
    }
  </Styled>;
};
