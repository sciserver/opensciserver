import { FC } from 'react';
import styled from 'styled-components';
import {
  Button,
  IconButton,
  Drawer
} from '@mui/material';
import { Close as CloseIcon } from '@mui/icons-material';
import { Job } from 'src/graphql/typings';
import { useRouter } from 'next/router';

import { CommandBox } from 'components/common/commandBox';

const StyledDrawer = styled(Drawer)`
  .MuiDrawer-paper {
    width: 100%;
    max-height: 50%;
    overflow-y: auto;
    padding: 1rem;
  }

  .job-details-container {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 2rem;
    }

  .job-details {
    display: flex;
    gap: 3rem;

    .job-field {
      display : flex;
      flex-direction: column;
      gap: 0.5rem;
      align-items: center;

      p, h3 {
        margin: 0.2rem;
      }
    }
  }

  .actions {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
  }
`;

type Props = {
  job: Job;
  isOpen: boolean;
  setOpenRow: (jobId: Job | null) => void;
}

export const JobShortDetail: FC<Props> = ({ job, isOpen, setOpenRow }) => {

  const router = useRouter();

  return <StyledDrawer
    anchor="bottom"
    open={isOpen}
    onClose={() => setOpenRow(null)}
  >
    <div className="job-details-container">
      <div className="job-details">
        <div className="job-field">
          <h3>
            Job ID:
          </h3>
          <p>
            {job.id}
          </p>
        </div>
        <div className="job-field">
          <h3>
            Image:
          </h3>
          <p>
            {job.dockerImageName}
          </p>
        </div>
        <div className="job-field">
          <h3>
            Started:
          </h3>
          <p>
            {job.startTime ? new Date(job.startTime).toLocaleString() : 'N/A'}
          </p>
        </div>
        <div className="job-field">
          <h3>
            Ended:
          </h3>
          <p>
            {job.endTime ? new Date(job.endTime).toLocaleString() : 'N/A'}
          </p>
        </div>
      </div>
      <div className="actions">
        <IconButton
          className="close-icon"
          size="small"
          aria-label="close"
          color="inherit"
          onClick={() => setOpenRow(null)}
        >
          <CloseIcon fontSize="medium" />
        </IconButton>
        <Button onClick={() => router.push(`/jobs/${job.id}`)} variant="contained" color="primary">
          See full details
        </Button>
      </div>
    </div>
    {job.command &&
      <CommandBox command={job.command} maxHeight="30vh" />
    }
  </StyledDrawer>;
};
