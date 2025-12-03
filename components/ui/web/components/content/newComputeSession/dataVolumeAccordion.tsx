import { FC, useState } from 'react';
import { AccordionDetails } from '@mui/material';
import styled from 'styled-components';
import { remove } from 'lodash';

import { ParamAccordionSummary } from 'components/content/newComputeSession/paramAccordionSummary';
import { StyledAccordion } from 'components/common/accordion';
import { InfoCard } from 'components/common/infoCard';
import { DataVolume } from 'src/graphql/typings';

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
  dataVolumeList: DataVolume[],
  dataVolumesChoice: DataVolume[],
  setDataVolumesChoice: (dataVols: DataVolume[]) => void
};

export const DataVolAccordionSummary: FC<Props> = ({ dataVolumeList, dataVolumesChoice, setDataVolumesChoice }) => {

  const [open, setOpen] = useState<boolean>(false);

  const handleOnClickOption = (dv: DataVolume) => {
    const dvs = dataVolumesChoice;
    const tempDv = dvs.find(i => i.name === dv.name);

    if (tempDv) {
      remove(dvs, (i) => i.id === tempDv.id);
      setDataVolumesChoice([...dvs]);
      return;
    }

    dvs.push(dv);
    setDataVolumesChoice([...dvs]);

  };

  return <StyledAccordion open={open} setOpen={setOpen}>
    <ParamAccordionSummary
      open={open}
      title="Data Volumes"
      choice={dataVolumesChoice[0] ? { name: dataVolumesChoice[0].name, subtitle: dataVolumesChoice[0].description || 'Description for chosen dv' } : undefined}
      extraText={dataVolumesChoice.length > 1 ? `${dataVolumesChoice.length - 1}+ chosen` : ''}
    />
    <StyledAccordionDetails>
      {dataVolumeList.map(dv =>
        <InfoCard selected={dataVolumesChoice.some(dvc => dvc.id === dv.id)} title={dv.name} subtitle={dv.description} action={() => handleOnClickOption(dv)} />
      )}
    </StyledAccordionDetails>
  </StyledAccordion>;
};