import { describe, expect, it } from 'vitest';
import { getDefaultUserVolumeIds, isDefaultUserVolume } from '../utils/userVolumes';

const volume = (id: string, name: string, rootVolumeName: string, owner: string) => ({ id, name, rootVolumeName, owner }) as never;

describe('isDefaultUserVolume', () => {
  it('is the user\'s own persistent in Storage and scratch in Temporary, nothing else', () => {
    expect(isDefaultUserVolume(volume('1', 'persistent', 'Storage', 'me'), 'me')).toBe(true);
    expect(isDefaultUserVolume(volume('2', 'scratch', 'Temporary', 'me'), 'me')).toBe(true);
    expect(isDefaultUserVolume(volume('3', 'persistent', 'Temporary', 'me'), 'me')).toBe(false);
    expect(isDefaultUserVolume(volume('4', 'scratch', 'Storage', 'me'), 'me')).toBe(false);
    expect(isDefaultUserVolume(volume('5', 'persistent', 'Storage', 'other'), 'me')).toBe(false);
    expect(isDefaultUserVolume(volume('6', 'jobs', 'Temporary', 'me'), 'me')).toBe(false);
  });
});

describe('getDefaultUserVolumeIds', () => {
  it('returns the ids of the default volumes as strings', () => {
    const all = [volume('1', 'persistent', 'Storage', 'me'), volume('2', 'scratch', 'Temporary', 'me'), volume('3', 'persistent', 'Storage', 'other')];
    expect(getDefaultUserVolumeIds(all, 'me')).toEqual(['1', '2']);
  });
});
