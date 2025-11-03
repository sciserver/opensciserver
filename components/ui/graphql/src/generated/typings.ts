import { GraphQLResolveInfo, GraphQLScalarType, GraphQLScalarTypeConfig } from 'graphql';
import { Context } from '../main';
export type Maybe<T> = T | null;
export type InputMaybe<T> = Maybe<T>;
export type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
export type MakeOptional<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]?: Maybe<T[SubKey]> };
export type MakeMaybe<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]: Maybe<T[SubKey]> };
export type RequireFields<T, K extends keyof T> = Omit<T, K> & { [P in K]-?: NonNullable<T[P]> };
/** All built-in and custom scalars, mapped to their actual values */
export type Scalars = {
  ID: string;
  String: string;
  Boolean: boolean;
  Int: number;
  Float: number;
  DateTime: any;
  JSONObject: string;
  URL: any;
  UUID: any;
};

export type ComputeDataVolume = {
  __typename?: 'ComputeDataVolume';
  publisherDID: Scalars['String'];
  writable: Scalars['Boolean'];
};

export type Container = {
  __typename?: 'Container';
  accessedAt: Scalars['DateTime'];
  createdAt: Scalars['DateTime'];
  dataVolumes: Array<ComputeDataVolume>;
  description?: Maybe<Scalars['String']>;
  displayName: Scalars['String'];
  domainID: Scalars['Int'];
  domainName: Scalars['String'];
  id: Scalars['ID'];
  imageName: Scalars['String'];
  maxSecs: Scalars['Int'];
  name: Scalars['String'];
  nodeName: Scalars['String'];
  status: ContainerStatus;
  userVolumes: Array<Scalars['ID']>;
};

export type ContainerDetail = {
  __typename?: 'ContainerDetail';
  dataVolumes: Array<DataVolume>;
  id: Scalars['ID'];
  userVolumes: Array<UserVolume>;
};

export type ContainerDetailParams = {
  dataVolumeIds: Array<Scalars['ID']>;
  domainId: Scalars['ID'];
  userVolumeIds: Array<Scalars['ID']>;
};

export type ContainerParams = {
  dataVolumeIds: Array<Scalars['ID']>;
  domainName: Scalars['String'];
  imageName: Scalars['String'];
  userVolumeIds: Array<Scalars['ID']>;
};

export enum ContainerStatus {
  Created = 'CREATED',
  None = 'NONE'
}

export type CreateJobParams = {
  command: Scalars['String'];
  dockerComputeEndpoint: Scalars['String'];
  dockerImageName: Scalars['String'];
  name?: InputMaybe<Scalars['String']>;
  resultsFolderURI: Scalars['String'];
  scriptURI: Scalars['String'];
  submitterDID: Scalars['String'];
  userVolumes: Array<Scalars['ID']>;
  volumeContainers: Array<Scalars['ID']>;
};

export type DataVolume = {
  __typename?: 'DataVolume';
  allowedActions: Array<Maybe<Scalars['String']>>;
  description: Scalars['String'];
  displayName?: Maybe<Scalars['String']>;
  id: Scalars['ID'];
  name: Scalars['String'];
  owningResourceId?: Maybe<Scalars['ID']>;
  pathOnFileSystem?: Maybe<Scalars['String']>;
  publisherDID: Scalars['String'];
  racmUUID: Scalars['String'];
  resourceUUID?: Maybe<Scalars['ID']>;
  sharedWith: Array<Maybe<Scalars['String']>>;
  url?: Maybe<Scalars['URL']>;
  writable: Scalars['Boolean'];
};

export type Dataset = {
  __typename?: 'Dataset';
  catalog?: Maybe<Scalars['String']>;
  description?: Maybe<Scalars['String']>;
  logo?: Maybe<Scalars['URL']>;
  name: Scalars['String'];
  readme?: Maybe<Scalars['String']>;
  resources: Array<Resource>;
  source: Scalars['String'];
  summary: Scalars['String'];
  tags: Array<Scalars['String']>;
  volumeID: Scalars['ID'];
};

export type DatasetDetailInput = {
  catalog?: InputMaybe<Scalars['String']>;
  name: Scalars['String'];
  source: Scalars['String'];
  volumeID: Scalars['ID'];
};

export type Domain = {
  __typename?: 'Domain';
  apiEndpoint: Scalars['URL'];
  dataVolumes: Array<DataVolume>;
  description: Scalars['String'];
  id: Scalars['ID'];
  images: Array<Image>;
  name: Scalars['String'];
  publisherDID: Scalars['String'];
  racmUUID: Scalars['UUID'];
  userVolumes: Array<UserVolume>;
};

