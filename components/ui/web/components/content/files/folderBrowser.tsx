import { DragEvent, FC, KeyboardEvent, useContext, useEffect, useMemo, useRef, useState } from 'react';
import ReactMarkdown from 'react-markdown';
import styled from 'styled-components';
import { useMutation, useQuery } from '@apollo/client';
import { Alert, Button, ButtonBase, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle, LinearProgress } from '@mui/material';

import { UserContext } from 'context';
import { CREATE_FOLDER, DELETE_FILE, JSON_TREE, RENAME_FILE } from 'src/graphql/files';
import { JsonTree } from 'src/graphql/typings';
import { buildEntries, defaultFolderName, EntrySortKey, FileEntry, filterAndSortEntries, formatBytes, formatModified, validateEntryName } from 'src/utils/fileEntries';
import { FilesRoute } from 'src/utils/fileVolumes';
import { FILE_SERVICE_NOT_CONFIGURED, fetchText, fileServiceUrl, fileUrl, startDownload, toArray } from 'src/utils/fileTransfer';
import { joinPath, toVolumeRef } from 'src/utils/files';
import { LoadingAnimation } from 'components/common/loadingAnimation';

import { BORDER, CircleButton, DANGER, Icon, listStyles, MUTED } from './filesStyles';
import { EmptyMessage, MenuAction, RowMenu, SortHeader } from './listParts';

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
  /** Ask the parent to pick a destination for these names and copy or move them. */
  onTransfer: (kind: 'copy' | 'move', names: string[]) => void;
  /** Files dropped onto the list (manage mode only). */
  onDropFiles: (files: File[]) => void;
  notify: (message: string, severity?: 'success' | 'error') => void;
  /** Pick mode: files are greyed out, a click selects a folder and a double click opens it. */
  pick?: { selectedName: string | null; onSelect: (name: string) => void };
};

const COLUMNS = '36px minmax(0,2fr) minmax(0,1fr) minmax(0,1fr) 36px';

const Styled = styled.div<{ $busy: boolean }>`
  position: relative;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  opacity: ${({ $busy }) => ($busy ? 0.7 : 1)};
  pointer-events: ${({ $busy }) => ($busy ? 'none' : 'auto')};

  ${listStyles(COLUMNS)}

  .list-row.disabled {
    cursor: default;
  }

  .progress {
    flex: none;
  }

  .selection-bar {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 0 12px 0 6px;
    min-height: 52px;
    background: #e8f1f8;
    flex: none;
    color: ${({ theme }) => theme.palette.primary.main};
  }

  .selection-count {
    font-weight: 600;
    margin-right: auto;
  }

  .list-row.creating {
    background: rgba(57, 140, 191, 0.1);
  }

  .name-cell {
    font-weight: 400;
    gap: 10px;
  }

  .name-cell.folder {
    font-weight: 500;
  }

  .name-editor {
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 0;
  }

  .name-editor input {
    flex: 1;
    min-width: 0;
    height: 28px;
    padding: 0 8px;
    border: 1px solid ${({ theme }) => theme.palette.secondary.main};
    border-radius: 4px;
    outline: none;
    font: 500 14px 'Noto Sans', sans-serif;
    box-shadow: 0 0 0 2px rgba(57, 140, 191, 0.18);
  }

  .name-editor input[aria-invalid='true'] {
    border-color: ${DANGER};
  }

  .name-hint {
    grid-column: 3 / 6;
    font-size: 12px;
    padding-left: 12px;
    color: ${MUTED};
  }

  .name-hint.invalid {
    color: ${DANGER};
  }

  .readme-hint {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 0 12px;
    min-height: 36px;
    font-size: 13px;
    background: rgba(57, 140, 191, 0.08);
    border-bottom: 1px solid ${BORDER};
    flex: none;
  }

  .readme {
    margin: 16px;
    padding: 16px;
    border: 1px solid ${BORDER};
    border-radius: 4px;
    background: #fbfcfd;
    font-size: 14px;
  }

  .readme-title {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 8px;
    font-size: 12px;
    font-weight: 600;
    color: ${MUTED};
  }

  .drop-overlay {
    position: absolute;
    inset: 6px;
    z-index: 35;
    border: 2px dashed ${({ theme }) => theme.palette.secondary.main};
    border-radius: 6px;
    background: rgba(232, 241, 248, 0.94);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    text-align: center;
    padding: 16px;
  }

  .drop-overlay > * {
    pointer-events: none;
  }

  .drop-message {
    font-weight: 600;
    font-size: 16px;
    color: ${({ theme }) => theme.palette.primary.main};
  }
`;

