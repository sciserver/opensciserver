import { FC, useContext } from 'react';
import { useQuery } from '@apollo/client';
import styled from 'styled-components';
import { Alert, Button, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle, LinearProgress } from '@mui/material';

import { UserContext } from 'context';
import { FILE_QUOTAS } from 'src/graphql/volumes';
import { FileService, FileUsage } from 'src/graphql/typings';
import { formatBytes } from 'src/utils/fileEntries';
import { groupQuotas } from 'src/utils/quotas';

import { BORDER, MUTED, ROW_BORDER } from './filesStyles';

const Content = styled(DialogContent)`
  && {
    min-height: 120px;
  }

  .loading {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
  }

  .muted {
    font-size: 14px;
    color: ${MUTED};
  }

  .footnote {
    font-size: 12px;
    color: ${MUTED};
  }

  .group {
    margin-bottom: 16px;
    border: 1px solid ${BORDER};
    border-radius: 4px;
    overflow: hidden;
  }

  .group-title {
    padding: 8px 16px;
    background: #f4f6f8;
    font-weight: 600;
    font-size: 14px;
  }

  .quota {
    padding: 10px 16px;
    border-top: 1px solid ${ROW_BORDER};
  }

  .quota-label {
    font-size: 14px;
    margin-bottom: 6px;
  }

  .quota-bar {
    height: 8px;
    border-radius: 4px;
  }

  .quota-used {
    margin-top: 4px;
    font-size: 12px;
    color: ${MUTED};
  }
`;

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
      <Content>
        {loading && <div className="loading"><CircularProgress size={16} /> Loading quotas…</div>}
        {error && <Alert severity="error">Could not load quotas: {error.message}</Alert>}
        {data && groups.length === 0 && <div className="muted">No quota information is available yet.</div>}
        {groups.map((group) => (
          <div key={group.rootVolumeId} className="group">
            <div className="group-title">{group.rootVolumeName}</div>
            {group.items.map((item) => (
              <div key={item.key} className="quota">
                <div className="quota-label">{item.label}</div>
                <LinearProgress
                  className="quota-bar"
                  variant="determinate"
                  value={item.fraction * 100}
                  color={item.full ? 'error' : 'success'}
                  aria-label={`${item.label}: ${formatBytes(item.used)} of ${formatBytes(item.quota)} used`}
                />
                <div className="quota-used">{formatBytes(item.used)} used out of {formatBytes(item.quota)}</div>
              </div>
            ))}
          </div>
        ))}
        <div className="footnote">File usage information can take up to 30 minutes to first appear or update.</div>
      </Content>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
};
