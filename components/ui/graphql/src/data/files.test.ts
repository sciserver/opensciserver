import { describe, expect, it } from 'vitest';

import { FileLocationInput, VolumeRefInput, VolumeType } from '../generated/typings';
import { createMockFetch, dataSourceOptions, TEST_TOKEN } from '../test/mockFetch';
import { FilesAPI } from './files';

const BASE = 'https://files.test/api/';

const userVolume: VolumeRefInput = {
  volumeType: VolumeType.Uservolume,
  rootVolumeName: 'Storage',
  owner: 'bob',
  volumeName: 'my vol'
};
const dataVolume: VolumeRefInput = { volumeType: VolumeType.Datavolume, volumeName: 'dv' };

const setup = (...replies: Parameters<typeof createMockFetch>) => {
  const mock = createMockFetch(...replies);
  return { api: new FilesAPI(dataSourceOptions(mock.fetch)), requests: mock.requests };
};

describe('FilesAPI', () => {
  describe('getJsonTree', () => {
    const tree = {
      queryPath: '/a',
      root: {
        name: 'a',
        creationTime: '2026-01-01T00:00:00Z',
        lastModified: '2026-01-02T00:00:00Z',
        folders: [{ name: 'sub', creationTime: 'c', lastModified: 'm', ignored: true }],
        files: [{ name: 'f.txt', size: 12, creationTime: 'c', lastModified: 'm', ignored: true }]
      }
    };

    it('requests level 2 of the subfolder with the auth token', async () => {
      const { api, requests } = setup({ body: tree });
      await api.getJsonTree(userVolume, '/a b');

      expect(requests()).toHaveLength(1);
      expect(requests()[0].method).toBe('GET');
      expect(requests()[0].url).toBe(`${BASE}jsontree/Storage/bob/my%20vol/a%20b/?level=2`);
      expect(requests()[0].headers['x-auth-token'] ?? requests()[0].headers['X-Auth-Token']).toBe(TEST_TOKEN);
    });

    it('maps the response, dropping fields the schema does not expose', async () => {
      const { api } = setup({ body: tree });
      const result = await api.getJsonTree(dataVolume, '');

      expect(result).toEqual({
        queryPath: '/a',
        root: {
          name: 'a',
          creationTime: '2026-01-01T00:00:00Z',
          lastModified: '2026-01-02T00:00:00Z',
          folders: [{ name: 'sub', creationTime: 'c', lastModified: 'm' }],
          files: [{ name: 'f.txt', size: 12, creationTime: 'c', lastModified: 'm' }]
        }
      });
    });

    it('uses a raw path as-is for callers that already hold one', async () => {
      const { api, requests } = setup({ body: tree });
      await api.getJsonTreeByPath('jobs/results');

      expect(requests()[0].url).toBe(`${BASE}jsontree/jobs/results/?level=2`);
    });
  });

  describe('mutations', () => {
    it('createFolder PUTs to folder/', async () => {
      const { api, requests } = setup({});
      await expect(api.createFolder(userVolume, '/a', 'new folder')).resolves.toBe(true);

      expect(requests()[0].method).toBe('PUT');
      expect(requests()[0].url).toBe(`${BASE}folder/Storage/bob/my%20vol/a/new%20folder`);
    });

    it('deleteFile DELETEs from data/', async () => {
      const { api, requests } = setup({});
      await expect(api.deleteFile(dataVolume, '/a', 'old.txt')).resolves.toBe(true);

      expect(requests()[0].method).toBe('DELETE');
      expect(requests()[0].url).toBe(`${BASE}data/dv/a/old.txt`);
    });

    it('renameFile moves within the same volume without copying', async () => {
      const { api, requests } = setup({});
      await api.renameFile(userVolume, '/a', 'old.txt', 'new.txt');

      const [request] = requests();
      expect(request.method).toBe('PUT');
      expect(request.url).toBe(`${BASE}data/Storage/bob/my%20vol/a/old.txt?replaceExisting=false&doCopy=false`);
      expect(request.body).toEqual({
        destinationPath: '/a/new.txt',
        destinationRootVolume: 'Storage',
        destinationUserVolume: 'my vol',
        destinationDataVolume: null,
        destinationOwnerName: 'bob',
        destinationFileService: null
      });
    });

    const source: FileLocationInput = { volume: userVolume, path: '/src' };
    const destination: FileLocationInput = { volume: dataVolume, path: '' };

    it('moveFile sends doCopy=false and the destination volume', async () => {
      const { api, requests } = setup({});
      await api.moveFile(source, 'f.txt', destination);

      const [request] = requests();
      expect(request.url).toBe(`${BASE}data/Storage/bob/my%20vol/src/f.txt?replaceExisting=false&doCopy=false`);
      expect(request.body).toMatchObject({
        destinationPath: '/f.txt',
        destinationDataVolume: 'dv',
        destinationUserVolume: null,
        destinationRootVolume: null
      });
    });

    it('copyFile sends doCopy=true and uses newName as the destination name', async () => {
      const { api, requests } = setup({});
      await api.copyFile(source, 'f.txt', { volume: dataVolume, path: '/dest' }, 'f.txt (1)');

      const [request] = requests();
      expect(request.url).toBe(`${BASE}data/Storage/bob/my%20vol/src/f.txt?replaceExisting=false&doCopy=true`);
      expect(request.body).toMatchObject({ destinationPath: '/dest/f.txt (1)', destinationDataVolume: 'dv' });
    });
  });

  it('surfaces a fileservice error instead of returning success', async () => {
    const { api } = setup({ status: 409, body: { status: 'error', error: 'exists' } });

    await expect(api.copyFile({ volume: userVolume, path: '' }, 'f', { volume: userVolume, path: '' }))
      .rejects.toThrow(/409/);
  });
});
