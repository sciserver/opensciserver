/* eslint-disable import/no-extraneous-dependencies */
/* eslint-disable import/no-cycle */
import { RESTDataSource, AugmentedRequest } from '@apollo/datasource-rest';
import type { KeyValueCache } from '@apollo/utils.keyvaluecache';
import { sortBy } from 'lodash';

import { environment } from '../environment';
import { CreateJobParams, Job, JobDetails, JobFilters, JobMessage } from '../generated/typings';
import { VolumesAPI } from './volumes';

export class JobsAPI extends RESTDataSource {
  override baseURL = `${environment.racm.jobsUrl}`;
  private filesURL = `${environment.files.baseUrl}`;

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
  async getJobs(filters: JobFilters[] | undefined | null, top = 10): Promise<Job[]> {
    const jobsres = await this.get(`${this.baseURL!}jobs?top=${top}`) || [];

    let jobs: Job[] = jobsres.map((r: any) => this.jobReducer(r));

    if (filters) {
      for (const f of filters) {
        jobs = jobs.filter((j: Job) => j[f.field as keyof Job] === f.value);
      }
    }
    return jobs;
  }

  async getJobDetails(jobId: string): Promise<JobDetails> {
    const job = await this.get(`${this.baseURL!}jobs/${jobId}`);
    const sanitizedURI = job.resultsFolderURI.replace('/home/idies/workspace/', '');
    const jobJsontree = await this.volumesAPI.getFilesByVolume(sanitizedURI) || {};
    const readMeFile = await this.get(`${this.filesURL}file/${sanitizedURI}/README.md`) || {};

    return {
      job: this.jobReducer(job),
      summary: readMeFile || 'No summary available',
      files: jobJsontree.root.files || []
    };
  }

  // MUTATIONS //
  async createJob(createJobParams: CreateJobParams): Promise<Job> {
    const response = await this.post(`${this.baseURL!}jobs/docker`, { body: createJobParams });

    return this.jobReducer(response);
  }

  // Reducers
  jobReducer(res: any): Job {
    let userVolumes = res.userVolumes ? res.userVolumes.map((uv: any) => this.volumesAPI.jobUserVolumeReducer(uv)) : [];
    userVolumes = sortBy(userVolumes, 'name');
    let dataVolumes = res.volumeContainers ? res.volumeContainers.map((dv: any) => this.volumesAPI.dataVolumeReducer(dv)) : [];
    dataVolumes = sortBy(dataVolumes, 'name');

    return {
      id: res.id,
      executorDID: res.executorDID,
      submitterDID: res.submitterDID,
      scriptURI: res.scriptURI,
      submitterTrustId: res.submitterTrustId,
      runByUUID: res.runByUUID,
      submissionTime: res.submissionTime,
      startTime: res.startTime,
      endTime: res.endTime,
      duration: res.duration,
      timeout: res.timeout,
      messages: res.messages ? res.messages.map((m: any) => this.jobMessageReducer(m)) : [],
      status: res.status,
      resultsFolderURI: res.resultsFolderURI || '',
      type: res.type,
      userVolumes,
      username: res.username,
      command: res.command || '',
      dockerComputeEndpoint: res.dockerComputeEndpoint,
      dockerComputeResourceContextUUID: res.dockerComputeResourceContextUUID,
      fullDockerCommand: res.fullDockerCommand || [],
      dockerImageName: res.dockerImageName,
      dataVolumes
    };
  }

  jobMessageReducer(res: any): JobMessage {
    return {
      id: res.id,
      content: res.content,
      label: res.label
    };
  }
}
