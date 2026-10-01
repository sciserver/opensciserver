import { UserVolume } from 'src/graphql/typings';

// The scratch and persistent User Volumes are auto-injected into every container
// regardless of whether they were explicitly requested, so they must be excluded
// when matching requested User Volumes against a user's own default ones.
// Mirrors getDefaultUserVolumeIds in components/ui/graphql/src/utils/userVolumes.ts.
export const getDefaultUserVolumeIds = (userVolumes: UserVolume[], userName: string): string[] => userVolumes
  .filter(uv => (
    (uv.name === 'persistent' && uv.rootVolumeName === 'Storage') ||
    (uv.name === 'scratch' && uv.rootVolumeName === 'Temporary'))
    && uv.owner === userName)
  .map(uv => uv.id.toString());
