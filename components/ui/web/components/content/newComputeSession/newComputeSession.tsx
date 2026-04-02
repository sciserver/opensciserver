import { FC, useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/router';
import { useQuery, ApolloError } from '@apollo/client';
import styled from 'styled-components';
import { Button, Chip, CircularProgress, Divider, IconButton } from '@mui/material';
import { Close as CloseIcon } from '@mui/icons-material';

import { DataVolume, Domain, Image, Job, UserVolume } from 'src/graphql/typings';
import { GET_DOMAINS } from 'src/graphql/domains';

import { NewComputeSessionOptions } from 'components/content/newComputeSession/newComputeSessionOptions';
import { NewComputeSessionForm } from 'components/content/newComputeSession/newComputeSessionForm';
import { LoadingAnimation } from 'components/common/loadingAnimation';

const Styled = styled.div`
  margin-top: -1%;
  margin-bottom: -10%;

  .content {
    display: grid;
    grid-template-columns: 60% 40%;
    grid-template-rows: auto;
    grid-template-areas: 
      "header header"
      "left-panel right-panel";
  }

  .header {
      grid-area: header;
      display: flex;
      justify-content: space-between;
      align-items: center;
      
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
  command?: string;
  setCommand?: (command: string) => void;
  resultsFolderURI?: string;
  setResultsFolderURI?: (uri: string) => void;
  submit: () => void;
  loadingSubmit: boolean;
  isJob?: boolean;
  editJob?: Job;
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
  command,
  setCommand,
  resultsFolderURI,
  setResultsFolderURI,
  submit,
  loadingSubmit,
  isJob,
  editJob
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

  const [commandError, setCommandError] = useState<boolean>(false);

  const domainList = useMemo<Domain[]>(() => {
    if (data && data.getDomains) {
      if (editJob) {
        const editJobDomain = (data.getDomains as Domain[]).find((d: Domain) => d.apiEndpoint === editJob.dockerComputeEndpoint);
        setDomainChoice(editJobDomain);
      }
      else {
        const defaultDomainName = isJob ? process.env.NEXT_PUBLIC_NEW_JOB_DOMAIN_NAME_DEFAULT : process.env.NEXT_PUBLIC_NEW_SESSION_DOMAIN_NAME_DEFAULT;
        setDomainChoice((data.getDomains as Domain[]).find(d => d.name === defaultDomainName));
      }
      return data.getDomains;
    }
    return [];
  }, [data, editJob]);

  const imageList = useMemo<Image[]>(() => {
    if (domainChoice) {
      if (editJob) {
        const editJobImage = domainChoice.images.find(d => d.name === editJob.dockerImageName);
        setImageChoice(editJobImage);
      }
      else {
        const defaultImageName = isJob ? process.env.NEXT_PUBLIC_NEW_JOB_IMAGE_NAME_DEFAULT : process.env.NEXT_PUBLIC_NEW_SESSION_IMAGE_NAME_DEFAULT;
        setImageChoice(domainChoice.images.find(d => d.name === defaultImageName));
      }
      return domainChoice.images;
    }
    return [];
  }, [domainChoice, editJob]);

  const dataVolumeList = useMemo<DataVolume[]>(() => {
    if (domainChoice) {
      if (editJob) {
        setDataVolumesChoice(editJob.dataVolumes.map(dv => domainChoice.dataVolumes.find(dvl => dvl.id === dv.id)!));
      }
      return domainChoice.dataVolumes;
    }
    return [];
  }, [domainChoice, editJob]);

  const userVolumeList = useMemo<UserVolume[]>(() => {
    if (domainChoice) {
      const defaultUVs = (domainChoice.userVolumes as UserVolume[]).filter(uv => uv.name === 'scratch' || uv.name === 'persistent');
      setUserVolumesChoice(defaultUVs);
      if (editJob) {
        setUserVolumesChoice(editJob.userVolumes.map(uv => domainChoice.userVolumes.find(uvl => uvl.id === uv.userVolumeId)!));
      }
      return domainChoice.userVolumes;
    }
    return [];
  }, [domainChoice, editJob]);

  const tabs = useMemo<string[]>(() => {
    let baseTabs = ['Domains', 'Images', 'Data vols', 'User vols'];
    if (isJob) {
      baseTabs = [...baseTabs, 'Command', 'Working Directory'];
    }
    return baseTabs;
  }, [isJob]);

  useEffect(() => {
    if (editJob) {
      const editResultsFolderURI = editJob.resultsFolderURI
        .split('/')
        // compm adds subdirs to the results folder, this indicates last non-dynamic index
        .slice(0, Number.parseInt(process.env.NEXT_PUBLIC_JOB_URI_CONSTANT_TERMINUS || '0'))
        .join('/');

      setCommand!(editJob.command);
      setResultsFolderURI!(editResultsFolderURI);
    }
  }, [editJob]);

  const handleSubmit = () => {
    if (isJob && (!command || !command.length)) {
      setCommandError(true);

      return;
    }
    submit();
  };

  return <Styled>
    <div className="header">
      <div className="title">
        <IconButton onClick={() => router.push('/jobs')} >
          <CloseIcon />
        </IconButton>
        <h3>New {isJob ? 'Job' : 'Compute Session'}</h3>
        <Chip color="warning" label="BETA" />
      </div>
      <div className="submit-button" >
        <Button type="submit" onClick={handleSubmit} variant="contained">
          {loadingSubmit ? <CircularProgress color="secondary" /> : 'Submit'}
        </Button>
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
          tabs={tabs}
          command={command}
          setCommand={setCommand}
          commandError={commandError}
          setCommandError={setCommandError}
          resultsFolderURI={resultsFolderURI}
          setResultsFolderURI={setResultsFolderURI}
          isEditJob={!!editJob}
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
    {loadingData &&
      <LoadingAnimation backDropIsOpen={loadingData} />
    }
  </Styled>;
};