import { DataVolume, UserVolume, VolumeType } from './../graphql/typings';
import { FileVolumeRoute, isMoveIntoItself, normalizePath } from './files';

export type FilesRoute = {
  volumeType: VolumeType;
  /** Undefined at the top level, where the volumes are listed. */
  volume?: Omit<FileVolumeRoute, 'volumeType'>;
  path: string;
};

type Query = Record<string, string | string[] | undefined>;

const first = (value: string | string[] | undefined): string | undefined => (Array.isArray(value) ? value[0] : value);

/** Reads the Files page location from the URL query. Unknown values fall back to the user volume list. */
export const parseFilesRoute = (query: Query): FilesRoute => {
  const volumeType = first(query.type) === 'data' ? VolumeType.Datavolume : VolumeType.Uservolume;
  const name = first(query.volume);
  if (!name) {
    return { volumeType, path: '' };
  }

  const owner = first(query.owner);
  const rootVolumeName = first(query.root);
  // A user volume is only addressable with its root volume and owner.
  if (volumeType === VolumeType.Uservolume && (!owner || !rootVolumeName)) {
    return { volumeType, path: '' };
  }

  return {
    volumeType,
    volume: volumeType === VolumeType.Uservolume ? { volumeName: name, rootVolumeName, owner } : { volumeName: name },
    path: normalizePath(first(query.path))
  };
};

/** Inverse of parseFilesRoute. */
export const filesRouteQuery = (route: FilesRoute): Record<string, string> => ({
  type: route.volumeType === VolumeType.Datavolume ? 'data' : 'user',
  ...(route.volume ? { volume: route.volume.volumeName } : {}),
  ...(route.volume?.rootVolumeName ? { root: route.volume.rootVolumeName } : {}),
  ...(route.volume?.owner ? { owner: route.volume.owner } : {}),
  ...(route.volume && route.path ? { path: route.path } : {})
});

export const hasAction = (allowedActions: (string | null)[] | undefined, action: string): boolean => (
  !!allowedActions?.some((allowed) => allowed?.toLowerCase() === action)
);

export const isUserVolumeWritable = (volume: Pick<UserVolume, 'allowedActions'>): boolean => hasAction(volume.allowedActions, 'write');

/** A user volume the signed-in user owns and has shared with others. */
export const isSharedByOwner = (volume: Pick<UserVolume, 'owner' | 'sharedWith'>, userName?: string): boolean => (
  !!userName && volume.owner === userName && volume.sharedWith.length > 0
);

export type VolumeRow = {
  key: string;
  route: FileVolumeRoute;
  name: string;
  /** "Storage", "Temporary" or "Read-only". */
  rootVolume: string;
  /** Owner for user volumes, description for data volumes. */
  detail: string;
  writable: boolean;
  shared: boolean;
  description: string;
  /** The signed-in user owns this user volume, so they can edit it. */
  owned: boolean;
  /** The server lets the user delete this volume. */
  canDelete: boolean;
  /** The user may change who the volume is shared with. */
  canShare: boolean;
};

export type SortKey = 'name' | 'rootVolume' | 'detail';

export const userVolumeRows = (rootVolumes: { userVolumes: UserVolume[] }[], userName?: string): VolumeRow[] => (
  rootVolumes.flatMap((root) => root.userVolumes).map((volume) => ({
    key: `${volume.rootVolumeName}/${volume.owner}/${volume.name}`,
    route: { volumeType: VolumeType.Uservolume, volumeName: volume.name, rootVolumeName: volume.rootVolumeName, owner: volume.owner },
    name: volume.name,
    rootVolume: volume.rootVolumeName,
    detail: volume.owner,
    writable: isUserVolumeWritable(volume),
    shared: isSharedByOwner(volume, userName),
    description: volume.description || '',
    owned: !!userName && volume.owner === userName,
    canDelete: hasAction(volume.allowedActions, 'delete'),
    canShare: hasAction(volume.allowedActions, 'grant') || (!!userName && volume.owner === userName)
  }))
);

