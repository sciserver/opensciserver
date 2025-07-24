import { FC, useState } from 'react';
import styled from 'styled-components';
import {
  Replay as ReplayIcon,
  ContentCopy as ContentCopyIcon,
  Close as CloseIcon,
  KeyboardArrowUp as KeyboardArrowUpIcon,
  KeyboardArrowDown as KeyboardArrowDownIcon
} from '@mui/icons-material';

import { Job } from 'src/graphql/typings';
import {
  Box,
  Button,
  Chip,
  Collapse,
  IconButton,
  Paper,
  Snackbar,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip,
  Typography
} from '@mui/material';

const Styled = styled.div`
margin-top: 2rem;
  
  .grid {  
    width: 100%;
     .columnHeader {
      font-style: normal;
      font-size: 14px;
      letter-spacing: 0.25px;
      font-weight: 600;
      text-transform: capitalize;
    }

    .job-details {
      display: flex;
      gap: 2rem;
      
      .job-field {
        display : flex;
        gap: 1rem;
        align-items: center;
      }
    }

    .command {
        pre {
          display: flex;
          justify-content: center;
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
    }

    .delete-icon {
      color: ${({ theme }) => theme.palette.icons.danger};
    }
    .run-icon {
      color: ${({ theme }) => theme.palette.icons.play};
    }
  }
`;

type Props = {
  jobsList: Job[];
  selectJob: (job: Job) => void;
  createJob: (variables: Job) => void;
}

export const JobsDataGrid: FC<Props> = ({ jobsList, selectJob, createJob }) => {
  // State to track which job rows are expanded by their ID
  const [openRows, setOpenRows] = useState<Set<string>>(new Set());
  const [copiedSnackbarOpen, setCopiedSnackbarOpen] = useState(false);

  // Toggle a specific row's open state
  const toggleRow = (jobId: string) => {
    setOpenRows(prevOpenRows => {
      const newOpenRows = new Set(prevOpenRows);
      if (newOpenRows.has(jobId)) {
        newOpenRows.delete(jobId);
        return newOpenRows;
      }

      newOpenRows.add(jobId);
      return newOpenRows;
    });
  };

  // Check if a specific row is open
  const isRowOpen = (jobId: string) => openRows.has(jobId);

  const getStatus = (job: Job) => {
    switch (job.status) {
      case 'SUCCESS': {
        return <Chip label="Success" color="success" />;
      }
      case 'ERROR': {
        return <Chip label="Error" color="error" />;
      }
      default: {
        return <Chip label={job.status} color="primary" />;
      }
    }
  };

  return <Styled>
    <Paper sx={{ width: '90%' }}>

      <TableContainer sx={{ maxHeight: 440 }}>
        <Table stickyHeader className="grid" aria-label=" Jobs Data Table">
          <TableHead>
            <TableRow>
              <TableCell />
              <TableCell className="columnHeader">Submitted At</TableCell>
              <TableCell className="columnHeader">Name</TableCell>
              <TableCell className="columnHeader">Status</TableCell>
              <TableCell className="columnHeader" align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {jobsList.map((job) => (
              <>
                <TableRow key={job.id}>
                  <TableCell>
                    <IconButton
                      aria-label="expand row"
                      size="small"
                      onClick={() => toggleRow(job.id)}
                    >
                      {isRowOpen(job.id) ? <KeyboardArrowUpIcon /> : <KeyboardArrowDownIcon />}
                    </IconButton>
                  </TableCell>
                  <TableCell className="cell" component="th" scope="row">
                    {new Date(job.submissionTime).toLocaleString()}
                  </TableCell>
                  <TableCell className="cell">{job.submitterDID}</TableCell>
                  <TableCell className="cell">{getStatus(job)}</TableCell>
                  <TableCell className="cell" align="right">
                    <Tooltip title="Re Run Job">
                      <IconButton color="primary" onClick={() => createJob(job)}>
                        <ReplayIcon />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
                <TableRow>
                  <TableCell style={{ paddingBottom: 0, paddingTop: 0 }} colSpan={6}>
                    <Collapse in={isRowOpen(job.id)} timeout="auto" unmountOnExit>
                      <Box sx={{ margin: 5 }}>
                        <div className="job-details">
                          <div>
                            <div className="job-field">
                              <Typography variant="h5" gutterBottom component="div">
                                Job ID:
                              </Typography>
                              <Typography variant="body1" gutterBottom component="div">
                                {job.id}
                              </Typography>
                            </div>
                            <div className="job-field">
                              <Typography variant="h5" gutterBottom component="div">
                                Image:
                              </Typography>
                              <Typography variant="body1" gutterBottom component="div">
                                {job.dockerImageName}
                              </Typography>
                            </div>
                          </div>
                          <div>
                            <div className="job-field">
                              <Typography variant="h5" gutterBottom component="div">
                                Started:
                              </Typography>
                              <Typography variant="body1" gutterBottom component="div">
                                {new Date(job.startTime).toLocaleString()}
                              </Typography>
                            </div>
                            <div className="job-field">
                              <Typography variant="h5" gutterBottom component="div">
                                Ended:
                              </Typography>
                              <Typography variant="body1" gutterBottom component="div">
                                {new Date(job.endTime).toLocaleString()}
                              </Typography>
                            </div>
                          </div>
                          <div>
                            <Button variant="contained" color="primary">
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
                </TableRow>
              </>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Paper>
  </Styled>;
};