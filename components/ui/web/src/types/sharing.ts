import { SharedWith } from '../graphql/typings';

export type ShareAction = 'read' | 'write' | 'grant' | 'delete';

/** Someone a volume is shared with, as the share endpoint takes it. */
export type ShareMember = Pick<SharedWith, 'id' | 'name' | 'type' | 'allowedActions'>;
