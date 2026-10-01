import { CSSProperties, FC, useContext, useMemo, useRef, useState } from 'react';
import { useQuery } from '@apollo/client';
import { Alert, Box, Button, ButtonBase, Snackbar } from '@mui/material';

import { UserContext } from 'context';
import { FILE_VOLUMES } from 'src/graphql/volumes';
import { FileService, VolumeType } from 'src/graphql/typings';
import { creatableRootVolumes, DEFAULT_FILES_ROUTE, dataVolumeRows, FilesRoute, filterAndSortRows, sameVolume, SortKey, userVolumeRows, VolumeRow, workspacePath } from 'src/utils/fileVolumes';
import { fileServiceUrl, fileUrl, toArray, uploadFile } from 'src/utils/fileTransfer';
import { joinPath } from 'src/utils/files';
import { LoadingAnimation } from 'components/common/loadingAnimation';

import { Breadcrumb } from './breadcrumb';
import { FolderBrowser } from './folderBrowser';
import { TransferDialog } from './transferDialog';
import { CreateVolumeDialog, DeleteVolumeDialog, EditVolumeDialog } from './volumeDialogs';
import { UploadItem, UploadsPanel } from './uploadsPanel';
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
  style?: CSSProperties;
};

const DONE_UPLOAD_MS = 4000;

const rowRoute = (row: VolumeRow): FilesRoute => {
  const { volumeType, ...volume } = row.route;
  return { volumeType, volume, path: '' };
};

