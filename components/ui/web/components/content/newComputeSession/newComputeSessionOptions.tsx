import { FC, useState } from 'react';
import styled from 'styled-components';
import { Button, CircularProgress, Divider, TextField, InputAdornment, Accordion, AccordionSummary, AccordionDetails } from '@mui/material';
import { Search as SearchIcon, ExpandMore as ExpandMoreIcon } from '@mui/icons-material';

import { LoadingAnimation } from 'components/common/loadingAnimation';

import { DataVolume, Domain, Image, UserVolume } from 'src/graphql/typings';
import { CustomizedTabs } from 'components/common/tabs';

const Styled = styled.div`
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
  
`;

type Props = {
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

export const NewComputeSessionOptions: FC<Props> = ({
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

  const [tabValue, setTabValue] = useState(0);
  return <Styled>
    <CustomizedTabs tabs={['Domains', 'Images', 'Data vols', 'User vols']} value={tabValue} setValue={setTabValue} />
    <Divider className="options-divider" />
    <div className="content-options">
      <TextField
        fullWidth
        className="search-bar-options"
        placeholder="Search by name"
        id="search-bar-new-session-options"
        size="small"
        InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon /></InputAdornment> }}
      />
      {tabValue === 0 &&
        domainList.map(domain =>
          <Accordion>
            <AccordionSummary
              expandIcon={<ExpandMoreIcon />}
              aria-controls="panel1-content"
              id="panel1-header"
            >
              {domain.name}
            </AccordionSummary>
            <AccordionDetails>
              {domain.description}
            </AccordionDetails>
          </Accordion>
        )
      }
    </div>
  </Styled >;
};