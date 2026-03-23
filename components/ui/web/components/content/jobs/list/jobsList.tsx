import { FC, useMemo } from 'react';
import Image from 'next/image';
import { useRouter } from 'next/router';
import { ApolloError, useMutation, useQuery } from '@apollo/client';
import { Button } from '@mui/material';
import styled from 'styled-components';
import Swal from 'sweetalert2';

import { CANCEL_JOB, GET_JOBS } from 'src/graphql/jobs';
import { Job, JobStatus } from 'src/graphql/typings';

import { LoadingAnimation } from 'components/common/loadingAnimation';
import { JobsDataGrid } from 'components/content/jobs/list/jobDatagrid';

import noContainersImg from 'public/No-containers.png';

const Styled = styled.div`
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

`;

const jobStatusPollingInterval = 5000; // 5 seconds
const jobStatusThatNeedPolling = new Set([JobStatus.Pending, JobStatus.Accepted, JobStatus.Queued, JobStatus.Started, JobStatus.Finished]);
export const jobStatusAllowCancel = new Set([JobStatus.Pending, JobStatus.Accepted, JobStatus.Queued, JobStatus.Started]);
export const jobStatusAllowRerun = new Set([JobStatus.Error, JobStatus.Success]);

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

  const jobsList = useMemo<Job[]>(() => {
    if (allJobs && allJobs.getJobs) {
      const jobs: Job[] = allJobs.getJobs;
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
    <h1>Jobs</h1>
    {loading &&
      <LoadingAnimation backDropIsOpen={loading} />
    }
    {jobsList.length > 0 &&
      <>
        <Button
          variant="contained"
          color="primary"
          className="new-job"
          onClick={() => router.push('/jobs/new')}
        >
          New Job
        </Button>
        <JobsDataGrid jobsList={jobsList} cancelJob={cancelJob} />
      </>
    }
    {!loading && !jobsList.length &&
      <div className="no-active-containers">
        <Image src={noContainersImg} width={400} alt="No containers illustration" />
        <h2>You haven't run any jobs yet</h2>
      </div>
    }

  </Styled>;
};