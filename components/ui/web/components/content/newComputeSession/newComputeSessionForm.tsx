import { FC } from 'react';
import styled from 'styled-components';
import { Button, CircularProgress, TextField } from '@mui/material';

import { Choice, SingleChoiceAccordionSummary } from 'components/content/newComputeSession/singleChoiceAccordion';
import { DataVolAccordionSummary } from 'components/content/newComputeSession/dataVolumeAccordion';
import { UserVolAccordionSummary } from 'components/content/newComputeSession/userVolumeAccordion';
import { LoadingAnimation } from 'components/common/loadingAnimation';

import { DataVolume, Domain, Image, UserVolume } from 'src/graphql/typings';
import { InfoCard } from 'components/common/infoCard';

const Styled = styled.div`
  
`;

type Props = {
  resourceName: string;
  setResourceName: (name: string) => void;
  domainChoice?: Domain;
  imageChoice?: Image;
  dataVolumesChoice: DataVolume[];
  userVolumesChoice: UserVolume[];
  submit: () => void;
  loadingSubmit: boolean;
  loadingData: boolean;
};

export const NewComputeSessionForm: FC<Props> = ({
  resourceName,
  setResourceName,
  domainChoice,
  imageChoice,
  dataVolumesChoice,
  userVolumesChoice,
  submit,
  loadingSubmit,
  loadingData
}) => {

  return <Styled>
    <div className="form">
      <TextField
        id="name-textfield"
        label="Session Name"
        variant="standard"
        value={resourceName}
        onChange={(e) => setResourceName(e.target.value)}
      />
      <h3>Domain</h3>
      <InfoCard
        title={domainChoice ? domainChoice.name : 'No domain selected'}
        subtitle={domainChoice ? domainChoice.name : ''}
        action={() => { }}
        width={200}
      />
      <h3>Image</h3>
      <InfoCard
        title={imageChoice ? imageChoice.name : 'No image selected'}
        subtitle={imageChoice ? imageChoice.name : ''}
        action={() => { }}
        width={200}
      />
      <h3>Data Volumes</h3>
      {dataVolumesChoice.map(dv =>
        <InfoCard
          title={dv.name}
          subtitle={dv.description || ''}
          action={() => { }}
          width={200}
        />
      )}
      <h3>User Volumes</h3>
      {userVolumesChoice.map(uv =>
        <InfoCard
          title={uv.name}
          subtitle={uv.description || ''}
          action={() => { }}
          width={200}
        />
      )}
      <Button className="submit-button" type="submit" onClick={submit} variant="contained">
        {loadingSubmit ? <CircularProgress color="secondary" /> : 'Submit'}
      </Button>
    </div>
    {loadingData &&
      <LoadingAnimation backDropIsOpen={loadingData} />
    }
  </Styled >;
};