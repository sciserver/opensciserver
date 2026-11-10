import { Root } from '../graphql/typings';
import { EntrySortKey, FileEntry } from '../types/files';

const toMillis = (value: unknown): number => {
  const millis = new Date(value as string | number).getTime();
  return Number.isNaN(millis) ? 0 : millis;
};

/** Flattens a getJsonTree root into one list of folders and files. */
export const buildEntries = (root?: Pick<Root, 'folders' | 'files'> | null): FileEntry[] => [
  ...(root?.folders || []).map((folder) => ({ name: folder.name, isFolder: true, size: 0, modified: toMillis(folder.lastModified) })),
  ...(root?.files || []).map((file) => ({ name: file.name, isFolder: false, size: file.size, modified: toMillis(file.lastModified) }))
];

/** Hidden by Linux convention: the name starts with a dot. */
export const isHiddenEntry = (entry: Pick<FileEntry, 'name'>): boolean => entry.name.startsWith('.');

/**
 * Folders always come first, then the chosen sort within each group. Names ignore a leading dot when sorting.
 * Entries whose name starts with a dot are left out unless `showHidden` is set.
 */
export const filterAndSortEntries = (entries: FileEntry[], filter: string, key: EntrySortKey, direction: 1 | -1, showHidden: boolean): FileEntry[] => {
  const needle = filter.trim().toLowerCase();
  const sortName = (entry: FileEntry) => entry.name.toLowerCase().replace(/^\./, '');
  const compare = (a: FileEntry, b: FileEntry) => {
    if (a.isFolder !== b.isFolder) {
      return a.isFolder ? -1 : 1;
    }
    const byKey = key === 'name' ? 0 : (a[key] - b[key]) * direction;
    return byKey || sortName(a).localeCompare(sortName(b)) * (key === 'name' ? direction : 1);
  };
  return entries
    .filter((entry) => (showHidden || !isHiddenEntry(entry)) && (!needle || entry.name.toLowerCase().includes(needle)))
    .sort(compare);
};

export const formatBytes = (bytes: number): string => {
  if (bytes < 1000) {
    return `${bytes} B`;
  }
  // This is IDIES: the scale goes past what any volume holds today.
  const units = ['KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
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

/**
 * Returns why `name` can't be used for a folder, file or rename, or '' when it is fine. `own` is the name being renamed.
 * The name is checked exactly as typed: whitespace is legal on the file system, so it is warned about (see
 * warnEntryName) rather than trimmed or refused.
 */
export const validateEntryName = (name: string, existingNames: Iterable<string>, own?: string): string => {
  if (name === '') {
    return 'Name can’t be empty';
  }
  if (/[/\\]/.test(name)) {
    return 'Name can’t contain / or \\';
  }
  if (name === '.' || name === '..') {
    return 'Name can’t be . or ..';
  }
  if (name !== own && new Set(existingNames).has(name)) {
    return 'A file or folder with this name exists';
  }
  return '';
};

/** A heads-up for names that are legal but easy to get wrong, or '' when there is nothing to say. */
export const warnEntryName = (name: string): string => (
  name !== '' && name !== name.trim()
    ? 'This name starts or ends with a space. That is allowed, but it is easy to miss and awkward in a terminal.'
    : ''
);

/** "New folder", then "New folder 2", "New folder 3"... */
export const defaultFolderName = (existingNames: Iterable<string>): string => {
  const taken = new Set(existingNames);
  let name = 'New folder';
  let counter = 2;
  while (taken.has(name)) {
    name = `New folder ${counter}`;
    counter += 1;
  }
  return name;
};