const CheckIcon = styled(Icon)<{ $active?: boolean; $dim?: boolean }>`
  color: ${({ theme, $active }) => ($active ? theme.palette.secondary.main : 'rgba(0,0,0,0.5)')};
  ${({ $dim }) => $dim && 'opacity: 0.42;'}
`;

const EntryIcon = styled(Icon)<{ $folder: boolean }>`
  flex: none;
  color: ${({ theme, $folder }) => ($folder ? theme.palette.secondary.main : 'rgba(0,0,0,0.55)')};
`;

const ConfirmButton = styled(CircleButton)<{ $invalid: boolean }>`
  && {
    color: ${({ theme, $invalid }) => ($invalid ? 'rgba(0,0,0,0.26)' : theme.palette.contrast2.main)};
  }
`;

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
      {/* The editor sits inside a clickable row, so its clicks must not reach the row. */}
      <div className="name-editor" role="presentation" onClick={(event) => event.stopPropagation()} onDoubleClick={(event) => event.stopPropagation()}>
        <EntryIcon $size={22} $folder $filled>{icon}</EntryIcon>
        <input
          ref={inputRef}
          value={value}
          aria-label="Name"
          aria-invalid={!!error}
          onChange={(event) => setValue(event.target.value)}
          onFocus={(event) => event.target.select()}
          onKeyDown={onKeyDown}
        />
        <ConfirmButton aria-label="Confirm" disabled={!!error} $invalid={!!error} onClick={() => onCommit(value.trim())}>
          <Icon>check</Icon>
        </ConfirmButton>
        <CircleButton aria-label="Cancel" onClick={onCancel}>
          <Icon>close</Icon>
        </CircleButton>
      </div>
      <div className={`name-hint${error ? ' invalid' : ''}`}>{error || hint}</div>
    </>
  );
};

