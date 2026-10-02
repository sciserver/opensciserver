import { FC, KeyboardEvent, useEffect, useMemo, useRef, useState } from 'react';
import { useMutation, useQuery } from '@apollo/client';
import { Alert, Box, Button, ButtonBase, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle, Menu, MenuItem } from '@mui/material';

import { CREATE_FOLDER, DELETE_FILE, JSON_TREE, RENAME_FILE } from 'src/graphql/files';
import { JsonTree } from 'src/graphql/typings';
import { buildEntries, defaultFolderName, EntrySortKey, FileEntry, filterAndSortEntries, formatBytes, formatModified, validateEntryName } from 'src/utils/fileEntries';
import { FilesRoute } from 'src/utils/fileVolumes';
import { joinPath, toVolumeRef } from 'src/utils/files';
import { LoadingAnimation } from 'components/common/loadingAnimation';

type Props = {
  route: FilesRoute & { volume: NonNullable<FilesRoute['volume']> };
  filter: string;
  /** Can the user change this folder? Hides the write actions when false. */
  writable: boolean;
  /** Increment to reload the current folder. */
  refreshSignal: number;
  /** Show the "new folder" name row. */
  creating: boolean;
  onCreatingDone: (createdName?: string) => void;
  onOpenFolder: (path: string) => void;
  notify: (message: string, severity?: 'success' | 'error') => void;
  /** Pick mode: files are greyed out, a click selects a folder and a double click opens it. */
  pick?: { selectedName: string | null; onSelect: (name: string) => void };
};

const COLUMNS = '36px minmax(0,2fr) minmax(0,1fr) minmax(0,1fr) 36px';
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

type NameInputProps = {
  initial: string;
  existingNames: string[];
  /** The name being renamed, which is allowed to stay as is. */
  own?: string;
  icon: string;
  hint: string;
  onCommit: (name: string) => void;
  onCancel: () => void;
};

/** Inline name editor shared by "new folder" and "rename". Enter commits, Escape cancels. */
const NameInput: FC<NameInputProps> = ({ initial, existingNames, own, icon, hint, onCommit, onCancel }) => {
  const [value, setValue] = useState(initial);
  const inputRef = useRef<HTMLInputElement>(null);
  // The editor only appears after the user asks for it, so take focus right away.
  useEffect(() => inputRef.current?.focus(), []);
  const error = validateEntryName(value, existingNames, own);

  const onKeyDown = (event: KeyboardEvent) => {
    if (event.key === 'Enter' && !error) {
      onCommit(value.trim());
    }
    else if (event.key === 'Escape') {
      onCancel();
    }
  };

  return (
    <>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25, minWidth: 0 }} onClick={(event) => event.stopPropagation()} onDoubleClick={(event) => event.stopPropagation()}>
        <span className="material-symbols-outlined" style={{ fontSize: 22, flex: 'none', color: '#398CBF', fontVariationSettings: '\'FILL\' 1' }}>{icon}</span>
        <input
          ref={inputRef}
          value={value}
          aria-label="Name"
          aria-invalid={!!error}
          onChange={(event) => setValue(event.target.value)}
          onFocus={(event) => event.target.select()}
          onKeyDown={onKeyDown}
          style={{ flex: 1, minWidth: 0, height: 28, padding: '0 8px', border: `1px solid ${error ? '#C62828' : '#398CBF'}`, borderRadius: 4, outline: 'none', font: '500 14px \'Noto Sans\', sans-serif', boxShadow: '0 0 0 2px rgba(57,140,191,0.18)' }}
        />
        <ButtonBase aria-label="Confirm" disabled={!!error} onClick={() => onCommit(value.trim())} sx={{ width: 32, height: 32, borderRadius: '50%', color: error ? 'rgba(0,0,0,0.26)' : '#20A183' }}>
          <span className="material-symbols-outlined" style={{ fontSize: 20 }}>check</span>
        </ButtonBase>
        <ButtonBase aria-label="Cancel" onClick={onCancel} sx={{ width: 32, height: 32, borderRadius: '50%', color: 'rgba(0,0,0,0.6)' }}>
          <span className="material-symbols-outlined" style={{ fontSize: 20 }}>close</span>
        </ButtonBase>
      </Box>
      <Box sx={{ gridColumn: '3 / 6', fontSize: 12, pl: 1.5, color: error ? '#C62828' : 'rgba(0,0,0,0.6)' }}>{error || hint}</Box>
    </>
  );
};

