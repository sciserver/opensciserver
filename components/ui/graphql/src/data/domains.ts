/* eslint-disable import/no-extraneous-dependencies */
/* eslint-disable import/no-cycle */
import { RESTDataSource, AugmentedRequest } from '@apollo/datasource-rest';
import type { KeyValueCache } from '@apollo/utils.keyvaluecache';
import { sortBy } from 'lodash';

import { environment } from '../environment';
import { Domain, Image } from '../generated/typings';
import { CALLER, VolumesAPI } from './volumes';

export class DomainsAPI extends RESTDataSource {
  override baseURL = `${environment.racm.jobsUrl}`;
  private token: string;
  private volumesAPI: VolumesAPI;

  constructor(options: { token: string; cache: KeyValueCache, volumesAPI: VolumesAPI }) {
    super(options); // this sends our server's `cache` through
    this.token = options.token;
    this.volumesAPI = options.volumesAPI;
  }

  override willSendRequest(path: string, request: AugmentedRequest) {
    request.headers['X-Auth-Token'] = this.token;
  }

  // QUERIES //
  async getDomains(jobs = false): Promise<Domain[]> {
    const res = await this.get(`${this.baseURL!}computedomains${!jobs ? '' : '?batch=true'}`) || [];
    let domains = res.map((r: any) => this.domainReducer(r));
    domains = sortBy(domains, 'name');

    return domains;
  }

  async getDomainByID(id: string): Promise<Domain> {
    const resDomains = await this.getDomains();

    // Look for requested domain. If Domain doesn't exist or user 
    // doesn't have access to it, throw an Error.
    const resDomain = resDomains.find(rd => rd.publisherDID === id);
    if (!resDomain) {
      throw new Error(`Domain ${id} doesn't exist or user doesn't have access to it.`);
    }

    return resDomain;
  }

  async getDomainByName(domainName: string): Promise<Domain> {
    const resDomains = await this.getDomains();

    // Look for requested domain. If Domain doesn't exist or user 
    // doesn't have access to it, throw an Error.
    const resDomain = resDomains.find(rd => rd.name === domainName);
    if (!resDomain) {
      throw new Error(`Domain ${domainName} doesn't exist or user doesn't have access to it.`);
    }

    return resDomain;
  }

  // Reducers
  domainReducer(res: any): Domain {

    let images = res.images.map((i: any) => this.imageReducer(i));
    images = sortBy(images, 'name');

    let dataVolumes = res.volumes.map((dv: any) => this.volumesAPI.dataVolumeReducer(dv));
    dataVolumes = sortBy(dataVolumes, 'name');

    let userVolumes = res.userVolumes.map((uv: any) => this.volumesAPI.userVolumeReducer(uv, CALLER.COMPUTE));
    userVolumes = sortBy(userVolumes, 'name');

    return {
      id: res.id,
      name: res.name,
      description: res.description,
      apiEndpoint: res.apiEndpoint,
      publisherDID: res.publisherDID,
      racmUUID: res.racmUUID,
      userVolumes,
      dataVolumes,
      images
    };
  }

  imageReducer(res: any): Image {
    return {
      id: res.id,
      name: res.name,
      description: res.description,
      publisherDID: res.publisherDID,
      racmUUID: res.racmUUID
    };
  }

}