export const FolderBrowser: FC<Props> = ({ route, filter, writable, refreshSignal, creating, onCreatingDone, onOpenFolder, onTransfer, onDropFiles, notify, pick }) => {
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
  const [dragging, setDragging] = useState(false);
  const [readme, setReadme] = useState('');
  const readmeRef = useRef<HTMLDivElement>(null);
  const { token } = useContext(UserContext);

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
  const hasReadme = all.some((entry) => !entry.isFolder && entry.name === 'README.md');
  useEffect(() => {
    let current = true;
    setReadme('');
    // The token fills in after the first render (see ContextWrapper); without one the request would just be refused.
    if (hasReadme && !pick && token && fileServiceUrl()) {
      fetchText(fileUrl(fileServiceUrl(), { volumeType, ...volume }, path, 'README.md'), token)
        .then((text) => current && setReadme(text))
        .catch(() => undefined);
    }
    return () => {
      current = false;
    };
  }, [hasReadme, path, volume.volumeName, volume.owner, volume.rootVolumeName, volumeType, pick, token]);

  const download = (entry: FileEntry) => {
    const base = fileServiceUrl();
    if (!base) {
      notify(FILE_SERVICE_NOT_CONFIGURED, 'error');
      return;
    }
    startDownload(fileUrl(base, { volumeType, ...volume }, path, entry.name), entry.name);
  };

  // Several downloads in a row are spaced out so browsers don't drop them.
  const downloadMany = (files: FileEntry[]) => {
    for (const [index, entry] of toArray(files.entries())) {
      setTimeout(() => download(entry), index * 1000);
    }
  };

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

  const menuItems = (entry: FileEntry): MenuAction[] => [
    ...(entry.isFolder ? [{ label: 'Open', icon: 'folder_open', color: 'text.primary', run: () => open(entry) }] : []),
    ...(entry.isFolder ? [] : [{ label: 'Download', icon: 'download', color: 'text.primary', run: () => download(entry) }]),
    { label: 'Copy to…', icon: 'content_copy', color: 'text.primary', run: () => onTransfer('copy', [entry.name]) },
    ...(writable ? [
      { label: 'Move to…', icon: 'drive_file_move', color: 'text.primary', run: () => onTransfer('move', [entry.name]) },
      { label: 'Rename', icon: 'edit', color: 'text.primary', run: () => setRenaming(entry.name) },
      { label: 'Delete', icon: 'delete', color: '#C62828', run: () => setConfirmDelete([entry.name]) }
    ] : [])
  ];

  const emptyHint = writable ? 'Drop files here or use Upload.' : '';

  const hasFiles = (event: DragEvent) => !pick && toArray(event.dataTransfer?.types || []).includes('Files');
  function onDragOver(event: DragEvent) {
    if (hasFiles(event)) {
      event.preventDefault();
      setDragging(true);
    }
  }

  return (
    <Styled $busy={busy} onDragEnter={onDragOver} onDragOver={onDragOver}>
      {(busy || (loading && !!data)) && <LinearProgress className="progress" aria-label="Working" />}
      {readme && (
        <div className="readme-hint">
          <Icon $size={18} $color="#398CBF">article</Icon>
          <span>This folder has a README. It is shown at the bottom of the list.</span>
          <Button size="small" endIcon={<Icon $size={18}>arrow_downward</Icon>} onClick={() => readmeRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })}>
            Jump to README
          </Button>
        </div>
      )}
      {selected.length > 0 && !pick && (
        <div className="selection-bar">
          <CircleButton $size={36} aria-label="Clear selection" onClick={() => setChecked(new Set())}>
            <Icon>close</Icon>
          </CircleButton>
          <span className="selection-count">{selected.length} selected</span>
          {/* Folders can't be downloaded, so Download is only offered when every selected item is a file. */}
          {selected.every((entry) => !entry.isFolder) && (
            <Button startIcon={<Icon $size={18}>download</Icon>} onClick={() => downloadMany(selected)}>
              Download
            </Button>
          )}
          <Button startIcon={<Icon $size={18}>content_copy</Icon>} onClick={() => onTransfer('copy', selected.map((entry) => entry.name))}>
            Copy
          </Button>
          {writable && (
            <Button startIcon={<Icon $size={18}>drive_file_move</Icon>} onClick={() => onTransfer('move', selected.map((entry) => entry.name))}>
              Move
            </Button>
          )}
          {writable && (
            <Button color="error" startIcon={<Icon $size={18}>delete</Icon>} onClick={() => setConfirmDelete(selected.map((entry) => entry.name))}>
              Delete
            </Button>
          )}
        </div>
      )}
      <div className="list-header" role="row">
        {pick ? <span /> : (
          <ButtonBase
            aria-label={allSelected ? 'Deselect all' : 'Select all'}
            disabled={entries.length === 0}
            onClick={() => setChecked(allSelected ? new Set() : new Set(entries.map((entry) => entry.name)))}
          >
            <CheckIcon $active={selected.length > 0}>{allSelected ? 'check_box' : selected.length ? 'indeterminate_check_box' : 'check_box_outline_blank'}</CheckIcon>
          </ButtonBase>
        )}
        {headers.map(({ key, label }) => (
          <SortHeader key={key} label={label} active={sortKey === key} direction={direction} onClick={() => onSort(key)} />
        ))}
        <span />
      </div>
      <div className="list-body">
        {showLoading && <LoadingAnimation backDropIsOpen />}
        {showError && (
          <Alert severity="error" action={<Button color="inherit" size="small" onClick={reload}>Retry</Button>}>
            Could not load this folder: {error?.message}
          </Alert>
        )}
        {creating && writable && (
          <div className="list-row creating">
            <span />
            <NameInput
              initial={defaultFolderName(names)}
              existingNames={names}
              icon="create_new_folder"
              hint={pick ? 'Enter to create and select' : 'Enter to create · Esc to cancel'}
              onCommit={runCreate}
              onCancel={() => onCreatingDone()}
            />
          </div>
        )}
        {entries.map((entry) => {
          const isChecked = checked.has(entry.name);
          const disabled = !!pick && !entry.isFolder;
          const picked = !!pick && pick.selectedName === entry.name;
          const clickable = !disabled && (!!pick || entry.isFolder);
          const dim = disabled ? ' dim' : '';
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
            <div
              key={entry.name}
              role="row"
              className={`list-row${clickable ? ' clickable' : ''}${disabled ? ' disabled' : ''}${picked ? ' selected' : ''}${isChecked && !picked ? ' checked' : ''}`}
              tabIndex={clickable ? 0 : -1}
              onClick={onClick}
              onKeyDown={(event) => event.key === 'Enter' && event.target === event.currentTarget && onClick()}
              onDoubleClick={() => pick && !disabled && open(entry)}
            >
              {pick ? (
                <CheckIcon $dim={disabled} $active={picked}>{disabled ? '' : picked ? 'radio_button_checked' : 'radio_button_unchecked'}</CheckIcon>
              ) : (
                <ButtonBase
                  aria-label={`Select ${entry.name}`}
                  aria-pressed={isChecked}
                  onClick={(event) => {
                    event.stopPropagation();
                    toggle(entry.name);
                  }}
                >
                  <CheckIcon $active={isChecked}>{isChecked ? 'check_box' : 'check_box_outline_blank'}</CheckIcon>
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
                  <div className={`name-cell${entry.isFolder ? ' folder' : ''}${dim}`}>
                    <EntryIcon $size={22} $folder={entry.isFolder} $filled={entry.isFolder}>{entry.isFolder ? 'folder' : 'description'}</EntryIcon>
                    <span className="name-text">{entry.name}</span>
                  </div>
                  <span className={`text-cell${dim}`}>{formatModified(entry.modified)}</span>
                  <span className={`text-cell${dim}`}>{entry.isFolder ? '—' : formatBytes(entry.size)}</span>
                  {pick && !disabled && (
                    <CircleButton
                      aria-label={`Open ${entry.name}`}
                      onClick={(event) => {
                        event.stopPropagation();
                        open(entry);
                      }}
                    >
                      <Icon>chevron_right</Icon>
                    </CircleButton>
                  )}
                  {!pick && items.length > 0 && (
                    <CircleButton
                      aria-label={`More actions for ${entry.name}`}
                      onClick={(event) => {
                        event.stopPropagation();
                        setMenu({ anchor: event.currentTarget, entry });
                      }}
                    >
                      <Icon>more_vert</Icon>
                    </CircleButton>
                  )}
                </>
              )}
            </div>
          );
        })}
        {!showLoading && !showError && all.length === 0 && !creating && <EmptyMessage icon="folder_open" title="This folder is empty" hint={emptyHint} />}
        {readme && (
          <div className="readme" ref={readmeRef}>
            <div className="readme-title">
              <Icon $size={16}>article</Icon>
              README.md
            </div>
            <ReactMarkdown>{readme}</ReactMarkdown>
          </div>
        )}
        {!showLoading && !showError && all.length > 0 && entries.length === 0 && (
          <EmptyMessage icon="search_off" title={`No matches for “${filter.trim()}”`} hint="Try a different name." />
        )}
      </div>
      {dragging && (
        <div
          className="drop-overlay"
          role="presentation"
          onDragOver={(event) => event.preventDefault()}
          onDragLeave={(event) => !event.currentTarget.contains(event.relatedTarget as Node) && setDragging(false)}
          onDrop={(event) => {
            event.preventDefault();
            setDragging(false);
            onDropFiles(toArray(event.dataTransfer.files));
          }}
        >
          <Icon $size={40}>upload_file</Icon>
          <div className="drop-message">
            {writable ? `Drop to upload to ${path.split('/').filter(Boolean).pop() || volume.volumeName}` : 'Open a writable folder to upload'}
          </div>
        </div>
      )}
      <RowMenu anchor={menu?.anchor ?? null} items={menu ? menuItems(menu.entry) : []} onClose={() => setMenu(null)} />
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
    </Styled>
  );
};
