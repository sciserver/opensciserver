import { BrowserContext, Page, Route } from '@playwright/test';
import { SERVICES } from '../../playwright.config';

type FileEntry = { name: string; size: number };
type Folder = { folders: string[]; files: FileEntry[] };
type SharedWith = { id: string; name: string; type: 'USER' | 'GROUP'; allowedActions: string[] };
type UserVolume = {
  id: string; name: string; owner: string; rootVolumeName: string; description: string; allowedActions: string[]; sharedWith: SharedWith[];
};

export type Call = { operation: string; variables: Record<string, any> };

const typed = <T extends object>(__typename: string, value: T) => ({ __typename, ...value });

/**
 * In-memory stand-in for the GraphQL middleware and the file service. Tests read `calls` to see what the UI sent,
 * and `failNext` to make one operation fail.
 */
export class FakeBackend {
  calls: Call[] = [];

  uploads: string[] = [];

  /** The X-Auth-Token header of every README request, in order. */
  readmeTokens: string[] = [];

  private failures = new Map<string, string>();

  readonly user = { id: '1', userName: 'ana', email: 'ana@example.org', visibility: 'public' };

  volumes: UserVolume[] = [
    { id: '1', name: 'persistent', owner: 'ana', rootVolumeName: 'Storage', description: 'Default volume', allowedActions: ['read', 'write'], sharedWith: [] },
    { id: '2', name: 'FESS', owner: 'ana', rootVolumeName: 'Storage', description: 'Shared project', allowedActions: ['read', 'write', 'delete', 'grant'], sharedWith: [{ id: '7', name: 'bob', type: 'USER', allowedActions: ['read'] }] },
    { id: '3', name: 'NotebookExamples', owner: 'arik', rootVolumeName: 'Storage', description: '', allowedActions: ['read'], sharedWith: [] },
    { id: '4', name: 'scratch', owner: 'ana', rootVolumeName: 'Temporary', description: '', allowedActions: ['read', 'write'], sharedWith: [] }
  ];

  dataVolumes = [
    { id: '10', name: 'sdss_das', displayName: 'SDSS DAS', description: 'Sloan Digital Sky Survey data', allowedActions: ['read'] },
    { id: '11', name: 'gaia_dr3', displayName: 'Gaia DR3', description: 'Gaia data release 3', allowedActions: ['read', 'write'] },
    // Two volumes can share a name (the real list has two "droid-workspace"); only the id tells them apart.
    { id: '12', name: 'droid-workspace', displayName: 'droid-workspace', description: 'Droid workspace', allowedActions: ['read'] },
    { id: '13', name: 'droid-workspace', displayName: 'droid-workspace', description: 'Droid workspace (copy)', allowedActions: ['read'] }
  ];

  directory = [
    { id: '7', name: 'bob', type: 'USER' },
    { id: '8', name: 'carol', type: 'USER' },
    { id: '8', name: 'astro-group', type: 'GROUP' }
  ];

  private tree: Record<string, Folder> = {
    'USERVOLUME:Storage/ana/persistent': {
      folders: ['data', 'notebooks'],
      files: [{ name: 'README.md', size: 40 }, { name: 'results.csv', size: 2048 }, { name: 'a.txt', size: 5 }]
    },
    'USERVOLUME:Storage/ana/persistent/data': { folders: [], files: [{ name: 'cutout.fits', size: 8_100_000 }] },
    'USERVOLUME:Storage/ana/persistent/notebooks': { folders: [], files: [] },
    'USERVOLUME:Storage/ana/FESS': { folders: [], files: [] },
    'USERVOLUME:Storage/arik/NotebookExamples': { folders: ['examples'], files: [{ name: 'intro.ipynb', size: 900 }] },
    'USERVOLUME:Temporary/ana/scratch': { folders: [], files: [] },
    'DATAVOLUME:sdss_das': { folders: ['dr17'], files: [{ name: 'README.txt', size: 100 }] },
    'DATAVOLUME:gaia_dr3': { folders: [], files: [] },
    'DATAVOLUME:droid-workspace': { folders: [], files: [] }
  };

  /** The next call of this operation answers with a GraphQL error. */
  failNext(operation: string, message = 'Something went wrong') {
    this.failures.set(operation, message);
  }

