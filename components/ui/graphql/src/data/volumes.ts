// eslint-disable-next-line eslint-comments/disable-enable-pair
/* eslint-disable import/no-cycle */
import { RESTDataSource, AugmentedRequest } from '@apollo/datasource-rest';
import type { KeyValueCache } from '@apollo/utils.keyvaluecache';

import { environment } from '../environment';
import {
  DataVolume,
  FileService,
  RootVolume,
  UserVolume,
  ComputeDataVolume,
  FileUsage,
  JobUserVolume,
  SharedWith,
  SharedWithInput
} from '../generated/typings';

const userVolumePath = (rootVolumeName: string, owner: string, name: string) => `${rootVolumeName}/${owner}/${encodeURIComponent(name)}`;

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

  async getFileUsage(): Promise<FileUsage[]> {
    const res = await this.get(`${this.baseURL!}usage`);

    return ((res as any[]) || []).map((r: any) => this.fileUsageReducer(r));
  }

  // MUTATIONS //
  async createUserVolume(rootVolumeName: string, owner: string, name: string, description?: string | null): Promise<boolean> {
    await this.put(`${this.baseURL!}volume/${userVolumePath(rootVolumeName, owner, name)}`, {
      body: { description: description || '' }
    });
    return true;
  }

  async updateUserVolume(
    rootVolumeName: string,
    owner: string,
    name: string,
    newName: string,
    description?: string | null
  ): Promise<boolean> {
    await this.patch(`${this.baseURL!}volume/${userVolumePath(rootVolumeName, owner, name)}`, {
      body: { name: newName, description: description || '' }
    });
    return true;
  }

  async deleteUserVolume(rootVolumeName: string, owner: string, name: string): Promise<boolean> {
    await this.delete(`${this.baseURL!}volume/${userVolumePath(rootVolumeName, owner, name)}/`);
    return true;
  }

  async shareUserVolume(rootVolumeName: string, owner: string, name: string, sharedWith: SharedWithInput[]): Promise<boolean> {
    await this.patch(`${this.baseURL!}share/${userVolumePath(rootVolumeName, owner, name)}/`, {
      body: sharedWith.map(s => ({ ...s, id: Number(s.id) }))
    });
    return true;
  }

  // Reducers
  fileUsageReducer(res: any): FileUsage {
    return {
      rootVolumeId: res.rootVolumeId,
      userVolumeId: res.userVolumeId,
      username: res.username,
      type: res.type,
      numberOfBytesUsed: res.numberOfBytesUsed || 0,
      numberOfBytesQuota: res.numberOfBytesQuota || 0
    };
  }

  sharedWithReducer(res: any): SharedWith {
    return {
      id: res.id,
      name: res.name,
      type: res.type,
      allowedActions: res.allowedActions || []
    };
  }

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
      sharedWith: (res.sharedWith as [any] | undefined)?.map((r: any) => this.sharedWithReducer(r)) || [],
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
      sharedWith: (res.sharedWith as [any] | undefined)?.map((r: any) => this.sharedWithReducer(r)) || [],
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
      sharedWith: (res.sharedWith as [any] | undefined)?.map((r: any) => this.sharedWithReducer(r)) || [],
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
}