export const FileBrowser: FC<FileBrowserProps> = ({ mode = 'manage', location, initialLocation = DEFAULT_FILES_ROUTE, onLocationChange, writableOnly, pickLabel = 'Use this folder', onSelect, style }) => {
  const pick = mode === 'pick';
  const mustBeWritable = writableOnly ?? pick;
  const { user, token } = useContext(UserContext);
  const { data, loading, error, refetch } = useQuery<{ getVolumes?: FileService | null }>(FILE_VOLUMES);

  const [internal, setInternal] = useState<FilesRoute>(initialLocation);
  const route = location ?? internal;

  const [filter, setFilter] = useState('');
  const [sortKey, setSortKey] = useState<SortKey>('name');
  const [direction, setDirection] = useState<1 | -1>(1);
  const [picked, setPicked] = useState<VolumeRow | null>(null);
  const [pickedFolder, setPickedFolder] = useState<string | null>(null);
  const [refreshSignal, setRefreshSignal] = useState(0);
  const [creating, setCreating] = useState(false);
  const [volumeDialog, setVolumeDialog] = useState<{ kind: 'create' } | { kind: 'edit' | 'delete'; row: VolumeRow } | null>(null);
  const [uploads, setUploads] = useState<UploadItem[]>([]);
  const uploadId = useRef(0);
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
    ...(row.owned ? [{ label: 'Edit', icon: 'edit', run: () => setVolumeDialog({ kind: 'edit', row }) }] : []),
    ...(row.canDelete ? [{ label: 'Delete', icon: 'delete', color: '#C62828', run: () => setVolumeDialog({ kind: 'delete', row }) }] : [])
  ];

  const onVolumeDone = (message: string, severity: 'success' | 'error') => {
    setVolumeDialog(null);
    setToast({ message, severity });
    refetch().catch(() => undefined);
  };

  const patchUpload = (id: number, patch: Partial<UploadItem>) => setUploads((current) => current.map((item) => (item.id === id ? { ...item, ...patch } : item)));

  const upload = (files: File[]) => {
    if (!route.volume || !currentRow?.writable) {
      setToast({ message: 'Open a writable folder to upload', severity: 'error' });
      return;
    }
    const { volume, volumeType, path } = route;
    const where = path.split('/').filter(Boolean).pop() || volume.volumeName;
    for (const file of files) {
      uploadId.current += 1;
      const id = uploadId.current;
      const handle = uploadFile(fileUrl(fileServiceUrl(), { volumeType, ...volume }, path, file.name), file, token, (progress) => patchUpload(id, { progress }));
      setUploads((current) => [...current, { id, name: file.name, where, progress: 0, status: 'uploading', abort: handle.abort }]);
      handle.promise
        .then(() => {
          patchUpload(id, { status: 'done', progress: 1 });
          // A finished upload clears itself; failures stay until dismissed.
          setTimeout(() => setUploads((current) => current.filter((item) => item.id !== id)), DONE_UPLOAD_MS);
        })
        .finally(() => setRefreshSignal((value) => value + 1))
        .catch((error_: Error) => patchUpload(id, { status: 'error', error: error_.message }));
    }
  };

  return (
    <Box sx={{ position: 'relative', display: 'flex', flexDirection: 'column', minHeight: 320, bgcolor: 'background.paper', border: '1px solid #dde2e7', borderRadius: '6px', overflow: 'hidden', ...style }}>
      <VolumeTabs
        value={route.volumeType}
        counts={{ [VolumeType.Uservolume]: data ? userRows.length : undefined, [VolumeType.Datavolume]: data ? dataRows.length : undefined }}
        onChange={(type) => go({ volumeType: type, path: '' })}
      />
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, px: 1.5, minHeight: 52, flex: 'none' }}>
        {canCreateVolume && (
          <Button variant="contained" startIcon={<span className="material-symbols-outlined" style={{ fontSize: 18 }}>add</span>} onClick={() => setVolumeDialog({ kind: 'create' })}>
            Create user volume
          </Button>
        )}
        {route.volume && currentRow?.writable && !pick && (
          <>
            <Button variant="contained" startIcon={<span className="material-symbols-outlined" style={{ fontSize: 18 }}>upload</span>} onClick={() => fileInput.current?.click()}>
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
          <Button variant="outlined" startIcon={<span className="material-symbols-outlined" style={{ fontSize: 18 }}>create_new_folder</span>} onClick={() => setCreating(true)}>
            New folder
          </Button>
        )}
        {route.volume && currentRow && !currentRow.writable && !pick && (
          <Box component="span" sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.75, fontSize: 13, color: 'rgba(0,0,0,0.6)' }}>
            <span className="material-symbols-outlined" style={{ fontSize: 18 }}>lock</span>
            {isData ? 'Data volumes are read-only' : 'Read-only: shared with you'}
          </Box>
        )}
        {pick && <Box component="span" sx={{ fontSize: 13, color: 'rgba(0,0,0,0.6)' }}>Click to select · double-click to open</Box>}
        <Box sx={{ flex: 1 }} />
        {(
          <Box component="label" sx={{ display: 'flex', alignItems: 'center', gap: 0.75, height: 32, px: 1.25, border: '1px solid #d5dae0', borderRadius: 1, width: 200, bgcolor: '#fff' }}>
            <span className="material-symbols-outlined" style={{ fontSize: 18, color: 'rgba(0,0,0,0.5)' }}>search</span>
            <input
              value={filter}
              onChange={(event) => setFilter(event.target.value)}
              placeholder={route.volume ? 'Filter this folder' : 'Filter volumes'}
              aria-label={route.volume ? 'Filter this folder' : 'Filter volumes'}
              style={{ border: 0, outline: 'none', font: '13px \'Noto Sans\', sans-serif', minWidth: 0, flex: 1, background: 'transparent' }}
            />
          </Box>
        )}
        {route.volume && (
          <ButtonBase aria-label="Refresh" title="Refresh" onClick={() => setRefreshSignal((value) => value + 1)} sx={{ width: 32, height: 32, borderRadius: '50%', color: 'rgba(0,0,0,0.6)' }}>
            <span className="material-symbols-outlined" style={{ fontSize: 20 }}>refresh</span>
          </ButtonBase>
        )}
      </Box>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, px: 1.5, py: 0.75, minHeight: 42, bgcolor: '#f4f6f8', borderTop: '1px solid #e6e9ed', borderBottom: '1px solid #e6e9ed', flex: 'none' }}>
        <Breadcrumb
          route={route}
          onRoot={() => go({ volumeType: route.volumeType, path: '' })}
          onPath={(path) => go({ ...route, path })}
        />
        {route.volume?.owner && (
          <Box sx={{ display: 'flex', gap: 1.75, fontSize: 12, color: 'rgba(0,0,0,0.6)', flex: 'none' }}>
            <span>Owner <b style={{ color: '#202124' }}>{route.volume.owner}</b></span>
            <span>{route.volume.rootVolumeName} volume</span>
          </Box>
        )}
      </Box>
      {error && <Alert severity="error" sx={{ m: 2 }}>Could not load volumes: {error.message}</Alert>}
      <LoadingAnimation backDropIsOpen={loading} />
      {!loading && !error && !route.volume && (
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
      {volumeDialog?.kind === 'delete' && <DeleteVolumeDialog volume={volumeDialog.row} onClose={() => setVolumeDialog(null)} onDone={onVolumeDone} />}
      <UploadsPanel uploads={uploads} onDismiss={(id) => setUploads((current) => current.filter((item) => item.id !== id))} />
      <Snackbar open={!!toast} autoHideDuration={4000} onClose={() => setToast(null)} anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
        <Alert severity={toast?.severity ?? 'success'} variant="filled" onClose={() => setToast(null)}>{toast?.message}</Alert>
      </Snackbar>
      {pick && (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, py: 1.25, pl: 2, pr: 1.5, borderTop: '1px solid #e6e9ed', bgcolor: '#f7f9fb', flex: 'none' }}>
          <span className="material-symbols-outlined" style={{ fontSize: 22, color: '#398CBF', fontVariationSettings: '\'FILL\' 1' }}>folder_open</span>
          <Box sx={{ flex: 1, minWidth: 0 }}>
            <Box sx={{ fontSize: 11, fontWeight: 600, letterSpacing: 0.5, textTransform: 'uppercase', color: 'rgba(0,0,0,0.6)' }}>{pickLabel}</Box>
            <Box title={targetPath} sx={{ font: '13px ui-monospace, Menlo, monospace', color: targetPath ? 'text.primary' : 'rgba(0,0,0,0.5)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {targetPath || 'Select a volume or folder'}
            </Box>
          </Box>
          <Button variant="contained" disabled={!canConfirm} onClick={() => target && onSelect?.({ route: target, workspacePath: targetPath })}>
            {pickLabel}
          </Button>
        </Box>
      )}
    </Box>
  );
};
