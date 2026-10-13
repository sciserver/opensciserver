import { FileUsage, RootVolume, UserVolume } from '../graphql/typings';
import { QuotaGroup, QuotaItem } from '../types/quotas';

// Only the ids and names are needed to label a quota, so that is all the caller has to provide.
type RootVolumeNames = Pick<RootVolume, 'id' | 'name'> & { userVolumes: Pick<UserVolume, 'id' | 'name'>[] };

const fractionOf = (used: number, quota: number): number => (quota > 0 ? Math.min(1, used / quota) : 1);

/**
 * Groups usage by root volume, as the old dashboard did. Groups are sorted by root volume name; inside a
 * group by quota type, then the signed-in user's own quota first, then username, then volume name.
 */
export const groupQuotas = (usage: FileUsage[], roots: RootVolumeNames[], userName?: string): QuotaGroup[] => {
  const rootName = new Map(roots.map((root) => [String(root.id), root.name || `Root volume ${root.id}`]));
  const volumeName = new Map(roots.flatMap((root) => root.userVolumes).map((volume) => [String(volume.id), volume.name]));

  const groups: Record<string, (QuotaItem & { type: string; username: string })[]> = {};
  for (const entry of usage) {
    const rootVolumeId = String(entry.rootVolumeId ?? '');
    const label = entry.username
      ? `${entry.username}’s user volumes`
      : volumeName.get(String(entry.userVolumeId)) || `Volume ${entry.userVolumeId}`;
    const item = {
      key: `${rootVolumeId}/${entry.type}/${entry.username}/${entry.userVolumeId}`,
      label,
      used: entry.numberOfBytesUsed,
      quota: entry.numberOfBytesQuota,
      fraction: fractionOf(entry.numberOfBytesUsed, entry.numberOfBytesQuota),
      full: entry.numberOfBytesUsed >= entry.numberOfBytesQuota,
      type: entry.type || '',
      username: entry.username || ''
    };
    groups[rootVolumeId] = [...(groups[rootVolumeId] || []), item];
  }

  return Object.entries(groups)
    .map(([rootVolumeId, items]) => ({
      rootVolumeId,
      rootVolumeName: rootName.get(rootVolumeId) || `Root volume ${rootVolumeId}`,
      items: items
        .sort((a, b) => (
          a.type.localeCompare(b.type)
          || Number(a.username !== userName) - Number(b.username !== userName)
          || a.username.localeCompare(b.username)
          || a.label.localeCompare(b.label)
        ))
        .map(({ type, username, ...item }) => item)
    }))
    .sort((a, b) => a.rootVolumeName.localeCompare(b.rootVolumeName));
};
