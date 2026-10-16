import { describe, expect, it } from 'vitest';

import { VolumeType } from '../generated/typings';
import { createMockFetch, dataSourceOptions } from '../test/mockFetch';
import { DatasetsAPI } from './datasets';

const BASE = 'https://files.test/api/';

const setup = (...replies: Parameters<typeof createMockFetch>) => {
  const mock = createMockFetch(...replies);
  return { api: new DatasetsAPI(dataSourceOptions(mock.fetch)), requests: mock.requests };
};

describe('DatasetsAPI', () => {
  it('sends volume paths raw in the multiple-file body, where the service splits them on "/" without decoding', () => {
    const { api } = setup();
    const body = api.getReqBody(
      VolumeType.Uservolume,
      [],
      [{ name: 'Storage', userVolumes: [{ id: '1', owner: 'bob@example.org', name: 'my vol' }] }] as any
    );

    expect(body).toEqual([{ filePath: 'Storage/bob@example.org/my vol/.sciserver/datasets-index.json', volumeID: '1' }]);
  });

  it('encodes every segment of the source when it becomes part of the dataset index URL', async () => {
    const { api, requests } = setup({ body: { datasets: [{ name: 'd', summary: 's' }] } });
    await api.getDataset({ name: 'd', volumeID: '1', source: 'Root Vol/bob@example.org/my vol', catalog: undefined } as any);

    expect(requests()[0].url).toBe(`${BASE}file/Root%20Vol/bob%40example.org/my%20vol/datasets-index.json`);
  });
});