export type File = {
  __typename?: 'File';
  creationTime: Scalars['DateTime'];
  lastModified: Scalars['DateTime'];
  name: Scalars['String'];
  size: Scalars['Float'];
};

export type FileService = {
  __typename?: 'FileService';
  apiEndpoint?: Maybe<Scalars['URL']>;
  dataVolumes?: Maybe<Array<DataVolume>>;
  description?: Maybe<Scalars['String']>;
  identifier: Scalars['ID'];
  name?: Maybe<Scalars['String']>;
  rootVolumes?: Maybe<Array<RootVolume>>;
};

export type Folder = {
  __typename?: 'Folder';
  creationTime: Scalars['DateTime'];
  lastModified: Scalars['DateTime'];
  name: Scalars['String'];
};

export type Image = {
  __typename?: 'Image';
  description: Scalars['String'];
  id: Scalars['ID'];
  name: Scalars['String'];
  publisherDID: Scalars['String'];
  racmUUID: Scalars['String'];
};

export type JsonTree = {
  __typename?: 'JSONTree';
  queryPath: Scalars['String'];
  root: Root;
};

export type Job = {
  __typename?: 'Job';
  command: Scalars['String'];
  dataVolumes: Array<DataVolume>;
  dockerComputeEndpoint: Scalars['String'];
  dockerComputeResourceContextUUID?: Maybe<Scalars['UUID']>;
  dockerImageName: Scalars['String'];
  duration: Scalars['Float'];
  endTime?: Maybe<Scalars['DateTime']>;
  executorDID?: Maybe<Scalars['String']>;
  fullDockerCommand: Array<Scalars['String']>;
  id: Scalars['ID'];
  messages: Array<JobMessage>;
  resultsFolderURI: Scalars['String'];
  runByUUID?: Maybe<Scalars['String']>;
  scriptURI?: Maybe<Scalars['String']>;
  startTime?: Maybe<Scalars['DateTime']>;
  status: JobStatus;
  submissionTime: Scalars['DateTime'];
  submitterDID: Scalars['String'];
  submitterTrustId?: Maybe<Scalars['String']>;
  timeout?: Maybe<Scalars['Int']>;
  type: Scalars['String'];
  userVolumes: Array<JobUserVolume>;
  username?: Maybe<Scalars['String']>;
};

export type JobDetails = {
  __typename?: 'JobDetails';
  files: Array<File>;
  job: Job;
  summary: Scalars['String'];
};

export type JobFilters = {
  field: Scalars['String'];
  value: Scalars['String'];
};

export type JobMessage = {
  __typename?: 'JobMessage';
  content: Scalars['String'];
  id: Scalars['ID'];
  label: Scalars['String'];
};

export enum JobStatus {
  Accepted = 'ACCEPTED',
  Canceled = 'CANCELED',
  Error = 'ERROR',
  Finished = 'FINISHED',
  Pending = 'PENDING',
  Queued = 'QUEUED',
  Started = 'STARTED',
  Success = 'SUCCESS'
}

export type JobUserVolume = {
  __typename?: 'JobUserVolume';
  fullPath: Scalars['String'];
  id: Scalars['ID'];
  needsWriteAccess: Scalars['Boolean'];
  userVolumeId: Scalars['ID'];
};

export type Mutation = {
  __typename?: 'Mutation';
  createJob: Job;
  deleteContainer: Scalars['Boolean'];
  login: Scalars['String'];
  root?: Maybe<Scalars['String']>;
};


export type MutationCreateJobArgs = {
  createJobParams: CreateJobParams;
};


export type MutationDeleteContainerArgs = {
  containerId: Scalars['ID'];
  domainId: Scalars['ID'];
};


export type MutationLoginArgs = {
  password: Scalars['String'];
  username: Scalars['String'];
};

export type Query = {
  __typename?: 'Query';
  getContainerDetail: ContainerDetail;
  getContainerID: Scalars['ID'];
  getContainers: Array<Container>;
  getDataset?: Maybe<Dataset>;
  getDatasets: Array<Dataset>;
  getDomainByID?: Maybe<Domain>;
  getDomains: Array<Domain>;
  getJobDetails: JobDetails;
  getJobs: Array<Job>;
  getJsonTree: JsonTree;
  getUser: User;
  getVolumes?: Maybe<FileService>;
  pingContainer?: Maybe<Scalars['Boolean']>;
  root?: Maybe<Scalars['String']>;
};


