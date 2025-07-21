// eslint-disable-next-line eslint-comments/disable-enable-pair
/* eslint-disable import/no-cycle */
import { RESTDataSource, AugmentedRequest } from '@apollo/datasource-rest';
import type { KeyValueCache } from '@apollo/utils.keyvaluecache';

import { environment } from '../environment';
import {
  DataVolume,
  FileService,
  Folder,
  File,
  JsonTree,
  RootVolume,
  UserVolume,
  ComputeDataVolume,
  JobUserVolume
} from '../generated/typings';

export enum CALLER {
  FILESERVICE,
  COMPUTE
}

export class VolumesAPI extends RESTDataSource {
  override baseURL = `${environment.files.baseUrl}`;
  private token: string;

  constructor(options: { token: string; cache: KeyValueCache }) {
    super(options); // this sends our server's `cache` through
    this.token = options.token;
  }

  override willSendRequest(path: string, request: AugmentedRequest) {
    request.headers['X-Auth-Token'] = this.token;
  }

  // QUERIES //
  async getVolumes(): Promise<FileService> {
    const res = await this.get(`${this.baseURL!}volumes/`);

    return this.fileSystemReducer(res);
  }

  async getFilesByVolume(volumeName: string): Promise<JsonTree> {
    const results = await this.get(`${this.baseURL!}jsontree/${volumeName}/?level=2`);

    return this.jsonTreeReducer(results);
  }

  // Reducers
  fileSystemReducer(res: any): FileService {
    return {
      identifier: res.identifier, name: res.name,
      description: res.description,
      apiEndpoint: res.apiEndpoint,
      rootVolumes: (res.rootVolumes as [any])?.map((r: any) => this.rootVolumeReducer(r)),
      dataVolumes: (res.dataVolumes as [any])?.map((r: any) => this.dataVolumeReducer(r))
    };
  }

  dataVolumeReducer(res: any): DataVolume {
    return {
      id: res.id,
      name: res.name,
      displayName: res.displayName,
      resourceUUID: res.resourceUUID || '',
      publisherDID: res.publisherDID || '',
      racmUUID: res.racmUUID || '',
      description: res.description || '',
      pathOnFileSystem: res.pathOnFileSystem,
      writable: res.allowedActions ? res.allowedActions.includes('write') : false,
      url: res.url,
      allowedActions: res.allowedActions,
      sharedWith: res.sharedWith,
      owningResourceId: res.owningResourceId
    };
  }

  computeDataVolumeReducer(res: any): ComputeDataVolume {
    return {
      publisherDID: res.publisherDID,
      writable: res.writable
    };
  }

  rootVolumeReducer(res: any): RootVolume {
    return {
      id: res.id,
      resourceUUID: res.resourceUUID,
      name: res.name,
      description: res.description,
      pathOnFileSystem: res.pathOnFileSystem,
      containsSharedVolumes: res.containsSharedVolumes,
      allowedActions: res.allowedActions,
      sharedWith: res.sharedWith,
      owningResourceId: res.owningResourceId,
      userVolumes: (res.userVolumes as [any])?.map((r: any) => this.userVolumeReducer(r, CALLER.FILESERVICE, res.name)) || []
    };
  }

  userVolumeReducer(res: any, caller: CALLER, rootVolumeName = ''): UserVolume {
    return {
      id: res.id,
      resourceUUID: res.resourceUUID,
      name: res.name,
      description: res.description,
      relativePath: res.relativePath,
      allowedActions: res.allowedActions,
      sharedWith: res.sharedWith,
      owningResourceId: res.owningResourceId,
      owner: res.owner,
      rootVolumeName: caller === CALLER.COMPUTE ? res.rootVolumeName : rootVolumeName
    };
  }

  jobUserVolumeReducer(res: any): JobUserVolume {
    return {
      id: res.id,
      userVolumeId: res.userVolumeId,
      fullPath: res.fullPath,
      needsWriteAccess: res.needsWriteAccess
    };
  }

  jsonTreeReducer(res: any): JsonTree {
    const { root } = res;
    return {
      root: {
        name: root.name,
        creationTime: root.creationTime,
        lastModified: root.lastModified,
        folders: (root.folders as [any])?.map((r: any) => this.folderReducer(r)),
        files: (root.files as [any])?.map((r: any) => this.fileReducer(r))
      },
      queryPath: res.queryPath
    };
  }

  folderReducer(res: any): Folder {
    return {
      name: res.name,
      creationTime: res.creationTime,
      lastModified: res.lastModified
    };
  }

  fileReducer(res: any): File {
    return {
      name: res.name,
      size: res.size,
      creationTime: res.creationTime,
      lastModified: res.lastModified
    };
  }

}
