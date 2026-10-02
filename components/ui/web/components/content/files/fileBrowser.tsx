import { FC, useContext, useMemo, useRef, useState } from 'react';
import { useQuery } from '@apollo/client';
import styled from 'styled-components';
import { Alert, Button, LinearProgress, Snackbar } from '@mui/material';

import { UserContext } from 'context';
import { FILE_VOLUMES } from 'src/graphql/volumes';
import { FileService, VolumeType } from 'src/graphql/typings';
import { creatableRootVolumes, DEFAULT_FILES_ROUTE, dataVolumeRows, FilesRoute, filterAndSortRows, sameVolume, SortKey, userVolumeRows, VolumeRow, workspacePath } from 'src/utils/fileVolumes';
import { toArray } from 'src/utils/fileTransfer';
import { joinPath } from 'src/utils/files';
import { LoadingAnimation } from 'components/common/loadingAnimation';

import { Breadcrumb } from './breadcrumb';
import { BORDER, CircleButton, Icon, MUTED } from './filesStyles';
import { FolderBrowser } from './folderBrowser';
import { TransferDialog } from './transferDialog';
import { QuotasDialog } from './quotasDialog';
import { ShareDialog } from './shareDialog';
import { CreateVolumeDialog, DeleteVolumeDialog, EditVolumeDialog } from './volumeDialogs';
import { UploadsPanel } from './uploadsPanel';
import { useUploads } from './useUploads';
import { VolumeList } from './volumeList';
import { VolumeTabs } from './volumeTabs';

export type FileBrowserSelection = {
  route: FilesRoute;
  /** Where the selection is mounted inside a compute container. */
  workspacePath: string;
};

export type FileBrowserProps = {
  /** manage: the Files page. pick: choose a folder inside a form (e.g. the job working directory). */
  mode?: 'manage' | 'pick';
  /** Controlled location. Leave unset to let the browser keep its own. */
  location?: FilesRoute;
  /** Starting location when uncontrolled. */
  initialLocation?: FilesRoute;
  onLocationChange?: (location: FilesRoute) => void;
  /** pick: only volumes you can write to can be chosen. Defaults to true in pick mode. */
  writableOnly?: boolean;
  pickLabel?: string;
  onSelect?: (selection: FileBrowserSelection) => void;
  className?: string;
};

const Styled = styled.div`
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 320px;
  background: ${({ theme }) => theme.palette.background.paper};
  border: 1px solid #dde2e7;
  border-radius: 6px;
  overflow: hidden;

  .toolbar {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 0 12px;
    min-height: 52px;
    flex: none;
  }

  .spacer {
    flex: 1;
  }

  .note {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: ${MUTED};
  }

  .filter {
    display: flex;
    align-items: center;
    gap: 6px;
    height: 32px;
    padding: 0 10px;
    width: 200px;
    border: 1px solid #d5dae0;
    border-radius: 4px;
    background: #fff;
  }

  .filter input {
    flex: 1;
    min-width: 0;
    border: 0;
    outline: none;
    font: 13px 'Noto Sans', sans-serif;
    background: transparent;
  }

  .path-bar {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 6px 12px;
    min-height: 42px;
    background: #f4f6f8;
    border-top: 1px solid ${BORDER};
    border-bottom: 1px solid ${BORDER};
    flex: none;
  }

  .volume-meta {
    display: flex;
    gap: 14px;
    font-size: 12px;
    color: ${MUTED};
    flex: none;
  }

  .volume-meta b {
    color: ${({ theme }) => theme.palette.text.primary};
  }

  .reloading {
    flex: none;
  }

  .pick-footer {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 12px 10px 16px;
    border-top: 1px solid ${BORDER};
    background: #f7f9fb;
    flex: none;
  }

  .pick-target {
    flex: 1;
    min-width: 0;
  }

  .pick-label {
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 0.5px;
    text-transform: uppercase;
    color: ${MUTED};
  }

  .pick-path {
    font: 13px ui-monospace, Menlo, monospace;
    color: ${({ theme }) => theme.palette.text.primary};
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .pick-path.empty {
    color: rgba(0, 0, 0, 0.5);
  }
`;

