import { FC, useMemo } from 'react';
import { ApolloError, useMutation, useQuery } from '@apollo/client';
import { useRouter } from 'next/router';
import Image from 'next/image';
import styled from 'styled-components';

import { CREATE_JOB, GET_JOBS } from 'src/graphql/jobs';
import { Job } from 'src/graphql/typings';

import { LoadingAnimation } from 'components/common/loadingAnimation';

import noContainersImg from 'public/No-containers.png';
import { JobsDataGrid } from './jobDatagrid';
import Swal from 'sweetalert2';

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

  .grid {
    width: 95%;
    border: none;

     .MuiDataGrid-columnHeader {
      font-style: normal;
      font-size: 14px;
      letter-spacing: 0.25px;
      font-weight: 600;
      text-transform: capitalize;
      .MuiCheckbox-root {
        height: 100%;        
        padding: 15px;
      }
    }

    .MuiDataGrid-cell {
        padding: 12px 25px;
        font-weight: 500;
        border-top: 1px solid #E0E0E0;
    }

    .icon {
      color: ${({ theme }) => theme.palette.icons.danger};
    }
  }
`;

type Props = {
  selectJob: (job: Job) => void;
}

export const JobsList: FC<Props> = ({ selectJob }) => {

  const router = useRouter();

  const { loading, data: allJobs } = useQuery(GET_JOBS,
    {
      onError: (error: ApolloError) => {
        if (error.message.includes('Unauthorized')) {
          router.push('/login?callbackURL=/jobs');
        }
      }
    }
  );

  const [createJob, { data: newJob, error }] = useMutation(CREATE_JOB, {
    onError: () => Swal.fire({
      title: 'Unable to add docker job',
      text: 'Please email <a href=\"mailto:sciserver-helpdesk@jhu.edu\">sciserver-helpdesk@jhu.edu</a> for more assistance.',
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

  const jobsList = useMemo<Job[]>(() => {
    if (allJobs && allJobs.getJobs) {
      return allJobs.getJobs;
    }
    if (newJob && newJob.createJob) {
      return [...allJobs.getJobs, newJob.createJob];
    }
    return [];
  }, [allJobs, newJob]);

  const createJobHandler = (job: Job) => {
    createJob({
      variables: {
        createJobParams: {
          dockerComputeEndpoint: job.dockerComputeEndpoint,
          dockerImageName: job.dockerImageName,
          resultsFolderURI: job.resultsFolderURI,
          submitterDID: job.submitterDID,
          volumeContainers: job.dataVolumes.map(dv => dv.publisherDID),
          userVolumes: job.userVolumes.map(uv => uv.id),
          command: job.command,
          scriptURI: job.scriptURI || ''
        }
      }
    });
  };

  return <Styled>
    {loading &&
      <LoadingAnimation backDropIsOpen={loading} />
    }
    {jobsList.length > 0 &&
      <JobsDataGrid createJob={createJobHandler} jobsList={jobsList} selectJob={selectJob} />
    }
    {!loading && !jobsList.length &&
      <div className="no-active-containers">
        <Image src={noContainersImg} width={400} alt="No containers illustration" />
        <h2>You haven't run any jobs yet</h2>
      </div>
    }

  </Styled>;
};