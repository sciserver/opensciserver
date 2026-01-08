import { FC } from 'react';
import styled from 'styled-components';
import { Button, CircularProgress, TextField } from '@mui/material';

import { LoadingAnimation } from 'components/common/loadingAnimation';

import { DataVolume, Domain, Image, UserVolume } from 'src/graphql/typings';
import { OptionCard } from 'components/common/optionCard';
import { CommandForm } from '../jobs/new/commandForm';

const Styled = styled.div`
  width: 100%;

  .session-name {
    width: 100%;
    margin-bottom: 1.5rem;
  }

  .summary {
    display: flex;
    flex-wrap: wrap;
    gap: 2rem;
  }

  .submit-button {
    display: flex;
    justify-content: center;
    margin: 2rem 0.1rem;
  }
`;

type Props = {
  sessionName: string;
  setSessionName: (name: string) => void;
  domainChoice?: Domain;
  imageChoice?: Image;
  dataVolumesChoice: DataVolume[];
  userVolumesChoice: UserVolume[];
  submit: () => void;
  loadingSubmit: boolean;
  loadingData: boolean;
  isJob?: boolean;
  command?: string;
  setCommand?: (cmd: string) => void;
};

export const NewComputeSessionForm: FC<Props> = ({
  sessionName,
  setSessionName,
  domainChoice,
  imageChoice,
  dataVolumesChoice,
  userVolumesChoice,
  submit,
  loadingSubmit,
  loadingData,
  isJob = false,
  command,
  setCommand
}) => {

  return <Styled>
    <TextField
      id="name-textfield"
      label="Session Name"
      variant="standard"
      value={sessionName}
      className="session-name"
      onChange={(e) => setSessionName(e.target.value)}
    />
    <div className="summary">
      <div>
        <h3>Domain</h3>
        <OptionCard
          title={domainChoice ? domainChoice.name : 'No domain selected'}
          description={domainChoice ? domainChoice.name : ''}
          action={() => { }}
          width={250}
        />
      </div>
      <div>
        <h3>Image</h3>
        <OptionCard
          title={imageChoice ? imageChoice.name : 'No image selected'}
          description={imageChoice ? imageChoice.name : ''}
          action={() => { }}
          width={250}
        />
      </div>
    </div>
    <h3>Data Volumes</h3>
    {dataVolumesChoice.length === 0 && <p>No data volumes selected</p>}
    <div className="summary">
      {dataVolumesChoice.map(dv =>
        <OptionCard
          title={dv.name}
          description={dv.description || ''}
          action={() => { }}
          width={200}
        />
      )}
    </div>
    <h3>User Volumes</h3>
    <div className="summary">
      {userVolumesChoice.map(uv =>
        <OptionCard
          title={`${uv.name} (${uv.owner})`}
          description={uv.description || ''}
          action={() => { }}
          width={200}
        />
      )}
    </div>
    <div className="submit-button" >
      <Button type="submit" onClick={submit} variant="contained">
        {loadingSubmit ? <CircularProgress color="secondary" /> : 'Submit'}
      </Button>
    </div>
    {loadingData &&
      <LoadingAnimation backDropIsOpen={loadingData} />
    }
  </Styled >;
};