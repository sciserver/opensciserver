import { describe, expect, it } from 'vitest';
import { PrincipalType } from '../graphql/typings';
import { addMember, buildSharePayload, removeMember, searchPrincipals, sharingChanged, toggleAction } from './sharing';

const user = { id: '5', name: 'ana', type: PrincipalType.User };
const group = { id: '5', name: 'astro', type: PrincipalType.Group };
const member = (p: typeof user, allowedActions: string[]) => ({ ...p, allowedActions });

describe('sharing members', () => {
  it('adds with read access and ignores duplicates, telling a user from a group with the same id', () => {
    const one = addMember([], user);
    expect(one).toEqual([member(user, ['read'])]);
    expect(addMember(one, user)).toBe(one);
    expect(addMember(one, group)).toHaveLength(2);
  });

  it('toggles one action on one member only', () => {
    const members = [member(user, ['read']), member(group, ['read'])];
    const next = toggleAction(members, user, 'write');
    expect(next[0].allowedActions).toEqual(['read', 'write']);
    expect(next[1].allowedActions).toEqual(['read']);
    expect(toggleAction(next, user, 'write')[0].allowedActions).toEqual(['read']);
  });

  it('removes by type and id', () => {
    expect(removeMember([member(user, ['read']), member(group, ['read'])], user)).toEqual([member(group, ['read'])]);
  });
});

describe('buildSharePayload', () => {
  it('sends removed members with no actions so access is revoked', () => {
    const original = [member(user, ['read']), member(group, ['read', 'write'])];
    const current = [member(group, ['read', 'write'])];
    expect(buildSharePayload(original, current)).toEqual([member(group, ['read', 'write']), member(user, [])]);
  });

  it('detects changes in membership and in actions', () => {
    const original = [member(user, ['read'])];
    expect(sharingChanged(original, [member(user, ['read'])])).toBe(false);
    expect(sharingChanged(original, [member(user, ['read', 'write'])])).toBe(true);
    expect(sharingChanged(original, [])).toBe(true);
  });
});

describe('searchPrincipals', () => {
  const all = [user, group, { id: '6', name: 'bob', type: PrincipalType.User }];

  it('hides members, filters by name and caps the list', () => {
    expect(searchPrincipals(all, [member(user, ['read'])], '', 10).shown.map((p) => p.name)).toEqual(['astro', 'bob']);
    expect(searchPrincipals(all, [], 'BO', 10).shown.map((p) => p.name)).toEqual(['bob']);
    expect(searchPrincipals(all, [], '', 1)).toMatchObject({ total: 3 });
  });
});
