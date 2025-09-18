import { FC, useMemo, useState } from 'react';
import { useRouter } from 'next/router';
import { useQuery } from '@apollo/client';
import styled from 'styled-components';

import { IconButton, Snackbar, Typography } from '@mui/material';
import {
  ArrowBackIos as ArrowBackIcon,
  ContentCopy as ContentCopyIcon,
  Close as CloseIcon,
  Download as DownloadIcon
} from '@mui/icons-material';
import { DataGrid, GridActionsCellItem, GridColDef } from '@mui/x-data-grid';

import { sanitize } from 'dompurify';
import ReactMarkdown from 'react-markdown';
import rehypeRaw from 'rehype-raw';
import { filesize } from 'filesize';

import { File, JobDetails } from 'src/graphql/typings';
import { JOB_DETAIL_VIEW } from 'src/graphql/jobs';

import { CustomizedTabs } from 'components/common/tabs';
import { LoadingAnimation } from 'components/common/loadingAnimation';

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
    width: 80%;
    
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

  .html-description{
    padding: 5px 50px 40px 20px;
    width: 90%;
    
    code {
      text-wrap: wrap;
    }
  }

   .grid {
    width: 77%;
    margin: 30px 20px;

     .MuiDataGrid-columnHeader {
      font-style: normal;
      font-size: 14px;
      letter-spacing: 0.25px;
      font-weight: 600;
      text-transform: capitalize;
      .MuiCheckbox-root {
        height: 100%;        
        padding: 15px;
      }
    }

    .MuiDataGrid-cell {
        padding: 12px 25px;
        font-weight: 500;
        border-top: 1px solid #E0E0E0;
    }
  }

`;

const tabOptions = ['Summary', 'Files'];
export const JobFullDetail: FC = () => {

  const router = useRouter();
  const { id } = router.query;

  const [error, setError] = useState<boolean>(false);
  const [tabValue, setTabValue] = useState<number>(0);
  const [copiedSnackbarOpen, setCopiedSnackbarOpen] = useState(false);

  const { loading, data } = useQuery(JOB_DETAIL_VIEW,
    {
      onError: () => setError(true),
      onCompleted: () => setError(false),
      variables: { jobId: id }
    }
  );

  const jobDetail = useMemo<JobDetails>(() => {

    if (data && data.getJobDetails) {
      return data.getJobDetails;
    }
  }, [data]);


  const getDownloadURL = (row: File): string => {
    return `${process.env.NEXT_PUBLIC_FILE_SERVICE_URL}file/${jobDetail.job.resultsFolderURI.replace('/home/idies/workspace/', '')}/${row.name}`;
  };

  const columns: GridColDef<File>[] = [
    {
      field: 'name',
      headerName: 'File Name',
      width: 300
    },
    {
      field: 'size',
      headerName: 'Size',
      width: 150,
      valueGetter: (value, row: File) => filesize(row.size, { standard: 'jedec' })
    },
    {
      field: 'creationTime',
      headerName: 'Creation Time',
      width: 250,
      valueGetter: (value, row: File) => new Date(row.creationTime).toLocaleString()
    },
    {
      field: 'lastModified',
      headerName: 'Last Modified',
      width: 250,
      valueGetter: (value, row: File) => new Date(row.lastModified).toLocaleString()
    },
    {
      field: 'actions',
      type: 'actions',
      width: 100,
      getActions: (params) => [
        <GridActionsCellItem
          icon={<DownloadIcon className="run-icon" />}
          label="Download"
          onClick={() => window.open(getDownloadURL(params.row), '_blank')}
        />
      ]
    }
  ];

  return <Styled>
    {jobDetail &&
      <div>
        <div className="header">
          <IconButton onClick={() => router.back()} >
            <ArrowBackIcon />
          </IconButton>
          <div>
            <div className="job-field">
              <Typography variant="h5" gutterBottom component="div">
                Job ID:
              </Typography>
              <Typography variant="body1" gutterBottom component="div">
                {jobDetail.job.id}
              </Typography>
            </div>
            <div className="job-field">
              <Typography variant="h5" gutterBottom component="div">
                Image:
              </Typography>
              <Typography variant="body1" gutterBottom component="div">
                {jobDetail.job.dockerImageName}
              </Typography>
            </div>
          </div>
          <div>
            <div className="job-field">
              <Typography variant="h5" gutterBottom component="div">
                Started:
              </Typography>
              <Typography variant="body1" gutterBottom component="div">
                {jobDetail.job.startTime ? new Date(jobDetail.job.startTime).toLocaleString() : 'N/A'}
              </Typography>
            </div>
            <div className="job-field">
              <Typography variant="h5" gutterBottom component="div">
                Ended:
              </Typography>
              <Typography variant="body1" gutterBottom component="div">
                {jobDetail.job.endTime ? new Date(jobDetail.job.endTime).toLocaleString() : 'N/A'}
              </Typography>
            </div>
          </div>
        </div>
        {jobDetail.job.command &&
          <div className="command">
            <pre>
              {jobDetail.job.command}
              <IconButton
                className="copy-icon"
                size="small"
                aria-label="close"
                color="inherit"
                onClick={() => {
                  navigator.clipboard.writeText(jobDetail.job.command);
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
        <CustomizedTabs tabs={tabOptions} value={tabValue} setValue={setTabValue} />
        <div>
          {tabValue === 0 ?
            <div className="html-description">
              <ReactMarkdown className="html-description" rehypePlugins={[rehypeRaw]}>{sanitize(jobDetail.summary)}</ReactMarkdown>
            </div>
            :
            <DataGrid
              columns={columns}
              rows={jobDetail.files || []}
              className="grid"
              getRowId={(row) => row.name}
              disableRowSelectionOnClick
              aria-label="compute sessions list"
              pageSizeOptions={[5, 10, 25]}
            />
          }
        </div>
      </div>
    }
    {loading &&
      <LoadingAnimation backDropIsOpen={loading} />
    }
    {error &&
      <>
        <h2>There was an error loading the details for this Job.</h2>
        <p>{error}</p>
      </>
    }
  </Styled>;
};