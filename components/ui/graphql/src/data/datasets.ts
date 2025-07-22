// eslint-disable-next-line eslint-comments/disable-enable-pair
/* eslint-disable import/no-cycle */
import { RESTDataSource, AugmentedRequest } from '@apollo/datasource-rest';
import type { KeyValueCache } from '@apollo/utils.keyvaluecache';
import flatMap from 'lodash/flatMap';
import map from 'lodash/map';

import { environment } from '../environment';
import { FileService, Dataset, Resource, VolumeType, DataVolume, RootVolume, DatasetDetailInput } from '../generated/typings';
import { convertBase64toJSON } from '../utils/file';

type MultipleFileRes = {
  filePath: string
  data: string
  statusCode: string
}

type MultipleFileReq = {
  filePath: string
  volumeID: string
}

export class DatasetsAPI extends RESTDataSource {
  override baseURL = `${environment.files.baseUrl}`;
  private token: string;

  constructor(options: { token: string; cache: KeyValueCache }) {
    super(options); // this sends our server's `cache` through
    this.token = options.token;
  }

  override willSendRequest(_: string, request: AugmentedRequest) {
    request.headers['X-Auth-Token'] = this.token;
  }

  // QUERIES //

  async getDatasets(volumes: FileService, volumeType: VolumeType): Promise<Dataset[]> {

    const dataVols = volumes.dataVolumes || [];
    const rootVols = volumes.rootVolumes || [];

    const reqBody: MultipleFileReq[] = this.getReqBody(volumeType, dataVols, rootVols);
    const files = await this.post(`${this.baseURL!}multiple-file/`, { body: reqBody });

    let datasets: Dataset[] = [];
    if (files.statusCode === 'OK') {
      for (const fileReq of reqBody) {
        const dataResponse: MultipleFileRes[] = files.fileDataResponse;

        const fileRes = dataResponse.find(f => f.filePath === fileReq.filePath);
        if (fileRes && fileRes.statusCode === 'OK') {
          const json = convertBase64toJSON(fileRes.data);
          const source = fileReq.filePath.split('/').slice(0, -1).join('/');
          const { volumeID } = fileReq;

          datasets = [...datasets, ...this.datasetListReducer(json.datasets, source, volumeID)];
        }
      }
    }
    return datasets;
  }

  getReqBody(volumeType: VolumeType, dataVols: DataVolume[], rootVols: RootVolume[]): MultipleFileReq[] {
    switch (volumeType) {
      case VolumeType.Datavolume:
        return map(dataVols, dv => ({ filePath: `${dv.name}/.sciserver/datasets-index.json`, volumeID: dv.id }));
      case VolumeType.Uservolume:
        return flatMap(rootVols, rv => map(rv.userVolumes, uv => ({ filePath: `${rv.name}/${uv.owner}/${uv.name}/.sciserver/datasets-index.json`, volumeID: uv.id })));

      default:
        return [];
    }
  }

  async getDataset({ name, volumeID, source, catalog }: DatasetDetailInput): Promise<Dataset> {
    const fileRes = await this.get(`${this.baseURL!}file/${source}/datasets-index.json`);

    if (fileRes && fileRes.datasets) {
      const datasetsRes = this.datasetListReducer(fileRes.datasets, source, volumeID);
      const dataset = datasetsRes.find(d => d.name === name && (catalog ? catalog === d.catalog : true));
      if (dataset) {
        return dataset;
      }
    }
    throw new Error('Dataset not found.');
  }

  // Reducers
  datasetListReducer(resDatasets: any[], source: string, volumeID: string): Dataset[] {
    const ds: Dataset[] = [];
    for (const res of resDatasets) {
      if (res.name && res.summary) {
        ds.push(this.datasetReducer(res, source, volumeID));
      }
    }
    return ds;
  }

  datasetReducer(res: any, source: string, volumeID: string): Dataset {
    return {
      name: res.name,
      summary: res.summary,
      logo: res.logo,
      description: res.description,
      catalog: res.catalog,
      tags: res.tags,
      resources: res.resources != null ? this.resourceReducer(res.resources) : [],
      source,
      volumeID
    };
  };

  resourceReducer(resources: any[]): Resource[] {
    const arr: Resource[] = [];
    for (const r of resources) {

      if (r.kind && r.link && r.name) {
        arr.push(
          {
            name: r.name,
            link: r.link,
            kind: r.kind,
            description: r.description
          }
        );
      }
    }
    return arr;
  }

}
