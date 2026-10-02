import { describe, expect, it } from 'vitest';

import { VolumeRefInput, VolumeType } from '../generated/typings';
import { buildFileUrlPath, destinationBody, normalizePath, volumeUrlPart } from './volumePath';

const userVolume: VolumeRefInput = {
  volumeType: VolumeType.Uservolume,
  rootVolumeName: 'Storage',
  owner: 'bob',
  volumeName: 'my vol'
};
const dataVolume: VolumeRefInput = { volumeType: VolumeType.Datavolume, volumeName: 'SDSS DR16' };

describe('volumePath', () => {
  describe('volumeUrlPart', () => {
    it('addresses user volumes as root/owner/name and encodes only the name', () => {
      expect(volumeUrlPart(userVolume)).toBe('Storage/bob/my%20vol');
    });

    it('addresses data volumes by name only', () => {
      expect(volumeUrlPart(dataVolume)).toBe('SDSS%20DR16');
    });

    it('rejects a user volume without root volume or owner', () => {
      expect(() => volumeUrlPart({ ...userVolume, owner: undefined })).toThrow(/required for user volumes/);
      expect(() => volumeUrlPart({ ...userVolume, rootVolumeName: undefined })).toThrow(/required for user volumes/);
    });
  });

  describe('buildFileUrlPath', () => {
    it('returns just the volume for the root folder', () => {
      expect(buildFileUrlPath(userVolume, '')).toBe('Storage/bob/my%20vol');
      expect(buildFileUrlPath(userVolume, '/')).toBe('Storage/bob/my%20vol');
      expect(buildFileUrlPath(userVolume)).toBe('Storage/bob/my%20vol');
    });

    it('encodes every path segment and the file name', () => {
      expect(buildFileUrlPath(userVolume, '/a b//c', 'f#1?.txt'))
        .toBe('Storage/bob/my%20vol/a%20b/c/f%231%3F.txt');
    });

    it('does not double-encode already encoded characters in names', () => {
      expect(buildFileUrlPath(dataVolume, '/100%', 'x')).toBe('SDSS%20DR16/100%25/x');
    });
  });

  describe('normalizePath', () => {
    it.each([
      ['', ''],
      ['/', ''],
      ['a/b', '/a/b'],
      ['/a//b/', '/a/b']
    ])('normalizes %j to %j', (input, expected) => {
      expect(normalizePath(input)).toBe(expected);
    });

    it('treats null and undefined as the volume root', () => {
      expect(normalizePath(null)).toBe('');
      expect(normalizePath(undefined)).toBe('');
    });
  });

  describe('destinationBody', () => {
    it('fills the user volume fields and nulls the data volume field', () => {
      expect(destinationBody(userVolume, '/x')).toEqual({
        destinationPath: '/x',
        destinationRootVolume: 'Storage',
        destinationUserVolume: 'my vol',
        destinationDataVolume: null,
        destinationOwnerName: 'bob',
        destinationFileService: null
      });
    });

    it('fills the data volume field and nulls the user volume fields', () => {
      expect(destinationBody(dataVolume, '/x')).toEqual({
        destinationPath: '/x',
        destinationRootVolume: null,
        destinationUserVolume: null,
        destinationDataVolume: 'SDSS DR16',
        destinationOwnerName: null,
        destinationFileService: null
      });
    });
  });
});
