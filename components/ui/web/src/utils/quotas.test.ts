import { describe, expect, it } from 'vitest';
import { groupQuotas } from './quotas';

const roots = [
  { id: '1', name: 'Temporary', userVolumes: [{ id: '10', name: 'scratch' }] },
  { id: '2', name: 'Storage', userVolumes: [{ id: '20', name: 'persistent' }, { id: '21', name: 'FESS' }] }
];
const usage = (over: Record<string, unknown>) => ({ rootVolumeId: '2', type: 'A', numberOfBytesUsed: 10, numberOfBytesQuota: 100, ...over }) as never;

describe('groupQuotas', () => {
  it('groups by root volume name, sorted', () => {
    const groups = groupQuotas([usage({ rootVolumeId: '1', username: 'ana' }), usage({ username: 'ana' })], roots, 'ana');
    expect(groups.map((g) => g.rootVolumeName)).toEqual(['Storage', 'Temporary']);
  });

  it('labels per-user quotas by username and volume quotas by volume name', () => {
    const [group] = groupQuotas([usage({ username: 'ana' }), usage({ userVolumeId: '21' })], roots, 'ana');
    expect(group.items.map((i) => i.label)).toEqual(['ana’s user volumes', 'FESS']);
  });

  it('puts the signed-in user first and flags full quotas', () => {
    const [group] = groupQuotas([usage({ username: 'zed' }), usage({ username: 'me', numberOfBytesUsed: 100 })], roots, 'me');
    expect(group.items.map((i) => i.label)).toEqual(['me’s user volumes', 'zed’s user volumes']);
    expect(group.items[0]).toMatchObject({ fraction: 1, full: true });
    expect(group.items[1]).toMatchObject({ fraction: 0.1, full: false });
  });

  it('treats a zero quota as full instead of dividing by zero', () => {
    const [group] = groupQuotas([usage({ username: 'a', numberOfBytesQuota: 0, numberOfBytesUsed: 0 })], roots);
    expect(group.items[0]).toMatchObject({ fraction: 1, full: true });
  });
});
