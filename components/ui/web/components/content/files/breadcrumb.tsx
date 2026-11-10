import { FC } from 'react';
import styled from 'styled-components';
import { ButtonBase } from '@mui/material';
import { VolumeType } from 'src/graphql/typings';
import { FilesRoute } from 'src/types/files';
import { BREADCRUMB_MAX_CHARS } from 'src/config/files';
import { fitPathSegments, pathSegments } from 'src/utils/files';

import { ACCENT_WASH, AccentIcon, FaintIcon, TEXT, ACCENT } from './filesStyles';

type Props = {
  route: FilesRoute;
  /** Go to the volume list of the current type. */
  onRoot: () => void;
  /** Go to a folder of the current volume ('' is the volume root). */
  onPath: (path: string) => void;
};

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
    color: ${({ $current }) => ($current ? TEXT : ACCENT)};
    max-width: 220px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    display: block;
  }

  &&:hover {
    background: ${ACCENT_WASH};
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

  // The volume name always shows, so it counts against the budget of characters for the folders.
  const budget = Math.max(BREADCRUMB_MAX_CHARS - (route.volume?.volumeName.length ?? 0), 0);
  const { visible, collapsedTo } = fitPathSegments(pathSegments(route.path), budget);
  const visibleFolders: Crumb[] = [
    ...(collapsedTo === undefined ? [] : [{ label: '…', path: collapsedTo }]),
    ...visible.map((folder) => ({ label: folder.name, path: folder.path }))
  ];
  const crumbs: Crumb[] = route.volume ? [{ label: route.volume.volumeName, path: '' }, ...visibleFolders] : [];

  return (
    <Styled aria-label="Breadcrumb">
      <RootButton onClick={onRoot} disabled={!route.volume} $current={!route.volume}>
        <AccentIcon $size={17} $filled>{typeIcon}</AccentIcon>
        {typeLabel}
      </RootButton>
      {crumbs.map((crumb, index) => {
        const last = index === crumbs.length - 1;
        return (
          <span key={`${index}-${crumb.label}`} className="crumb">
            <FaintIcon $size={18}>chevron_right</FaintIcon>
            <CrumbButton onClick={() => onPath(crumb.path ?? '')} disabled={last} aria-current={last ? 'page' : undefined} $current={last}>
              {crumb.label}
            </CrumbButton>
          </span>
        );
      })}
    </Styled>
  );
};
