/* eslint-disable import/no-cycle */
import { VolumeRefInput, VolumeType } from '../generated/typings';

const segments = (path?: string | null): string[] => (path || '').split('/').filter(part => part.length > 0);

// Path inside a volume as sent in request bodies: '' for the volume root, otherwise '/a/b'
export const normalizePath = (path?: string | null): string => segments(path).map(part => `/${part}`).join('');

// Volume portion of a fileservice URL. User volumes are addressed as
// {rootVolume}/{owner}/{userVolume}, data volumes just by their name.
export const volumeUrlPart = (volume: VolumeRefInput): string => {
  if (volume.volumeType === VolumeType.Uservolume) {
    if (!volume.rootVolumeName || !volume.owner) {
      throw new Error('rootVolumeName and owner are required for user volumes');
    }
    return `${volume.rootVolumeName}/${volume.owner}/${encodeURIComponent(volume.volumeName)}`;
  }
  return encodeURIComponent(volume.volumeName);
};

// {volume}/{path}/{name} with every path segment URL encoded
export const buildFileUrlPath = (volume: VolumeRefInput, path?: string | null, name?: string | null): string => {
  const parts = [...segments(path), ...(name ? [name] : [])].map(encodeURIComponent);
  return [volumeUrlPart(volume), ...parts].join('/');
};

// Destination fields the fileservice expects when moving, copying or renaming
export const destinationBody = (volume: VolumeRefInput, path: string) => {
  const isUserVolume = volume.volumeType === VolumeType.Uservolume;
  return {
    destinationPath: path,
    destinationRootVolume: isUserVolume ? volume.rootVolumeName : null,
    destinationUserVolume: isUserVolume ? volume.volumeName : null,
    destinationDataVolume: isUserVolume ? null : volume.volumeName,
    destinationOwnerName: isUserVolume ? volume.owner : null,
    destinationFileService: null
  };
};
