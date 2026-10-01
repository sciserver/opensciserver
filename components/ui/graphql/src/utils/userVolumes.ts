/* eslint-disable import/no-cycle */
import { Domain } from '../generated/typings';

// The scratch and persistent User Volumes are auto-injected into every container
// regardless of whether they were explicitly requested, so they must be excluded
// when matching requested User Volumes against a user's own default ones.
export const getDefaultUserVolumeIds = (domain: Domain, userName: string): string[] => domain.userVolumes
  .filter(r => (
    (r.name === 'persistent' && r.rootVolumeName === 'Storage') ||
    (r.name === 'scratch' && r.rootVolumeName === 'Temporary'))
    && r.owner === userName)
  .map(r => r.id.toString());
