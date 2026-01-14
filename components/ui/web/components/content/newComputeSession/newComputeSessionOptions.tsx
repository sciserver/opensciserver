import { FC, useState } from 'react';
import styled from 'styled-components';
import { Divider } from '@mui/material';

import { DataVolume, Domain, Image, UserVolume } from 'src/graphql/typings';
import { CustomizedTabs } from 'components/common/tabs';
import { DomainOptions } from './domainOptions';
import { ImageOptions } from './imageOptions';
import { DataVolumeOptions } from './dataVolumeOptions';
import { UserVolumeOptions } from './userVolumeOptions';
import { CommandForm } from '../jobs/new/commandForm';
import { WorkingDirectoryForm } from '../jobs/new/workingDirectoryForm';

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

  .MuiAccordion-root{
    box-shadow: none;
  }
  
  .option-items {
    display: flex;
    flex-wrap: wrap;
    gap: 2rem;
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
  tabs: string[];
  command?: string;
  setCommand?: (command: string) => void;
  commandError?: boolean;
  setCommandError?: (error: boolean) => void;
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
  userVolumeList,
  command,
  setCommand,
  commandError,
  setCommandError,
  tabs
}) => {

  const [tabValue, setTabValue] = useState(0);

  return <Styled>
    <CustomizedTabs tabs={tabs} value={tabValue} setValue={setTabValue} />
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
      {/* Job Specific Components */}
      {tabValue === 4 &&
        <CommandForm command={command} setCommand={setCommand} commandError={commandError} setCommandError={setCommandError} />
      }
      {tabValue === 5 &&
        <>working directory</>
        // <WorkingDirectoryForm />
      }
    </div>
  </Styled >;
};