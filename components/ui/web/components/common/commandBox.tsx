import { FC, useState } from 'react';
import styled from 'styled-components';
import { IconButton, Snackbar } from '@mui/material';
import {
  ContentCopy as ContentCopyIcon,
  Close as CloseIcon
} from '@mui/icons-material';

type Props = {
  command: string;
  width?: string;
  maxHeight?: string;
};

const Styled = styled.div<{ width: string; maxHeight: string }>`
  width: ${props => props.width};

  pre {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    background: #000;
    border: 1px solid #ddd;
    border-radius: 5px;
    color: #ddd;
    font-family: monospace;
    font-size: 12px;
    white-space: pre-wrap;
    word-break: break-word;
    overflow-wrap: anywhere;
    max-height: ${props => props.maxHeight};
    overflow: auto;

    line-height: 1.6;
    padding: 1em 1.5em;
  }

  .copy-icon {
    padding-left: 5rem;
    position: sticky;
    top: 0;
  }
`;

export const CommandBox: FC<Props> = ({ command, width = '100%', maxHeight = '40vh' }) => {

  const [copiedSnackbarOpen, setCopiedSnackbarOpen] = useState(false);

  return <Styled className="command" {...{ width, maxHeight }}>
    <pre>
      {command}
      <IconButton
        className="copy-icon"
        size="small"
        aria-label="copy"
        color="inherit"
        onClick={() => {
          navigator.clipboard.writeText(command);
          setCopiedSnackbarOpen(true);
        }}
      >
        <ContentCopyIcon fontSize="medium" />
      </IconButton>
    </pre>
    <Snackbar
      open={copiedSnackbarOpen}
      autoHideDuration={5000}
      anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      onClose={() => setCopiedSnackbarOpen(false)}
      message="Copied to clipboard!"
      action={<>
        <IconButton
          size="small"
          aria-label="close"
          color="inherit"
          onClick={() => setCopiedSnackbarOpen(false)}
        >
          <CloseIcon fontSize="small" />
        </IconButton>
      </>}
    />
  </Styled>;
};