export const FolderBrowser: FC<Props> = ({ route, filter, writable, refreshSignal, creating, onCreatingDone, onOpenFolder, notify, pick }) => {
  const { volume, volumeType, path } = route;
  const volumeRef = useMemo(() => toVolumeRef({ volumeType, ...volume }), [volumeType, volume.volumeName, volume.owner, volume.rootVolumeName]);
  const { data, loading, error, refetch } = useQuery<{ getJsonTree: JsonTree }>(JSON_TREE, {
    variables: { volume: volumeRef, path },
    fetchPolicy: 'cache-and-network',
    notifyOnNetworkStatusChange: true
  });
  const [createFolder] = useMutation(CREATE_FOLDER);
  const [renameFile] = useMutation(RENAME_FILE);
  const [deleteFile] = useMutation(DELETE_FILE);

  const [sortKey, setSortKey] = useState<EntrySortKey>('name');
  const [direction, setDirection] = useState<1 | -1>(1);
  const [checked, setChecked] = useState<Set<string>>(new Set());
  const [renaming, setRenaming] = useState<string | null>(null);
  const [menu, setMenu] = useState<{ anchor: HTMLElement; entry: FileEntry } | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<string[] | null>(null);
  const [busy, setBusy] = useState(false);

  // A different folder or volume starts with nothing selected or being edited.
  useEffect(() => {
    setChecked(new Set());
    setRenaming(null);
    setMenu(null);
    setConfirmDelete(null);
  }, [path, volume.volumeName, volume.owner, volume.rootVolumeName, volumeType]);

  const reload = () => refetch().catch(() => undefined);

  const firstRefresh = useRef(true);
  useEffect(() => {
    if (firstRefresh.current) {
      firstRefresh.current = false;
      return;
    }
    setChecked(new Set());
    reload();
  }, [refreshSignal]);

  const all = useMemo(() => buildEntries(data?.getJsonTree.root), [data]);
  const names = useMemo(() => all.map((entry) => entry.name), [all]);
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

  const open = (entry: FileEntry) => onOpenFolder(joinPath(path, entry.name));

  const runCreate = async (name: string) => {
    setBusy(true);
    try {
      await createFolder({ variables: { volume: volumeRef, path, name } });
      onCreatingDone(name);
      notify(`Created “${name}”`, 'success');
    }
    catch (error_) {
      notify(`Could not create “${name}”: ${(error_ as Error).message}`, 'error');
    }
    setBusy(false);
    reload();
  };

  const runRename = async (name: string, newName: string) => {
    setRenaming(null);
    if (newName === name) {
      return;
    }
    setBusy(true);
    try {
      await renameFile({ variables: { volume: volumeRef, path, name, newName } });
      notify(`Renamed to “${newName}”`, 'success');
    }
    catch (error_) {
      notify(`Could not rename “${name}”: ${(error_ as Error).message}`, 'error');
    }
    setBusy(false);
    reload();
  };

  const runDelete = async (targets: string[]) => {
    setConfirmDelete(null);
    setBusy(true);
    const results = await Promise.allSettled(targets.map((name) => deleteFile({ variables: { volume: volumeRef, path, name } })));
    const failed = targets.filter((_, index) => results[index].status === 'rejected');
    const firstFailure = results.find((result): result is PromiseRejectedResult => result.status === 'rejected');
    if (failed.length === 0) {
      notify(targets.length > 1 ? `Deleted ${targets.length} items` : `Deleted “${targets[0]}”`, 'success');
    }
    else {
      // The first reason is enough to act on (permissions, missing item, ...).
      notify(`Could not delete ${failed.length > 1 ? `${failed.length} items` : `“${failed[0]}”`}: ${(firstFailure?.reason as Error).message}`, 'error');
    }
    setChecked(new Set(failed));
    setBusy(false);
    reload();
  };

  const headers: { key: EntrySortKey; label: string }[] = [
    { key: 'name', label: 'Name' },
    { key: 'modified', label: 'Modified' },
    { key: 'size', label: 'Size' }
  ];

  // Without cached data the first load (or a failed one) has nothing to list.
  const showLoading = loading && !data;
  const showError = !!error && !data;

  const menuItems = (entry: FileEntry) => [
    ...(entry.isFolder ? [{ label: 'Open', icon: 'folder_open', color: 'text.primary', run: () => open(entry) }] : []),
    ...(writable ? [
      { label: 'Rename', icon: 'edit', color: 'text.primary', run: () => setRenaming(entry.name) },
      { label: 'Delete', icon: 'delete', color: '#C62828', run: () => setConfirmDelete([entry.name]) }
    ] : [])
  ];

  const emptyHint = writable ? 'Use New folder to add a folder.' : '';

  return (
    <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column', opacity: busy ? 0.7 : 1, pointerEvents: busy ? 'none' : 'auto' }}>
      {selected.length > 0 && !pick && (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75, pl: 0.75, pr: 1.5, minHeight: 52, bgcolor: '#e8f1f8', flex: 'none' }}>
          <ButtonBase aria-label="Clear selection" onClick={() => setChecked(new Set())} sx={{ width: 36, height: 36, borderRadius: '50%', color: 'primary.main' }}>
            <span className="material-symbols-outlined" style={{ fontSize: 20 }}>close</span>
          </ButtonBase>
          <Box component="span" sx={{ fontWeight: 600, color: 'primary.main' }}>{selected.length} selected</Box>
          <Box sx={{ flex: 1 }} />
          {writable && (
            <Button color="error" startIcon={<span className="material-symbols-outlined" style={{ fontSize: 18 }}>delete</span>} onClick={() => setConfirmDelete(selected.map((entry) => entry.name))}>
              Delete
            </Button>
          )}
        </Box>
      )}
      <Box role="row" sx={{ display: 'grid', gridTemplateColumns: COLUMNS, alignItems: 'center', px: 1, height: 34, borderBottom: '1px solid #e6e9ed', flex: 'none' }}>
        {pick ? <span /> : (
          <ButtonBase
            aria-label={allSelected ? 'Deselect all' : 'Select all'}
            disabled={entries.length === 0}
            onClick={() => setChecked(allSelected ? new Set() : new Set(entries.map((entry) => entry.name)))}
          >
            <CheckIcon name={allSelected ? 'check_box' : selected.length ? 'indeterminate_check_box' : 'check_box_outline_blank'} active={selected.length > 0} />
          </ButtonBase>
        )}
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
        <span />
      </Box>
      <Box sx={{ flex: 1, minHeight: 0, overflow: 'auto' }}>
        <LoadingAnimation backDropIsOpen={showLoading} />
        {showError && (
          <Alert severity="error" sx={{ m: 2 }} action={<Button color="inherit" size="small" onClick={reload}>Retry</Button>}>
            Could not load this folder: {error?.message}
          </Alert>
        )}
        {creating && writable && (
          <Box sx={{ display: 'grid', gridTemplateColumns: COLUMNS, alignItems: 'center', px: 1, minHeight: ROW_HEIGHT, borderBottom: '1px solid #f0f2f4', bgcolor: 'rgba(57,140,191,0.10)' }}>
            <span />
            <NameInput
              initial={defaultFolderName(names)}
              existingNames={names}
              icon="create_new_folder"
              hint={pick ? 'Enter to create and select' : 'Enter to create · Esc to cancel'}
              onCommit={runCreate}
              onCancel={() => onCreatingDone()}
            />
          </Box>
        )}
        {entries.map((entry) => {
          const isChecked = checked.has(entry.name);
          const disabled = !!pick && !entry.isFolder;
          const picked = !!pick && pick.selectedName === entry.name;
          const dim = disabled ? 0.42 : 1;
          const items = pick ? [] : menuItems(entry);
          const onClick = () => {
            if (pick) {
              if (!disabled) {
                pick.onSelect(entry.name);
              }
            }
            else if (entry.isFolder) {
              open(entry);
            }
          };
          const isRenaming = renaming === entry.name;
          return (
            <Box
              key={entry.name}
              role="row"
              onClick={onClick}
              onDoubleClick={() => pick && !disabled && open(entry)}
              sx={{ display: 'grid', gridTemplateColumns: COLUMNS, alignItems: 'center', px: 1, minHeight: ROW_HEIGHT, borderBottom: '1px solid #f0f2f4', cursor: !disabled && (pick || entry.isFolder) ? 'pointer' : 'default', userSelect: 'none', bgcolor: picked ? 'rgba(57,140,191,0.16)' : isChecked ? 'rgba(57,140,191,0.07)' : 'transparent', '&:hover': { bgcolor: disabled ? undefined : 'rgba(57,140,191,0.08)' } }}
            >
              {pick ? (
                <span className="material-symbols-outlined" style={{ fontSize: 20, opacity: dim, color: picked ? '#398CBF' : 'rgba(0,0,0,0.5)' }}>
                  {disabled ? '' : picked ? 'radio_button_checked' : 'radio_button_unchecked'}
                </span>
              ) : (
                <ButtonBase
                  aria-label={`Select ${entry.name}`}
                  aria-pressed={isChecked}
                  onClick={(event) => {
                    event.stopPropagation(); toggle(entry.name); 
                  }}
                >
                  <CheckIcon name={isChecked ? 'check_box' : 'check_box_outline_blank'} active={isChecked} />
                </ButtonBase>
              )}
              {isRenaming ? (
                <NameInput
                  initial={entry.name}
                  existingNames={names}
                  own={entry.name}
                  icon={entry.isFolder ? 'folder' : 'description'}
                  hint="Enter to rename · Esc to cancel"
                  onCommit={(newName) => runRename(entry.name, newName)}
                  onCancel={() => setRenaming(null)}
                />
              ) : (
                <>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25, minWidth: 0, fontSize: 14, fontWeight: entry.isFolder ? 500 : 400, opacity: dim }}>
                    <span className="material-symbols-outlined" style={{ fontSize: 22, flex: 'none', color: entry.isFolder ? '#398CBF' : 'rgba(0,0,0,0.55)', fontVariationSettings: `'FILL' ${entry.isFolder ? 1 : 0}` }}>
                      {entry.isFolder ? 'folder' : 'description'}
                    </span>
                    <Box component="span" sx={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{entry.name}</Box>
                  </Box>
                  <Box component="span" sx={{ fontSize: 13, color: 'rgba(0,0,0,0.62)', whiteSpace: 'nowrap', opacity: dim }}>{formatModified(entry.modified)}</Box>
                  <Box component="span" sx={{ fontSize: 13, color: 'rgba(0,0,0,0.62)', whiteSpace: 'nowrap', opacity: dim }}>{entry.isFolder ? '—' : formatBytes(entry.size)}</Box>
                  {pick && !disabled && (
                    <ButtonBase aria-label={`Open ${entry.name}`} onClick={(event) => {
                      event.stopPropagation(); open(entry); 
                    }} sx={{ width: 32, height: 32, borderRadius: '50%', color: 'rgba(0,0,0,0.6)' }}>
                      <span className="material-symbols-outlined" style={{ fontSize: 20 }}>chevron_right</span>
                    </ButtonBase>
                  )}
                  {!pick && items.length > 0 && (
                    <ButtonBase aria-label={`More actions for ${entry.name}`} onClick={(event) => {
                      event.stopPropagation(); setMenu({ anchor: event.currentTarget, entry }); 
                    }} sx={{ width: 32, height: 32, borderRadius: '50%', color: 'rgba(0,0,0,0.6)' }}>
                      <span className="material-symbols-outlined" style={{ fontSize: 20 }}>more_vert</span>
                    </ButtonBase>
                  )}
                </>
              )}
            </Box>
          );
        })}
        {!showLoading && !showError && all.length === 0 && !creating && <Message icon="folder_open" title="This folder is empty" hint={emptyHint} />}
        {!showLoading && !showError && all.length > 0 && entries.length === 0 && (
          <Message icon="search_off" title={`No matches for “${filter.trim()}”`} hint="Try a different name." />
        )}
      </Box>
      <Menu anchorEl={menu?.anchor} open={!!menu} onClose={() => setMenu(null)}>
        {(menu ? menuItems(menu.entry) : []).map((item) => (
          <MenuItem key={item.label} onClick={() => {
            setMenu(null); item.run(); 
          }} sx={{ gap: 1.5, fontSize: 14, color: item.color }}>
            <span className="material-symbols-outlined" style={{ fontSize: 18 }}>{item.icon}</span>
            {item.label}
          </MenuItem>
        ))}
      </Menu>
      <Dialog open={!!confirmDelete} onClose={() => setConfirmDelete(null)}>
        <DialogTitle>{confirmDelete && confirmDelete.length > 1 ? `Delete ${confirmDelete.length} items?` : 'Delete item?'}</DialogTitle>
        <DialogContent>
          <DialogContentText>
            {confirmDelete && confirmDelete.length === 1 ? `“${confirmDelete[0]}” will be permanently deleted. ` : 'These items will be permanently deleted. '}
            Folders are deleted with everything inside them. This can’t be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmDelete(null)}>Cancel</Button>
          <Button color="error" variant="contained" onClick={() => confirmDelete && runDelete(confirmDelete)}>Delete</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};
