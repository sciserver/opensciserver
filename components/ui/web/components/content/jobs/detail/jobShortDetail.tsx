import { FC, useState } from 'react';
import styled from 'styled-components';
import {
  Button,
  IconButton,
  Snackbar,
  Drawer
} from '@mui/material';
import {
  ContentCopy as ContentCopyIcon,
  Close as CloseIcon
} from '@mui/icons-material';
import { Job } from 'src/graphql/typings';
import { useRouter } from 'next/router';

const StyledDrawer = styled(Drawer)`
  .MuiDrawer-paper {
    width: 100%;
    max-height: 50%;
    overflow-y: auto;
    padding: 1rem;
  }

  .job-details-container {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 2rem;
    }

  .job-details {
    display: flex;
    gap: 3rem;
    
    .job-field {
      display : flex;
      flex-direction: column;
      gap: 0.5rem;
      align-items: center;

      p, h3 {
        margin: 0.2rem;
      }
    }
  }
  
  .actions {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
  }


  .command {
    pre {
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: #000;
      border: 1px solid #ddd;
      border-radius: 5px;
      color: #ddd;
      font-family: monospace;
      font-size: 12px;
      text-wrap: wrap;
      
      line-height: 1.6;
      padding: 1em 1.5em;
    }
  }
    
  .copy-icon {
    padding-left: 5rem;
  }
  
`;

type Props = {
  job: Job;
  isOpen: boolean;
  setOpenRow: (jobId: Job | null) => void;
}

export const JobShortDetail: FC<Props> = ({ job, isOpen, setOpenRow }) => {

  const router = useRouter();
  const [copiedSnackbarOpen, setCopiedSnackbarOpen] = useState(false);

  return <StyledDrawer
    anchor="bottom"
    open={isOpen}
    onClose={() => setOpenRow(null)}
  >
    <div className="job-details-container">
      <div className="job-details">
        <div className="job-field">
          <h3>
            Job ID:
          </h3>
          <p>
            {job.id}
          </p>
        </div>
        <div className="job-field">
          <h3>
            Image:
          </h3>
          <p>
            {job.dockerImageName}
          </p>
        </div>
        <div className="job-field">
          <h3>
            Started:
          </h3>
          <p>
            {job.startTime ? new Date(job.startTime).toLocaleString() : 'N/A'}
          </p>
        </div>
        <div className="job-field">
          <h3>
            Ended:
          </h3>
          <p>
            {job.endTime ? new Date(job.endTime).toLocaleString() : 'N/A'}
          </p>
        </div>
      </div>
      <div className="actions">
        <IconButton
          className="close-icon"
          size="small"
          aria-label="close"
          color="inherit"
          onClick={() => setOpenRow(null)}
        >
          <CloseIcon fontSize="medium" />
        </IconButton>
        <Button onClick={() => router.push(`/jobs/${job.id}`)} variant="contained" color="primary">
          See full details
        </Button>
      </div>
    </div>
    {job.command &&
      <div className="command">
        <pre>
          {job.command}
          <IconButton
            className="copy-icon"
            size="small"
            aria-label="copy"
            color="inherit"
            onClick={() => {
              navigator.clipboard.writeText(job.command);
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
      </div>
    }
  </StyledDrawer>;
};
