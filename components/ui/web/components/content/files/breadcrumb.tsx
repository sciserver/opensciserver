import { FC } from 'react';
import styled from 'styled-components';
import { ButtonBase } from '@mui/material';
import { VolumeType } from 'src/graphql/typings';
import { FilesRoute } from 'src/utils/fileVolumes';
import { pathSegments } from 'src/utils/files';

import { Icon } from './filesStyles';

type Props = {
  route: FilesRoute;
  /** Go to the volume list of the current type. */
  onRoot: () => void;
  /** Go to a folder of the current volume ('' is the volume root). */
  onPath: (path: string) => void;
};

const MAX_FOLDER_CRUMBS = 3;

type Crumb = { label: string; path?: string };

const Styled = styled.nav`
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px;
  min-width: 0;
  flex: 1;

  .crumb {
    display: inline-flex;
    align-items: center;
    gap: 2px;
    min-width: 0;
  }
`;

const CrumbButton = styled(ButtonBase)<{ $current?: boolean }>`
  && {
    padding: 3px 6px;
    border-radius: 4px;
    font-size: 13px;
    font-weight: ${({ $current }) => ($current ? 600 : 400)};
    color: ${({ theme, $current }) => ($current ? theme.palette.text.primary : theme.palette.secondary.main)};
    max-width: 220px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    display: block;
  }

  &&:hover {
    background: rgba(57, 140, 191, 0.1);
  }
`;

const RootButton = styled(CrumbButton)`
  && {
    display: inline-flex;
    gap: 6px;
    margin-left: -6px;
    max-width: none;
  }
`;

/** Volume type › volume › folders. Long paths collapse their middle into an ellipsis. */
export const Breadcrumb: FC<Props> = ({ route, onRoot, onPath }) => {
  const typeLabel = route.volumeType === VolumeType.Datavolume ? 'Data volumes' : 'User volumes';
  const typeIcon = route.volumeType === VolumeType.Datavolume ? 'database' : 'folder';

  const folders = pathSegments(route.path);
  const collapsed = folders.length > MAX_FOLDER_CRUMBS + 1;
  const visibleFolders: Crumb[] = collapsed
    ? [{ label: '…', path: folders[folders.length - MAX_FOLDER_CRUMBS - 1].path }, ...folders.slice(-MAX_FOLDER_CRUMBS).map((f) => ({ label: f.name, path: f.path }))]
    : folders.map((f) => ({ label: f.name, path: f.path }));
  const crumbs: Crumb[] = route.volume ? [{ label: route.volume.volumeName, path: '' }, ...visibleFolders] : [];

  return (
    <Styled aria-label="Breadcrumb">
      <RootButton onClick={onRoot} disabled={!route.volume} $current={!route.volume}>
        <Icon $size={17} $filled $color="#398CBF">{typeIcon}</Icon>
        {typeLabel}
      </RootButton>
      {crumbs.map((crumb, index) => {
        const last = index === crumbs.length - 1;
        return (
          <span key={`${index}-${crumb.label}`} className="crumb">
            <Icon $size={18} $color="rgba(0,0,0,0.38)">chevron_right</Icon>
            <CrumbButton onClick={() => onPath(crumb.path ?? '')} disabled={last} aria-current={last ? 'page' : undefined} $current={last}>
              {crumb.label}
            </CrumbButton>
          </span>
        );
      })}
    </Styled>
  );
};
