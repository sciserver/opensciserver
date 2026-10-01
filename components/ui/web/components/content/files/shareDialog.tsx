import { FC, useEffect, useMemo, useState } from 'react';
import { useLazyQuery, useMutation, useQuery } from '@apollo/client';
import { Alert, Box, Button, ButtonBase, Checkbox, Chip, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle, FormControlLabel, LinearProgress, TextField, ToggleButton, ToggleButtonGroup } from '@mui/material';

import { PUBLIC_USERS_AND_GROUPS } from 'src/graphql/accounts';
import { SHARE_USER_VOLUME, SHARING_DETAILS } from 'src/graphql/volumes';
import { FileService, PrincipalType, SharedWith, SharePrincipal } from 'src/graphql/typings';
import { VolumeRow } from 'src/utils/fileVolumes';
import { addMember, buildSharePayload, principalKey, removeMember, SHARE_ACTIONS, searchPrincipals, sharingChanged, toggleAction } from 'src/utils/sharing';

type Props = {
  volume: VolumeRow;
  onClose: () => void;
  /** Called once the sharing change went through. A failure stays in the dialog so the edits are not lost. */
  onDone: (message: string) => void;
};

const DIRECTORY_LIMIT = 50;

const ICON = { [PrincipalType.User]: 'person', [PrincipalType.Group]: 'group' };
const LABEL = { [PrincipalType.User]: 'User', [PrincipalType.Group]: 'Group' };

/** Users and groups look alike in a list, so each name carries a colored label as well as its icon. */
const TypeChip: FC<{ type: PrincipalType }> = ({ type }) => (
  <Chip
    size="small"
    label={LABEL[type]}
    sx={{ height: 20, fontSize: 11, fontWeight: 600, flex: 'none', bgcolor: type === PrincipalType.Group ? 'rgba(32,161,131,0.14)' : 'rgba(57,140,191,0.14)', color: type === PrincipalType.Group ? '#157A63' : '#1F6A96' }}
  />
);

/**
 * Share a user volume with users and groups. Nothing is fetched until this opens: the volume's current
 * sharing list, and the directory of everyone it could be shared with.
 */
