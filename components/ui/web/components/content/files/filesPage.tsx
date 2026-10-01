import { FC, useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/router';
import { Box } from '@mui/material';

import { filesRouteQuery, FilesRoute, parseFilesRoute } from 'src/utils/fileVolumes';

import { FileBrowser } from './fileBrowser';

/** The Files page: the reusable FileBrowser, with its location kept in the URL. */
export const FilesPage: FC = () => {
  const router = useRouter();
  // router.isReady differs between the server HTML and the first client render, so wait for the client explicitly
  // (rendering on isReady alone breaks hydration). Until then router.query is empty and a deep link would flash the volume list.
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);
  const location = useMemo(() => parseFilesRoute(router.query), [router.query]);

  const onLocationChange = (next: FilesRoute) => router.push({ pathname: '/files', query: filesRouteQuery(next) });

  return (
    <Box sx={{ pr: 3 }}>
      <Box component="h2" sx={{ m: '6px 0 16px', fontSize: 26 }}>Files</Box>
      {mounted && router.isReady && <FileBrowser mode="manage" location={location} onLocationChange={onLocationChange} style={{ height: 600 }} />}
    </Box>
  );
};
