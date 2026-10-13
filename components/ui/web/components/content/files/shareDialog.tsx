import { FC, useEffect, useMemo, useState } from 'react';
import { useLazyQuery, useMutation, useQuery } from '@apollo/client';
import styled from 'styled-components';
import { Alert, Button, Checkbox, Chip, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle, FormControlLabel, LinearProgress, TextField, ToggleButton, ToggleButtonGroup } from '@mui/material';

import { PUBLIC_USERS_AND_GROUPS } from 'src/graphql/accounts';
import { SHARE_USER_VOLUME, SHARING_DETAILS } from 'src/graphql/volumes';
import { FileService, PrincipalType, SharedWith, SharePrincipal } from 'src/graphql/typings';
import { VolumeRow } from 'src/utils/fileVolumes';
import { addMember, buildSharePayload, principalKey, removeMember, SHARE_ACTIONS, searchPrincipals, sharingChanged, toggleAction } from 'src/utils/sharing';

import { BORDER, CircleButton, Icon, MUTED, ROW_BORDER, ROW_HOVER } from './filesStyles';

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
const TypeChip = styled(Chip)<{ $group: boolean }>`
  && {
    height: 20px;
    flex: none;
    font-size: 11px;
    font-weight: 600;
    background: ${({ $group }) => ($group ? 'rgba(32,161,131,0.14)' : 'rgba(57,140,191,0.14)')};
    color: ${({ $group }) => ($group ? '#157A63' : '#1F6A96')};
  }
`;

const Content = styled(DialogContent)`
  && {
    display: grid;
    grid-template-columns: 1fr;
    gap: 16px;
    min-height: 360px;
  }

  @media (min-width: 1094px) {
    && {
      grid-template-columns: 1fr 1fr;
    }
  }

  .full-width {
    grid-column: 1 / -1;
  }

  .pane {
    display: flex;
    flex-direction: column;
    min-height: 0;
  }

  .pane-title {
    font-weight: 600;
    font-size: 14px;
    margin-bottom: 8px;
  }

  .type-filter {
    margin-top: 8px;
  }

  .type-filter button {
    padding-left: 12px;
    padding-right: 12px;
    text-transform: none;
  }

  .list {
    flex: 1;
    min-height: 0;
    overflow: auto;
    border: 1px solid ${BORDER};
    border-radius: 4px;
  }

  .list.directory {
    max-height: 320px;
    margin-top: 8px;
  }

  .list.members {
    max-height: 360px;
  }

  .loading {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 16px;
    font-size: 13px;
  }

  .hint {
    padding: 16px;
    font-size: 13px;
    color: ${MUTED};
  }

  .more {
    padding: 12px;
    font-size: 12px;
    color: ${MUTED};
  }

  .member {
    padding: 6px 12px;
    border-bottom: 1px solid ${ROW_BORDER};
  }

  .member-line {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .member-name {
    flex: 1;
    font-size: 14px;
    font-weight: 500;
  }

  .actions {
    display: flex;
    flex-wrap: wrap;
    margin-left: 32px;
  }

  .actions label {
    margin-right: 12px;
  }

  .actions .MuiFormControlLabel-label {
    font-size: 13px;
  }
`;

const Candidate = styled.button`
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 6px 12px;
  border: 0;
  border-bottom: 1px solid ${ROW_BORDER};
  background: none;
  text-align: left;
  cursor: pointer;
  font: inherit;

  &:hover {
    background: ${ROW_HOVER};
  }

  .name {
    flex: 1;
    font-size: 14px;
  }
`;

const PrincipalIcon = styled(Icon)`
  color: ${({ theme }) => theme.palette.secondary.main};
`;

/**
 * Share a user volume with users and groups. Nothing is fetched until this opens: the volume's current
 * sharing list, and the directory of everyone it could be shared with.
 */