export type QueryGetContainerDetailArgs = {
  containerDetailParams: ContainerDetailParams;
};


export type QueryGetContainerIdArgs = {
  containerParams: ContainerParams;
};


export type QueryGetDatasetArgs = {
  params: DatasetDetailInput;
};


export type QueryGetDatasetsArgs = {
  volumeType: VolumeType;
};


export type QueryGetDomainByIdArgs = {
  id: Scalars['ID'];
};


export type QueryGetDomainsArgs = {
  jobs?: InputMaybe<Scalars['Boolean']>;
};


export type QueryGetJobDetailsArgs = {
  jobId: Scalars['ID'];
};


export type QueryGetJobsArgs = {
  filters?: InputMaybe<Array<JobFilters>>;
  top?: InputMaybe<Scalars['Int']>;
};


export type QueryGetJsonTreeArgs = {
  volumeName: Scalars['String'];
};


export type QueryPingContainerArgs = {
  containerId: Scalars['ID'];
};

export type Resource = {
  __typename?: 'Resource';
  description?: Maybe<Scalars['String']>;
  kind: Scalars['String'];
  link: Scalars['String'];
  name: Scalars['String'];
};

export type Root = {
  __typename?: 'Root';
  creationTime: Scalars['DateTime'];
  files: Array<File>;
  folders: Array<Folder>;
  lastModified: Scalars['DateTime'];
  name: Scalars['String'];
};

export type RootVolume = {
  __typename?: 'RootVolume';
  allowedActions: Array<Maybe<Scalars['String']>>;
  containsSharedVolumes: Scalars['Boolean'];
  description?: Maybe<Scalars['String']>;
  id: Scalars['ID'];
  name?: Maybe<Scalars['String']>;
  owningResourceId?: Maybe<Scalars['ID']>;
  pathOnFileSystem?: Maybe<Scalars['String']>;
  resourceUUID: Scalars['ID'];
  sharedWith: Array<Maybe<Scalars['String']>>;
  userVolumes: Array<UserVolume>;
};

export type Subscription = {
  __typename?: 'Subscription';
  root?: Maybe<Scalars['String']>;
};

export type User = {
  __typename?: 'User';
  email: Scalars['String'];
  id: Scalars['ID'];
  userName: Scalars['String'];
  visibility: Scalars['String'];
};

export type UserVolume = {
  __typename?: 'UserVolume';
  allowedActions: Array<Maybe<Scalars['String']>>;
  description?: Maybe<Scalars['String']>;
  id: Scalars['ID'];
  name: Scalars['String'];
  owner: Scalars['String'];
  owningResourceId?: Maybe<Scalars['ID']>;
  relativePath?: Maybe<Scalars['String']>;
  resourceUUID: Scalars['ID'];
  rootVolumeName: Scalars['String'];
  sharedWith: Array<Maybe<Scalars['String']>>;
};

export enum VolumeType {
  Datavolume = 'DATAVOLUME',
  Uservolume = 'USERVOLUME'
}



export type ResolverTypeWrapper<T> = Promise<T> | T;


export type ResolverWithResolve<TResult, TParent, TContext, TArgs> = {
  resolve: ResolverFn<TResult, TParent, TContext, TArgs>;
};
export type Resolver<TResult, TParent = {}, TContext = {}, TArgs = {}> = ResolverFn<TResult, TParent, TContext, TArgs> | ResolverWithResolve<TResult, TParent, TContext, TArgs>;

export type ResolverFn<TResult, TParent, TContext, TArgs> = (
  parent: TParent,
  args: TArgs,
  context: TContext,
  info: GraphQLResolveInfo
) => Promise<TResult> | TResult;

export type SubscriptionSubscribeFn<TResult, TParent, TContext, TArgs> = (
  parent: TParent,
  args: TArgs,
  context: TContext,
  info: GraphQLResolveInfo
) => AsyncIterable<TResult> | Promise<AsyncIterable<TResult>>;

export type SubscriptionResolveFn<TResult, TParent, TContext, TArgs> = (
  parent: TParent,
  args: TArgs,
  context: TContext,
  info: GraphQLResolveInfo
) => TResult | Promise<TResult>;

