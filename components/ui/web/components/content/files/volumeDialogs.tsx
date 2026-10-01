import { FC, useState } from 'react';
import { useMutation } from '@apollo/client';
import { Button, CircularProgress, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle, LinearProgress, MenuItem, TextField } from '@mui/material';

import { CREATE_USER_VOLUME, DELETE_USER_VOLUME, UPDATE_USER_VOLUME } from 'src/graphql/volumes';
import { validateVolumeName, VolumeRow } from 'src/utils/fileVolumes';

type RootOption = { name?: string | null; description?: string | null };

type Done = (message: string, severity: 'success' | 'error') => void;

type CreateProps = {
  owner: string;
  roots: RootOption[];
  /** Names of the user's own volumes, as `root/name`, to catch duplicates before the server does. */
  existing: string[];
  onClose: () => void;
  onDone: Done;
};

export const CreateVolumeDialog: FC<CreateProps> = ({ owner, roots, existing, onClose, onDone }) => {
  const [createUserVolume] = useMutation(CREATE_USER_VOLUME);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [root, setRoot] = useState(roots.length === 1 ? roots[0].name || '' : '');
  const [saving, setSaving] = useState(false);

  const taken = existing.filter((key) => key.startsWith(`${root}/`)).map((key) => key.slice(root.length + 1));
  const error = name ? validateVolumeName(name, taken) : '';
  const canSave = !!root && !!name.trim() && !error && !saving;

  const save = async () => {
    setSaving(true);
    try {
      await createUserVolume({ variables: { rootVolumeName: root, owner, name: name.trim(), description: description.trim() || null } });
      onDone(`Created volume “${name.trim()}”`, 'success');
    }
    catch (error_) {
      onDone(`Could not create the volume: ${(error_ as Error).message}`, 'error');
    }
  };

  return (
    <Dialog open fullWidth maxWidth="xs" onClose={saving ? undefined : onClose}>
      <DialogTitle>Create user volume</DialogTitle>
      {saving && <LinearProgress />}
      <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: '8px !important' }}>
        <TextField label="Name" value={name} onChange={(event) => setName(event.target.value)} error={!!error} helperText={error} fullWidth size="small" />
        <TextField label="Description" value={description} onChange={(event) => setDescription(event.target.value)} multiline minRows={2} fullWidth size="small" />
        <TextField
          select
          label="Root volume"
          value={root}
          onChange={(event) => setRoot(event.target.value)}
          helperText="Where this volume is mounted. Different root volumes have different storage options."
          fullWidth
          size="small"
        >
          {roots.map((option) => (
            <MenuItem key={option.name} value={option.name || ''}>{option.name}{option.description ? ` - ${option.description}` : ''}</MenuItem>
          ))}
        </TextField>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={saving}>Cancel</Button>
        <Button variant="contained" onClick={save} disabled={!canSave} startIcon={saving ? <CircularProgress size={16} color="inherit" /> : undefined}>{saving ? 'Creating…' : 'Create volume'}</Button>
      </DialogActions>
    </Dialog>
  );
};

type EditProps = {
  volume: VolumeRow;
  /** Names of the user's other volumes in the same root volume. */
  siblings: string[];
  onClose: () => void;
  onDone: Done;
};

export const EditVolumeDialog: FC<EditProps> = ({ volume, siblings, onClose, onDone }) => {
  const [updateUserVolume] = useMutation(UPDATE_USER_VOLUME);
  const [name, setName] = useState(volume.name);
  const [description, setDescription] = useState(volume.description);
  const [saving, setSaving] = useState(false);

  const error = validateVolumeName(name, siblings, volume.name);
  const unchanged = name.trim() === volume.name && description === volume.description;

  const save = async () => {
    setSaving(true);
    try {
      await updateUserVolume({ variables: { rootVolumeName: volume.route.rootVolumeName, owner: volume.route.owner, name: volume.name, newName: name.trim(), description } });
      onDone(`Updated volume “${name.trim()}”`, 'success');
    }
    catch (error_) {
      onDone(`Could not update “${volume.name}”: ${(error_ as Error).message}`, 'error');
    }
  };

  return (
    <Dialog open fullWidth maxWidth="xs" onClose={saving ? undefined : onClose}>
      <DialogTitle>Edit user volume</DialogTitle>
      {saving && <LinearProgress />}
      <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: '8px !important' }}>
        <TextField label="Name" value={name} onChange={(event) => setName(event.target.value)} error={!!error} helperText={error} fullWidth size="small" />
        <TextField label="Description" value={description} onChange={(event) => setDescription(event.target.value)} multiline minRows={2} fullWidth size="small" />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={saving}>Cancel</Button>
        <Button variant="contained" onClick={save} disabled={!!error || unchanged || saving} startIcon={saving ? <CircularProgress size={16} color="inherit" /> : undefined}>{saving ? 'Saving…' : 'Save changes'}</Button>
      </DialogActions>
    </Dialog>
  );
};

type DeleteProps = {
  volume: VolumeRow;
  onClose: () => void;
  onDone: Done;
};

export const DeleteVolumeDialog: FC<DeleteProps> = ({ volume, onClose, onDone }) => {
  const [deleteUserVolume] = useMutation(DELETE_USER_VOLUME);
  const [deleting, setDeleting] = useState(false);

  const remove = async () => {
    setDeleting(true);
    try {
      await deleteUserVolume({ variables: { rootVolumeName: volume.route.rootVolumeName, owner: volume.route.owner, name: volume.name } });
      onDone(`Deleted volume “${volume.name}”`, 'success');
    }
    catch (error) {
      onDone(`Could not delete “${volume.name}”: ${(error as Error).message}`, 'error');
    }
  };

  return (
    <Dialog open onClose={deleting ? undefined : onClose}>
      <DialogTitle>Delete user volume?</DialogTitle>
      {deleting && <LinearProgress />}
      <DialogContent>
        <DialogContentText>
          “{volume.name}” and every file in it will be permanently deleted. Anyone it is shared with loses access. This can’t be undone.
        </DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={deleting}>Cancel</Button>
        <Button color="error" variant="contained" onClick={remove} disabled={deleting} startIcon={deleting ? <CircularProgress size={16} color="inherit" /> : undefined}>{deleting ? 'Deleting…' : 'Delete volume'}</Button>
      </DialogActions>
    </Dialog>
  );
};