export const ShareDialog: FC<Props> = ({ volume, onClose, onDone }) => {
  const { route } = volume;
  const { data: sharing, error: sharingError } = useQuery<{ getVolumes?: FileService | null }>(SHARING_DETAILS, { fetchPolicy: 'no-cache' });
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

  const loadingVolume = !sharing && !sharingError;
  // Loaded fine, but the volume is not in the answer: it was renamed or deleted since the list was loaded.
  const notFound = !!sharing && !original;

  return (
    <Dialog open fullWidth maxWidth="md" onClose={saving ? undefined : onClose}>
      <DialogTitle>Share “{volume.name}”</DialogTitle>
      {(loadingVolume || saving) && <LinearProgress />}
      <Content>
        {sharingError && <Alert className="full-width" severity="error">Could not load sharing: {sharingError.message}</Alert>}
        {notFound && (
          <Alert className="full-width" severity="warning">
            This volume can no longer be found. It may have been renamed or deleted. Close this dialog and reload the list.
          </Alert>
        )}
        {failure && <Alert className="full-width" severity="error">{failure}</Alert>}
        <div className="pane">
          <div className="pane-title">Add users and groups</div>
          <TextField size="small" placeholder="Search by name" value={search} onChange={(event) => setSearch(event.target.value)} disabled={!original} inputProps={{ 'aria-label': 'Search users and groups' }} />
          <ToggleButtonGroup
            className="type-filter"
            exclusive
            size="small"
            value={typeFilter}
            onChange={(_, value) => value && setTypeFilter(value)}
            aria-label="Show users, groups or both"
          >
            <ToggleButton value="ALL">All</ToggleButton>
            <ToggleButton value={PrincipalType.User}>Users</ToggleButton>
            <ToggleButton value={PrincipalType.Group}>Groups</ToggleButton>
          </ToggleButtonGroup>
          <div className="list directory">
            {directoryLoading && <div className="loading"><CircularProgress size={16} /> Loading users and groups…</div>}
            {directoryError && <Alert severity="error">Could not load users and groups: {directoryError.message}</Alert>}
            {shown.map((principal) => (
              <Candidate key={principalKey(principal)} type="button" onClick={() => setMembers((current) => addMember(current, principal))}>
                <PrincipalIcon>{ICON[principal.type]}</PrincipalIcon>
                <span className="name">{principal.name}</span>
                <TypeChip size="small" label={LABEL[principal.type]} $group={principal.type === PrincipalType.Group} />
                <Icon $color={MUTED}>add</Icon>
              </Candidate>
            ))}
            {directory && shown.length === 0 && <div className="hint">No users or groups match.</div>}
            {total > shown.length && <div className="more">Showing {shown.length} of {total}. Type to narrow the list.</div>}
          </div>
        </div>
        <div className="pane">
          <div className="pane-title">Who has access</div>
          <div className="list members">
            {original && members.length === 0 && <div className="hint">This volume isn’t shared with anyone.</div>}
            {members.map((member) => (
              <div key={principalKey(member)} className="member">
                <div className="member-line">
                  <PrincipalIcon>{ICON[member.type]}</PrincipalIcon>
                  <span className="member-name">{member.name}</span>
                  <TypeChip size="small" label={LABEL[member.type]} $group={member.type === PrincipalType.Group} />
                  <CircleButton $size={28} aria-label={`Remove ${member.name}`} onClick={() => setMembers((current) => removeMember(current, member))}>
                    <Icon $size={18}>close</Icon>
                  </CircleButton>
                </div>
                <div className="actions">
                  {SHARE_ACTIONS.map((action) => (
                    <FormControlLabel
                      key={action}
                      label={action[0].toUpperCase() + action.slice(1)}
                      control={<Checkbox size="small" checked={member.allowedActions.includes(action)} onChange={() => setMembers((current) => toggleAction(current, member, action))} />}
                    />
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      </Content>
      <DialogActions>
        <Button onClick={onClose} disabled={saving}>Cancel</Button>
        <Button variant="contained" onClick={save} disabled={!changed || saving} startIcon={saving ? <CircularProgress size={16} color="inherit" /> : undefined}>
          {saving ? 'Saving…' : 'Save changes'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};
