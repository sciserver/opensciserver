import { FC, useMemo, useState } from 'react';
import { useRouter } from 'next/router';
import { useQuery, ApolloError } from '@apollo/client';
import styled from 'styled-components';
import { Chip, Divider, IconButton } from '@mui/material';
import { Close as CloseIcon } from '@mui/icons-material';

import { DataVolume, Domain, Image, UserVolume } from 'src/graphql/typings';
import { GET_DOMAINS } from 'src/graphql/domains';

import { NewComputeSessionOptions } from 'components/content/newComputeSession/newComputeSessionOptions';
import { NewComputeSessionForm } from 'components/content/newComputeSession/newComputeSessionForm';

const Styled = styled.div`
  margin-top: -1%;
  margin-bottom: -10%;

  .content {
    display: grid;
    grid-template-columns: 40% 60%;
    grid-template-rows: auto;
    grid-template-areas: 
      "header header"
      "left-panel right-panel";
  }

  .header {
      grid-area: header;
      
      .title {
        display: flex;
        align-items: center;
        gap: 0.5rem;

        .alert {
          color: ${({ theme }) => theme.palette.warning.dark};
        }
      }
    }

    .divider {
      margin-left: -3rem; 
      margin-right: -4rem;
    }

    .left-panel {
      height: 100%;
      padding-bottom: 2rem;
      grid-area: left-panel;
      border-right: 1px solid ${({ theme }) => theme.palette.divider};
    }

    .right-panel {
      padding: 1rem;
      grid-area: right-panel;
      margin-left: 2rem;
    }
      
    .step-buttons {
      display: flex;
      justify-content: space-between;
      margin-top: 1rem;
    }
`;

type Props = {
  sessionName: string;
  setSessionName: (name: string) => void;
  domainChoice?: Domain;
  setDomainChoice: (domain: Domain | undefined) => void;
  imageChoice?: Image;
  setImageChoice: (image: Image | undefined) => void;
  dataVolumesChoice: DataVolume[];
  setDataVolumesChoice: (dataVols: DataVolume[]) => void;
  userVolumesChoice: UserVolume[];
  setUserVolumesChoice: (userVols: UserVolume[]) => void;
  submit: () => void;
  loadingSubmit: boolean;
  isJob?: boolean;
};

export const NewComputeSession: FC<Props> = ({
  sessionName,
  setSessionName,
  domainChoice,
  setDomainChoice,
  imageChoice,
  setImageChoice,
  dataVolumesChoice,
  setDataVolumesChoice,
  userVolumesChoice,
  setUserVolumesChoice,
  submit,
  loadingSubmit,
  isJob
}) => {

  const router = useRouter();

  const { loading: loadingData, data } = useQuery(GET_DOMAINS,
    {
      variables: { jobs: isJob },
      onError: (error: ApolloError) => {
        if (error.message.includes('Unauthorized')) {
          router.push('/login?callbackURL=/jobs/new');
        }
      }
    }
  );

  const domainList = useMemo<Domain[]>(() => {
    if (data && data.getDomains) {
      const defaultDomainName = isJob ? process.env.NEXT_PUBLIC_NEW_JOB_DOMAIN_NAME_DEFAULT : process.env.NEXT_PUBLIC_NEW_SESSION_DOMAIN_NAME_DEFAULT;
      setDomainChoice((data.getDomains as Domain[]).find(d => d.name === defaultDomainName));
      return data.getDomains;
    }
    return [];
  }, [data]);

  const imageList = useMemo<Image[]>(() => {
    if (domainChoice) {
      const defaultImageName = isJob ? process.env.NEXT_PUBLIC_NEW_JOB_IMAGE_NAME_DEFAULT : process.env.NEXT_PUBLIC_NEW_SESSION_IMAGE_NAME_DEFAULT;
      setImageChoice(domainChoice.images.find(d => d.name === defaultImageName));
      return domainChoice.images;
    }
    return [];
  }, [domainChoice]);

  const dataVolumeList = useMemo<DataVolume[]>(() => {
    if (domainChoice) {
      return domainChoice.dataVolumes;
    }
    return [];
  }, [domainChoice]);

  const userVolumeList = useMemo<UserVolume[]>(() => {
    if (domainChoice) {
      const defaultUVs = (domainChoice.userVolumes as UserVolume[]).filter(uv => uv.name === 'scratch' || uv.name === 'persistent');
      setUserVolumesChoice(defaultUVs);
      return domainChoice.userVolumes;
    }
    return [];
  }, [domainChoice]);

  return <Styled>
    <div className="header">
      <div className="title">
        <IconButton onClick={() => router.push('/jobs')} >
          <CloseIcon />
        </IconButton>
        <h3>New {isJob ? 'Job' : 'Compute Session'}</h3>
        <Chip color="warning" label="BETA" />
      </div>
    </div>
    <Divider className="divider" />
    <div className="content">
      <div className="left-panel">
        <NewComputeSessionOptions
          domainList={domainList}
          domainChoice={domainChoice}
          setDomainChoice={setDomainChoice}
          imageList={imageList}
          imageChoice={imageChoice}
          setImageChoice={setImageChoice}
          dataVolumeList={dataVolumeList}
          dataVolumesChoice={dataVolumesChoice}
          setDataVolumesChoice={setDataVolumesChoice}
          userVolumeList={userVolumeList}
          userVolumesChoice={userVolumesChoice}
          setUserVolumesChoice={setUserVolumesChoice}
        />
      </div>
      <div className="right-panel">
        <NewComputeSessionForm
          sessionName={sessionName}
          setSessionName={setSessionName}
          domainChoice={domainChoice}
          imageChoice={imageChoice}
          dataVolumesChoice={dataVolumesChoice}
          userVolumesChoice={userVolumesChoice}
          submit={submit}
          loadingSubmit={loadingSubmit}
          loadingData={loadingData}
          isJob
        />
      </div>
    </div>
  </Styled>;
};