import { describe, expect, it } from 'vitest';

import { PrincipalType } from '../generated/typings';
import { createMockFetch, dataSourceOptions } from '../test/mockFetch';
import { VolumesAPI } from './volumes';

const BASE = 'https://files.test/api/';

const setup = (...replies: Parameters<typeof createMockFetch>) => {
  const mock = createMockFetch(...replies);
  return { api: new VolumesAPI(dataSourceOptions(mock.fetch)), requests: mock.requests };
};

describe('VolumesAPI', () => {
  it('createUserVolume PUTs the description and encodes the name', async () => {
    const { api, requests } = setup({});
    await expect(api.createUserVolume('Storage', 'bob', 'my vol', 'notes')).resolves.toBe(true);

    expect(requests()[0]).toMatchObject({
      method: 'PUT',
      url: `${BASE}volume/Storage/bob/my%20vol`,
      body: { description: 'notes' }
    });
  });

  it('updateUserVolume PATCHes the new name and description', async () => {
    const { api, requests } = setup({});
    await api.updateUserVolume('Storage', 'bob', 'old', 'new', null);

    expect(requests()[0]).toMatchObject({
      method: 'PATCH',
      url: `${BASE}volume/Storage/bob/old`,
      body: { name: 'new', description: '' }
    });
  });

  it('deleteUserVolume DELETEs with a trailing slash', async () => {
    const { api, requests } = setup({});
    await api.deleteUserVolume('Storage', 'bob', 'my vol');

    expect(requests()[0]).toMatchObject({ method: 'DELETE', url: `${BASE}volume/Storage/bob/my%20vol/` });
  });

  it('shareUserVolume PATCHes the full list with numeric ids', async () => {
    const { api, requests } = setup({});
    await api.shareUserVolume('Storage', 'bob', 'vol', [
      { id: '12', name: 'alice', type: PrincipalType.User, allowedActions: ['read', 'write'] },
      { id: '7', name: 'lab', type: PrincipalType.Group, allowedActions: [] }
    ]);

    expect(requests()[0]).toMatchObject({
      method: 'PATCH',
      url: `${BASE}share/Storage/bob/vol/`,
      body: [
        { id: 12, name: 'alice', type: 'USER', allowedActions: ['read', 'write'] },
        { id: 7, name: 'lab', type: 'GROUP', allowedActions: [] }
      ]
    });
  });

  it('getFileUsage maps rows and defaults missing byte counts to 0', async () => {
    const { api, requests } = setup({
      body: [
        { rootVolumeId: 1, userVolumeId: 5, type: 'USERVOLUME', numberOfBytesUsed: 10, numberOfBytesQuota: 100 },
        { rootVolumeId: 1, username: 'bob', type: 'ROOTVOLUME' }
      ]
    });
    const usage = await api.getFileUsage();

    expect(requests()[0].url).toBe(`${BASE}usage`);
    expect(usage[0]).toMatchObject({ userVolumeId: 5, numberOfBytesUsed: 10, numberOfBytesQuota: 100 });
    expect(usage[1]).toMatchObject({ username: 'bob', numberOfBytesUsed: 0, numberOfBytesQuota: 0 });
  });

  it('getVolumes returns sharedWith as objects and defaults it to an empty list', async () => {
    const { api } = setup({
      body: {
        identifier: 'fs1',
        name: 'FS',
        rootVolumes: [{
          id: 1,
          resourceUUID: 'r',
          name: 'Storage',
          containsSharedVolumes: true,
          allowedActions: ['create'],
          userVolumes: [
            { id: 2, name: 'a', owner: 'bob', sharedWith: [{ id: 9, name: 'alice', type: 'USER', allowedActions: ['read'] }] },
            { id: 3, name: 'b', owner: 'bob' }
          ]
        }],
        dataVolumes: []
      }
    });
    const fileService = await api.getVolumes();
    const [shared, unshared] = fileService.rootVolumes![0].userVolumes;

    expect(shared.sharedWith).toEqual([{ id: 9, name: 'alice', type: 'USER', allowedActions: ['read'] }]);
    expect(unshared.sharedWith).toEqual([]);
    expect(shared.rootVolumeName).toBe('Storage');
  });
});
