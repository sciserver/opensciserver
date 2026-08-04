import { FC, useEffect, useMemo, useState } from 'react';
import Image from 'next/image';
import { useRouter } from 'next/router';
import { ApolloError, useQuery } from '@apollo/client';
import { Button, Chip } from '@mui/material';
import { DataGrid, GridColDef } from '@mui/x-data-grid';
import styled from 'styled-components';

import { GET_JOBS } from 'src/graphql/jobs';
import { Job, JobStatus } from 'src/graphql/typings';

import { jobStatusAllowRerun, RerunJobAction } from 'components/content/jobs/list/RerunJobAction';
import { CancelJobAction, jobStatusAllowCancel } from 'components/content/jobs/list/CancelJobAction';

import { LoadingAnimation } from 'components/common/loadingAnimation';
import noContainersImg from 'public/No-containers.png';
import { JobShortDetail } from '../detail/jobShortDetail';

const Styled = styled.div`

  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  height: 100%; 

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
    flex: 1;
    min-height: 0;
    height: clamp(420px, 70vh, 720px);

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
      font-weight: 800;
      text-transform: capitalize;
      background-color: ${({ theme }) => theme.palette.background.paper};
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

export const jobStatusPollingInterval = 5000; // 5 seconds
const jobStatusThatNeedPolling = new Set([JobStatus.Pending, JobStatus.Accepted, JobStatus.Queued, JobStatus.Started, JobStatus.Finished]);

export const JobsList: FC = () => {

  const router = useRouter();

  // State to track which job rows are expanded by their ID
  const [openRow, setOpenRow] = useState<Job | null>(null);
  const [paginationModel, setPaginationModel] = useState({
    pageSize: 10,
    page: 0
  });

  const [pageCursors, setPageCursors] = useState<string[]>(['']);
  const currentCursor = pageCursors[paginationModel.page] || '';
  const { loading, data: allJobs, previousData, startPolling, stopPolling, refetch } = useQuery(GET_JOBS,
    {
      fetchPolicy: 'cache-and-network',
      variables: {
        top: paginationModel.pageSize,
        end: currentCursor,
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

  const jobsData = allJobs ?? previousData;

  const totalJobs = useMemo(() => {
    if (jobsData?.getJobs) {
      return jobsData.getJobs.totalJobs;
    }
    return 0;
  }, [jobsData]);

  const jobsList = useMemo<Job[]>(() => {
    if (jobsData?.getJobs) {
      const jobs: Job[] = jobsData.getJobs.jobs;
      if (jobs.some(job => jobStatusThatNeedPolling.has(job.status))) {
        startPolling(jobStatusPollingInterval);
      }
      else {
        stopPolling();
      }
      return jobs;
    }
    return [];
  }, [jobsData, startPolling, stopPolling]);

  const columns: GridColDef[] = [
    {
      field: 'submissionTime',
      headerName: 'Submitted At',
      headerClassName: 'column-header',
      flex: 1,
      valueGetter: (row: string) => new Date(row).toLocaleString()
    },
    {
      field: 'submitterDID',
      headerName: 'Name',
      headerClassName: 'column-header',
      flex: 1
    },
    {
      field: 'status',
      headerName: 'Status',
      headerClassName: 'column-header',
      flex: 0.8,
      renderCell: (params) => (
        <Chip label={params.value} color={getStatus(params.row)} />
      )
    },
    {
      field: 'actions',
      headerName: 'Actions',
      headerClassName: 'column-header',
      flex: 0.8,
      renderCell: (params) => (
        <>
          {params.row.resultsFolderURI.length > 0 && jobStatusAllowRerun.has(params.row.status) &&
            <RerunJobAction job={params.row} startPolling={startPolling} />
          }
          {jobStatusAllowCancel.has(params.row.status) &&
            <CancelJobAction job={params.row} refetch={refetch} />
          }
        </>
      )
    }
  ];


  // Keeps track of the cursor (submissionDate of last job in the current list) 
  // for the next page when the user navigates through pages
  useEffect(() => {
    if (!allJobs?.getJobs?.jobs?.length) {
      return;
    }

    const nextCursor = allJobs.getJobs.jobs[allJobs.getJobs.jobs.length - 1]?.submissionTime || '';

    setPageCursors(prevCursors => {
      if (prevCursors[paginationModel.page + 1] === nextCursor) {
        return prevCursors;
      }

      const nextCursors = [...prevCursors];
      nextCursors[paginationModel.page + 1] = nextCursor;
      return nextCursors;
    });
  }, [allJobs, paginationModel.page]);

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
    <div className="grid">
      <DataGrid
        rows={jobsList}
        columns={columns}
        loading={loading}
        paginationModel={paginationModel}
        onPaginationModelChange={(model) => setPaginationModel(model)}
        slots={{
          noRowsOverlay: () => (
            <div className="no-active-containers">
              <Image src={noContainersImg} width={400} alt="No containers illustration" />
              <h2>You haven't run any jobs yet</h2>
            </div>
          )
        }}
        rowCount={totalJobs}
        paginationMode="server"
        pageSizeOptions={[10, 25, 50]}
        disableRowSelectionOnClick
        onRowClick={(params) => setOpenRow(params.row)}
        sx={{
          height: '90%',
          width: '100%'
        }}
        getRowId={(row) => row.id}
      />
    </div>
    {openRow &&
      <JobShortDetail job={openRow} isOpen={true} setOpenRow={setOpenRow} />
    }
  </Styled>;
};
