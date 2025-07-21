/* eslint-disable import/no-cycle */
import { AugmentedRequest, RESTDataSource } from '@apollo/datasource-rest';
import type { KeyValueCache } from '@apollo/utils.keyvaluecache';

import { environment } from '../environment';
import { Container, ContainerDetail, ContainerDetailParams, ContainerParams, ContainerStatus, Domain } from '../generated/typings';
import { DomainsAPI } from './domains';
import { VolumesAPI } from './volumes';
import { AccountsAPI } from './accounts';

export class ContainersAPI extends RESTDataSource {
  override baseURL = `${environment.compute.baseUrl}`;
  private token: string;

  private domainsAPI: DomainsAPI;
  private accountsAPI: AccountsAPI;
  private volumesAPI: VolumesAPI;

  constructor(options: {
    token: string
    cache: KeyValueCache
    domainsAPI: DomainsAPI
    accountsAPI: AccountsAPI
    volumesAPI: VolumesAPI
  }) {
    super(options); // this sends our server's `cache` through
    this.token = options.token;
    this.volumesAPI = options.volumesAPI;
    this.domainsAPI = options.domainsAPI;
    this.accountsAPI = options.accountsAPI;
  }

  override willSendRequest(path: string, request: AugmentedRequest) {
    request.headers['X-Auth-Token'] = this.token;
    if (request.method === 'POST') {
      request.headers['X-Description'] = 'NOTEBOOK';
      request.headers['Content-Type'] = 'application/json';
    }
  }

  // QUERIES //
  async pingContainer(containerId: string): Promise<boolean> {
    try {
      await this.post(`${this.baseURL!}container/${containerId}/ping`);
      return true;
    }
    catch {
      return false;
    }
  }

  async getContainers(): Promise<Container[]> {
    const res = await this.get(`${this.baseURL!}containers/`) || [];

    return this.containersReducer(res);
  }

  async getContainerID(containerParams: ContainerParams): Promise<string> {

    const container = await this.getContainer(containerParams);

    if (!container) {
      const resDomain = await this.domainsAPI.getDomainByName(containerParams.domainName);
      const volumes = await this.getVolumeReqs(containerParams, resDomain);

      const newContainerIDRes = await this.post(`${this.baseURL}domains/${resDomain.publisherDID}/containers`,
        {
          body: {
            domain: resDomain.publisherDID,
            dockerImageName: containerParams.imageName,
            volumeContainers: volumes.dataVolumes,
            userVolumes: volumes.userVolumes
          }
        }
      );

      return newContainerIDRes;
    }

    return container.id;
  }

  async getContainerDetail({ domainId, dataVolumeIds, userVolumeIds }: ContainerDetailParams): Promise<ContainerDetail> {
    const resDomain = await this.domainsAPI.getDomainByID(domainId);

    return this.containerDetailReducer(resDomain, dataVolumeIds, userVolumeIds);
  }

  // MUTATIONS //
  async deleteContainer(domainId: string, containerId: string): Promise<boolean> {
    try {
      await this.delete(`${this.baseURL!}domains/${domainId}/containers/${containerId}`);
      return true;
    }
    catch (error) {
      console.log(error);

      return false;
    }
  }

  // Additional Methods
  async getContainer(containerParams: ContainerParams): Promise<Container | undefined> {
    const containers = await this.getContainers();
    const container = containers.find(
      c => c.imageName === containerParams.imageName && c.domainName === containerParams.domainName
        && containerParams.dataVolumeIds.every(dvReq => (c.dataVolumes.map(dv => dv.publisherDID) as string[]).includes(dvReq))
        && containerParams.userVolumeIds.every(uvReq => c.userVolumes.map(uv => uv.toString()).includes(uvReq))
    );

    return container;
  }

  async getVolumeReqs(containerParams: ContainerParams, domain: Domain) {
    // Check if User has access to requested User and Data Volumes
    // If not throw Error
    if (!containerParams.userVolumeIds.every(uvReq => domain.userVolumes.map(uv => uv.id.toString()).includes(uvReq)) ||
      !containerParams.dataVolumeIds.every(dvReq => domain.dataVolumes.map(dv => dv.publisherDID).includes(dvReq))) {
      throw new Error('User Volume or Data Volume does not exist or user does not have access to it.');
    }

    const resUser = await this.accountsAPI.getUser();

    const userVolumes = [];
    const dataVolumes = [];

    for (const r of domain.userVolumes) {
      const reqUV = containerParams.userVolumeIds.find(uv => uv === r.id.toString());
      if (reqUV ||
        ((
          (r.name === 'persistent' && r.rootVolumeName === 'Storage') ||
          (r.name === 'scratch' && r.rootVolumeName === 'Temporary'))
          && r.owner === resUser.userName)
      ) {
        userVolumes.push({ userVolumeId: r.id.toString() });
      }
    }

    for (const publisherDID of containerParams.dataVolumeIds) {
      dataVolumes.push({ publisherDID });
    }

    return { dataVolumes, userVolumes };
  }

  // Reducers
  containersReducer(resContainers: any): Container[] {
    const conts: Container[] = [];

    for (const res of resContainers) {
      if (res.json) {
        conts.push(this.containerReducer(res));
      }
    }
    return conts;
  }

  containerReducer(res: any): Container {
    return {
      id: res.id,
      name: res.name,
      displayName: res.displayName,
      nodeName: res.nodeName,
      status: res.status || ContainerStatus.None,
      imageName: res.json.dockerImageName,
      domainName: res.domainName,
      domainID: res.domainId,
      createdAt: new Date(res.createdAt),
      accessedAt: new Date(res.accessedAt),
      maxSecs: res.maxSecs,
      userVolumes: res.json.userVolumes.map((r: any) => r.userVolumeId),
      dataVolumes: res.json.volumeContainers.map((r: any) => this.volumesAPI.computeDataVolumeReducer(r)),
      description: res.description
    };
  }

  containerDetailReducer(domain: Domain, dataVolumeIds: string[], userVolumeIds: string[]): ContainerDetail {
    return {
      id: domain.id,
      dataVolumes: domain.dataVolumes.filter(dv => dataVolumeIds.includes(dv.publisherDID)),
      userVolumes: domain.userVolumes.filter(uv => userVolumeIds.includes(uv.id.toString()))
    };
  }
}
