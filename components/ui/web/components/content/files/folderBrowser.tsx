import { FC, useEffect, useMemo, useRef, useState } from 'react';
import { useQuery } from '@apollo/client';
import { Alert, Box, Button, ButtonBase } from '@mui/material';

import { JSON_TREE } from 'src/graphql/files';
import { JsonTree } from 'src/graphql/typings';
import { buildEntries, EntrySortKey, filterAndSortEntries, formatBytes, formatModified, FileEntry } from 'src/utils/fileEntries';
import { FilesRoute } from 'src/utils/fileVolumes';
import { joinPath, toVolumeRef } from 'src/utils/files';
import { LoadingAnimation } from 'components/common/loadingAnimation';

type Props = {
  route: FilesRoute & { volume: NonNullable<FilesRoute['volume']> };
  filter: string;
  /** Increment to reload the current folder. */
  refreshSignal: number;
  onOpenFolder: (path: string) => void;
};

const COLUMNS = '36px minmax(0,2fr) minmax(0,1fr) minmax(0,1fr)';
const ROW_HEIGHT = 38;

const CheckIcon: FC<{ name: string; active?: boolean }> = ({ name, active }) => (
  <span className="material-symbols-outlined" style={{ fontSize: 20, color: active ? '#398CBF' : 'rgba(0,0,0,0.5)' }}>{name}</span>
);

const Message: FC<{ icon: string; title: string; hint?: string }> = ({ icon, title, hint }) => (
  <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 0.75, py: 6, px: 2, color: 'rgba(0,0,0,0.6)', textAlign: 'center' }}>
    <span className="material-symbols-outlined" style={{ fontSize: 36, color: '#B0C1D9' }}>{icon}</span>
    <Box sx={{ fontWeight: 600, color: 'text.primary' }}>{title}</Box>
    {hint && <Box sx={{ fontSize: 13 }}>{hint}</Box>}
  </Box>
);

