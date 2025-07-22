import { FC, useState } from 'react';
import { AccordionDetails } from '@mui/material';
import styled from 'styled-components';

import { ParamAccordionSummary } from 'components/content/compute/newSession/paramAccordionSummary';
import { StyledAccordion } from 'components/common/accordion';
import { InfoCard } from 'components/common/infoCard';
import { Image } from 'src/graphql/typings';

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
  imageList: Image[],
  imageChoice?: Image,
  setImageChoice: (image: Image) => void
};

export const ImageAccordionSummary: FC<Props> = ({ imageList, imageChoice, setImageChoice }) => {

  const [open, setOpen] = useState<boolean>(false);

  const handleOnClickOption = (img: Image) => {
    setImageChoice(img);
    setOpen(false);
  };

  return <StyledAccordion open={open} setOpen={setOpen}>
    <ParamAccordionSummary
      open={open}
      title="Image"
      choice={imageChoice ? { name: imageChoice.name, subtitle: imageChoice.description || 'Description for chosen image' } : undefined}
    />
    <StyledAccordionDetails>
      {imageList.map(img =>
        <InfoCard
          selected={imageChoice?.id === img.id}
          title={img.name}
          subtitle={img.description}
          action={() => handleOnClickOption(img)}
        />
      )}
    </StyledAccordionDetails>
  </StyledAccordion>;
};