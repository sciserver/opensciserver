import { FC } from 'react';
import styled from 'styled-components';
import { Avatar, Card, CardActionArea, CardContent, CardHeader, CardMedia, IconButton } from '@mui/material';
import { MoreVert as MoreVertIcon, StarBorder as StarBorderIcon } from '@mui/icons-material';

type Props = {
  title: string;
  description: string;
  action: () => void;
  selected?: boolean;
  width?: number;
  imageSource?: string;
};

const StyledCard = styled(Card) <{ width: number }>`
  width: ${props => props.width}px;
  &:hover {
    cursor: pointer;
    box-shadow: rgba(0, 0, 0, 0.2) 0px 5px 5px 0px, rgba(0, 0, 0, 0.14) 0px 8px 10px 0px, rgba(0, 0, 0, 0.12) 0px 3px 14px 0px;
  }

  &.selected {
      box-shadow: inset 0 0 0.5rem  ${({ theme }) => theme.palette.secondary.light};
  }
    
  .media {
    @media only screen and (min-width: 600px) {
      height: 140px; 
      width: 550px;
    }
      @media only screen and (max-width: 600px) {
        height: 140px; 
        width: 250;
      }
  }
`;

export const SelectedOptionCard: FC<Props> = ({ title, description, action, imageSource, width = 200, selected = false }) => {
  return (
    <StyledCard {...{ width }} className={selected ? 'selected' : ''}>
      {imageSource &&
        <CardMedia
          className="media"
          image={imageSource}
        />
      }
      <CardHeader
        action={
          <>
            <IconButton><StarBorderIcon /></IconButton>
            <IconButton><MoreVertIcon /></IconButton>
          </>
        }
      />
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
