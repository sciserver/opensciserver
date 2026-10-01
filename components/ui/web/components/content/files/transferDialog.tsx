import { ComponentType, FC, useState } from 'react';
import { useApolloClient, useMutation } from '@apollo/client';
import { Dialog, DialogContent, DialogTitle } from '@mui/material';

import { COPY_FILE, JSON_TREE, MOVE_FILE } from 'src/graphql/files';
import { JsonTree } from 'src/graphql/typings';
import { buildEntries } from 'src/utils/fileEntries';
import { FilesRoute, sameVolume } from 'src/utils/fileVolumes';
import { isMoveIntoItself, planTransferNames, toVolumeRef } from 'src/utils/files';

import type { FileBrowserProps, FileBrowserSelection } from './fileBrowser';

type Located = FilesRoute & { volume: NonNullable<FilesRoute['volume']> };

type Props = {
  kind: 'copy' | 'move';
  /** Names inside the source folder. */
  names: string[];
  source: Located;
  /** The FileBrowser itself, passed in so this file doesn't import it back. */
  Picker: ComponentType<FileBrowserProps>;
  onClose: () => void;
  /** Called once the transfer ran, whatever its outcome, so the caller can reload. */
  onDone: (message: string, severity: 'success' | 'error') => void;
};

const toLocation = (route: Located) => ({ volume: toVolumeRef({ volumeType: route.volumeType, ...route.volume }), path: route.path });

const titleFor = (kind: Props['kind'], names: string[]) => `${kind === 'copy' ? 'Copy' : 'Move'} ${names.length > 1 ? `${names.length} items` : `“${names[0]}”`}`;

/** Pick a destination folder, then copy or move. A taken name becomes "name (N)" and is sent as newName. */
export const TransferDialog: FC<Props> = ({ kind, names, source, Picker, onClose, onDone }) => {
  const client = useApolloClient();
  const [copyFile] = useMutation(COPY_FILE);
  const [moveFile] = useMutation(MOVE_FILE);
  const [running, setRunning] = useState(false);

  const run = async ({ route }: FileBrowserSelection) => {
    const destination = route as Located;
    const sameFolder = sameVolume(source.volume, destination.volume) && source.volumeType === destination.volumeType && source.path === destination.path;
    const sameVol = source.volumeType === destination.volumeType && sameVolume(source.volume, destination.volume);

    if (kind === 'move' && sameFolder) {
      onDone('Those items are already in that folder', 'error');
      return;
    }
    if (kind === 'move' && sameVol && names.some((name) => isMoveIntoItself(source.path, name, destination.path))) {
      onDone('A folder can’t be moved into itself', 'error');
      return;
    }

    setRunning(true);
    let failed: string[] = [];
    try {
      // Name conflicts are resolved here against the destination listing; a file added in between still surfaces as an error.
      const { data } = await client.query<{ getJsonTree: JsonTree }>({ query: JSON_TREE, variables: toLocation(destination), fetchPolicy: 'network-only' });
      const plan = planTransferNames(names, buildEntries(data.getJsonTree.root).map((entry) => entry.name));
      const mutate = kind === 'copy' ? copyFile : moveFile;
      for (const { name, newName } of plan) {
        try {
          // eslint-disable-next-line no-await-in-loop
          await mutate({ variables: { source: toLocation(source), name, destination: toLocation(destination), newName } });
        }
        catch {
          failed = [...failed, name];
        }
      }
    }
    catch (error) {
      onDone(`Could not read the destination folder: ${(error as Error).message}`, 'error');
      return;
    }

    const verb = kind === 'copy' ? 'Copied' : 'Moved';
    if (failed.length === 0) {
      onDone(`${verb} ${names.length > 1 ? `${names.length} items` : `“${names[0]}”`}`, 'success');
    }
    else {
      onDone(`Could not ${kind} ${failed.length > 1 ? `${failed.length} items` : `“${failed[0]}”`}`, 'error');
    }
  };

  return (
    <Dialog open fullWidth maxWidth="md" onClose={running ? undefined : onClose}>
      <DialogTitle sx={{ pb: 0.5 }}>{titleFor(kind, names)} to…</DialogTitle>
      <DialogContent sx={{ pt: 1 }}>
        <Picker
          mode="pick"
          initialLocation={source}
          pickLabel={kind === 'copy' ? 'Copy here' : 'Move here'}
          onSelect={run}
          style={{ height: 460, marginTop: 8, opacity: running ? 0.6 : 1, pointerEvents: running ? 'none' : 'auto' }}
        />
      </DialogContent>
    </Dialog>
  );
};
