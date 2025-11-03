import { FC, useState } from 'react';
import { AccordionDetails } from '@mui/material';
import styled from 'styled-components';

import { ParamAccordionSummary } from 'components/content/newResource/paramAccordionSummary';
import { UserVolume } from 'src/graphql/typings';
import { StyledAccordion } from 'components/common/accordion';
import { InfoCard } from 'components/common/infoCard';

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
  userVolumeChoice?: UserVolume,
  setUserVolumeChoice: (userVolume: UserVolume) => void
};

export const WorkingDirectoryAccordionSummary: FC<Props> = ({ userVolumeList, userVolumeChoice, setUserVolumeChoice }) => {

  const [open, setOpen] = useState<boolean>(false);

  const handleOnClickOption = (uv: UserVolume) => {
    setUserVolumeChoice(uv);
    setOpen(false);
  };

  return <StyledAccordion open={open} setOpen={setOpen}>
    <ParamAccordionSummary
      open={open}
      title="Working Directory"
      choice={userVolumeChoice ? { name: `${userVolumeChoice.name} (${userVolumeChoice.owner})`, subtitle: userVolumeChoice.description || 'No description available' } : undefined}
    />
    <StyledAccordionDetails>
      {userVolumeList.map(uv =>
        <InfoCard
          key={uv.id}
          selected={userVolumeChoice && userVolumeChoice.id === uv.id}
          title={`${uv.name} (${uv.owner})`}
          subtitle={uv.description || 'No description available'}
          action={() => handleOnClickOption(uv)}
        />
      )}
    </StyledAccordionDetails>
  </StyledAccordion>;
};