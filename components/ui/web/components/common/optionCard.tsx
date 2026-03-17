import { FC } from 'react';
import styled from 'styled-components';
import { Card, CardActionArea, CardContent } from '@mui/material';


type Props = {
  title: string;
  description: string;
  action: () => void;
  selected?: boolean;
  width?: number;
};

const StyledCard = styled(Card) <{ width: number }>`
  display: flex;
  width: ${props => props.width}px;
  &:hover {
    cursor: pointer;
    box-shadow: rgba(0, 0, 0, 0.2) 0px 5px 5px 0px, rgba(0, 0, 0, 0.14) 0px 8px 10px 0px, rgba(0, 0, 0, 0.12) 0px 3px 14px 0px;
  }

  &.selected {
      box-shadow: inset 0 0 0.5rem  ${({ theme }) => theme.palette.secondary.light};
  }

`;

export const OptionCard: FC<Props> = ({ title, description, action, width = 200, selected = false }) => {
  return (
    <StyledCard {...{ width }} className={selected ? 'selected' : ''}>
      <CardActionArea
        onClick={action}
      >
        <CardContent >
          <h3>{title}</h3>
          <span className="caption">{description}</span>
        </CardContent>
      </CardActionArea>
    </StyledCard>
  );
}; 