  called(operation: string): Call[] {
    return this.calls.filter((call) => call.operation === operation);
  }

  async install(context: BrowserContext, page: Page) {
    await context.addCookies([{ name: 'portalCookie', value: 'e2e-token', url: 'http://localhost:3200' }]);
    // Fonts, analytics and other third-party hosts are not part of what we test.
    await page.route(/^https?:\/\/(?!localhost)/, (route) => route.abort());
    await page.route(SERVICES.graphql, (route) => this.graphql(route));
    await page.route(`${SERVICES.fileService}**`, (route) => this.fileService(route));
  }

  private keyOf(volume: { volumeType: string; volumeName: string; rootVolumeName?: string; owner?: string }, path = ''): string {
    const head = volume.volumeType === 'USERVOLUME' ? `${volume.rootVolumeName}/${volume.owner}/${volume.volumeName}` : volume.volumeName;
    return `${volume.volumeType}:${head}${path === '/' ? '' : path}`;
  }

  private folder(key: string): Folder {
    if (!this.tree[key]) {
      throw new Error('Not found');
    }
    return this.tree[key];
  }

  private async fileService(route: Route) {
    const request = route.request();
    const url = new URL(request.url());
    const segments = url.pathname.split('/api/file/')[1].split('/').map((segment) => decodeURIComponent(segment));
    if (request.method() === 'PUT') {
      const name = segments[segments.length - 1];
      this.uploads.push(segments.join('/'));
      const key = `USERVOLUME:${segments.slice(0, -1).join('/')}`;
      this.tree[key]?.files.push({ name, size: request.postDataBuffer()?.length ?? 0 });
      await route.fulfill({ status: 200, body: '' });
      return;
    }
    this.readmeTokens.push(request.headers()['x-auth-token'] ?? '');
    await route.fulfill({ status: 200, contentType: 'text/plain', body: '# Project notes\n\nHello from the README.' });
  }

  private async graphql(route: Route) {
    const { operationName, variables = {} } = route.request().postDataJSON();
    this.calls.push({ operation: operationName, variables });
    const failure = this.failures.get(operationName);
    if (failure) {
      this.failures.delete(operationName);
      await route.fulfill({ json: { data: null, errors: [{ message: failure }] } });
      return;
    }
    try {
      await route.fulfill({ json: { data: this.resolve(operationName, variables) } });
    }
    catch (error) {
      await route.fulfill({ json: { data: null, errors: [{ message: (error as Error).message }] } });
    }
  }

  private userVolume(v: UserVolume) {
    return typed('UserVolume', { ...v, sharedWith: v.sharedWith.map((s) => typed('SharedWith', s)) });
  }

