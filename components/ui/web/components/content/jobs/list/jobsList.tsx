import { FC, useMemo } from 'react';
import Image from 'next/image';
import { useRouter } from 'next/router';
import { ApolloError, useQuery } from '@apollo/client';
import styled from 'styled-components';

import { GET_JOBS } from 'src/graphql/jobs';
import { Job } from 'src/graphql/typings';

import noContainersImg from 'public/No-containers.png';
import { LoadingAnimation } from 'components/common/loadingAnimation';
import { JobsDataGrid } from 'components/content/jobs/list/jobDatagrid';

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

`;

export const JobsList: FC = () => {

  const router = useRouter();

  const { loading, data: allJobs } = useQuery(GET_JOBS,
    {
      variables: {
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

  const jobsList = useMemo<Job[]>(() => {
    if (allJobs && allJobs.getJobs) {
      return allJobs.getJobs;
    }
    return [];
  }, [allJobs]);

  return <Styled>
    <h1>Jobs</h1>
    {loading &&
      <LoadingAnimation backDropIsOpen={loading} />
    }
    {jobsList.length > 0 &&
      <JobsDataGrid jobsList={jobsList} />
    }
    {!loading && !jobsList.length &&
      <div className="no-active-containers">
        <Image src={noContainersImg} width={400} alt="No containers illustration" />
        <h2>You haven't run any jobs yet</h2>
      </div>
    }

  </Styled>;
};