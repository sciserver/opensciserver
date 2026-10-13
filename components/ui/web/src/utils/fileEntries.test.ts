import { describe, expect, it } from 'vitest';
import { buildEntries, defaultFolderName, filterAndSortEntries, formatBytes, formatModified, validateEntryName } from './fileEntries';

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
    expect(names(filterAndSortEntries(entries, '', 'name', 1))).toEqual(['.hidden', 'logs', 'A.txt', 'b.csv']);
  });

  it('keeps folders first when the direction flips', () => {
    expect(names(filterAndSortEntries(entries, '', 'name', -1))).toEqual(['logs', '.hidden', 'b.csv', 'A.txt']);
  });

  it('sorts files by size', () => {
    expect(names(filterAndSortEntries(entries, '', 'size', -1)).slice(2)).toEqual(['b.csv', 'A.txt']);
  });

  it('filters case-insensitively', () => {
    expect(names(filterAndSortEntries(entries, 'CSV', 'name', 1))).toEqual(['b.csv']);
  });
});

describe('formatters', () => {
  it('formats bytes', () => {
    expect([0, 999, 1000, 41_400, 8_100_000, 3e9].map((bytes) => formatBytes(bytes))).toEqual(['0 B', '999 B', '1.0 KB', '41.4 KB', '8.1 MB', '3.0 GB']);
  });

  it('formats dates and unknowns', () => {
    expect(formatModified(0)).toBe('—');
    expect(formatModified(new Date(2026, 0, 21, 15, 2).getTime())).toBe('2026-01-21 15:02');
  });
});

describe('validateEntryName', () => {
  const existing = ['a.txt', 'logs'];

  it('rejects empty, slashes and dot names', () => {
    expect(validateEntryName('  ', existing)).toMatch(/empty/);
    expect(validateEntryName('a/b', existing)).toMatch(/contain/);
    expect(validateEntryName('a\\b', existing)).toMatch(/contain/);
    expect(validateEntryName('..', existing)).toMatch(/\.\./);
  });

  it('rejects an existing name unless it is the one being renamed', () => {
    expect(validateEntryName('logs', existing)).toMatch(/exists/);
    expect(validateEntryName('logs', existing, 'logs')).toBe('');
    expect(validateEntryName(' fresh ', existing)).toBe('');
  });
});

describe('defaultFolderName', () => {
  it('numbers from 2', () => {
    expect(defaultFolderName(['x'])).toBe('New folder');
    expect(defaultFolderName(['New folder', 'New folder 2'])).toBe('New folder 3');
  });
});
