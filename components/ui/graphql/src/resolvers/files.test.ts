import { ApolloServer } from '@apollo/server';
import { makeExecutableSchema } from '@graphql-tools/schema';
import { describe, expect, it } from 'vitest';

import type { Context } from '../main';
import { FilesAPI } from '../data/files';
import { createMockFetch, dataSourceOptions } from '../test/mockFetch';
import { resolvers } from './root';
import { typeDefs } from '../types/root';

// Runs real operations through the assembled schema, resolvers and data source. Only the
// fileservice HTTP layer is faked.
const run = async (query: string, variables: Record<string, unknown>, ...replies: Parameters<typeof createMockFetch>) => {
  const mock = createMockFetch(...replies);
  const server = new ApolloServer<Context>({ schema: makeExecutableSchema({ typeDefs, resolvers }) });
  const contextValue = { dataSources: { filesAPI: new FilesAPI(dataSourceOptions(mock.fetch)) } } as unknown as Context;
  const { body } = await server.executeOperation({ query, variables }, { contextValue });

  if (body.kind !== 'single') throw new Error('expected a single response');
  return { result: body.singleResult, requests: mock.requests };
};

const volume = { volumeType: 'USERVOLUME', volumeName: 'vol', rootVolumeName: 'Storage', owner: 'bob' };

describe('files operations through GraphQL', () => {
  it('getJsonTree resolves a folder listing', async () => {
    const { result, requests } = await run(
      `query ($volume: VolumeRefInput!, $path: String) {
        getJsonTree(volume: $volume, path: $path) { queryPath root { folders { name } files { name size } } }
      }`,
      { volume, path: '/data' },
      {
        body: {
          queryPath: '/data',
          root: {
            name: 'data',
            creationTime: '2026-01-01T00:00:00Z',
            lastModified: '2026-01-01T00:00:00Z',
            folders: [{ name: 'sub', creationTime: '2026-01-01T00:00:00Z', lastModified: '2026-01-01T00:00:00Z' }],
            files: [{ name: 'a.csv', size: 5, creationTime: '2026-01-01T00:00:00Z', lastModified: '2026-01-01T00:00:00Z' }]
          }
        }
      }
    );

    expect(result.errors).toBeUndefined();
    expect(result.data).toEqual({
      getJsonTree: { queryPath: '/data', root: { folders: [{ name: 'sub' }], files: [{ name: 'a.csv', size: 5 }] } }
    });
    expect(requests()[0].url).toBe('https://files.test/api/jsontree/Storage/bob/vol/data/?level=2');
  });

  it('rejects an invalid volume reference before calling the fileservice', async () => {
    const { result, requests } = await run(
      `query ($volume: VolumeRefInput!) { getJsonTree(volume: $volume) { queryPath } }`,
      { volume: { volumeType: 'USERVOLUME', volumeName: 'vol' } }
    );

    expect(result.errors?.[0].message).toMatch(/required for user volumes/);
    expect(requests()).toHaveLength(0);
  });

  it('copyFile sends a copy request and returns true', async () => {
    const { result, requests } = await run(
      `mutation ($source: FileLocationInput!, $destination: FileLocationInput!) {
        copyFile(source: $source, name: "a.csv", destination: $destination, newName: "a.csv (1)")
      }`,
      {
        source: { volume, path: '/data' },
        destination: { volume: { volumeType: 'DATAVOLUME', volumeName: 'dv' }, path: '/out' }
      },
      {}
    );

    expect(result.errors).toBeUndefined();
    expect(result.data).toEqual({ copyFile: true });
    expect(requests()[0].url).toContain('doCopy=true');
    expect(requests()[0].body).toMatchObject({ destinationPath: '/out/a.csv (1)', destinationDataVolume: 'dv' });
  });

  it('turns a fileservice error into a GraphQL error', async () => {
    const { result } = await run(
      `mutation ($volume: VolumeRefInput!) { deleteFile(volume: $volume, path: "/", name: "x") }`,
      { volume },
      { status: 403, body: { status: 'error', error: 'forbidden' } }
    );

    expect(result.errors?.[0].message).toMatch(/403/);
    expect(result.data).toBeNull();
  });
});