export interface SubscriptionSubscriberObject<TResult, TKey extends string, TParent, TContext, TArgs> {
  subscribe: SubscriptionSubscribeFn<{ [key in TKey]: TResult }, TParent, TContext, TArgs>;
  resolve?: SubscriptionResolveFn<TResult, { [key in TKey]: TResult }, TContext, TArgs>;
}

export interface SubscriptionResolverObject<TResult, TParent, TContext, TArgs> {
  subscribe: SubscriptionSubscribeFn<any, TParent, TContext, TArgs>;
  resolve: SubscriptionResolveFn<TResult, any, TContext, TArgs>;
}

export type SubscriptionObject<TResult, TKey extends string, TParent, TContext, TArgs> =
  | SubscriptionSubscriberObject<TResult, TKey, TParent, TContext, TArgs>
  | SubscriptionResolverObject<TResult, TParent, TContext, TArgs>;

export type SubscriptionResolver<TResult, TKey extends string, TParent = {}, TContext = {}, TArgs = {}> =
  | ((...args: any[]) => SubscriptionObject<TResult, TKey, TParent, TContext, TArgs>)
  | SubscriptionObject<TResult, TKey, TParent, TContext, TArgs>;

export type TypeResolveFn<TTypes, TParent = {}, TContext = {}> = (
  parent: TParent,
  context: TContext,
  info: GraphQLResolveInfo
) => Maybe<TTypes> | Promise<Maybe<TTypes>>;

export type IsTypeOfResolverFn<T = {}, TContext = {}> = (obj: T, context: TContext, info: GraphQLResolveInfo) => boolean | Promise<boolean>;

export type NextResolverFn<T> = () => Promise<T>;

export type DirectiveResolverFn<TResult = {}, TParent = {}, TContext = {}, TArgs = {}> = (
  next: NextResolverFn<TResult>,
  parent: TParent,
  args: TArgs,
  context: TContext,
  info: GraphQLResolveInfo
) => TResult | Promise<TResult>;

/** Mapping between all available schema types and the resolvers types */
export type ResolversTypes = {
  Boolean: ResolverTypeWrapper<Scalars['Boolean']>;
  ComputeDataVolume: ResolverTypeWrapper<ComputeDataVolume>;
  Container: ResolverTypeWrapper<Container>;
  ContainerDetail: ResolverTypeWrapper<ContainerDetail>;
  ContainerDetailParams: ContainerDetailParams;
  ContainerParams: ContainerParams;
  ContainerStatus: ContainerStatus;
  CreateJobParams: CreateJobParams;
  DataVolume: ResolverTypeWrapper<DataVolume>;
  Dataset: ResolverTypeWrapper<Dataset>;
  DatasetDetailInput: DatasetDetailInput;
  DateTime: ResolverTypeWrapper<Scalars['DateTime']>;
  Domain: ResolverTypeWrapper<Domain>;
  File: ResolverTypeWrapper<File>;
  FileService: ResolverTypeWrapper<FileService>;
  Float: ResolverTypeWrapper<Scalars['Float']>;
  Folder: ResolverTypeWrapper<Folder>;
  ID: ResolverTypeWrapper<Scalars['ID']>;
  Image: ResolverTypeWrapper<Image>;
  Int: ResolverTypeWrapper<Scalars['Int']>;
  JSONObject: ResolverTypeWrapper<Scalars['JSONObject']>;
  JSONTree: ResolverTypeWrapper<JsonTree>;
  Job: ResolverTypeWrapper<Job>;
  JobDetails: ResolverTypeWrapper<JobDetails>;
  JobFilters: JobFilters;
  JobMessage: ResolverTypeWrapper<JobMessage>;
  JobStatus: JobStatus;
  JobUserVolume: ResolverTypeWrapper<JobUserVolume>;
  Mutation: ResolverTypeWrapper<{}>;
  Query: ResolverTypeWrapper<{}>;
  Resource: ResolverTypeWrapper<Resource>;
  Root: ResolverTypeWrapper<Root>;
  RootVolume: ResolverTypeWrapper<RootVolume>;
  String: ResolverTypeWrapper<Scalars['String']>;
  Subscription: ResolverTypeWrapper<{}>;
  URL: ResolverTypeWrapper<Scalars['URL']>;
  UUID: ResolverTypeWrapper<Scalars['UUID']>;
  User: ResolverTypeWrapper<User>;
  UserVolume: ResolverTypeWrapper<UserVolume>;
  VolumeType: VolumeType;
};

