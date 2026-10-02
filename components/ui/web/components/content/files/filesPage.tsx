import { FC, useMemo } from 'react';
import { useRouter } from 'next/router';
import { Box } from '@mui/material';

import { filesRouteQuery, FilesRoute, parseFilesRoute } from 'src/utils/fileVolumes';

import { FileBrowser } from './fileBrowser';

/** The Files page: the reusable FileBrowser, with its location kept in the URL. */
export const FilesPage: FC = () => {
  const router = useRouter();
  const location = useMemo(() => parseFilesRoute(router.query), [router.query]);

  const onLocationChange = (next: FilesRoute) => router.push({ pathname: '/files', query: filesRouteQuery(next) });

  return (
    <Box sx={{ pr: 3 }}>
      <Box component="h2" sx={{ m: '6px 0 16px', fontSize: 26 }}>Files</Box>
      <FileBrowser mode="manage" location={location} onLocationChange={onLocationChange} style={{ height: 600 }} />
    </Box>
  );
};
