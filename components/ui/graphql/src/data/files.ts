// eslint-disable-next-line eslint-comments/disable-enable-pair
/* eslint-disable import/no-cycle */
import { RESTDataSource, AugmentedRequest } from '@apollo/datasource-rest';
import type { KeyValueCache } from '@apollo/utils.keyvaluecache';

import { environment } from '../environment';
import { Folder, File, JsonTree, VolumeRefInput, FileLocationInput } from '../generated/typings';
import { buildFileUrlPath, destinationBody, normalizePath } from '../utils/volumePath';

export class FilesAPI extends RESTDataSource {
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
  async getJsonTree(volume: VolumeRefInput, path?: string | null): Promise<JsonTree> {
    return this.getJsonTreeByPath(buildFileUrlPath(volume, path));
  }

  // For callers that already hold a raw fileservice path (e.g. a job results folder)
  async getJsonTreeByPath(urlPath: string): Promise<JsonTree> {
    const results = await this.get(`${this.baseURL!}jsontree/${urlPath}/?level=2`);

    return this.jsonTreeReducer(results);
  }

  // MUTATIONS //
  async createFolder(volume: VolumeRefInput, path: string, name: string): Promise<boolean> {
    await this.put(`${this.baseURL!}folder/${buildFileUrlPath(volume, path, name)}`);
    return true;
  }

  async deleteFile(volume: VolumeRefInput, path: string, name: string): Promise<boolean> {
    await this.delete(`${this.baseURL!}data/${buildFileUrlPath(volume, path, name)}`);
    return true;
  }

  async renameFile(volume: VolumeRefInput, path: string, name: string, newName: string): Promise<boolean> {
    return this.transfer(
      { volume, path },
      name,
      { volume, path },
      newName,
      false
    );
  }

  async moveFile(source: FileLocationInput, name: string, destination: FileLocationInput, newName?: string | null): Promise<boolean> {
    return this.transfer(source, name, destination, newName || name, false);
  }

  async copyFile(source: FileLocationInput, name: string, destination: FileLocationInput, newName?: string | null): Promise<boolean> {
    return this.transfer(source, name, destination, newName || name, true);
  }

  // Rename, move and copy are the same fileservice endpoint; only doCopy and the destination differ
  private async transfer(
    source: FileLocationInput,
    name: string,
    destination: FileLocationInput,
    destinationName: string,
    doCopy: boolean
  ): Promise<boolean> {
    await this.put(`${this.baseURL!}data/${buildFileUrlPath(source.volume, source.path, name)}`, {
      params: { replaceExisting: 'false', doCopy: String(doCopy) },
      body: destinationBody(destination.volume, `${normalizePath(destination.path)}/${destinationName}`)
    });
    return true;
  }

  // Reducers
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
