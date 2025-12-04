import { FC, useState } from 'react';
import { AccordionDetails } from '@mui/material';
import styled from 'styled-components';
import { remove } from 'lodash';

import { ParamAccordionSummary } from 'components/content/newResource/paramAccordionSummary';
import { StyledAccordion } from 'components/common/accordion';
import { InfoCard } from 'components/common/infoCard';
import { UserVolume } from 'src/graphql/typings';

const StyledAccordionDetails = styled(AccordionDetails)`
    height: auto; 
    display: flex;
    flex-wrap: wrap;
    gap: 1rem;

    .selected {
      box-shadow: inset 0 0 10px  ${({ theme }) => theme.palette.secondary.light};
    }
`;


type Props = {
  userVolumeList: UserVolume[],
  userVolumesChoice: UserVolume[],
  setUserVolumesChoice: (userVols: UserVolume[]) => void
};

export const UserVolAccordionSummary: FC<Props> = ({ userVolumeList, userVolumesChoice, setUserVolumesChoice }) => {

  const [open, setOpen] = useState<boolean>(false);

  const handleOnClickOption = (uv: UserVolume) => {
    const uvs = userVolumesChoice;
    const tempUv = uvs.find(i => i.id === uv.id);

    if (tempUv) {
      remove(uvs, (i) => i.id === uv.id);
      setUserVolumesChoice([...uvs]);
      return;
    }
    uvs.push(uv);
    setUserVolumesChoice([...uvs]);
  };

  return <StyledAccordion open={open} setOpen={setOpen}>
    <ParamAccordionSummary
      open={open}
      title="User Volumes"
      choice={userVolumesChoice[0] ? { name: `${userVolumesChoice[0].name} (${userVolumesChoice[0].owner})`, subtitle: userVolumesChoice[0].description || 'No description available' } : undefined}
      extraText={userVolumesChoice.length > 1 ? `${userVolumesChoice.length - 1}+ chosen` : ''}
    />
    <StyledAccordionDetails>
      {userVolumeList.map(uv =>
        <InfoCard selected={userVolumesChoice.some(uvc => uvc.id === uv.id)} title={`${uv.name} (${uv.owner})`} subtitle={uv.description || ''} action={() => handleOnClickOption(uv)} />
      )}
    </StyledAccordionDetails>
  </StyledAccordion>;
};