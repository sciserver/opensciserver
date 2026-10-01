import { describe, expect, it } from 'vitest';
import { VolumeType } from '../graphql/typings';
import { joinPath, normalizePath, parentPath, pathSegments, resolveFreeName, toVolumeRef } from './files';

describe('toVolumeRef', () => {
  it('keeps root volume and owner for user volumes', () => {
    expect(toVolumeRef({ volumeType: VolumeType.Uservolume, volumeName: 'scratch', rootVolumeName: 'Storage', owner: 'ana' }))
      .toEqual({ volumeType: VolumeType.Uservolume, volumeName: 'scratch', rootVolumeName: 'Storage', owner: 'ana' });
  });

  it('drops root volume and owner for data volumes', () => {
    expect(toVolumeRef({ volumeType: VolumeType.Datavolume, volumeName: 'sdss', rootVolumeName: 'x', owner: 'y' }))
      .toEqual({ volumeType: VolumeType.Datavolume, volumeName: 'sdss' });
  });
});

describe('paths', () => {
  it('normalizes to "" or "/a/b"', () => {
    expect(normalizePath(undefined)).toBe('');
    expect(normalizePath('/')).toBe('');
    expect(normalizePath('a//b/')).toBe('/a/b');
  });

  it('joins and finds the parent', () => {
    expect(joinPath('', 'a')).toBe('/a');
    expect(joinPath('/a', 'b')).toBe('/a/b');
    expect(parentPath('/a/b')).toBe('/a');
    expect(parentPath('/a')).toBe('');
    expect(parentPath('')).toBe('');
  });

  it('builds breadcrumb segments', () => {
    expect(pathSegments('/a/b')).toEqual([{ name: 'a', path: '/a' }, { name: 'b', path: '/a/b' }]);
    expect(pathSegments('')).toEqual([]);
  });
});

describe('resolveFreeName', () => {
  it('returns the name when free', () => {
    expect(resolveFreeName('a.txt', ['b.txt'])).toBe('a.txt');
  });

  it('appends (N) after the whole name, like the old dashboard', () => {
    expect(resolveFreeName('a.txt', ['a.txt'])).toBe('a.txt (1)');
    expect(resolveFreeName('a.txt', ['a.txt', 'a.txt (1)', 'a.txt (2)'])).toBe('a.txt (3)');
  });
});
