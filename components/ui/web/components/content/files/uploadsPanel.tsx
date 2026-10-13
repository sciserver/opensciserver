import { FC } from 'react';
import { Box, ButtonBase, LinearProgress } from '@mui/material';

export type UploadItem = {
  id: number;
  name: string;
  /** Destination folder, for display. */
  where: string;
  /** 0-1. */
  progress: number;
  status: 'uploading' | 'done' | 'error';
  error?: string;
  abort: () => void;
};

type Props = {
  uploads: UploadItem[];
  onDismiss: (id: number) => void;
};

const ICON = { uploading: 'upload', done: 'check_circle', error: 'error' };
const COLOR = { uploading: '#398CBF', done: '#20A183', error: '#C62828' };

/** Floating list of uploads in this session: progress while running, then the outcome until dismissed. */
export const UploadsPanel: FC<Props> = ({ uploads, onDismiss }) => (uploads.length === 0 ? null : (
  <Box
    role="status"
    aria-label="Uploads"
    sx={{ position: 'absolute', left: 16, bottom: 16, zIndex: 20, width: 320, maxHeight: 240, overflow: 'auto', bgcolor: '#fff', borderRadius: 1, boxShadow: '0 5px 5px -3px rgba(0,0,0,0.2), 0 8px 10px 1px rgba(0,0,0,0.14), 0 3px 14px 2px rgba(0,0,0,0.12)' }}
  >
    {uploads.map((item) => (
      <Box key={item.id} sx={{ display: 'flex', alignItems: 'center', gap: 1, px: 1.5, py: 1, borderBottom: '1px solid #f0f2f4' }}>
        <span className="material-symbols-outlined" style={{ fontSize: 20, color: COLOR[item.status] }}>{ICON[item.status]}</span>
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Box title={item.name} sx={{ fontSize: 13, fontWeight: 500, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{item.name}</Box>
          {item.status === 'uploading' && <LinearProgress variant="determinate" value={Math.round(item.progress * 100)} sx={{ mt: 0.5 }} />}
          {item.status === 'done' && <Box sx={{ fontSize: 12, color: 'rgba(0,0,0,0.6)' }}>Uploaded to {item.where}</Box>}
          {item.status === 'error' && <Box sx={{ fontSize: 12, color: '#C62828' }}>{item.error}</Box>}
        </Box>
        <ButtonBase
          aria-label={item.status === 'uploading' ? `Cancel upload of ${item.name}` : `Dismiss ${item.name}`}
          onClick={() => (item.status === 'uploading' ? item.abort() : onDismiss(item.id))}
          sx={{ width: 28, height: 28, borderRadius: '50%', color: 'rgba(0,0,0,0.6)' }}
        >
          <span className="material-symbols-outlined" style={{ fontSize: 18 }}>close</span>
        </ButtonBase>
      </Box>
    ))}
  </Box>
));