export const FolderBrowser: FC<Props> = ({ route, filter, refreshSignal, onOpenFolder }) => {
  const { volume, volumeType, path } = route;
  const { data, loading, error, refetch } = useQuery<{ getJsonTree: JsonTree }>(JSON_TREE, {
    variables: { volume: toVolumeRef({ volumeType, ...volume }), path },
    fetchPolicy: 'cache-and-network',
    notifyOnNetworkStatusChange: true
  });

  const [sortKey, setSortKey] = useState<EntrySortKey>('name');
  const [direction, setDirection] = useState<1 | -1>(1);
  const [checked, setChecked] = useState<Set<string>>(new Set());

  // A different folder or volume starts with nothing selected.
  useEffect(() => setChecked(new Set()), [path, volume.volumeName, volume.owner, volume.rootVolumeName, volumeType]);

  const firstRefresh = useRef(true);
  useEffect(() => {
    if (firstRefresh.current) {
      firstRefresh.current = false;
      return;
    }
    setChecked(new Set());
    refetch().catch(() => undefined);
  }, [refreshSignal]);

  const all = useMemo(() => buildEntries(data?.getJsonTree.root), [data]);
  const entries = useMemo(() => filterAndSortEntries(all, filter, sortKey, direction), [all, filter, sortKey, direction]);

  // Drop selections that a filter or reload removed.
  const selected = entries.filter((entry) => checked.has(entry.name));
  const allSelected = entries.length > 0 && selected.length === entries.length;

  const toggle = (name: string) => setChecked((current) => {
    const next = new Set(current);
    if (next.has(name)) {
      next.delete(name);
    }
    else {
      next.add(name);
    }
    return next;
  });

  const onSort = (key: EntrySortKey) => {
    if (key === sortKey) {
      setDirection((current) => (current === 1 ? -1 : 1));
      return;
    }
    setSortKey(key);
    setDirection(1);
  };

  const openEntry = (entry: FileEntry) => {
    if (entry.isFolder) {
      onOpenFolder(joinPath(path, entry.name));
    }
  };

  const headers: { key: EntrySortKey; label: string }[] = [
    { key: 'name', label: 'Name' },
    { key: 'modified', label: 'Modified' },
    { key: 'size', label: 'Size' }
  ];

  // Without cached data the first load (or a failed one) has nothing to list.
  const showLoading = loading && !data;
  const showError = !!error && !data;

  return (
    <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
      {selected.length > 0 && (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75, pl: 0.75, pr: 1.5, minHeight: 52, bgcolor: '#e8f1f8', flex: 'none' }}>
          <ButtonBase aria-label="Clear selection" onClick={() => setChecked(new Set())} sx={{ width: 36, height: 36, borderRadius: '50%', color: 'primary.main' }}>
            <span className="material-symbols-outlined" style={{ fontSize: 20 }}>close</span>
          </ButtonBase>
          <Box component="span" sx={{ fontWeight: 600, color: 'primary.main' }}>{selected.length} selected</Box>
        </Box>
      )}
      <Box role="row" sx={{ display: 'grid', gridTemplateColumns: COLUMNS, alignItems: 'center', px: 1, height: 34, borderBottom: '1px solid #e6e9ed', flex: 'none' }}>
        <ButtonBase
          aria-label={allSelected ? 'Deselect all' : 'Select all'}
          disabled={entries.length === 0}
          onClick={() => setChecked(allSelected ? new Set() : new Set(entries.map((entry) => entry.name)))}
        >
          <CheckIcon name={allSelected ? 'check_box' : selected.length ? 'indeterminate_check_box' : 'check_box_outline_blank'} active={selected.length > 0} />
        </ButtonBase>
        {headers.map(({ key, label }) => (
          <ButtonBase
            key={key}
            onClick={() => onSort(key)}
            aria-sort={sortKey === key ? (direction === 1 ? 'ascending' : 'descending') : 'none'}
            sx={{ justifySelf: 'start', gap: 0.5, fontSize: 12, fontWeight: 600, color: 'rgba(0,0,0,0.6)' }}
          >
            {label}
            <span className="material-symbols-outlined" style={{ fontSize: 15 }}>
              {sortKey === key ? (direction === 1 ? 'arrow_upward' : 'arrow_downward') : 'unfold_more'}
            </span>
          </ButtonBase>
        ))}
      </Box>
      <Box sx={{ flex: 1, minHeight: 0, overflow: 'auto' }}>
        <LoadingAnimation backDropIsOpen={showLoading} />
        {showError && (
          <Alert severity="error" sx={{ m: 2 }} action={<Button color="inherit" size="small" onClick={() => refetch().catch(() => undefined)}>Retry</Button>}>
            Could not load this folder: {error?.message}
          </Alert>
        )}
        {entries.map((entry) => {
          const isChecked = checked.has(entry.name);
          return (
            <Box
              key={entry.name}
              onClick={() => openEntry(entry)}
              sx={{ display: 'grid', gridTemplateColumns: COLUMNS, alignItems: 'center', px: 1, minHeight: ROW_HEIGHT, borderBottom: '1px solid #f0f2f4', cursor: entry.isFolder ? 'pointer' : 'default', bgcolor: isChecked ? 'rgba(57,140,191,0.07)' : 'transparent', '&:hover': { bgcolor: 'rgba(57,140,191,0.08)' } }}
            >
              <ButtonBase
                aria-label={`Select ${entry.name}`}
                aria-pressed={isChecked}
                onClick={(event) => {
                  event.stopPropagation(); toggle(entry.name); 
                }}
              >
                <CheckIcon name={isChecked ? 'check_box' : 'check_box_outline_blank'} active={isChecked} />
              </ButtonBase>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25, minWidth: 0, fontSize: 14, fontWeight: entry.isFolder ? 500 : 400 }}>
                <span className="material-symbols-outlined" style={{ fontSize: 22, flex: 'none', color: entry.isFolder ? '#398CBF' : 'rgba(0,0,0,0.55)', fontVariationSettings: `'FILL' ${entry.isFolder ? 1 : 0}` }}>
                  {entry.isFolder ? 'folder' : 'description'}
                </span>
                <Box component="span" sx={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{entry.name}</Box>
              </Box>
              <Box component="span" sx={{ fontSize: 13, color: 'rgba(0,0,0,0.62)', whiteSpace: 'nowrap' }}>{formatModified(entry.modified)}</Box>
              <Box component="span" sx={{ fontSize: 13, color: 'rgba(0,0,0,0.62)', whiteSpace: 'nowrap' }}>{entry.isFolder ? '—' : formatBytes(entry.size)}</Box>
            </Box>
          );
        })}
        {!showLoading && !showError && all.length === 0 && <Message icon="folder_open" title="This folder is empty" />}
        {!showLoading && !showError && all.length > 0 && entries.length === 0 && (
          <Message icon="search_off" title="Nothing matches your filter" hint="Try a different name." />
        )}
      </Box>
    </Box>
  );
};
