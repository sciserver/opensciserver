import { FC } from 'react';
import styled from 'styled-components';
import { LinearProgress } from '@mui/material';

import { CircleButton, DANGER, Icon, MUTED, ROW_BORDER } from './filesStyles';

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

const Styled = styled.div`
  position: absolute;
  left: 16px;
  bottom: 16px;
  z-index: 20;
  width: 320px;
  max-height: 240px;
  overflow: auto;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 5px 5px -3px rgba(0, 0, 0, 0.2), 0 8px 10px 1px rgba(0, 0, 0, 0.14), 0 3px 14px 2px rgba(0, 0, 0, 0.12);

  .upload {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    border-bottom: 1px solid ${ROW_BORDER};
  }

  .details {
    flex: 1;
    min-width: 0;
  }

  .name {
    font-size: 13px;
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .progress {
    margin-top: 4px;
  }

  .note {
    font-size: 12px;
    color: ${MUTED};
  }

  .failed {
    font-size: 12px;
    color: ${DANGER};
  }
`;

const statusColor = (status: UploadItem['status'], palette: { secondary: { main: string }; contrast2: { main: string } }) => {
  if (status === 'done') {
    return palette.contrast2.main;
  }
  return status === 'error' ? DANGER : palette.secondary.main;
};

const StatusIcon = styled(Icon)<{ $status: UploadItem['status'] }>`
  color: ${({ theme, $status }) => statusColor($status, theme.palette)};
`;

/** Floating list of uploads in this session: progress while running, then the outcome until dismissed. */
export const UploadsPanel: FC<Props> = ({ uploads, onDismiss }) => (uploads.length === 0 ? null : (
  <Styled role="status" aria-label="Uploads">
    {uploads.map((item) => (
      <div key={item.id} className="upload">
        <StatusIcon $status={item.status}>{ICON[item.status]}</StatusIcon>
        <div className="details">
          <div className="name" title={item.name}>{item.name}</div>
          {item.status === 'uploading' && <LinearProgress className="progress" variant="determinate" value={Math.round(item.progress * 100)} />}
          {item.status === 'done' && <div className="note">Uploaded to {item.where}</div>}
          {item.status === 'error' && <div className="failed">{item.error}</div>}
        </div>
        <CircleButton
          $size={28}
          aria-label={item.status === 'uploading' ? `Cancel upload of ${item.name}` : `Dismiss ${item.name}`}
          onClick={() => (item.status === 'uploading' ? item.abort() : onDismiss(item.id))}
        >
          <Icon $size={18}>close</Icon>
        </CircleButton>
      </div>
    ))}
  </Styled>
));
