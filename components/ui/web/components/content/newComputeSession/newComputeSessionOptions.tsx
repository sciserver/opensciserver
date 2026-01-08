import { FC, useState } from 'react';
import styled from 'styled-components';
import { Button, CircularProgress, Divider, TextField, InputAdornment, Accordion, AccordionSummary, AccordionDetails } from '@mui/material';
import { Search as SearchIcon, ExpandMore as ExpandMoreIcon } from '@mui/icons-material';

import { DataVolume, Domain, Image, UserVolume } from 'src/graphql/typings';
import { CustomizedTabs } from 'components/common/tabs';
import { DomainOptions } from './domainOptions';
import { ImageOptions } from './imageOptions';
import { DataVolumeOptions } from './dataVolumeOptions';
import { UserVolumeOptions } from './userVolumeOptions';

const Styled = styled.div`
  overflow-y: auto; 

  .options-divider {
    margin-left: -3rem;
    margin-bottom: 1rem;
  }

  .content-options {
    margin-right: 2rem;

    .search-bar-options {
      margin-bottom: 1.5rem;
    }
  }

  section {
    display: grid;
    grid-gap: 1rem;
    grid-auto-flow: column;
    overflow: auto;
    height: 7rem;
  }

  article {
    white-space: nowrap;
    margin: 10px 0;
  }
  
`;

type Props = {
  domainList: Domain[];
  domainChoice?: Domain;
  setDomainChoice: (domain: Domain) => void;
  imageList: Image[];
  imageChoice?: Image;
  setImageChoice: (image: Image) => void;
  dataVolumeList: DataVolume[];
  dataVolumesChoice: DataVolume[];
  setDataVolumesChoice: (dataVols: DataVolume[]) => void;
  userVolumeList: UserVolume[];
  userVolumesChoice: UserVolume[];
  setUserVolumesChoice: (userVols: UserVolume[]) => void;
};

export const NewComputeSessionOptions: FC<Props> = ({
  domainList,
  domainChoice,
  setDomainChoice,
  imageList,
  imageChoice,
  setImageChoice,
  dataVolumesChoice,
  setDataVolumesChoice,
  userVolumesChoice,
  setUserVolumesChoice,
  dataVolumeList,
  userVolumeList
}) => {

  const [tabValue, setTabValue] = useState(0);

  return <Styled>
    <CustomizedTabs tabs={['Domains', 'Images', 'Data vols', 'User vols']} value={tabValue} setValue={setTabValue} />
    <Divider className="options-divider" />
    <div className="content-options">
      {tabValue === 0 &&
        <DomainOptions
          domainList={domainList}
          domainChoice={domainChoice}
          setDomainChoice={setDomainChoice}
        />
      }
      {tabValue === 1 &&
        <ImageOptions
          imageList={imageList}
          imageChoice={imageChoice}
          setImageChoice={setImageChoice}
        />
      }
      {tabValue === 2 &&
        <DataVolumeOptions
          dataVolumeList={dataVolumeList}
          dataVolumesChoice={dataVolumesChoice}
          setDataVolumesChoice={setDataVolumesChoice}
        />
      }
      {tabValue === 3 &&
        <UserVolumeOptions
          userVolumeList={userVolumeList}
          userVolumesChoice={userVolumesChoice}
          setUserVolumesChoice={setUserVolumesChoice}
        />
      }
    </div>
  </Styled >;
};