import { describe, expect, it } from 'vitest';
import { VolumeType, PrincipalType } from '../graphql/typings';
import { dataVolumeRows, filesRouteQuery, filterAndSortRows, isSharedByOwner, parseFilesRoute, sameVolume, userVolumeRows, workspacePath, creatableRootVolumes, validateVolumeName } from './fileVolumes';

const userVolume = (name: string, owner: string, rootVolumeName: string, allowedActions: string[], shared = false) => ({
  id: name, name, owner, rootVolumeName, allowedActions, resourceUUID: name,
  sharedWith: shared ? [{ id: '1', name: 'x', type: PrincipalType.User, allowedActions: ['read'] }] : []
});

describe('parseFilesRoute', () => {
  it('defaults to the user volume list', () => {
    expect(parseFilesRoute({})).toEqual({ volumeType: VolumeType.Uservolume, path: '' });
  });

  it('reads a user volume with a path', () => {
    expect(parseFilesRoute({ type: 'user', volume: 'persistent', root: 'Storage', owner: 'me', path: 'a//b' })).toEqual({
      volumeType: VolumeType.Uservolume,
      volume: { volumeName: 'persistent', rootVolumeName: 'Storage', owner: 'me' },
      path: '/a/b'
    });
  });

  it('ignores a user volume missing its root or owner', () => {
    expect(parseFilesRoute({ volume: 'persistent', owner: 'me' }).volume).toBeUndefined();
  });

  it('reads a data volume by name only', () => {
    expect(parseFilesRoute({ type: 'data', volume: 'SDSS' })).toEqual({ volumeType: VolumeType.Datavolume, volume: { volumeName: 'SDSS' }, path: '' });
  });

  it('round-trips through filesRouteQuery', () => {
    const route = parseFilesRoute({ type: 'user', volume: 'v', root: 'Storage', owner: 'me', path: '/a' });
    expect(parseFilesRoute(filesRouteQuery(route))).toEqual(route);
  });
});

describe('volume rows', () => {
  const rows = userVolumeRows([{
    userVolumes: [
      userVolume('persistent', 'me', 'Storage', ['read', 'write'], true),
      userVolume('theirs', 'other', 'Storage', ['read'], true)
    ] 
  }], 'me');

  it('flags writable and shared-by-me volumes', () => {
    expect(rows.map((r) => [r.name, r.writable, r.shared])).toEqual([['persistent', true, true], ['theirs', false, false]]);
  });

  it('only counts sharing for the owner', () => {
    expect(isSharedByOwner(userVolume('v', 'other', 'Storage', [], true), 'me')).toBe(false);
  });

  it('treats data volumes as read-only even when the server says writable', () => {
    const [row] = dataVolumeRows([{ id: '1', name: 'sdss', displayName: 'SDSS', description: 'd', writable: true, allowedActions: ['read', 'write'], publisherDID: '', racmUUID: '', sharedWith: [] }]);
    expect(row).toMatchObject({ name: 'SDSS', rootVolume: 'Read-only', writable: false });
  });

  it('filters by name and sorts', () => {
    expect(filterAndSortRows(rows, 'PERS', 'name', 1).map((r) => r.name)).toEqual(['persistent']);
    expect(filterAndSortRows(rows, '', 'name', -1).map((r) => r.name)).toEqual(['theirs', 'persistent']);
  });
});

describe('workspacePath', () => {
  it('maps user and data volumes to their container mount', () => {
    const user = parseFilesRoute({ type: 'user', volume: 'persistent', root: 'Storage', owner: 'me', path: '/a/b' });
    expect(workspacePath(user)).toBe('/home/idies/workspace/Storage/me/persistent/a/b');
    expect(workspacePath(parseFilesRoute({ type: 'data', volume: 'sdss' }))).toBe('/home/idies/workspace/sdss');
    expect(workspacePath(parseFilesRoute({}))).toBe('');
  });

  it('compares volumes by name, root and owner', () => {
    expect(sameVolume({ volumeName: 'v', owner: 'a', rootVolumeName: 'Storage' }, { volumeName: 'v', owner: 'b', rootVolumeName: 'Storage' })).toBe(false);
  });
});

describe('volume management helpers', () => {
  it('flags owned volumes and the ones the server lets you delete', () => {
    const rows = userVolumeRows([{
      userVolumes: [
        userVolume('mine', 'me', 'Storage', ['read', 'write', 'delete']),
        userVolume('persistent', 'me', 'Storage', ['read', 'write']),
        userVolume('theirs', 'other', 'Storage', ['read', 'delete'])
      ] 
    }], 'me');
    expect(rows.map((r) => [r.name, r.owned, r.canDelete])).toEqual([['mine', true, true], ['persistent', true, false], ['theirs', false, true]]);
  });

  it('lists only root volumes you can create in, sorted', () => {
    const roots = [{ name: 'Temporary', allowedActions: ['create'] }, { name: 'Workspace', allowedActions: ['read'] }, { name: 'Storage', allowedActions: ['create', 'read'] }];
    expect(creatableRootVolumes(roots).map((r) => r.name)).toEqual(['Storage', 'Temporary']);
  });

  it('validates volume names', () => {
    expect(validateVolumeName(' ')).toMatch(/empty/);
    expect(validateVolumeName('a/b')).toMatch(/contain/);
    expect(validateVolumeName('x', ['x'])).toMatch(/already/);
    expect(validateVolumeName('x', ['x'], 'x')).toBe('');
  });
});
