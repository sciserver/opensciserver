import { VolumeType } from '../graphql/typings';
import { FilesRoute } from '../types/files';
import { ShareAction } from '../types/sharing';

/** The Files screen starts at the list of the user's volumes. */
export const DEFAULT_FILES_ROUTE: FilesRoute = { volumeType: VolumeType.Uservolume, path: '' };

/** Where volumes are mounted inside a compute container. NEXT_PUBLIC_JOB_WORKSPACE_PATH overrides it for jobs. */
export const DEFAULT_WORKSPACE_ROOT = '/home/idies/workspace/';

export const FILE_SERVICE_NOT_CONFIGURED = 'The file service address is not configured (NEXT_PUBLIC_FILE_SERVICE_URL).';

/** The actions a volume can be shared with, in the order the sharing dialog lists them. */
export const SHARE_ACTIONS: ShareAction[] = ['read', 'write', 'grant', 'delete'];