const ErrorAlert = styled(Alert)`
  && {
    margin: 16px;
  }
`;

const rowRoute = (row: VolumeRow): FilesRoute => {
  const { volumeType, ...volume } = row.route;
  return { volumeType, volume, path: '' };
};

export const FileBrowser: FC<FileBrowserProps> = ({ mode = 'manage', location, initialLocation = DEFAULT_FILES_ROUTE, onLocationChange, writableOnly, pickLabel = 'Use this folder', onSelect, className }) => {
  const pick = mode === 'pick';
  const mustBeWritable = writableOnly ?? pick;
  const { user, token } = useContext(UserContext);
  const { data, loading, error, refetch } = useQuery<{ getVolumes?: FileService | null }>(FILE_VOLUMES, { notifyOnNetworkStatusChange: true });
  // First load shows the backdrop; later reloads (after a change) keep the list and show a progress bar.
  const initialLoading = loading && !data;
  const reloading = loading && !!data;

  const [internal, setInternal] = useState<FilesRoute>(initialLocation);
  const route = location ?? internal;

  const [filter, setFilter] = useState('');
  const [sortKey, setSortKey] = useState<SortKey>('name');
  const [direction, setDirection] = useState<1 | -1>(1);
  const [picked, setPicked] = useState<VolumeRow | null>(null);
  const [pickedFolder, setPickedFolder] = useState<string | null>(null);
  const [refreshSignal, setRefreshSignal] = useState(0);
  const [creating, setCreating] = useState(false);
  const [volumeDialog, setVolumeDialog] = useState<{ kind: 'create' } | { kind: 'quotas' } | { kind: 'edit' | 'delete' | 'share'; row: VolumeRow } | null>(null);
  const fileInput = useRef<HTMLInputElement>(null);
  const [transfer, setTransfer] = useState<{ kind: 'copy' | 'move'; names: string[] } | null>(null);
  const [toast, setToast] = useState<{ message: string; severity: 'success' | 'error' } | null>(null);

  const userRows = useMemo(() => userVolumeRows(data?.getVolumes?.rootVolumes || [], user?.userName), [data, user]);
  const dataRows = useMemo(() => dataVolumeRows(data?.getVolumes?.dataVolumes || []), [data]);
  const isData = route.volumeType === VolumeType.Datavolume;
  const rows = useMemo(
    () => filterAndSortRows(isData ? dataRows : userRows, filter, sortKey, direction),
    [isData, dataRows, userRows, filter, sortKey, direction]
  );

  const go = (next: FilesRoute) => {
    setFilter('');
    setPicked(null);
    setPickedFolder(null);
    setCreating(false);
    if (!location) {
      setInternal(next);
    }
    onLocationChange?.(next);
  };

  const onSort = (key: SortKey) => {
    if (key === sortKey) {
      setDirection((current) => (current === 1 ? -1 : 1));
      return;
    }
    setSortKey(key);
    setDirection(1);
  };

  const openVolume = (row: VolumeRow) => go(rowRoute(row));

  // pick: the chosen row, or else the folder you are standing in.
  const currentRow = route.volume ? [...userRows, ...dataRows].find((row) => row.route.volumeType === route.volumeType && sameVolume(row.route, route.volume!)) : undefined;
  const pickedRoute = picked ? rowRoute(picked) : null;
  const folderRoute = route.volume && pickedFolder ? { ...route, path: joinPath(route.path, pickedFolder) } : null;
  const target: FilesRoute | null = pickedRoute ?? folderRoute ?? (route.volume ? route : null);
  const targetWritable = picked ? picked.writable : !!currentRow?.writable;
  const canConfirm = !!target && (!mustBeWritable || targetWritable);
  const targetPath = target ? workspacePath(target) : '';

  const creatableRoots = useMemo(() => creatableRootVolumes(data?.getVolumes?.rootVolumes || []), [data]);
  const canCreateVolume = !pick && !route.volume && route.volumeType === VolumeType.Uservolume && !!user && creatableRoots.length > 0;

  const volumeMenu = (row: VolumeRow) => [
    ...(row.canShare ? [{ label: 'Sharing', icon: 'group', run: () => setVolumeDialog({ kind: 'share', row }) }] : []),
    ...(row.owned ? [{ label: 'Edit', icon: 'edit', run: () => setVolumeDialog({ kind: 'edit', row }) }] : []),
    ...(row.canDelete ? [{ label: 'Delete', icon: 'delete', color: '#C62828', run: () => setVolumeDialog({ kind: 'delete', row }) }] : [])
  ];

  // Dialogs report success only; a failure stays inside the dialog.
  const onVolumeDone = (message: string) => {
    setVolumeDialog(null);
    setToast({ message, severity: 'success' });
    refetch().catch(() => undefined);
  };

  const { uploads, upload, dismiss: dismissUpload } = useUploads({
    token,
    target: route.volume && currentRow?.writable ? { volume: { volumeType: route.volumeType, ...route.volume }, path: route.path } : undefined,
    notifyError: (message) => setToast({ message, severity: 'error' }),
    onBatchDone: () => setRefreshSignal((value) => value + 1)
  });

  return (
    <Styled className={className}>
      <VolumeTabs
        value={route.volumeType}
        counts={{ [VolumeType.Uservolume]: data ? userRows.length : undefined, [VolumeType.Datavolume]: data ? dataRows.length : undefined }}
        onChange={(type) => go({ volumeType: type, path: '' })}
      />
      <div className="toolbar">
        {canCreateVolume && (
          <Button variant="contained" startIcon={<Icon $size={18}>add</Icon>} onClick={() => setVolumeDialog({ kind: 'create' })}>
            Create user volume
          </Button>
        )}
        {canCreateVolume && (
          <Button onClick={() => setVolumeDialog({ kind: 'quotas' })}>View quotas</Button>
        )}
        {route.volume && currentRow?.writable && !pick && (
          <>
            <Button variant="contained" startIcon={<Icon $size={18}>upload</Icon>} onClick={() => fileInput.current?.click()}>
              Upload
            </Button>
            <input
              ref={fileInput}
              type="file"
              multiple
              hidden
              data-testid="upload-input"
              onChange={(event) => {
                upload(toArray(event.target.files || []));
                event.target.value = '';
              }}
            />
          </>
        )}
        {route.volume && currentRow?.writable && (
          <Button variant="outlined" startIcon={<Icon $size={18}>create_new_folder</Icon>} onClick={() => setCreating(true)}>
            New folder
          </Button>
        )}
        {route.volume && currentRow && !currentRow.writable && !pick && (
          <span className="note">
            <Icon $size={18}>lock</Icon>
            {isData ? 'Read-only: you can only read this data volume' : 'Read-only: shared with you'}
          </span>
        )}
        {pick && <span className="note">Click to select · double-click to open</span>}
        <span className="spacer" />
        <label className="filter">
          <Icon $size={18} $color="rgba(0,0,0,0.5)">search</Icon>
          <input
            value={filter}
            onChange={(event) => setFilter(event.target.value)}
            placeholder={route.volume ? 'Filter this folder' : 'Filter volumes'}
            aria-label={route.volume ? 'Filter this folder' : 'Filter volumes'}
          />
        </label>
        {route.volume && (
          <CircleButton aria-label="Refresh" title="Refresh" onClick={() => setRefreshSignal((value) => value + 1)}>
            <Icon>refresh</Icon>
          </CircleButton>
        )}
      </div>
      <div className="path-bar">
        <Breadcrumb
          route={route}
          onRoot={() => go({ volumeType: route.volumeType, path: '' })}
          onPath={(path) => go({ ...route, path })}
        />
        {route.volume?.owner && (
          <div className="volume-meta">
            <span>Owner <b>{route.volume.owner}</b></span>
            <span>{route.volume.rootVolumeName} volume</span>
          </div>
        )}
      </div>
      {reloading && <LinearProgress className="reloading" aria-label="Reloading volumes" />}
      {error && <ErrorAlert severity="error">Could not load volumes: {error.message}</ErrorAlert>}
      {initialLoading && <LoadingAnimation backDropIsOpen />}
      {!initialLoading && !error && !route.volume && (
        <VolumeList
          volumeType={route.volumeType}
          rows={rows}
          sortKey={sortKey}
          direction={direction}
          onSort={onSort}
          onOpen={openVolume}
          menuItems={pick ? undefined : volumeMenu}
          pick={pick ? { selectedKey: picked?.key ?? null, isDisabled: (row) => mustBeWritable && !row.writable, onSelect: setPicked } : undefined}
        />
      )}
      {route.volume && (
        <FolderBrowser
          route={{ ...route, volume: route.volume }}
          filter={filter}
          refreshSignal={refreshSignal}
          writable={!!currentRow?.writable}
          creating={creating}
          onCreatingDone={(created) => {
            setCreating(false);
            if (created && pick) {
              setPickedFolder(created);
            }
          }}
          notify={(message, severity = 'success') => setToast({ message, severity })}
          onTransfer={(kind, names) => setTransfer({ kind, names })}
          onDropFiles={upload}
          onOpenFolder={(path) => go({ ...route, path })}
          pick={pick ? { selectedName: pickedFolder, onSelect: setPickedFolder } : undefined}
        />
      )}
      {transfer && route.volume && (
        <TransferDialog
          kind={transfer.kind}
          names={transfer.names}
          source={{ ...route, volume: route.volume }}
          Picker={FileBrowser}
          onClose={() => setTransfer(null)}
          onDone={(message, severity) => {
            setTransfer(null);
            setToast({ message, severity });
            setRefreshSignal((value) => value + 1);
          }}
        />
      )}
      {volumeDialog?.kind === 'create' && user && (
        <CreateVolumeDialog
          owner={user.userName}
          roots={creatableRoots}
          existing={userRows.filter((row) => row.owned).map((row) => `${row.rootVolume}/${row.name}`)}
          onClose={() => setVolumeDialog(null)}
          onDone={onVolumeDone}
        />
      )}
      {volumeDialog?.kind === 'edit' && (
        <EditVolumeDialog
          volume={volumeDialog.row}
          siblings={userRows.filter((row) => row.owned && row.rootVolume === volumeDialog.row.rootVolume).map((row) => row.name)}
          onClose={() => setVolumeDialog(null)}
          onDone={onVolumeDone}
        />
      )}
      {volumeDialog?.kind === 'quotas' && <QuotasDialog onClose={() => setVolumeDialog(null)} />}
      {volumeDialog?.kind === 'share' && <ShareDialog volume={volumeDialog.row} onClose={() => setVolumeDialog(null)} onDone={onVolumeDone} />}
      {volumeDialog?.kind === 'delete' && <DeleteVolumeDialog volume={volumeDialog.row} onClose={() => setVolumeDialog(null)} onDone={onVolumeDone} />}
      <UploadsPanel uploads={uploads} onDismiss={dismissUpload} />
      <Snackbar open={!!toast} autoHideDuration={4000} onClose={() => setToast(null)} anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
        <Alert severity={toast?.severity ?? 'success'} variant="filled" onClose={() => setToast(null)}>{toast?.message}</Alert>
      </Snackbar>
      {pick && (
        <div className="pick-footer">
          <Icon $size={22} $filled $color="#398CBF">folder_open</Icon>
          <div className="pick-target">
            <div className="pick-label">{pickLabel}</div>
            <div className={`pick-path${targetPath ? '' : ' empty'}`} title={targetPath}>
              {targetPath || 'Select a volume or folder'}
            </div>
          </div>
          <Button variant="contained" disabled={!canConfirm} onClick={() => target && onSelect?.({ route: target, workspacePath: targetPath })}>
            {pickLabel}
          </Button>
        </div>
      )}
    </Styled>
  );
};
