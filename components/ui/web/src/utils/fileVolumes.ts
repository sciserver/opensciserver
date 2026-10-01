import { DataVolume, UserVolume, VolumeType } from './../graphql/typings';
import { FileVolumeRoute, normalizePath } from './files';

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
    shared: isSharedByOwner(volume, userName)
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
  shared: false
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

/** Where the route is mounted inside a compute container, e.g. /home/idies/workspace/Storage/me/persistent/a. '' at the top level. */
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
