import { FC, useMemo, useState } from 'react';
import Image from 'next/image';
import { useRouter } from 'next/router';
import { ApolloError, useMutation, useQuery } from '@apollo/client';
import { Button, Chip, IconButton, Tooltip } from '@mui/material';
import {
  Cancel as CancelIcon,
  Replay as ReplayIcon
} from '@mui/icons-material';
import styled from 'styled-components';
import Swal from 'sweetalert2';

import { CANCEL_JOB, CREATE_JOB, GET_JOBS } from 'src/graphql/jobs';
import { Job, JobStatus } from 'src/graphql/typings';

import { LoadingAnimation } from 'components/common/loadingAnimation';

import noContainersImg from 'public/No-containers.png';
import { DataGrid, GridColDef } from '@mui/x-data-grid';

const Styled = styled.div`
  .header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 0.5rem;
  }
    
  .no-active-containers {
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .resources {
    margin: 1rem;
    display: flex;
    gap: 1rem;
  }

  .new-job {
    display: block;
    margin: 1rem 3rem 1rem auto; /* pushes the button to the right */
  }

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

const jobStatusPollingInterval = 5000; // 5 seconds
const jobStatusThatNeedPolling = new Set([JobStatus.Pending, JobStatus.Accepted, JobStatus.Queued, JobStatus.Started, JobStatus.Finished]);
export const jobStatusAllowCancel = new Set([JobStatus.Pending, JobStatus.Accepted, JobStatus.Queued, JobStatus.Started]);
export const jobStatusAllowRerun = new Set([JobStatus.Error, JobStatus.Success]);
export const ReRunJobModalWording = {
  title: 'Rerun job',
  text: `Do you want to run this job again as is, or would you like to review and modify the job parameters before submitting?`,
  icon: 'question',
  showCancelButton: true,
  showDenyButton: true,
  confirmButtonText: 'Rerun unmodified',
  denyButtonText: 'Review and modify',
  cancelButtonText: 'Cancel'
};

export const JobsList: FC = () => {

  const router = useRouter();

  const { loading, data: allJobs, startPolling, stopPolling, refetch } = useQuery(GET_JOBS,
    {
      fetchPolicy: 'cache-and-network',
      variables: {
        top: 100,
        filters: {
          field: 'type',
          value: 'jobm.model.COMPMDockerJobModel'
        }
      },
      onError: (error: ApolloError) => {
        if (error.message.includes('Unauthorized')) {
          router.push('/login?callbackURL=/jobs');
        }
      }
    }
  );

  const [cancelJob] = useMutation(CANCEL_JOB, {
    onError: () => Swal.fire({
      title: 'Unable to cancel job',
      text: `Please try again. If the problem persists, contact us at <a href=\"mailto:${process.env.NEXT_PUBLIC_HELPDESK_EMAIL}\">${process.env.NEXT_PUBLIC_HELPDESK_EMAIL}</a> for more assistance.`,
      icon: 'error',
      confirmButtonText: 'OK'
    }).then(() => refetch()),
    onCompleted: () => Swal.fire({
      title: 'Job cancelled',
      text: 'The job has been successfully cancelled.',
      icon: 'success',
      confirmButtonText: 'OK'
    }).then(() => refetch())
  });

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
    await Swal.fire(ReRunJobModalWording as any).then((result) => {
      if (result.isConfirmed) {
        const resultsFolderURI = job.resultsFolderURI
          .split('/')
          // compm adds subdirs to the results folder, this indicates last non-dynamic index
          .slice(0, Number.parseInt(process.env.NEXT_PUBLIC_JOB_URI_CONSTANT_TERMINUS || '0'))
          .join('/');

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

  const columns: GridColDef[] = [
    { field: 'submissionTime', headerName: 'Submitted At', width: 200, valueGetter: (row: any) => new Date(row.submissionTime).toLocaleString() },
    { field: 'submitterDID', headerName: 'Name', width: 200 },
    {
      field: 'status',
      headerName: 'Status',
      width: 150,
      renderCell: (params) => (
        <Chip label={params.value} color={getStatus(params.row)} />
      )
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 150,
      renderCell: (params) => (
        <>
          {params.row.resultsFolderURI.length > 0 && jobStatusAllowRerun.has(params.row.status) &&
            <Tooltip title="Re-run Job">
              <IconButton onClick={(e) => {
                e.stopPropagation();
                rerunJob(params.row);
              }} size="small">
                <ReplayIcon className="replay-icon" />
              </IconButton>
            </Tooltip>
          }
          {jobStatusAllowCancel.has(params.row.status) &&
            <Tooltip title="Cancel Job">
              <IconButton onClick={(e) => {
                e.stopPropagation();
                cancelJob({ variables: { jobId: params.row.id } });
              }} size="medium">
                <CancelIcon className="delete-icon" />
              </IconButton>
            </Tooltip>
          }
        </>
      )
    }
  ];

  const totalJobs = useMemo(() => {
    if (allJobs && allJobs.getJobs) {
      return allJobs.getJobs.totalJobs;
    }
    return 0;
  }, [allJobs]);

  const jobsList = useMemo<Job[]>(() => {
    if (allJobs && allJobs.getJobs) {
      const jobs: Job[] = allJobs.getJobs.jobs;
      if (jobs.some(job => jobStatusThatNeedPolling.has(job.status))) {
        console.info('Starting polling');
        startPolling(jobStatusPollingInterval);
      }
      else {
        console.info('Stopping polling');
        stopPolling();
      }
      return jobs;
    }
    return [];
  }, [allJobs, startPolling, stopPolling]);

  return <Styled>
    <div className="header">
      <h1>Jobs</h1>
      <Button
        variant="contained"
        color="primary"
        className="new-job"
        aria-label="New Job"
        onClick={() => router.push('/jobs/new')}
      >
        New Job
      </Button>
    </div>
    {loading &&
      <LoadingAnimation backDropIsOpen={loading} />
    }
    <DataGrid
      rows={jobsList}
      columns={columns}
      slots={{
        noRowsOverlay: () => (
          <div className="no-active-containers">
            <Image src={noContainersImg} width={400} alt="No containers illustration" />
            <h2>You haven't run any jobs yet</h2>
          </div>
        )
      }}
      rowCount={totalJobs}
      initialState={{ pagination: { paginationModel: { page: 1, pageSize: 10 } } }}
      autoHeight
      pageSizeOptions={[10, 25, 50]}
      disableRowSelectionOnClick
      onRowClick={(params) => toggleRow(params.row.id)}
      getRowClassName={(params) => isRowOpen(params.row.id) ? 'job-row open' : 'job-row'}
      sx={{
        '& .MuiDataGrid-row': { cursor: 'pointer' },
        '& .MuiDataGrid-row:hover': { backgroundColor: 'rgba(0, 0, 0, 0.04)' }
      }}
      getRowId={(row) => row.id}
    />
  </Styled>;
};