import { FC, useMemo, useState } from 'react';
import { useRouter } from 'next/router';
import { useQuery, ApolloError, useMutation } from '@apollo/client';
import styled from 'styled-components';
import { Button, Chip, IconButton, Step, StepLabel, Stepper } from '@mui/material';
import { Close as CloseIcon } from '@mui/icons-material';
import Swal from 'sweetalert2';

import { DataVolume, Domain, Image, UserVolume } from 'src/graphql/typings';
import { GET_DOMAINS } from 'src/graphql/domains';
import { CREATE_JOB } from 'src/graphql/jobs';

import { NewResource, NewSessionType } from 'components/content/newResource/newResource';
import { CommandForm } from 'components/content/jobs/new/commandForm';

const Styled = styled.div`
  .header {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-bottom: 1.5rem;

    .alert {
      color: ${({ theme }) => theme.palette.warning.dark};
    }
  }

  .step-buttons {
    display: flex;
    justify-content: space-between;
    margin-top: 1rem;
  }
`;

const steps = ['Select Domain & Image', 'Set Command', 'Submit'];
export const NewJob: FC = () => {

  const router = useRouter();

  const [loadingSubmit, setLoadingSubmit] = useState<boolean>(false);
  const [activeStep, setActiveStep] = useState<number>(0);
  const [commandError, setCommandError] = useState<boolean>(false);

  const { loading: loadingData, data } = useQuery(GET_DOMAINS,
    {
      variables: { jobs: true },
      onError: (error: ApolloError) => {
        if (error.message.includes('Unauthorized')) {
          router.push('/login?callbackURL=/compute');
        }
      }
    }
  );

  const [createJob] = useMutation(CREATE_JOB, {
    onError: () => Swal.fire({
      title: 'Unable to add docker job',
      text: 'Please email <a href=\"mailto:sciserver-helpdesk@jhu.edu\">sciserver-helpdesk@jhu.edu</a> for more assistance.',
      icon: 'error',
      confirmButtonText: 'OK'
    }).then(() => {
      setLoadingSubmit(false);
      return;
    }).catch(Error),
    onCompleted: () => Swal.fire({
      title: 'Job created successfully',
      text: 'Your job has been created and is now queued.',
      icon: 'success',
      confirmButtonText: 'OK'
    }).then(() => {
      setLoadingSubmit(false);
      router.push('/jobs');
    })
  });

  const [jobName, setJobName] = useState<string>('');
  const [domainChoice, setDomainChoice] = useState<Domain>();
  const [imageChoice, setImageChoice] = useState<Image>();
  const [dataVolumesChoice, setDataVolumesChoice] = useState<DataVolume[]>([]);
  const [userVolumesChoice, setUserVolumesChoice] = useState<UserVolume[]>([]);
  const [useTemporaryVolume, setUseTemporaryVolume] = useState<boolean>(true);
  const [workingDirectoryUserVolumesChoice, setWorkingDirectoryUserVolumesChoice] = useState<UserVolume>();

  const [command, setCommand] = useState<string>('');

  const domainList = useMemo<Domain[]>(() => {
    if (data && data.getDomains) {
      setDomainChoice((data.getDomains as Domain[]).find(d => d.name === process.env.NEXT_PUBLIC_NEW_JOB_DOMAIN_NAME_DEFAULT));
      return data.getDomains;
    }
    return [];
  }, [data]);

  const imageList = useMemo<Image[]>(() => {
    if (domainChoice) {
      setImageChoice(domainChoice.images.find(d => d.name === process.env.NEXT_PUBLIC_NEW_JOB_IMAGE_NAME_DEFAULT));
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

  const temporaryWorkingDirPath = useMemo<string>(() => {
    return `/home/idies/workspace/Temporary/${userVolumeList.find(uv => uv.name === 'scratch')?.owner ?? ''}/jobs/`;
  }, [userVolumeList]);

  // eslint-disable-next-line unicorn/consistent-function-scoping
  const submit = () => {
    if (!command.length) {
      setCommandError(true);
      return;
    }

    if (!useTemporaryVolume && !workingDirectoryUserVolumesChoice) {
      return;
    }
    setLoadingSubmit(true);

    const resultsFolderURI = useTemporaryVolume ?
      temporaryWorkingDirPath :
      `/home/idies/workspace/${workingDirectoryUserVolumesChoice!.rootVolumeName}/${workingDirectoryUserVolumesChoice!.owner}/${workingDirectoryUserVolumesChoice!.name}/`;

    createJob({
      variables: {
        createJobParams: {
          dockerComputeEndpoint: domainChoice!.apiEndpoint,
          dockerImageName: imageChoice!.name,
          resultsFolderURI,
          submitterDID: jobName,
          scriptURI: '',
          volumeContainers: dataVolumesChoice.map(dv => dv.publisherDID),
          userVolumes: userVolumesChoice.map(uv => uv.id),
          command
        }
      }
    });
  };

  return <Styled>
    <div className="header">
      <IconButton onClick={() => router.push('/jobs')} >
        <CloseIcon />
      </IconButton>
      <h1>New Job</h1>
      <Chip color="warning" label="BETA" />
    </div>
    <Stepper activeStep={activeStep} alternativeLabel>
      {steps.map((label) => (
        <Step key={label}>
          <StepLabel>{label}</StepLabel>
        </Step>
      ))}
    </Stepper>
    {activeStep === 0 &&
      <NewResource
        sessionType={NewSessionType.JOB}
        resourceName={jobName}
        setResourceName={setJobName}
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
        submit={() => setActiveStep(1)}
        loadingSubmit={loadingSubmit}
        loadingData={loadingData}
      />
    }
    {activeStep === 1 &&
      <div>
        <CommandForm
          command={command}
          setCommand={setCommand}
          commandError={commandError}
          setCommandError={setCommandError}
          useTemporaryVolume={useTemporaryVolume}
          setUseTemporaryVolume={setUseTemporaryVolume}
          temporaryWorkingDirPath={temporaryWorkingDirPath}
          workingDirectoryUserVolumesChoice={workingDirectoryUserVolumesChoice}
          setWorkingDirectoryUserVolumesChoice={setWorkingDirectoryUserVolumesChoice}
          userVolumesChoice={userVolumesChoice}
          setActiveStep={setActiveStep}
          submit={submit}
        />
        <div className="step-buttons">
          <Button className="submit-button" onClick={() => setActiveStep(0)} variant="contained">Previous</Button>
          <Button className="submit-button" type="submit" onClick={submit} variant="contained">Submit</Button>
        </div>
      </div>
    }
  </Styled>;
};