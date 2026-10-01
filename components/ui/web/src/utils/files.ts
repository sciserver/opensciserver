import { VolumeRefInput, VolumeType } from '../graphql/typings';

export type FileVolumeRoute = {
  volumeType: VolumeType;
  volumeName: string;
  rootVolumeName?: string | null;
  owner?: string | null;
};

/** Builds the GraphQL volume reference. User volumes need rootVolumeName and owner. */
export const toVolumeRef = (volume: FileVolumeRoute): VolumeRefInput => ({
  volumeType: volume.volumeType,
  volumeName: volume.volumeName,
  ...(volume.volumeType === VolumeType.Uservolume
    ? { rootVolumeName: volume.rootVolumeName, owner: volume.owner }
    : {})
});

/** Normalises a folder path to '' (volume root) or '/a/b'. */
export const normalizePath = (path?: string | null): string => {
  const parts = (path || '').split('/').filter((part) => part.length > 0);
  return parts.length ? `/${parts.join('/')}` : '';
};

export const joinPath = (path: string, name: string): string => normalizePath(`${path}/${name}`);

export const parentPath = (path: string): string => {
  const parts = normalizePath(path).split('/').filter(Boolean);
  return parts.length > 1 ? `/${parts.slice(0, -1).join('/')}` : '';
};

/** One entry per path segment, for breadcrumbs. */
export const pathSegments = (path: string): { name: string; path: string }[] => {
  const parts = normalizePath(path).split('/').filter(Boolean);
  return parts.map((name, index) => ({ name, path: `/${parts.slice(0, index + 1).join('/')}` }));
};

/**
 * Returns `name` if free, otherwise the first free "name (N)". Like the old dashboard,
 * the suffix goes after the whole name, extension included ("a.txt" -> "a.txt (1)").
 */
export const resolveFreeName = (name: string, existingNames: Iterable<string>): string => {
  const taken = new Set(existingNames);
  let candidate = name;
  let counter = 1;
  while (taken.has(candidate)) {
    candidate = `${name} (${counter})`;
    counter += 1;
  }
  return candidate;
};
