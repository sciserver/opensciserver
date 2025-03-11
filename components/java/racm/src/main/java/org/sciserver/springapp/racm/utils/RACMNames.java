package org.sciserver.springapp.racm.utils;

/**
 * This class represents names of context classes, resource types, actions and roles that must be available as reference data in the database.<br/>
 * The class also defines validation/testing methods that should be called to check that database and source code are in synch.
 *
 */
public class RACMNames {
	private RACMNames() {}

	// ~~~~~~~~~ System RACM names ~~~~~~~~~~
	/** name of Root level System context class */
	public static final String SYSTEM_CONTEXTCLASS_NAME = "System";

	public static final String CONTEXT_ROOTRESOURCE_PUBDID = "__rootcontext__";
	public static final String CONTEXT_ROOTRESOURCE_UUID = "505190c6-0ffd-4e16-8e24-04796e44e59b";
	public static final String SYSTEM_ADMIN_ROLE = "admin";
	public static final String SYSTEM_PUBLIC_ROLE = "public";
	public static final String CONTEXT_UUID = "00cdea23-09de-4eb3-bd73-b2b459cf5a4d";
	public static final String CONTEXTCLASS_RELEASE = "0.x";
	public static final String A_REGISTER_COMPUTE_DOMAIN = "registerComputeDomain";
	public static final String A_REGISTER_COMPMM = "registerCOMPM";
	public static final String A_CREATE_GROUP = "createGroup";
	public static final String A_CREATE_PUBLIC_GROUP = "createPublicGroup";
	public static final String A_CREATE_CONTEXT_CLASS = "createContextClass";
	public static final String A_CREATE_CONTEXT = "createContext";
	public static final String A_GRANT_ROOT_PRIVILEGE = "grantRootPrivilege";
	public static final String A_QUERY_JOQL = "queryJOQL";
	public static final String A_CREATE_ANY_RESOURCE = "createAnyResource";
	public static final String A_GRANT = "grant";

	// may need to be moved to standard, publicly accessible config file.
	public static final String USERGROUP_ADMIN = "admin";
	public static final String USERGROUP_PUBLIC = "public";


	//  ~~~~~~  DockerComputeDomain RACM names ~~~~~~~~
	// ContextClass
	public static final String DOCKER_COMPUTE_DOMAIN_CC_NAME = "DockerComputeDomain";
	public static final String R_COMPUTE_DOMAIN_ROOT_ADMIN = "admin";
	//actions on __context__
	public static final String A_CREATE_DOCKER_CONTAINER = "createDockerContainer";
	public static final String A_REGISTER_VOLUME_CONTAINER = "registerVolumeContainer";
	public static final String A_REGISTER_DOCKER_IMAGE = "registerDockerImage";

	// PublicVolume ResourceType
	public static final String RT_VOLUME_CONTAINER = "VolumeContainer";
	public static final String A_VOLUME_CONTAINER_READ = "read";
	public static final String A_VOLUME_CONTAINER_WRITE = "write";
	public static final String A_VOLUME_CONTAINER_GRANT = "grant";
	public static final String R_VOLUME_CONTAINER_USER = "user";
	public static final String R_VOLUME_CONTAINER_ADMIN = "admin";

	// DockerImage ResourceType
	public static final String RT_DOCKER_IMAGE = "DockerImage";
	public static final String A_DOCKER_IMAGE_CREATE_CONTAINER = "createContainer";
	public static final String A_DOCKER_IMAGE_UNREGISTER = "unregister";
	public static final String A_DOCKER_IMAGE_GRANT = "grant";
	public static final String R_DOCKER_IMAGE_USER = "user";
	public static final String R_DOCKER_IMAGE_ADMIN = "admin";

	// DockerContainer ResourceType
	public static final String RT_DOCKER_CONTAINER = "ExecutableContainer";
	public static final String A_DOCKER_CONTAINER_STOP = "stop";
	public static final String A_DOCKER_CONTAINER_START = "start";
	public static final String A_DOCKER_CONTAINER_DELETE = "delete";
	public static final String A_DOCKER_CONTAINER_READ = "read";
	public static final String A_DOCKER_CONTAINER_WRITE = "write";
	public static final String A_DOCKER_CONTAINER_GRANT = "grant";
	public static final String R_DOCKER_CONTAINER_READER = "reader";
	public static final String R_DOCKER_CONTAINER_WRITER = "writer";
	public static final String R_DOCKER_CONTAINER_OWNER = "owner";

	//  ~~~~~~  DockerComputeDomain RACM names ~~~~~~~~
	// ContextClass
	public static final String RDB_COMPUTE_DOMAIN_CC_NAME = "RDBComputeDomain";
	public static final String R_RDB_DOMAIN_ROOT_ADMIN = "admin";
	//actions on __context__
	public static final String A_REGISTER_DATABASE_CONTEXT = "registerDatabaseContext";

	// PublicVolume ResourceType
	public static final String RT_DATABASE_CONTEXT = "DatabaseContext";
	public static final String A_DATABASE_CONTEXT_QUERY = "QUERY";
	public static final String A_DATABASE_CONTEXT_UPDATE = "UPDATE";
	public static final String A_DATABASE_CONTEXT_GRANT = "GRANT";
	public static final String R_DATABASE_CONTEXT_READER = "reader";
	public static final String R_DATABASE_CONTEXT_WRITER = "writer";
	public static final String R_DATABASE_CONTEXT_ADMIN = "admin";


	// ~~~~~ JODBM RACM names ~~~~~
	public static final String CC_JOBM_NAME = "JOBM";
	public static final String A_JOBM_DEFINE_JOB_TYPE = "defineJobType";
	public static final String A_JOBM_SUBMIT_JOB = "submitJob";
	public static final String RT_JOBM_JOB_TYPE = "JobType";
	public static final String A_JOBM_JOB_TYPE_UPDATE = "update";
	public static final String A_JOBM_JOB_TYPE_USE = "use";
	public static final String RT_JOBM_JOB = "JOBM.Job";
	public static final String A_JOBM_JOB_VIEW = "view";

	public static final String CC_CASJOBS_NAME = "CasJobs";
	public static final String A_CASJOBS_REGISTER_DATABASECONTEXT = "registerDatabaseContext";
	public static final String RT_CASJOBS_DATABASECONTEXT = "Casjobs.DatabaseContext";
	public static final String A_CASJOBS_DATABASECONTEXT_SUBMIT_QUERY = "submitQuery";
	public static final String A_CASJOBS_DATABASECONTEXT_VIEW_SCHEMA = "viewSchema";
}