/** Mapping between all available schema types and the resolvers parents */
export type ResolversParentTypes = {
  Boolean: Scalars['Boolean'];
  ComputeDataVolume: ComputeDataVolume;
  Container: Container;
  ContainerDetail: ContainerDetail;
  ContainerDetailParams: ContainerDetailParams;
  ContainerParams: ContainerParams;
  CreateJobParams: CreateJobParams;
  DataVolume: DataVolume;
  Dataset: Dataset;
  DatasetDetailInput: DatasetDetailInput;
  DateTime: Scalars['DateTime'];
  Domain: Domain;
  File: File;
  FileService: FileService;
  Float: Scalars['Float'];
  Folder: Folder;
  ID: Scalars['ID'];
  Image: Image;
  Int: Scalars['Int'];
  JSONObject: Scalars['JSONObject'];
  JSONTree: JsonTree;
  Job: Job;
  JobDetails: JobDetails;
  JobFilters: JobFilters;
  JobMessage: JobMessage;
  JobUserVolume: JobUserVolume;
  Mutation: {};
  Query: {};
  Resource: Resource;
  Root: Root;
  RootVolume: RootVolume;
  String: Scalars['String'];
  Subscription: {};
  URL: Scalars['URL'];
  UUID: Scalars['UUID'];
  User: User;
  UserVolume: UserVolume;
};

