import { PrincipalType, SharedWith, SharePrincipal } from '../graphql/typings';

export const SHARE_ACTIONS = ['read', 'write', 'grant', 'delete'] as const;
export type ShareAction = typeof SHARE_ACTIONS[number];

type Member = Pick<SharedWith, 'id' | 'name' | 'type' | 'allowedActions'>;

/** User and group ids can collide, so a principal is identified by type and id together. */
export const principalKey = (principal: { id: string | number; type: PrincipalType }): string => `${principal.type}:${principal.id}`;

/** Adds a user or group with read access, unless they are already a member. */
export const addMember = (members: Member[], principal: Pick<SharePrincipal, 'id' | 'name' | 'type'>): Member[] => (
  members.some((member) => principalKey(member) === principalKey(principal))
    ? members
    : [...members, { id: principal.id, name: principal.name, type: principal.type, allowedActions: ['read'] }]
);

export const removeMember = (members: Member[], target: Pick<Member, 'id' | 'type'>): Member[] => (
  members.filter((member) => principalKey(member) !== principalKey(target))
);

export const toggleAction = (members: Member[], target: Pick<Member, 'id' | 'type'>, action: ShareAction): Member[] => (
  members.map((member) => {
    if (principalKey(member) !== principalKey(target)) {
      return member;
    }
    const has = member.allowedActions.includes(action);
    return { ...member, allowedActions: has ? member.allowedActions.filter((a) => a !== action) : [...member.allowedActions, action] };
  })
);

/**
 * The share endpoint replaces the whole list, and revokes access by receiving a member with no actions.
 * So the payload is the current members plus everyone who was removed, with an empty action list.
 */
export const buildSharePayload = (original: Member[], current: Member[]): Member[] => {
  const kept = new Set(current.map((member) => principalKey(member)));
  const removed = original.filter((member) => !kept.has(principalKey(member))).map((member) => ({ ...member, allowedActions: [] as string[] }));
  return [...current, ...removed].map(({ id, name, type, allowedActions }) => ({ id, name, type, allowedActions }));
};

export const sharingChanged = (original: Member[], current: Member[]): boolean => {
  if (original.length !== current.length) {
    return true;
  }
  const before = new Map(original.map((member) => [principalKey(member), [...member.allowedActions].sort().join(',')]));
  return current.some((member) => before.get(principalKey(member)) !== [...member.allowedActions].sort().join(','));
};

/** Principals matching the search that aren't members yet, capped so a huge directory stays fast to render. */
export const searchPrincipals = (
  principals: SharePrincipal[],
  members: Member[],
  needle: string,
  limit: number,
  type?: PrincipalType
): { shown: SharePrincipal[]; total: number } => {
  const query = needle.trim().toLowerCase();
  const memberKeys = new Set(members.map((member) => principalKey(member)));
  const matches = principals.filter((principal) => !memberKeys.has(principalKey(principal)) && (!type || principal.type === type) && (!query || principal.name.toLowerCase().includes(query)));
  return { shown: matches.slice(0, limit), total: matches.length };
};