export const ShareDialog: FC<Props> = ({ volume, onClose, onDone }) => {
  const { route } = volume;
  const { data: sharing, error: sharingError } = useQuery<{ getVolumes?: FileService | null }>(SHARING_DETAILS, { fetchPolicy: 'network-only' });
  const [loadDirectory, { data: directory, loading: directoryLoading, error: directoryError }] = useLazyQuery<{ getPublicUsersAndGroups: SharePrincipal[] }>(PUBLIC_USERS_AND_GROUPS);
  const [shareUserVolume] = useMutation(SHARE_USER_VOLUME);

  const original = useMemo<SharedWith[] | null>(() => {
    const volumes = sharing?.getVolumes?.rootVolumes?.flatMap((root) => root.userVolumes);
    const match = volumes?.find((v) => v.name === route.volumeName && v.owner === route.owner && v.rootVolumeName === route.rootVolumeName);
    return match ? match.sharedWith : null;
  }, [sharing, route]);

  const [members, setMembers] = useState<SharedWith[]>([]);
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState<PrincipalType | 'ALL'>('ALL');
  const [saving, setSaving] = useState(false);
  const [failure, setFailure] = useState('');

  // The directory is the big one, so it starts loading only once we know the volume's own sharing.
  useEffect(() => {
    if (original) {
      setMembers(original);
      loadDirectory();
    }
  }, [original]);

  const { shown, total } = useMemo(
    () => searchPrincipals(directory?.getPublicUsersAndGroups || [], members, search, DIRECTORY_LIMIT, typeFilter === 'ALL' ? undefined : typeFilter),
    [directory, members, search, typeFilter]
  );
  const changed = !!original && sharingChanged(original, members);

  const save = async () => {
    if (!original) {
      return;
    }
    setSaving(true);
    setFailure('');
    try {
      await shareUserVolume({ variables: { rootVolumeName: route.rootVolumeName, owner: route.owner, name: route.volumeName, sharedWith: buildSharePayload(original, members) } });
      onDone(`Updated sharing for “${volume.name}”`);
    }
    catch (error) {
      setFailure(`Could not update sharing for “${volume.name}”: ${(error as Error).message}`);
      setSaving(false);
    }
  };

  const loadingVolume = !original && !sharingError;

  return (
    <Dialog open fullWidth maxWidth="md" onClose={saving ? undefined : onClose}>
      <DialogTitle>Share “{volume.name}”</DialogTitle>
      {(loadingVolume || saving) && <LinearProgress />}
      <DialogContent sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 2, minHeight: 360 }}>
        {sharingError && <Alert severity="error" sx={{ gridColumn: '1 / -1' }}>Could not load sharing: {sharingError.message}</Alert>}
        {failure && <Alert severity="error" sx={{ gridColumn: '1 / -1' }}>{failure}</Alert>}
        <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: 0 }}>
          <Box sx={{ fontWeight: 600, fontSize: 14, mb: 1 }}>Add users and groups</Box>
          <TextField size="small" placeholder="Search by name" value={search} onChange={(event) => setSearch(event.target.value)} disabled={!original} inputProps={{ 'aria-label': 'Search users and groups' }} />
          <ToggleButtonGroup
            exclusive
            size="small"
            value={typeFilter}
            onChange={(_, value) => value && setTypeFilter(value)}
            aria-label="Show users, groups or both"
            sx={{ mt: 1 }}
          >
            <ToggleButton value="ALL" sx={{ px: 1.5, textTransform: 'none' }}>All</ToggleButton>
            <ToggleButton value={PrincipalType.User} sx={{ px: 1.5, textTransform: 'none' }}>Users</ToggleButton>
            <ToggleButton value={PrincipalType.Group} sx={{ px: 1.5, textTransform: 'none' }}>Groups</ToggleButton>
          </ToggleButtonGroup>
          <Box sx={{ flex: 1, minHeight: 0, maxHeight: 320, overflow: 'auto', mt: 1, border: '1px solid #e6e9ed', borderRadius: 1 }}>
            {directoryLoading && <Box sx={{ p: 2, display: 'flex', alignItems: 'center', gap: 1, fontSize: 13 }}><CircularProgress size={16} /> Loading users and groups…</Box>}
            {directoryError && <Alert severity="error">Could not load users and groups: {directoryError.message}</Alert>}
            {shown.map((principal) => (
              <ButtonBase
                key={principalKey(principal)}
                onClick={() => setMembers((current) => addMember(current, principal))}
                sx={{ display: 'flex', width: '100%', justifyContent: 'flex-start', gap: 1.25, px: 1.5, py: 0.75, textAlign: 'left', borderBottom: '1px solid #f0f2f4', '&:hover': { bgcolor: 'rgba(57,140,191,0.08)' } }}
              >
                <span className="material-symbols-outlined" style={{ fontSize: 20, color: '#398CBF' }}>{ICON[principal.type]}</span>
                <Box component="span" sx={{ flex: 1, fontSize: 14 }}>{principal.name}</Box>
                <TypeChip type={principal.type} />
                <span className="material-symbols-outlined" style={{ fontSize: 20, color: 'rgba(0,0,0,0.5)' }}>add</span>
              </ButtonBase>
            ))}
            {directory && shown.length === 0 && <Box sx={{ p: 2, fontSize: 13, color: 'rgba(0,0,0,0.6)' }}>No users or groups match.</Box>}
            {total > shown.length && <Box sx={{ p: 1.5, fontSize: 12, color: 'rgba(0,0,0,0.6)' }}>Showing {shown.length} of {total}. Type to narrow the list.</Box>}
          </Box>
        </Box>
        <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: 0 }}>
          <Box sx={{ fontWeight: 600, fontSize: 14, mb: 1 }}>Who has access</Box>
          <Box sx={{ flex: 1, maxHeight: 360, overflow: 'auto', border: '1px solid #e6e9ed', borderRadius: 1 }}>
            {original && members.length === 0 && <Box sx={{ p: 2, fontSize: 13, color: 'rgba(0,0,0,0.6)' }}>This volume isn’t shared with anyone.</Box>}
            {members.map((member) => (
              <Box key={principalKey(member)} sx={{ px: 1.5, py: 0.75, borderBottom: '1px solid #f0f2f4' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25 }}>
                  <span className="material-symbols-outlined" style={{ fontSize: 20, color: '#398CBF' }}>{ICON[member.type]}</span>
                  <Box component="span" sx={{ flex: 1, fontSize: 14, fontWeight: 500 }}>{member.name}</Box>
                  <TypeChip type={member.type} />
                  <ButtonBase aria-label={`Remove ${member.name}`} onClick={() => setMembers((current) => removeMember(current, member))} sx={{ width: 28, height: 28, borderRadius: '50%', color: 'rgba(0,0,0,0.6)' }}>
                    <span className="material-symbols-outlined" style={{ fontSize: 18 }}>close</span>
                  </ButtonBase>
                </Box>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', ml: 4 }}>
                  {SHARE_ACTIONS.map((action) => (
                    <FormControlLabel
                      key={action}
                      label={action[0].toUpperCase() + action.slice(1)}
                      sx={{ mr: 1.5, '& .MuiFormControlLabel-label': { fontSize: 13 } }}
                      control={<Checkbox size="small" checked={member.allowedActions.includes(action)} onChange={() => setMembers((current) => toggleAction(current, member, action))} />}
                    />
                  ))}
                </Box>
              </Box>
            ))}
          </Box>
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={saving}>Cancel</Button>
        <Button variant="contained" onClick={save} disabled={!changed || saving} startIcon={saving ? <CircularProgress size={16} color="inherit" /> : undefined}>
          {saving ? 'Saving…' : 'Save changes'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};
