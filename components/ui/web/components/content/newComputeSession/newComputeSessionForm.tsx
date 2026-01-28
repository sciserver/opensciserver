import { FC } from 'react';
import styled from 'styled-components';
import {
  Avatar,
  IconButton,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  ListSubheader,
  TextField
} from '@mui/material';
import { MoreHoriz as MoreHorizIcon, StarBorder as StarBorderIcon } from '@mui/icons-material';

import { DataVolume, Domain, Image, UserVolume } from 'src/graphql/typings';

import logo from 'public/logo-sm.png';

const Styled = styled.div`
  width: 100%;

  .session-name {
    width: 100%;
    margin-bottom: 1.5rem;
  }
  .summary {
    max-height: 500px;
    overflow-y: auto;
  }
  .description {
    max-width: 200px;
    }
`;

type Props = {
  sessionName: string;
  setSessionName: (name: string) => void;
  domainChoice?: Domain;
  imageChoice?: Image;
  dataVolumesChoice: DataVolume[];
  userVolumesChoice: UserVolume[];
  submit: () => void;
  loadingSubmit: boolean;
  loadingData: boolean;
  isJob?: boolean;
  command?: string;
  setCommand?: (cmd: string) => void;
};

export const NewComputeSessionForm: FC<Props> = ({
  sessionName,
  setSessionName,
  domainChoice,
  imageChoice,
  dataVolumesChoice,
  userVolumesChoice
}) => {

  return <Styled>
    <TextField
      id="name-textfield"
      label="Session Name"
      variant="standard"
      value={sessionName}
      className="session-name"
      onChange={(e) => setSessionName(e.target.value)}
    />
    <List
      className="summary"
      sx={{
        '& ul': { padding: 0 }
      }}
      subheader={<li />}
    >
      <li key={`section-domain`}>
        <ul>
          <ListSubheader><h3>Domain</h3></ListSubheader>
          <ListItem
            secondaryAction={
              <>
                <IconButton aria-label="star">
                  <StarBorderIcon />
                </IconButton>
                <IconButton aria-label="more options">
                  <MoreHorizIcon />
                </IconButton>
              </>
            }
          >
            <ListItemAvatar>
              <Avatar alt="Sciserver Logo" src={logo.src} />
            </ListItemAvatar>
            <ListItemText
              primary={domainChoice ? domainChoice.name : 'No domain selected'}
              secondary={<span className="description">
                {domainChoice ? domainChoice.description || '' : ''}
              </span>}
            />

          </ListItem>
        </ul>
      </li>
      <li key={`section-image`}>
        <ul>
          <ListSubheader><h3>Image</h3></ListSubheader>
          <ListItem
            secondaryAction={
              <>
                <IconButton aria-label="star">
                  <StarBorderIcon />
                </IconButton>
                <IconButton aria-label="more options">
                  <MoreHorizIcon />
                </IconButton>
              </>
            }
          >
            <ListItemAvatar>
              <Avatar alt="Sciserver Logo" src={logo.src} />
            </ListItemAvatar>
            <ListItemText
              primary={imageChoice ? imageChoice.name : 'No image selected'}
              secondary={imageChoice ? imageChoice.description || '' : ''}
            />
          </ListItem>
        </ul>
      </li>
      <li key={`section-data-volumes`}>
        <ul>
          <ListSubheader><h3>Data Volumes</h3></ListSubheader>
          {dataVolumesChoice.length === 0 &&
            <ListItem>
              <ListItemText primary="No data volumes selected" />
            </ListItem>
          }
          {dataVolumesChoice.map(dv =>
            <ListItem
              secondaryAction={
                <>
                  <IconButton aria-label="star">
                    <StarBorderIcon />
                  </IconButton>
                  <IconButton aria-label="more options">
                    <MoreHorizIcon />
                  </IconButton>
                </>
              }
            >
              <ListItemAvatar>
                <Avatar alt="Sciserver Logo" src={logo.src} />
              </ListItemAvatar>
              <ListItemText
                primary={dv.name}
                secondary={dv.description || ''}
              />
            </ListItem>
          )}
        </ul>
      </li>
      <li key={`section-user-volumes`}>
        <ul>
          <ListSubheader><h3>User Volumes</h3></ListSubheader>
          {userVolumesChoice.map(uv =>
            <ListItem
              secondaryAction={
                <>
                  <IconButton aria-label="star">
                    <StarBorderIcon />
                  </IconButton>
                  <IconButton aria-label="more options">
                    <MoreHorizIcon />
                  </IconButton>
                </>
              }
            >
              <ListItemAvatar>
                <Avatar alt="Sciserver Logo" src={logo.src} />
              </ListItemAvatar>
              <ListItemText
                primary={uv.name}
                secondary={uv.description || ''}
              />
            </ListItem>
          )}
        </ul>
      </li>
    </List>
  </Styled >;
};