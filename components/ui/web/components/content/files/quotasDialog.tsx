import { FC, useContext } from 'react';
import { useQuery } from '@apollo/client';
import { Alert, Box, Button, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle, LinearProgress } from '@mui/material';

import { UserContext } from 'context';
import { FILE_QUOTAS } from 'src/graphql/volumes';
import { FileService, FileUsage } from 'src/graphql/typings';
import { formatBytes } from 'src/utils/fileEntries';
import { groupQuotas } from 'src/utils/quotas';

type Props = { onClose: () => void };

/** Storage used against each quota, per root volume. Fetched when the dialog opens, never before. */
export const QuotasDialog: FC<Props> = ({ onClose }) => {
  const { user } = useContext(UserContext);
  const { data, loading, error } = useQuery<{ getFileUsage: FileUsage[]; getVolumes?: FileService | null }>(FILE_QUOTAS, { fetchPolicy: 'network-only' });
  const groups = data ? groupQuotas(data.getFileUsage, data.getVolumes?.rootVolumes || [], user?.userName) : [];

  return (
    <Dialog open fullWidth maxWidth="sm" onClose={onClose}>
      <DialogTitle>Quotas</DialogTitle>
      {loading && <LinearProgress />}
      <DialogContent sx={{ minHeight: 120 }}>
        {loading && <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, fontSize: 14 }}><CircularProgress size={16} /> Loading quotas…</Box>}
        {error && <Alert severity="error">Could not load quotas: {error.message}</Alert>}
        {data && groups.length === 0 && <Box sx={{ fontSize: 14, color: 'rgba(0,0,0,0.6)' }}>No quota information is available yet.</Box>}
        {groups.map((group) => (
          <Box key={group.rootVolumeId} sx={{ mb: 2, border: '1px solid #e6e9ed', borderRadius: 1, overflow: 'hidden' }}>
            <Box sx={{ px: 2, py: 1, bgcolor: '#f4f6f8', fontWeight: 600, fontSize: 14 }}>{group.rootVolumeName}</Box>
            {group.items.map((item) => (
              <Box key={item.key} sx={{ px: 2, py: 1.25, borderTop: '1px solid #f0f2f4' }}>
                <Box sx={{ fontSize: 14, mb: 0.75 }}>{item.label}</Box>
                <LinearProgress
                  variant="determinate"
                  value={item.fraction * 100}
                  color={item.full ? 'error' : 'success'}
                  aria-label={`${item.label}: ${formatBytes(item.used)} of ${formatBytes(item.quota)} used`}
                  sx={{ height: 8, borderRadius: 4 }}
                />
                <Box sx={{ mt: 0.5, fontSize: 12, color: 'rgba(0,0,0,0.6)' }}>{formatBytes(item.used)} used out of {formatBytes(item.quota)}</Box>
              </Box>
            ))}
          </Box>
        ))}
        <Box sx={{ fontSize: 12, color: 'rgba(0,0,0,0.6)' }}>File usage information can take up to 30 minutes to first appear or update.</Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
};
