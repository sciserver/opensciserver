import { FC, useState } from 'react';
import { useRouter } from 'next/router';
import styled from 'styled-components';
import {
  KeyboardArrowUp as KeyboardArrowUpIcon,
  KeyboardArrowDown as KeyboardArrowDownIcon,
  Cancel as CancelIcon
} from '@mui/icons-material';

import {
  Button,
  Chip,
  IconButton,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow
} from '@mui/material';

import { Job, JobStatus } from 'src/graphql/typings';
import { JobShortDetail } from 'components/content/jobs/detail/jobShortDetail';

const Styled = styled.div`
  margin-top: 2rem;
  
  .new-job {
    display: block;
    margin: 1rem 3rem 1rem auto; /* pushes the button to the right */
  }

  .grid {
    width: inherit;
  
    .column-header {
      font-style: normal;
      font-size: 14px;
      letter-spacing: 0.25px;
      font-weight: 600;
      text-transform: capitalize;
    }

    .delete-icon {
      color: ${({ theme }) => theme.palette.icons.danger};
    }
    .run-icon {
      color: ${({ theme }) => theme.palette.icons.play};
    }
  }
`;

const getStatus = (job: Job) => {
  switch (job.status) {
    case JobStatus.Success: {
      return 'success';
    }
    case JobStatus.Error: {
      return 'error';
    }
    default: {
      return 'secondary';
    }
  }
};

type Props = {
  jobsList: Job[];
  cancelJob: (jobId: { variables: { jobId: string; }; }) => void;
}

const jobStatusAllowCancel = new Set([JobStatus.Pending, JobStatus.Accepted, JobStatus.Queued, JobStatus.Started]);
export const JobsDataGrid: FC<Props> = ({ jobsList, cancelJob }) => {
  const router = useRouter();
  // State to track which job rows are expanded by their ID
  const [openRows, setOpenRows] = useState<Set<string>>(new Set());

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

  return <Styled>
    <Button
      variant="contained"
      color="primary"
      className="new-job"
      onClick={() => router.push('/jobs/new')}
    >
      New Job
    </Button>
    <Paper sx={{ width: '95%' }}>
      <TableContainer sx={{ maxHeight: 440, minWidth: '100%' }}>
        <Table stickyHeader className="grid" aria-label=" Jobs Data Table">
          <TableHead>
            <TableRow>
              <TableCell />
              <TableCell className="column-header">Submitted At</TableCell>
              <TableCell className="column-header">Name</TableCell>
              <TableCell className="column-header">Status</TableCell>
              <TableCell className="column-header"></TableCell>
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
                  <TableCell className="cell">
                    <Chip label={job.status} color={getStatus(job)} />
                  </TableCell>
                  <TableCell className="cell">
                    {jobStatusAllowCancel.has(job.status) &&
                      <IconButton onClick={() => cancelJob({ variables: { jobId: job.id } })} size="small">
                        <CancelIcon className="delete-icon" />
                      </IconButton>
                    }
                  </TableCell>
                </TableRow>
                <JobShortDetail job={job} isOpen={isRowOpen(job.id)} />
              </>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Paper>
  </Styled>;
};