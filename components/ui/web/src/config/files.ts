import { VolumeType } from '../graphql/typings';
import { FilesRoute } from '../types/files';
import { VolumeAction } from '../types/sharing';

/** The Files screen starts at the list of the user's volumes. */
export const DEFAULT_FILES_ROUTE: FilesRoute = { volumeType: VolumeType.Uservolume, path: '' };

/** Where volumes are mounted inside a compute container. NEXT_PUBLIC_JOB_WORKSPACE_PATH overrides it for jobs. */
export const DEFAULT_WORKSPACE_ROOT = '/home/idies/workspace/';

export const FILE_SERVICE_NOT_CONFIGURED = 'The file service address is not configured (NEXT_PUBLIC_FILE_SERVICE_URL).';

/** What can be allowed on a volume, in the order the sharing dialog and the access icons list them. */
export const VOLUME_ACTIONS: VolumeAction[] = ['read', 'write', 'grant', 'delete'];

/** A breadcrumb shows at most this many characters of folder names; older folders collapse into an ellipsis. */
export const BREADCRUMB_MAX_CHARS = 60;

/** How each volume action is shown: a label for screen readers and tooltips, and a Material Symbols icon. */
export const VOLUME_ACTION_DISPLAY: Record<VolumeAction, { label: string; icon: string }> = {
  read: { label: 'Read', icon: 'visibility' },
  write: { label: 'Write', icon: 'edit' },
  grant: { label: 'Grant', icon: 'share' },
  delete: { label: 'Delete', icon: 'delete' }
};
