import { Root } from '../graphql/typings';

export type FileEntry = {
  name: string;
  isFolder: boolean;
  /** Bytes. 0 for folders. */
  size: number;
  /** Epoch millis, or 0 when the server sent no usable date. */
  modified: number;
};

export type EntrySortKey = 'name' | 'modified' | 'size';

const toMillis = (value: unknown): number => {
  const millis = new Date(value as string | number).getTime();
  return Number.isNaN(millis) ? 0 : millis;
};

/** Flattens a getJsonTree root into one list of folders and files. */
export const buildEntries = (root?: Pick<Root, 'folders' | 'files'> | null): FileEntry[] => [
  ...(root?.folders || []).map((folder) => ({ name: folder.name, isFolder: true, size: 0, modified: toMillis(folder.lastModified) })),
  ...(root?.files || []).map((file) => ({ name: file.name, isFolder: false, size: file.size, modified: toMillis(file.lastModified) }))
];

/** Folders always come first, then the chosen sort within each group. Names ignore a leading dot. */
export const filterAndSortEntries = (entries: FileEntry[], filter: string, key: EntrySortKey, direction: 1 | -1): FileEntry[] => {
  const needle = filter.trim().toLowerCase();
  const sortName = (entry: FileEntry) => entry.name.toLowerCase().replace(/^\./, '');
  const compare = (a: FileEntry, b: FileEntry) => {
    if (a.isFolder !== b.isFolder) {
      return a.isFolder ? -1 : 1;
    }
    const byKey = key === 'name' ? 0 : (a[key] - b[key]) * direction;
    return byKey || sortName(a).localeCompare(sortName(b)) * (key === 'name' ? direction : 1);
  };
  return entries.filter((entry) => !needle || entry.name.toLowerCase().includes(needle)).sort(compare);
};

export const formatBytes = (bytes: number): string => {
  if (bytes < 1000) {
    return `${bytes} B`;
  }
  const units = ['KB', 'MB', 'GB', 'TB'];
  let value = bytes;
  let unit = -1;
  while (value >= 1000 && unit < units.length - 1) {
    value /= 1000;
    unit += 1;
  }
  return `${value.toFixed(1)} ${units[unit]}`;
};

const pad = (value: number) => String(value).padStart(2, '0');

/** "2026-01-21 15:22" in local time, or an em dash when unknown. */
export const formatModified = (millis: number): string => {
  if (!millis) {
    return '—';
  }
  const date = new Date(millis);
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
};