export type ComputeDataVolumeResolvers<ContextType = Context, ParentType extends ResolversParentTypes['ComputeDataVolume'] = ResolversParentTypes['ComputeDataVolume']> = {
  publisherDID?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  writable?: Resolver<ResolversTypes['Boolean'], ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type ContainerResolvers<ContextType = Context, ParentType extends ResolversParentTypes['Container'] = ResolversParentTypes['Container']> = {
  accessedAt?: Resolver<ResolversTypes['DateTime'], ParentType, ContextType>;
  createdAt?: Resolver<ResolversTypes['DateTime'], ParentType, ContextType>;
  dataVolumes?: Resolver<Array<ResolversTypes['ComputeDataVolume']>, ParentType, ContextType>;
  description?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  displayName?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  domainID?: Resolver<ResolversTypes['Int'], ParentType, ContextType>;
  domainName?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  id?: Resolver<ResolversTypes['ID'], ParentType, ContextType>;
  imageName?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  maxSecs?: Resolver<ResolversTypes['Int'], ParentType, ContextType>;
  name?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  nodeName?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  status?: Resolver<ResolversTypes['ContainerStatus'], ParentType, ContextType>;
  userVolumes?: Resolver<Array<ResolversTypes['ID']>, ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type ContainerDetailResolvers<ContextType = Context, ParentType extends ResolversParentTypes['ContainerDetail'] = ResolversParentTypes['ContainerDetail']> = {
  dataVolumes?: Resolver<Array<ResolversTypes['DataVolume']>, ParentType, ContextType>;
  id?: Resolver<ResolversTypes['ID'], ParentType, ContextType>;
  userVolumes?: Resolver<Array<ResolversTypes['UserVolume']>, ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type DataVolumeResolvers<ContextType = Context, ParentType extends ResolversParentTypes['DataVolume'] = ResolversParentTypes['DataVolume']> = {
  allowedActions?: Resolver<Array<Maybe<ResolversTypes['String']>>, ParentType, ContextType>;
  description?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  displayName?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  id?: Resolver<ResolversTypes['ID'], ParentType, ContextType>;
  name?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  owningResourceId?: Resolver<Maybe<ResolversTypes['ID']>, ParentType, ContextType>;
  pathOnFileSystem?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  publisherDID?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  racmUUID?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  resourceUUID?: Resolver<Maybe<ResolversTypes['ID']>, ParentType, ContextType>;
  sharedWith?: Resolver<Array<Maybe<ResolversTypes['String']>>, ParentType, ContextType>;
  url?: Resolver<Maybe<ResolversTypes['URL']>, ParentType, ContextType>;
  writable?: Resolver<ResolversTypes['Boolean'], ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type DatasetResolvers<ContextType = Context, ParentType extends ResolversParentTypes['Dataset'] = ResolversParentTypes['Dataset']> = {
  catalog?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  description?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  logo?: Resolver<Maybe<ResolversTypes['URL']>, ParentType, ContextType>;
  name?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  readme?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  resources?: Resolver<Array<ResolversTypes['Resource']>, ParentType, ContextType>;
  source?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  summary?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  tags?: Resolver<Array<ResolversTypes['String']>, ParentType, ContextType>;
  volumeID?: Resolver<ResolversTypes['ID'], ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export interface DateTimeScalarConfig extends GraphQLScalarTypeConfig<ResolversTypes['DateTime'], any> {
  name: 'DateTime';
}

export type DomainResolvers<ContextType = Context, ParentType extends ResolversParentTypes['Domain'] = ResolversParentTypes['Domain']> = {
  apiEndpoint?: Resolver<ResolversTypes['URL'], ParentType, ContextType>;
  dataVolumes?: Resolver<Array<ResolversTypes['DataVolume']>, ParentType, ContextType>;
  description?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  id?: Resolver<ResolversTypes['ID'], ParentType, ContextType>;
  images?: Resolver<Array<ResolversTypes['Image']>, ParentType, ContextType>;
  name?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  publisherDID?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  racmUUID?: Resolver<ResolversTypes['UUID'], ParentType, ContextType>;
  userVolumes?: Resolver<Array<ResolversTypes['UserVolume']>, ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type FileResolvers<ContextType = Context, ParentType extends ResolversParentTypes['File'] = ResolversParentTypes['File']> = {
  creationTime?: Resolver<ResolversTypes['DateTime'], ParentType, ContextType>;
  lastModified?: Resolver<ResolversTypes['DateTime'], ParentType, ContextType>;
  name?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  size?: Resolver<ResolversTypes['Float'], ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type FileServiceResolvers<ContextType = Context, ParentType extends ResolversParentTypes['FileService'] = ResolversParentTypes['FileService']> = {
  apiEndpoint?: Resolver<Maybe<ResolversTypes['URL']>, ParentType, ContextType>;
  dataVolumes?: Resolver<Maybe<Array<ResolversTypes['DataVolume']>>, ParentType, ContextType>;
  description?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  identifier?: Resolver<ResolversTypes['ID'], ParentType, ContextType>;
  name?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  rootVolumes?: Resolver<Maybe<Array<ResolversTypes['RootVolume']>>, ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type FolderResolvers<ContextType = Context, ParentType extends ResolversParentTypes['Folder'] = ResolversParentTypes['Folder']> = {
  creationTime?: Resolver<ResolversTypes['DateTime'], ParentType, ContextType>;
  lastModified?: Resolver<ResolversTypes['DateTime'], ParentType, ContextType>;
  name?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type ImageResolvers<ContextType = Context, ParentType extends ResolversParentTypes['Image'] = ResolversParentTypes['Image']> = {
  description?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  id?: Resolver<ResolversTypes['ID'], ParentType, ContextType>;
  name?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  publisherDID?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  racmUUID?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export interface JsonObjectScalarConfig extends GraphQLScalarTypeConfig<ResolversTypes['JSONObject'], any> {
  name: 'JSONObject';
}

export type JsonTreeResolvers<ContextType = Context, ParentType extends ResolversParentTypes['JSONTree'] = ResolversParentTypes['JSONTree']> = {
  queryPath?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  root?: Resolver<ResolversTypes['Root'], ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type JobResolvers<ContextType = Context, ParentType extends ResolversParentTypes['Job'] = ResolversParentTypes['Job']> = {
  command?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  dataVolumes?: Resolver<Array<ResolversTypes['DataVolume']>, ParentType, ContextType>;
  dockerComputeEndpoint?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  dockerComputeResourceContextUUID?: Resolver<Maybe<ResolversTypes['UUID']>, ParentType, ContextType>;
  dockerImageName?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  duration?: Resolver<ResolversTypes['Float'], ParentType, ContextType>;
  endTime?: Resolver<Maybe<ResolversTypes['DateTime']>, ParentType, ContextType>;
  executorDID?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  fullDockerCommand?: Resolver<Array<ResolversTypes['String']>, ParentType, ContextType>;
  id?: Resolver<ResolversTypes['ID'], ParentType, ContextType>;
  messages?: Resolver<Array<ResolversTypes['JobMessage']>, ParentType, ContextType>;
  resultsFolderURI?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  runByUUID?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  scriptURI?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  startTime?: Resolver<Maybe<ResolversTypes['DateTime']>, ParentType, ContextType>;
  status?: Resolver<ResolversTypes['JobStatus'], ParentType, ContextType>;
  submissionTime?: Resolver<ResolversTypes['DateTime'], ParentType, ContextType>;
  submitterDID?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  submitterTrustId?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  timeout?: Resolver<Maybe<ResolversTypes['Int']>, ParentType, ContextType>;
  type?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  userVolumes?: Resolver<Array<ResolversTypes['JobUserVolume']>, ParentType, ContextType>;
  username?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type JobDetailsResolvers<ContextType = Context, ParentType extends ResolversParentTypes['JobDetails'] = ResolversParentTypes['JobDetails']> = {
  files?: Resolver<Array<ResolversTypes['File']>, ParentType, ContextType>;
  job?: Resolver<ResolversTypes['Job'], ParentType, ContextType>;
  summary?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type JobMessageResolvers<ContextType = Context, ParentType extends ResolversParentTypes['JobMessage'] = ResolversParentTypes['JobMessage']> = {
  content?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  id?: Resolver<ResolversTypes['ID'], ParentType, ContextType>;
  label?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type JobUserVolumeResolvers<ContextType = Context, ParentType extends ResolversParentTypes['JobUserVolume'] = ResolversParentTypes['JobUserVolume']> = {
  fullPath?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  id?: Resolver<ResolversTypes['ID'], ParentType, ContextType>;
  needsWriteAccess?: Resolver<ResolversTypes['Boolean'], ParentType, ContextType>;
  userVolumeId?: Resolver<ResolversTypes['ID'], ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type MutationResolvers<ContextType = Context, ParentType extends ResolversParentTypes['Mutation'] = ResolversParentTypes['Mutation']> = {
  createJob?: Resolver<ResolversTypes['Job'], ParentType, ContextType, RequireFields<MutationCreateJobArgs, 'createJobParams'>>;
  deleteContainer?: Resolver<ResolversTypes['Boolean'], ParentType, ContextType, RequireFields<MutationDeleteContainerArgs, 'containerId' | 'domainId'>>;
  login?: Resolver<ResolversTypes['String'], ParentType, ContextType, RequireFields<MutationLoginArgs, 'password' | 'username'>>;
  root?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
};

export type QueryResolvers<ContextType = Context, ParentType extends ResolversParentTypes['Query'] = ResolversParentTypes['Query']> = {
  getContainerDetail?: Resolver<ResolversTypes['ContainerDetail'], ParentType, ContextType, RequireFields<QueryGetContainerDetailArgs, 'containerDetailParams'>>;
  getContainerID?: Resolver<ResolversTypes['ID'], ParentType, ContextType, RequireFields<QueryGetContainerIdArgs, 'containerParams'>>;
  getContainers?: Resolver<Array<ResolversTypes['Container']>, ParentType, ContextType>;
  getDataset?: Resolver<Maybe<ResolversTypes['Dataset']>, ParentType, ContextType, RequireFields<QueryGetDatasetArgs, 'params'>>;
  getDatasets?: Resolver<Array<ResolversTypes['Dataset']>, ParentType, ContextType, RequireFields<QueryGetDatasetsArgs, 'volumeType'>>;
  getDomainByID?: Resolver<Maybe<ResolversTypes['Domain']>, ParentType, ContextType, RequireFields<QueryGetDomainByIdArgs, 'id'>>;
  getDomains?: Resolver<Array<ResolversTypes['Domain']>, ParentType, ContextType, Partial<QueryGetDomainsArgs>>;
  getJobDetails?: Resolver<ResolversTypes['JobDetails'], ParentType, ContextType, RequireFields<QueryGetJobDetailsArgs, 'jobId'>>;
  getJobs?: Resolver<Array<ResolversTypes['Job']>, ParentType, ContextType, Partial<QueryGetJobsArgs>>;
  getJsonTree?: Resolver<ResolversTypes['JSONTree'], ParentType, ContextType, RequireFields<QueryGetJsonTreeArgs, 'volumeName'>>;
  getUser?: Resolver<ResolversTypes['User'], ParentType, ContextType>;
  getVolumes?: Resolver<Maybe<ResolversTypes['FileService']>, ParentType, ContextType>;
  pingContainer?: Resolver<Maybe<ResolversTypes['Boolean']>, ParentType, ContextType, RequireFields<QueryPingContainerArgs, 'containerId'>>;
  root?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
};

export type ResourceResolvers<ContextType = Context, ParentType extends ResolversParentTypes['Resource'] = ResolversParentTypes['Resource']> = {
  description?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  kind?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  link?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  name?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type RootResolvers<ContextType = Context, ParentType extends ResolversParentTypes['Root'] = ResolversParentTypes['Root']> = {
  creationTime?: Resolver<ResolversTypes['DateTime'], ParentType, ContextType>;
  files?: Resolver<Array<ResolversTypes['File']>, ParentType, ContextType>;
  folders?: Resolver<Array<ResolversTypes['Folder']>, ParentType, ContextType>;
  lastModified?: Resolver<ResolversTypes['DateTime'], ParentType, ContextType>;
  name?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type RootVolumeResolvers<ContextType = Context, ParentType extends ResolversParentTypes['RootVolume'] = ResolversParentTypes['RootVolume']> = {
  allowedActions?: Resolver<Array<Maybe<ResolversTypes['String']>>, ParentType, ContextType>;
  containsSharedVolumes?: Resolver<ResolversTypes['Boolean'], ParentType, ContextType>;
  description?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  id?: Resolver<ResolversTypes['ID'], ParentType, ContextType>;
  name?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  owningResourceId?: Resolver<Maybe<ResolversTypes['ID']>, ParentType, ContextType>;
  pathOnFileSystem?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  resourceUUID?: Resolver<ResolversTypes['ID'], ParentType, ContextType>;
  sharedWith?: Resolver<Array<Maybe<ResolversTypes['String']>>, ParentType, ContextType>;
  userVolumes?: Resolver<Array<ResolversTypes['UserVolume']>, ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type SubscriptionResolvers<ContextType = Context, ParentType extends ResolversParentTypes['Subscription'] = ResolversParentTypes['Subscription']> = {
  root?: SubscriptionResolver<Maybe<ResolversTypes['String']>, "root", ParentType, ContextType>;
};

export interface UrlScalarConfig extends GraphQLScalarTypeConfig<ResolversTypes['URL'], any> {
  name: 'URL';
}

export interface UuidScalarConfig extends GraphQLScalarTypeConfig<ResolversTypes['UUID'], any> {
  name: 'UUID';
}

export type UserResolvers<ContextType = Context, ParentType extends ResolversParentTypes['User'] = ResolversParentTypes['User']> = {
  email?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  id?: Resolver<ResolversTypes['ID'], ParentType, ContextType>;
  userName?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  visibility?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type UserVolumeResolvers<ContextType = Context, ParentType extends ResolversParentTypes['UserVolume'] = ResolversParentTypes['UserVolume']> = {
  allowedActions?: Resolver<Array<Maybe<ResolversTypes['String']>>, ParentType, ContextType>;
  description?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  id?: Resolver<ResolversTypes['ID'], ParentType, ContextType>;
  name?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  owner?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  owningResourceId?: Resolver<Maybe<ResolversTypes['ID']>, ParentType, ContextType>;
  relativePath?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  resourceUUID?: Resolver<ResolversTypes['ID'], ParentType, ContextType>;
  rootVolumeName?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  sharedWith?: Resolver<Array<Maybe<ResolversTypes['String']>>, ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type Resolvers<ContextType = Context> = {
  ComputeDataVolume?: ComputeDataVolumeResolvers<ContextType>;
  Container?: ContainerResolvers<ContextType>;
  ContainerDetail?: ContainerDetailResolvers<ContextType>;
  DataVolume?: DataVolumeResolvers<ContextType>;
  Dataset?: DatasetResolvers<ContextType>;
  DateTime?: GraphQLScalarType;
  Domain?: DomainResolvers<ContextType>;
  File?: FileResolvers<ContextType>;
  FileService?: FileServiceResolvers<ContextType>;
  Folder?: FolderResolvers<ContextType>;
  Image?: ImageResolvers<ContextType>;
  JSONObject?: GraphQLScalarType;
  JSONTree?: JsonTreeResolvers<ContextType>;
  Job?: JobResolvers<ContextType>;
  JobDetails?: JobDetailsResolvers<ContextType>;
  JobMessage?: JobMessageResolvers<ContextType>;
  JobUserVolume?: JobUserVolumeResolvers<ContextType>;
  Mutation?: MutationResolvers<ContextType>;
  Query?: QueryResolvers<ContextType>;
  Resource?: ResourceResolvers<ContextType>;
  Root?: RootResolvers<ContextType>;
  RootVolume?: RootVolumeResolvers<ContextType>;
  Subscription?: SubscriptionResolvers<ContextType>;
  URL?: GraphQLScalarType;
  UUID?: GraphQLScalarType;
  User?: UserResolvers<ContextType>;
  UserVolume?: UserVolumeResolvers<ContextType>;
};

