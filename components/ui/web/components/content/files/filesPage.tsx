import { FC, useContext, useMemo, useState } from 'react';
import { useRouter } from 'next/router';
import { useQuery } from '@apollo/client';
import { Alert, Box, ButtonBase } from '@mui/material';

import { UserContext } from 'context';
import { FILE_VOLUMES } from 'src/graphql/volumes';
import { FileService, VolumeType } from 'src/graphql/typings';
import { dataVolumeRows, filesRouteQuery, filterAndSortRows, parseFilesRoute, SortKey, userVolumeRows, VolumeRow } from 'src/utils/fileVolumes';
import { LoadingAnimation } from 'components/common/loadingAnimation';

import { Breadcrumb } from './breadcrumb';
import { VolumeList } from './volumeList';
import { FolderBrowser } from './folderBrowser';
import { VolumeTabs } from './volumeTabs';

export const FilesPage: FC = () => {
  const router = useRouter();
  const { user } = useContext(UserContext);
  const { data, loading, error } = useQuery<{ getVolumes?: FileService | null }>(FILE_VOLUMES);

  const [filter, setFilter] = useState('');
  const [refreshSignal, setRefreshSignal] = useState(0);
  const [sortKey, setSortKey] = useState<SortKey>('name');
  const [direction, setDirection] = useState<1 | -1>(1);

  const route = useMemo(() => parseFilesRoute(router.query), [router.query]);

  const userRows = useMemo(() => userVolumeRows(data?.getVolumes?.rootVolumes || [], user?.userName), [data, user]);
  const dataRows = useMemo(() => dataVolumeRows(data?.getVolumes?.dataVolumes || []), [data]);
  const isData = route.volumeType === VolumeType.Datavolume;
  const rows = useMemo(
    () => filterAndSortRows(isData ? dataRows : userRows, filter, sortKey, direction),
    [isData, dataRows, userRows, filter, sortKey, direction]
  );

  const go = (next: Parameters<typeof filesRouteQuery>[0]) => {
    setFilter('');
    router.push({ pathname: '/files', query: filesRouteQuery(next) });
  };

  const onSort = (key: SortKey) => {
    if (key === sortKey) {
      setDirection((current) => (current === 1 ? -1 : 1));
    }
    else {
      setSortKey(key); setDirection(1); 
    }
  };

  const openVolume = (row: VolumeRow) => {
    const { volumeType, ...volume } = row.route;
    go({ volumeType, volume, path: '' });
  };

  return (
    <Box sx={{ pr: 3 }}>
      <Box component="h2" sx={{ m: '6px 0 16px', fontSize: 26 }}>Files</Box>
      <Box sx={{ display: 'flex', flexDirection: 'column', height: 600, bgcolor: 'background.paper', border: '1px solid #dde2e7', borderRadius: '6px', overflow: 'hidden' }}>
        <VolumeTabs
          value={route.volumeType}
          counts={{ [VolumeType.Uservolume]: data ? userRows.length : undefined, [VolumeType.Datavolume]: data ? dataRows.length : undefined }}
          onChange={(type) => go({ volumeType: type, path: '' })}
        />
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, px: 1.5, minHeight: 52, flex: 'none' }}>
          <Box sx={{ flex: 1 }} />
          <Box component="label" sx={{ display: 'flex', alignItems: 'center', gap: 0.75, height: 32, px: 1.25, border: '1px solid #d5dae0', borderRadius: 1, width: 200, bgcolor: '#fff' }}>
            <span className="material-symbols-outlined" style={{ fontSize: 18, color: 'rgba(0,0,0,0.5)' }}>search</span>
            <input
              value={filter}
              onChange={(event) => setFilter(event.target.value)}
              placeholder={route.volume ? 'Filter this folder' : 'Filter volumes'}
              aria-label={route.volume ? 'Filter this folder' : 'Filter volumes'}
              style={{ border: 0, outline: 'none', font: '13px \'Noto Sans\', sans-serif', minWidth: 0, flex: 1, background: 'transparent' }}
            />
          </Box>
          {route.volume && (
            <ButtonBase aria-label="Refresh" title="Refresh" onClick={() => setRefreshSignal((value) => value + 1)} sx={{ width: 32, height: 32, borderRadius: '50%', color: 'rgba(0,0,0,0.6)' }}>
              <span className="material-symbols-outlined" style={{ fontSize: 20 }}>refresh</span>
            </ButtonBase>
          )}
        </Box>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, px: 1.5, py: 0.75, minHeight: 42, bgcolor: '#f4f6f8', borderTop: '1px solid #e6e9ed', borderBottom: '1px solid #e6e9ed', flex: 'none' }}>
          <Breadcrumb
            route={route}
            onRoot={() => go({ volumeType: route.volumeType, path: '' })}
            onPath={(path) => go({ ...route, path })}
          />
          {route.volume?.owner && (
            <Box sx={{ display: 'flex', gap: 1.75, fontSize: 12, color: 'rgba(0,0,0,0.6)', flex: 'none' }}>
              <span>Owner <b style={{ color: '#202124' }}>{route.volume.owner}</b></span>
              <span>{route.volume.rootVolumeName} volume</span>
            </Box>
          )}
        </Box>
        {!route.volume && error && <Alert severity="error" sx={{ m: 2 }}>Could not load volumes: {error.message}</Alert>}
        {!route.volume && <LoadingAnimation backDropIsOpen={loading} />}
        {!route.volume && !loading && !error && (
          <VolumeList volumeType={route.volumeType} rows={rows} sortKey={sortKey} direction={direction} onSort={onSort} onOpen={openVolume} />
        )}
        {route.volume && (
          <FolderBrowser
            route={{ ...route, volume: route.volume }}
            filter={filter}
            refreshSignal={refreshSignal}
            onOpenFolder={(path) => go({ ...route, path })}
          />
        )}
      </Box>
    </Box>
  );
};
