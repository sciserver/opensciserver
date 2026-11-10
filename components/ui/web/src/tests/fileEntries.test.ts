import { describe, expect, it } from 'vitest';
import { buildEntries, defaultFolderName, filterAndSortEntries, formatBytes, formatModified, isHiddenEntry, validateEntryName, warnEntryName } from '../utils/fileEntries';

const root = {
  folders: [{ name: 'logs', lastModified: '2026-01-21T15:22:00Z', creationTime: '' }, { name: '.hidden', lastModified: 'bad', creationTime: '' }],
  files: [
    { name: 'b.csv', size: 2000, lastModified: '2026-02-01T00:00:00Z', creationTime: '' },
    { name: 'A.txt', size: 10, lastModified: '2026-03-01T00:00:00Z', creationTime: '' }
  ]
};

describe('buildEntries', () => {
  it('flattens folders and files, tolerating bad dates', () => {
    const entries = buildEntries(root as never);
    expect(entries.map((e) => [e.name, e.isFolder])).toEqual([['logs', true], ['.hidden', true], ['b.csv', false], ['A.txt', false]]);
    expect(entries[1].modified).toBe(0);
    expect(buildEntries(null)).toEqual([]);
  });
});

describe('filterAndSortEntries', () => {
  const entries = buildEntries(root as never);
  const names = (list: ReturnType<typeof buildEntries>) => list.map((e) => e.name);

  it('puts folders first and ignores a leading dot and case when sorting by name', () => {
    expect(names(filterAndSortEntries(entries, '', 'name', 1, true))).toEqual(['.hidden', 'logs', 'A.txt', 'b.csv']);
  });

  it('keeps folders first when the direction flips', () => {
    expect(names(filterAndSortEntries(entries, '', 'name', -1, true))).toEqual(['logs', '.hidden', 'b.csv', 'A.txt']);
  });

  it('sorts files by size', () => {
    expect(names(filterAndSortEntries(entries, '', 'size', -1, true)).slice(2)).toEqual(['b.csv', 'A.txt']);
  });

  it('leaves out names that start with a dot unless hidden files are shown', () => {
    expect(names(filterAndSortEntries(entries, '', 'name', 1, false))).toEqual(['logs', 'A.txt', 'b.csv']);
    expect(names(filterAndSortEntries(entries, '', 'name', 1, true))).toContain('.hidden');
    expect(isHiddenEntry({ name: '.sciserver' })).toBe(true);
    expect(isHiddenEntry({ name: 'a.hidden' })).toBe(false);
  });

  it('filters case-insensitively', () => {
    expect(names(filterAndSortEntries(entries, 'CSV', 'name', 1, true))).toEqual(['b.csv']);
  });
});

describe('formatters', () => {
  it('formats bytes', () => {
    expect([0, 999, 1000, 41_400, 8_100_000, 3e9].map((bytes) => formatBytes(bytes))).toEqual(['0 B', '999 B', '1.0 KB', '41.4 KB', '8.1 MB', '3.0 GB']);
  });

  it('keeps going past terabytes, because this is IDIES', () => {
    expect([2e12, 5e15, 7e18, 1e21, 3e24].map((bytes) => formatBytes(bytes))).toEqual(['2.0 TB', '5.0 PB', '7.0 EB', '1.0 ZB', '3.0 YB']);
    // and stops at the largest unit instead of printing "undefined"
    expect(formatBytes(1e27)).toBe('1000.0 YB');
  });

  it('formats dates and unknowns', () => {
    expect(formatModified(0)).toBe('—');
    expect(formatModified(new Date(2026, 0, 21, 15, 2).getTime())).toBe('2026-01-21 15:02');
  });
});

describe('validateEntryName', () => {
  const existing = ['a.txt', 'logs'];

  it('rejects empty, slashes and dot names', () => {
    expect(validateEntryName('', existing)).toMatch(/empty/);
    expect(validateEntryName('a/b', existing)).toMatch(/contain/);
    expect(validateEntryName('a\\b', existing)).toMatch(/contain/);
    expect(validateEntryName('..', existing)).toMatch(/\.\./);
  });

  it('rejects an existing name unless it is the one being renamed', () => {
    expect(validateEntryName('logs', existing)).toMatch(/exists/);
    expect(validateEntryName('logs', existing, 'logs')).toBe('');
  });

  it('takes the name exactly as typed: whitespace is legal, and distinct from other whitespace', () => {
    expect(validateEntryName(' fresh ', existing)).toBe('');
    expect(validateEntryName('  ', existing)).toBe('');
    expect(validateEntryName(' ', [' '])).toMatch(/exists/);
    expect(validateEntryName('  ', [' '])).toBe('');
    // " logs" is not "logs"
    expect(validateEntryName(' logs', existing)).toBe('');
  });
});

describe('warnEntryName', () => {
  it('warns about leading or trailing whitespace but not about ordinary names', () => {
    expect(warnEntryName(' lead')).toMatch(/space/);
    expect(warnEntryName('trail ')).toMatch(/space/);
    expect(warnEntryName('   ')).toMatch(/space/);
    expect(warnEntryName('in side')).toBe('');
    expect(warnEntryName('')).toBe('');
  });
});

describe('defaultFolderName', () => {
  it('numbers from 2', () => {
    expect(defaultFolderName(['x'])).toBe('New folder');
    expect(defaultFolderName(['New folder', 'New folder 2'])).toBe('New folder 3');
  });
});
