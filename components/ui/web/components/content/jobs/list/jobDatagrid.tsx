import { FC, useState } from 'react';
import { useRouter } from 'next/router';
import { useMutation } from '@apollo/client';
import styled from 'styled-components';
import {
  KeyboardArrowUp as KeyboardArrowUpIcon,
  KeyboardArrowDown as KeyboardArrowDownIcon,
  Cancel as CancelIcon,
  Replay as ReplayIcon
} from '@mui/icons-material';
import Swal from 'sweetalert2';

import {
  Chip,
  IconButton,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip
} from '@mui/material';

import { Job, JobStatus } from 'src/graphql/typings';
import { CREATE_JOB } from 'src/graphql/jobs';

import { JobShortDetail } from 'components/content/jobs/detail/jobShortDetail';
import { jobStatusAllowCancel, jobStatusAllowRerun } from 'components/content/jobs/list/jobsList';

const Styled = styled.div`
  margin-top: 2rem;

  .grid {
    width: inherit;

    .job-row {
      &:hover {
        background-color: ${({ theme }) => theme.palette.action.hover};
        cursor: pointer;
      }
    }
  
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

export const JobsDataGrid: FC<Props> = ({ jobsList, cancelJob }) => {

  const router = useRouter();

  // State to track which job rows are expanded by their ID
  const [openRows, setOpenRows] = useState<Set<string>>(new Set());

  const [createJob] = useMutation(CREATE_JOB, {
    onError: () => Swal.fire({
      title: 'Unable to add job',
      text: `Please try again. If the problem persists, contact us at <a href=\"mailto:${process.env.NEXT_PUBLIC_HELPDESK_EMAIL}\">${process.env.NEXT_PUBLIC_HELPDESK_EMAIL}</a> for more assistance.`,
      icon: 'error',
      confirmButtonText: 'OK'
    }).then(() => {
      return;
    }).catch(Error),
    onCompleted: () => Swal.fire({
      title: 'Job created successfully',
      text: 'Your job has been created and is now queued.',
      icon: 'success',
      confirmButtonText: 'OK'
    }).then(() => {
      router.reload();
    })
  });

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

  const rerunJob = async (job: Job) => {
    await Swal.fire({
      title: 'Rerun job',
      text: `Do you want to run job ${job.id} again as is, or would you like to review and modify the job parameters before submitting?`,
      icon: 'question',
      showCancelButton: true,
      showDenyButton: true,
      confirmButtonText: 'Rerun unmodified',
      denyButtonText: 'Review and modify',
      cancelButtonText: 'Cancel'
    }).then((result) => {
      if (result.isConfirmed) {
        const resultsFolderURI = job.resultsFolderURI.split('/').slice(0, -2).join('/');

        createJob({
          variables: {
            createJobParams: {
              dockerComputeEndpoint: job.dockerComputeEndpoint,
              dockerImageName: job.dockerImageName,
              resultsFolderURI,
              submitterDID: job.submitterDID,
              volumeContainers: job.dataVolumes.map(dv => dv.publisherDID),
              userVolumes: job.userVolumes.map(uv => uv.id),
              command: job.command,
              scriptURI: job.scriptURI || ''
            }
          }
        });
        return;
      }
      if (result.isDenied) {
        router.push({
          pathname: '/jobs/new',
          query: { rerunFromJobId: job.id }
        });
      }
    }).then(() => {
      return;
    });
  };

  return <Styled>
    <Paper sx={{ width: '95%' }}>
      <TableContainer sx={{ maxHeight: 440, minWidth: '100%' }}>
        <Table stickyHeader className="grid" aria-label=" Jobs Data Table">
          <TableHead>
            <TableRow>
              <TableCell />
              <TableCell className="column-header">Submitted At</TableCell>
              <TableCell className="column-header">Name</TableCell>
              <TableCell className="column-header">Status</TableCell>
              <TableCell className="column-header" id="actions-header">
                Actions
              </TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {jobsList.map((job) => (
              <>
                <TableRow className="job-row" onClick={() => toggleRow(job.id)} key={job.id}>
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
                  <TableCell className="cell" id="actions-cell">
                    {job.resultsFolderURI.length > 0 && jobStatusAllowRerun.has(job.status) &&
                      <Tooltip title="Re-run Job">
                        <IconButton onClick={(e) => { e.stopPropagation(); rerunJob(job); }} size="small">
                          <ReplayIcon className="replay-icon" />
                        </IconButton>
                      </Tooltip>
                    }
                    {jobStatusAllowCancel.has(job.status) &&
                      <Tooltip title="Cancel Job">
                        <IconButton onClick={(e) => { e.stopPropagation(); cancelJob({ variables: { jobId: job.id } }); }} size="medium">
                          <CancelIcon className="delete-icon" />
                        </IconButton>
                      </Tooltip>
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