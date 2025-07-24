import { FC, useState } from 'react';
import styled from 'styled-components';
import { IconButton, Snackbar, Typography } from '@mui/material';
import {
  ArrowBackIos as ArrowBackIcon,
  ContentCopy as ContentCopyIcon,
  Close as CloseIcon
} from '@mui/icons-material';

import { Job } from 'src/graphql/typings';
import { CustomizedTabs } from 'components/common/tabs';
import { LoadingAnimation } from 'components/common/loadingAnimation';
import { useQuery } from '@apollo/client';
import { JOB_DETAIL_VIEW } from 'src/graphql/jobs';

type Props = {
  job: Job;
  back: () => void;
}

const Styled = styled.div`
  .header {
    display: flex;
    gap: 2rem;
    
    .job-field {
      display : flex;
      gap: 1rem;
      align-items: center;
    }
  }

  .command {
    width: 90%;
    
    pre {
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: #000;
      border: 1px solid #ddd;
      border-radius: 5px;
      color: #ddd;
      font-family: monospace;
      font-size: 12px;
      text-wrap: wrap;
      
      line-height: 1.6;
      padding: 1em 1.5em;
    }
  }
    
  .copy-icon {
    padding-left: 5rem;
  }

`;

const tabOptions = ['Summary', 'Output', 'Error'];
export const JobFullDetail: FC<Props> = ({ job, back }) => {

  const [error, setError] = useState<boolean>(false);
  const [tabValue, setTabValue] = useState<number>(1);
  const [copiedSnackbarOpen, setCopiedSnackbarOpen] = useState(false);


  const { loading, data } = useQuery(JOB_DETAIL_VIEW,
    {
      onError: () => setError(true),
      variables: {

      }
    }
  );


  return <Styled>
    <div className="header">
      <IconButton onClick={back} >
        <ArrowBackIcon />
      </IconButton>
      <div>
        <div className="job-field">
          <Typography variant="h5" gutterBottom component="div">
            Job ID:
          </Typography>
          <Typography variant="body1" gutterBottom component="div">
            {job.id}
          </Typography>
        </div>
        <div className="job-field">
          <Typography variant="h5" gutterBottom component="div">
            Image:
          </Typography>
          <Typography variant="body1" gutterBottom component="div">
            {job.dockerImageName}
          </Typography>
        </div>
      </div>
      <div>
        <div className="job-field">
          <Typography variant="h5" gutterBottom component="div">
            Started:
          </Typography>
          <Typography variant="body1" gutterBottom component="div">
            {new Date(job.startTime).toLocaleString()}
          </Typography>
        </div>
        <div className="job-field">
          <Typography variant="h5" gutterBottom component="div">
            Ended:
          </Typography>
          <Typography variant="body1" gutterBottom component="div">
            {new Date(job.endTime).toLocaleString()}
          </Typography>
        </div>
      </div>

    </div>
    {job.command &&
      <div className="command">
        <pre>
          {job.command}
          <IconButton
            className="copy-icon"
            size="small"
            aria-label="close"
            color="inherit"
            onClick={() => {
              navigator.clipboard.writeText(job.command);
              setCopiedSnackbarOpen(true);
            }}
          >
            <ContentCopyIcon fontSize="medium" />
          </IconButton>
        </pre>
        <Snackbar
          open={copiedSnackbarOpen}
          autoHideDuration={5000}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
          onClose={() => setCopiedSnackbarOpen(false)}
          message="Copied to clipboard!"
          action={<>
            <IconButton
              size="small"
              aria-label="close"
              color="inherit"
              onClick={() => setCopiedSnackbarOpen(false)}
            >
              <CloseIcon fontSize="small" />
            </IconButton>
          </>}
        />
      </div>
    }
    {loading &&
      <LoadingAnimation backDropIsOpen={loading} />
    }
    {error ?
      <>
        <h2>There was an error loading the details for this container.</h2>
        <p>{error}</p>
      </>
      :
      <>
        <CustomizedTabs tabs={tabOptions} value={tabValue} setValue={setTabValue} />
        <div>
          {tabValue === 0 ?
            <p>Summary Content</p>
            :
            <p>Output Content</p>
          }
        </div>
      </>
    }
  </Styled>;
};