  private resolve(operation: string, v: Record<string, any>): unknown {
    switch (operation) {
      case 'GetUser': {
        return { getUser: typed('User', this.user) };
      }
      case 'GetDomains': {
        return {
          getDomains: [typed('Domain', {
            id: '1',
            name: 'Small Jobs Domain',
            apiEndpoint: 'http://localhost:3100/compute',
            description: 'Shared systems for small jobs',
            images: [typed('Image', { id: '1', name: 'SciServer Essentials 4.0', description: 'Python, R and Julia' })],
            userVolumes: this.volumes.map((x) => typed('UserVolume', { id: x.id, name: x.name, owner: x.owner, description: x.description, rootVolumeName: x.rootVolumeName, allowedActions: x.allowedActions })),
            dataVolumes: []
          })]
        };
      }
      case 'fileVolumes':
      case 'sharingDetails':
      case 'fileQuotas': {
        return this.volumesPayload(operation);
      }
      case 'jsonTree': {
        const found = this.folder(this.keyOf(v.volume, v.path || ''));
        return {
          getJsonTree: typed('JSONTree', {
            queryPath: v.path || '/',
            root: typed('Root', {
              folders: found.folders.map((name) => typed('Folder', { name, lastModified: '2026-01-21T15:22:00Z' })),
              files: found.files.map((file) => typed('File', { ...file, lastModified: '2026-02-03T10:00:00Z' }))
            })
          })
        };
      }
      case 'createFolder': {
        const parent = this.folder(this.keyOf(v.volume, v.path));
        parent.folders.push(v.name);
        this.tree[this.keyOf(v.volume, `${v.path}/${v.name}`)] = { folders: [], files: [] };
        return { createFolder: true };
      }
      case 'renameFile': {
        const parent = this.folder(this.keyOf(v.volume, v.path));
        parent.folders = parent.folders.map((name) => (name === v.name ? v.newName : name));
        parent.files = parent.files.map((file) => (file.name === v.name ? { ...file, name: v.newName } : file));
        return { renameFile: true };
      }
      case 'deleteFile': {
        const parent = this.folder(this.keyOf(v.volume, v.path));
        parent.folders = parent.folders.filter((name) => name !== v.name);
        parent.files = parent.files.filter((file) => file.name !== v.name);
        return { deleteFile: true };
      }
      case 'copyFile':
      
      case 'moveFile': {
        const from = this.folder(this.keyOf(v.source.volume, v.source.path));
        const to = this.folder(this.keyOf(v.destination.volume, v.destination.path));
        const target = v.newName || v.name;
        const file = from.files.find((f) => f.name === v.name);
        if (file) {
          to.files.push({ ...file, name: target });
        }
        else {
          to.folders.push(target);
        }
        if (operation === 'moveFile') {
          from.files = from.files.filter((f) => f.name !== v.name);
          from.folders = from.folders.filter((name) => name !== v.name);
        }
        return { [operation]: true };
      }
      case 'createUserVolume': {
        this.volumes.push({ id: String(100 + this.volumes.length), name: v.name, owner: v.owner, rootVolumeName: v.rootVolumeName, description: v.description || '', allowedActions: ['read', 'write', 'delete', 'grant'], sharedWith: [] });
        this.tree[`USERVOLUME:${v.rootVolumeName}/${v.owner}/${v.name}`] = { folders: [], files: [] };
        return { createUserVolume: true };
      }
      case 'updateUserVolume': {
        const volume = this.volumes.find((x) => x.name === v.name && x.owner === v.owner && x.rootVolumeName === v.rootVolumeName);
        if (volume) {
          Object.assign(volume, { name: v.newName, description: v.description ?? volume.description });
        }
        return { updateUserVolume: true };
      }
      case 'deleteUserVolume': {
        this.volumes = this.volumes.filter((x) => !(x.name === v.name && x.owner === v.owner && x.rootVolumeName === v.rootVolumeName));
        return { deleteUserVolume: true };
      }
      case 'publicUsersAndGroups': {
        return { getPublicUsersAndGroups: this.directory.map((p) => typed('SharePrincipal', p)) };
      }
      case 'shareUserVolume': {
        const volume = this.volumes.find((x) => x.name === v.name && x.owner === v.owner && x.rootVolumeName === v.rootVolumeName);
        if (volume) {
          volume.sharedWith = (v.sharedWith as SharedWith[]).filter((s) => s.allowedActions.length > 0);
        }
        return { shareUserVolume: true };
      }
      default: {
        throw new Error(`The e2e backend doesn't know ${operation}`);
      }
    }
  }

  private volumesPayload(operation: string) {
    const roots = [
      { id: '1', name: 'Storage', description: 'Persistent storage', allowedActions: ['create', 'read'] },
      { id: '2', name: 'Temporary', description: 'Cleaned regularly', allowedActions: ['create', 'read'] }
    ].map((root) => typed('RootVolume', { ...root, userVolumes: this.volumes.filter((x) => x.rootVolumeName === root.name).map((x) => this.userVolume(x)) }));
    const getVolumes = typed('FileService', { rootVolumes: roots, dataVolumes: this.dataVolumes.map((d) => typed('DataVolume', d)) });
    if (operation === 'fileQuotas') {
      return {
        getVolumes,
        getFileUsage: [
          typed('FileUsage', { rootVolumeId: '1', userVolumeId: null, username: 'ana', type: 'USER', numberOfBytesUsed: 500_000_000, numberOfBytesQuota: 1_000_000_000 }),
          typed('FileUsage', { rootVolumeId: '2', userVolumeId: '4', username: null, type: 'VOLUME', numberOfBytesUsed: 2_000_000_000, numberOfBytesQuota: 2_000_000_000 })
        ]
      };
    }
    return { getVolumes };
  }
}