export const dataVolumeRows = (dataVolumes: DataVolume[]): VolumeRow[] => dataVolumes.map((volume) => ({
  key: volume.name,
  route: { volumeType: VolumeType.Datavolume, volumeName: volume.name },
  name: volume.displayName || volume.name,
  // Data volumes are read-only in the UI, whatever allowedActions says.
  rootVolume: 'Read-only',
  detail: volume.description,
  writable: false,
  shared: false,
  description: volume.description,
  owned: false,
  canDelete: false,
  canShare: false
}));

export const filterAndSortRows = (rows: VolumeRow[], filter: string, sortKey: SortKey, direction: 1 | -1): VolumeRow[] => {
  const needle = filter.trim().toLowerCase();
  const compare = (a: VolumeRow, b: VolumeRow) => a[sortKey].toLowerCase().localeCompare(b[sortKey].toLowerCase()) * direction;
  return rows.filter((row) => !needle || row.name.toLowerCase().includes(needle)).sort(compare);
};

export const DEFAULT_FILES_ROUTE: FilesRoute = { volumeType: VolumeType.Uservolume, path: '' };

export const sameVolume = (a: Omit<FileVolumeRoute, 'volumeType'>, b: Omit<FileVolumeRoute, 'volumeType'>): boolean => (
  a.volumeName === b.volumeName && (a.owner || '') === (b.owner || '') && (a.rootVolumeName || '') === (b.rootVolumeName || '')
);

/**
 * Where the route is mounted inside a compute container, e.g. /home/idies/workspace/Storage/me/persistent/a.
 * '' at the top level. This is the same convention the old dashboard used for its copy-path buttons
 * (dashboard NotebookTab.vue): user volumes as root/owner/name, data volumes by name.
 */
export const workspacePath = (route: FilesRoute): string => {
  if (!route.volume) {
    return '';
  }
  const { volumeName, rootVolumeName, owner } = route.volume;
  const base = route.volumeType === VolumeType.Uservolume
    ? `/home/idies/workspace/${rootVolumeName}/${owner}/${volumeName}`
    : `/home/idies/workspace/${volumeName}`;
  return `${base}${route.path}`;
};

/** Root volumes the user may create volumes in (the dashboard required the 'create' action), by name. */
export const creatableRootVolumes = <T extends { name?: string | null; allowedActions: (string | null)[] }>(rootVolumes: T[]): T[] => (
  rootVolumes.filter((root) => hasAction(root.allowedActions, 'create')).sort((a, b) => (a.name || '').localeCompare(b.name || ''))
);

/** Why a user volume name can't be used, or '' when it is fine. */
export const validateVolumeName = (name: string, existingNames: Iterable<string> = [], own?: string): string => {
  const trimmed = name.trim();
  if (!trimmed) {
    return 'Name can’t be empty';
  }
  if (trimmed.includes('/')) {
    return 'User volume name can’t contain /';
  }
  if (trimmed !== own && new Set(existingNames).has(trimmed)) {
    return 'You already have a volume with this name in that root volume';
  }
  return '';
};

/**
 * Why `names` (in the `source` folder) can't be copied or moved into `destination`, or '' when they can.
 * Copying into the same folder is fine (the copy gets a "(1)" name); moving there is a no-op.
 */
export const transferBlocker = (
  kind: 'copy' | 'move',
  source: Required<Pick<FilesRoute, 'volume'>> & FilesRoute,
  destination: Required<Pick<FilesRoute, 'volume'>> & FilesRoute,
  names: string[]
): string => {
  const sameVol = source.volumeType === destination.volumeType && sameVolume(source.volume, destination.volume);
  if (kind === 'move' && sameVol && source.path === destination.path) {
    return 'Those items are already in this folder. Choose a different destination.';
  }
  if (sameVol && names.some((name) => isMoveIntoItself(source.path, name, destination.path))) {
    return `A folder can’t be ${kind === 'copy' ? 'copied' : 'moved'} into itself. Choose a different destination.`;
  }
  return '';
};
