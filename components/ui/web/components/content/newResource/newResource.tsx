import { FC } from 'react';
import { useRouter } from 'next/router';
import styled from 'styled-components';
import {
  Button,
  Chip,
  CircularProgress,
  IconButton,
  TextField
} from '@mui/material';
import { Close as CloseIcon } from '@mui/icons-material';

import { DomainAccordionSummary } from 'components/content/newResource/domainAccordion';
import { ImageAccordionSummary } from 'components/content/newResource/imageAccordion';
import { DataVolAccordionSummary } from 'components/content/newResource/dataVolumeAccordion';
import { UserVolAccordionSummary } from 'components/content/newResource/userVolumeAccordion';
import { LoadingAnimation } from 'components/common/loadingAnimation';

import { DataVolume, Domain, Image, UserVolume } from 'src/graphql/typings';

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

  const router = useRouter();

  const navigateBack = () => {
    switch (sessionType) {
      case NewSessionType.JOB: {
        router.push('/jobs');
        break;
      }
      case NewSessionType.INTERACTIVE: {
        router.push('/compute');
        break;
      }
      default: {
        break;
      }
    }
  };

  return <Styled>
    <div className="header">
      <IconButton onClick={navigateBack} >
        <CloseIcon />
      </IconButton>
      <h1>New {sessionType === NewSessionType.JOB ? 'Job' : 'Compute Session'}</h1>
      <Chip color="warning" label="BETA" />
    </div>
    <div className="form">
      <TextField id="standard-basic" label={`${sessionType === NewSessionType.JOB ? 'Job' : 'Compute'} Name`} variant="standard" />
      <DomainAccordionSummary domainList={domainList} domainChoice={domainChoice} setDomainChoice={setDomainChoice} />
      <ImageAccordionSummary imageList={imageList} imageChoice={imageChoice} setImageChoice={setImageChoice} />
      <DataVolAccordionSummary dataVolumeList={dataVolumeList} dataVolumesChoice={dataVolumesChoice} setDataVolumesChoice={setDataVolumesChoice} />
      <UserVolAccordionSummary userVolumeList={userVolumeList} userVolumesChoice={userVolumesChoice} setUserVolumesChoice={setUserVolumesChoice} />
      <Button className="submit-button" type="submit" onClick={submit} variant="contained">
        {loadingSubmit ?
          <CircularProgress color="secondary" />
          :
          'Submit'
        }
      </Button>
    </div>
    {loadingData &&
      <LoadingAnimation backDropIsOpen={loadingData} />
    }
  </Styled >;
};