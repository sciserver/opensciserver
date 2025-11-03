import { FC } from 'react';
import styled from 'styled-components';
import { Button, CircularProgress, TextField } from '@mui/material';

import { Choice, SingleChoiceAccordionSummary } from 'components/content/newResource/singleChoiceAccordion';
import { DataVolAccordionSummary } from 'components/content/newResource/dataVolumeAccordion';
import { UserVolAccordionSummary } from 'components/content/newResource/userVolumeAccordion';
import { LoadingAnimation } from 'components/common/loadingAnimation';

import { DataVolume, Domain, Image, UserVolume } from 'src/graphql/typings';

const Styled = styled.div`
  
  .form {
    display: flex;
    flex-direction: column;
    gap: 2rem;
    margin: 2rem 0.5rem;
    width: 90%;
    
    .accordion-header {
      display: flex;
      align-items: center;
      gap: 2rem;
      
      .accordion-title { 
        display: flex;
        width: 120px;
        align-items: center;

        .title {
          text-transform: capitalize;
        }
      }
    }

    .submit-button {
      margin: 2rem 0.1rem;
    }
  }
`;

export enum NewSessionType {
  JOB = 'JOB',
  INTERACTIVE = 'INTERACTIVE'
}
type Props = {
  sessionType: NewSessionType;
  resourceName: string;
  setResourceName: (name: string) => void;
  domainList: Domain[];
  domainChoice?: Domain;
  setDomainChoice: (domain: Domain) => void;
  imageChoice?: Image;
  setImageChoice: (image: Image) => void;
  dataVolumesChoice: DataVolume[];
  setDataVolumesChoice: (dataVols: DataVolume[]) => void;
  userVolumesChoice: UserVolume[];
  setUserVolumesChoice: (userVols: UserVolume[]) => void;
  imageList: Image[];
  dataVolumeList: DataVolume[];
  userVolumeList: UserVolume[];
  submit: () => void;
  loadingSubmit: boolean;
  loadingData: boolean;
};

export const NewResource: FC<Props> = ({
  sessionType,
  resourceName,
  setResourceName,
  domainList,
  domainChoice,
  setDomainChoice,
  imageChoice,
  setImageChoice,
  dataVolumesChoice,
  setDataVolumesChoice,
  userVolumesChoice,
  setUserVolumesChoice,
  imageList,
  dataVolumeList,
  userVolumeList,
  submit,
  loadingSubmit,
  loadingData
}) => {

  return <Styled>
    <div className="form">
      <TextField
        id="name-textfield"
        label={`${sessionType === NewSessionType.JOB ? 'Job' : 'Compute'} Name`}
        variant="standard"
        value={resourceName}
        onChange={(e) => setResourceName(e.target.value)}
      />
      <SingleChoiceAccordionSummary
        title="Domain"
        choiceList={domainList}
        choice={domainChoice}
        setChoice={(domain: Choice) => setDomainChoice(domain as Choice & Domain)}
      />
      <SingleChoiceAccordionSummary
        title="Image"
        choiceList={imageList}
        choice={imageChoice}
        setChoice={(image: Choice) => setImageChoice(image as Choice & Image)}
      />
      <DataVolAccordionSummary
        dataVolumeList={dataVolumeList}
        dataVolumesChoice={dataVolumesChoice}
        setDataVolumesChoice={setDataVolumesChoice}
      />
      <UserVolAccordionSummary
        userVolumeList={userVolumeList}
        userVolumesChoice={userVolumesChoice}
        setUserVolumesChoice={setUserVolumesChoice}
      />
      <Button className="submit-button" type="submit" onClick={submit} variant="contained">
        {loadingSubmit ? <CircularProgress color="secondary" /> : sessionType === NewSessionType.JOB ? 'Next' : 'Submit'}
      </Button>
    </div>
    {loadingData &&
      <LoadingAnimation backDropIsOpen={loadingData} />
    }
  </Styled >;
};