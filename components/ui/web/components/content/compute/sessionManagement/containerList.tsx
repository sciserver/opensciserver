/* eslint-disable promise/catch-or-return */
import { FC, useMemo } from 'react';
import { ApolloError, useQuery } from '@apollo/client';
import { useRouter } from 'next/router';
import Image from 'next/image';
import styled from 'styled-components';
import Swal from 'sweetalert2';

import { GET_CONTAINERS } from 'src/graphql/containers';
import { Container } from 'src/graphql/typings';

import { LoadingAnimation } from 'components/common/loadingAnimation';
import { InfoCard } from 'components/common/infoCard';

import noContainersImg from 'public/No-containers.png';
import { ContainerDataGrid } from './containerDatagrid';

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
  selectContainer: (container: Container) => void;
}

export const ContainerList: FC<Props> = ({ selectContainer }) => {

  const router = useRouter();

  const { loading, data } = useQuery(GET_CONTAINERS,
    {
      onError: (error: ApolloError) => {
        if (error.message.includes('Unauthorized')) {
          router.push('/login?callbackURL=/compute');
        }
      }
    }
  );

  const showNewSessionModal = (pathToSession: string) => {
    Swal.fire({
      title: 'Notice about New Session creation',
      html: `This interactive session will terminate after a period of inactivity. 
      For long-running, asynchronous workloads please use <strong>JOBS</strong>. 
      Whenever you use SciServer, be sure to save your data in a user- or data-volume.
      `,
      icon: 'info',
      confirmButtonText: 'Continue',
      denyButtonText: 'Go to Jobs',
      showDenyButton: true
    }).then((result) => {
      if (result.isDenied) {
        router.push('/jobs/new');
      }
      if (result.isConfirmed) {
        router.push(pathToSession);
      }
    });

  };

  const newSessionOptions = [
    {
      title: 'New Quick Notebook',
      subtitle: 'Start a notebook with basic configuration for a quick start',
      imageSource: 'dataImages/SMUDGE_Disk_particles.png',
      action: () => showNewSessionModal(`/compute/run?${process.env.NEXT_PUBLIC_QUICK_START_CONFIG}`)
    },
    {
      title: 'New Custom Session',
      subtitle: 'Start a compute session with custom configuration',
      imageSource: 'dataImages/HEASARC.jpeg',
      action: () => showNewSessionModal('/compute/new')
    }
  ];

  const containerList = useMemo<Container[]>(() => {
    if (data && data.getContainers) {
      return data.getContainers;
    }
    return [];
  }, [data]);

  return <Styled>
    {loading &&
      <LoadingAnimation backDropIsOpen={loading} />
    }
    {containerList.length > 0 &&
      <ContainerDataGrid containerList={containerList} selectContainer={selectContainer} />
    }
    <div className="resources">
      {newSessionOptions.map(ns =>
        <InfoCard
          key={ns.title}
          title={ns.title}
          subtitle={ns.subtitle}
          imageSource={ns.imageSource}
          action={ns.action}
        />
      )}
    </div>
    {!loading && !containerList.length &&
      <div className="no-active-containers">
        <Image src={noContainersImg} width={400} alt="No containers illustration" />
        <h2>You don't have any active containers</h2>
      </div>
    }

  </Styled>;
};