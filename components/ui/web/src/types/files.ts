import { VolumeRefInput } from '../graphql/typings';
import { VolumeAction } from './sharing';

/** Where the Files screen is: a volume type, optionally a volume in it, and a folder path inside. */
export type FilesRoute = {
  volumeType: VolumeRefInput['volumeType'];
  /** Undefined at the top level, where the volumes are listed. */
  volume?: Omit<VolumeRefInput, 'volumeType'>;
  path: string;
};

/** A parsed URL query, as Next's router gives it. */
/** One folder of a path, with the path that leads to it. */
export type PathSegment = { name: string; path: string };

export type RouteQuery = Record<string, string | string[] | undefined>;

/** One row of the volume list: a user volume or a data volume with the flags the UI needs. */
export type VolumeRow = {
  key: string;
  route: VolumeRefInput;
  name: string;
  /** "Storage" or "Temporary" for user volumes, '' for data volumes. */
  rootVolume: string;
  /** The user volume's owner, '' for data volumes. */
  owner: string;
  description: string;
  /** What the user may do with the volume, from allowedActions, in VOLUME_ACTIONS order. */
  actions: VolumeAction[];
  writable: boolean;
  /** A user volume the signed-in user owns and has shared with others. */
  shared: boolean;
  /** A user volume someone else owns and shared with the signed-in user. */
  sharedWithMe: boolean;
  /** The signed-in user owns this user volume, so they can edit it. */
  owned: boolean;
  /** The server lets the user delete this volume. */
  canDelete: boolean;
  /** The user may change who the volume is shared with. */
  canShare: boolean;
};

export type SortKey = 'name' | 'rootVolume' | 'owner' | 'description';

/** A folder or a file in a listing, in one sortable shape. */
export type FileEntry = {
  name: string;
  isFolder: boolean;
  /** Bytes. 0 for folders. */
  size: number;
  /** Epoch millis, or 0 when the server sent no usable date. */
  modified: number;
};

export type EntrySortKey = 'name' | 'modified' | 'size';

export type UploadHandle = { promise: Promise<void>; abort: () => void };
