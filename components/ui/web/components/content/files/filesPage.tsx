import { FC, useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/router';
import styled from 'styled-components';

import { filesRouteQuery, FilesRoute, parseFilesRoute } from 'src/utils/fileVolumes';

import { FileBrowser } from './fileBrowser';

const Styled = styled.div`
  padding-right: 24px;

  h2 {
    margin: 6px 0 16px;
    font-size: 26px;
  }
`;

const Browser = styled(FileBrowser)`
  height: 600px;
`;

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
    <Styled>
      <h2>Files</h2>
      {mounted && router.isReady && <Browser mode="manage" location={location} onLocationChange={onLocationChange} />}
    </Styled>
  );
};
