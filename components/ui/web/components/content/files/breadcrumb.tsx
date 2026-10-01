import { FC } from 'react';
import { Box, ButtonBase } from '@mui/material';
import { VolumeType } from 'src/graphql/typings';
import { FilesRoute } from 'src/utils/fileVolumes';
import { pathSegments } from 'src/utils/files';

type Props = {
  route: FilesRoute;
  /** Go to the volume list of the current type. */
  onRoot: () => void;
  /** Go to a folder of the current volume ('' is the volume root). */
  onPath: (path: string) => void;
};

const MAX_FOLDER_CRUMBS = 3;

type Crumb = { label: string; path?: string };

const Icon: FC<{ name: string }> = ({ name }) => (
  <span className="material-symbols-outlined" style={{ fontSize: 18, color: 'rgba(0,0,0,0.38)' }}>{name}</span>
);

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
    <Box component="nav" aria-label="Breadcrumb" sx={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: '2px', minWidth: 0, flex: 1 }}>
      <ButtonBase
        onClick={onRoot}
        disabled={!route.volume}
        sx={{ gap: 0.75, px: 0.75, py: '3px', ml: -0.75, borderRadius: 1, fontSize: 13, fontWeight: route.volume ? 400 : 600, color: route.volume ? 'secondary.main' : 'text.primary', '&:hover': { bgcolor: 'rgba(57,140,191,0.1)' } }}
      >
        <span className="material-symbols-outlined" style={{ fontSize: 17, color: '#398CBF', fontVariationSettings: "'FILL' 1" }}>{typeIcon}</span>
        {typeLabel}
      </ButtonBase>
      {crumbs.map((crumb, index) => {
        const last = index === crumbs.length - 1;
        return (
          <Box key={`${index}-${crumb.label}`} component="span" sx={{ display: 'inline-flex', alignItems: 'center', gap: '2px', minWidth: 0 }}>
            <Icon name="chevron_right" />
            <ButtonBase
              onClick={() => onPath(crumb.path ?? '')}
              disabled={last}
              aria-current={last ? 'page' : undefined}
              sx={{ px: 0.75, py: '3px', borderRadius: 1, fontSize: 13, fontWeight: last ? 600 : 400, color: last ? 'text.primary' : 'secondary.main', maxWidth: 220, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', display: 'block', '&:hover': { bgcolor: 'rgba(57,140,191,0.1)' } }}
            >
              {crumb.label}
            </ButtonBase>
          </Box>
        );
      })}
    </Box>
  );
};
