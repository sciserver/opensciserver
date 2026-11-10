import { UserVolume } from 'src/graphql/typings';

/**
 * The user's own scratch and persistent volumes. They are auto-injected into every container regardless of whether
 * they were explicitly requested, and they are private: they are never shared.
 */
export const isDefaultUserVolume = (volume: Pick<UserVolume, 'name' | 'rootVolumeName' | 'owner'>, userName: string): boolean => (
  volume.owner === userName
  && ((volume.name === 'persistent' && volume.rootVolumeName === 'Storage') || (volume.name === 'scratch' && volume.rootVolumeName === 'Temporary'))
);

// These must be excluded when matching requested User Volumes against a user's own default ones.
// Mirrors getDefaultUserVolumeIds in components/ui/graphql/src/utils/userVolumes.ts.
export const getDefaultUserVolumeIds = (userVolumes: UserVolume[], userName: string): string[] => userVolumes
  .filter(uv => isDefaultUserVolume(uv, userName))
  .map(uv => uv.id.toString());
