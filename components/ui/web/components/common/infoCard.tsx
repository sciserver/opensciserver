import { FC } from 'react';
import styled from 'styled-components';
import { Card, CardHeader, CardMedia } from '@mui/material';


type Props = {
  title: string;
  subtitle: string;
  action: () => void;
  imageSource?: string;
  width?: number;
  selected?: boolean;
};

const StyledCard = styled(Card) <{ width: number }>`
  display: flex;
  width: ${props => props.width}px;

  &:hover {
    cursor: pointer;
    box-shadow: rgba(0, 0, 0, 0.2) 0px 5px 5px 0px, rgba(0, 0, 0, 0.14) 0px 8px 10px 0px, rgba(0, 0, 0, 0.12) 0px 3px 14px 0px;
  }

`;

export const InfoCard: FC<Props> = ({ title, subtitle, action, imageSource, width = 350, selected = false }) => {
  return (
    <StyledCard className={selected ? 'selected' : ''} {...{ width }} onClick={action}>
      {imageSource &&
        <CardMedia
          component="img"
          sx={{ width: 100 }}
          image={imageSource}
          alt="Image"
        />
      }
      <CardHeader
        title={title}
        subheader={subtitle}
      />
    </StyledCard>
  );
}; 
