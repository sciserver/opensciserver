import { FC, useState } from 'react';
import styled from 'styled-components';
import {
  TableRow,
  TableCell,
  Collapse,
  Typography,
  Button,
  IconButton,
  Snackbar,
  Box
} from '@mui/material';
import {
  ContentCopy as ContentCopyIcon,
  Close as CloseIcon
} from '@mui/icons-material';
import { Job } from 'src/graphql/typings';
import { useRouter } from 'next/router';

const StyledTableRow = styled(TableRow)`
  .job-details {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    
    .job-field {
      display : flex;
      gap: 0.5rem;
      align-items: center;

      p, h3 {
        margin: 0.2rem;
      }
    }
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
}

export const JobShortDetail: FC<Props> = ({ job, isOpen }) => {

  const router = useRouter();
  const [copiedSnackbarOpen, setCopiedSnackbarOpen] = useState(false);

  return <StyledTableRow>
    <TableCell style={{ paddingBottom: 0, paddingTop: 0 }} colSpan={6}>
      <Collapse in={isOpen} timeout="auto" unmountOnExit>
        <Box sx={{ margin: 1.5 }}>
          <div className="job-details">
            <div>
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
            </div>
            <div>
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
            <div>
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
                  aria-label="close"
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
        </Box>
      </Collapse>
    </TableCell>
  </StyledTableRow>;
};
