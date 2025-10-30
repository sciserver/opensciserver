import { FC, useMemo, useState } from 'react';
import { useRouter } from 'next/router';
import { useQuery, ApolloError } from '@apollo/client';
import styled from 'styled-components';
import { Button, Checkbox, Chip, IconButton, Step, StepLabel, Stepper, TextField } from '@mui/material';
import { Close as CloseIcon } from '@mui/icons-material';

import { DataVolume, Domain, Image, UserVolume } from 'src/graphql/typings';
import { GET_DOMAINS } from 'src/graphql/domains';
import { NewResource, NewSessionType } from 'components/content/newResource/newResource';
import { WorkingDirectoryAccordionSummary } from '../newResource/workingDirectoryAccordion';

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

  .command {
    display: flex;
    flex-direction: column;
    gap: 1rem;
    margin: 2rem 0.5rem;

    p {
      margin: 0;

      .path {
        font-family: monospace;
        color: ${({ theme }) => theme.palette.error.light};
      }
    }

    .bullet {
      margin-left: 1.5rem;
    }

    .step-buttons {
      display: flex;
      justify-content: space-between;
      margin-top: 1rem;
    }

    .checkbox-container {
      display: flex;
      align-items: center;
      gap: 0.2rem;

    }
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

  const [domainChoice, setDomainChoice] = useState<Domain>();
  const [imageChoice, setImageChoice] = useState<Image>();
  const [dataVolumesChoice, setDataVolumesChoice] = useState<DataVolume[]>([]);
  const [userVolumesChoice, setUserVolumesChoice] = useState<UserVolume[]>([]);
  const [useTemporaryVolume, setUseTemporaryVolume] = useState<boolean>(true);
  const [workingDirectoryUserVolumesChoice, setWorkingDirectoryUserVolumesChoice] = useState<UserVolume>();

  const [command, setCommand] = useState<string>('');

  const domainList = useMemo<Domain[]>(() => {
    if (data && data.getDomains) {
      setDomainChoice((data.getDomains as Domain[]).find(d => d.name == process.env.NEXT_PUBLIC_NEW_JOB_DOMAIN_NAME_DEFAULT));
      return data.getDomains;
    }
    return [];
  }, [data]);

  const imageList = useMemo<Image[]>(() => {
    if (domainChoice) {
      setImageChoice(domainChoice.images.find(d => d.name == process.env.NEXT_PUBLIC_NEW_JOB_IMAGE_NAME_DEFAULT));
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
      const defaultUVs = (domainChoice.userVolumes as UserVolume[]).filter(uv => uv.name == 'scratch' || uv.name == 'persistent');
      setUserVolumesChoice(defaultUVs);
      return domainChoice.userVolumes;
    }
    return [];
  }, [domainChoice]);

  // eslint-disable-next-line unicorn/consistent-function-scoping
  const submit = () => {
    if (!command.length) {
      setCommandError(true);
      return;
    }
  };

  return <Styled>
    <div className="header">
      <IconButton onClick={() => router.push('/compute')} >
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
      <div className="command">
        <TextField
          id="command-multiline"
          label="Command"
          multiline
          rows={10}
          value={command}
          onChange={(e) => {
            setCommand(e.target.value);
            setCommandError(false);
          }}
          error={commandError}
          helperText={commandError ? 'Command cannot be empty' : ''}
        />
        <h4>Working Directory</h4>
        <p>
          Select a location to store standard input/output logs, which will also serve as the current working directory for this job.
          To use other writable user volumes, enable them in the Files tab. <strong>Do not use relative paths in the command.</strong>
        </p>

        <div className="checkbox-container">
          <Checkbox checked={useTemporaryVolume} onChange={() => setUseTemporaryVolume(!useTemporaryVolume)} />
          <span className="caption">
            Create and use a new folder in the “jobs” temporary volume. The folder will be created automatically.
          </span>
        </div>
        {useTemporaryVolume ?
          <div>
            <p className="bullet">
              • A copy of this command will be placed in a unique, nested subfolder of <i className="path">/home/idies/workspace/Temporary/jjaime/jobs/</i>.
            </p>
            <p className="bullet">
              • Relative paths will be resolved from this location.
            </p>
          </div>
          :
          <WorkingDirectoryAccordionSummary userVolumeList={userVolumesChoice} userVolumeChoice={workingDirectoryUserVolumesChoice} setUserVolumeChoice={setWorkingDirectoryUserVolumesChoice} />
        }
        <div className="step-buttons">
          <Button className="submit-button" onClick={() => setActiveStep(0)} variant="contained">Previous</Button>
          <Button className="submit-button" type="submit" onClick={submit} variant="contained">Submit</Button>
        </div>
      </div>
    }
  </Styled>;
};