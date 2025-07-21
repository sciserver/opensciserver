import { FC, useCallback } from 'react';
import styled from 'styled-components';
import { DataGrid, GridActionsCellItem, GridColDef, GridRenderCellParams, GridRowParams } from '@mui/x-data-grid';
import { Replay as ReplayIcon } from '@mui/icons-material';

import { Job } from 'src/graphql/typings';
import { Chip } from '@mui/material';

const Styled = styled.div`
  .grid {
    width: 95%;
    border: none;

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

    .delete-icon {
      color: ${({ theme }) => theme.palette.icons.danger};
    }
    .run-icon {
      color: ${({ theme }) => theme.palette.icons.play};
    }
  }
`;

type Props = {
  jobsList: Job[];
  selectJob: (job: Job) => void;
  createJob: (variables: Job) => void;
}

export const JobsDataGrid: FC<Props> = ({ jobsList, selectJob, createJob }) => {

  const rerunJob = useCallback(
    (params: GridRowParams<Job>) => () => {
      createJob(params.row);
    },
    []);

  const columns: GridColDef<Job>[] = [
    {
      field: 'submissionTime',
      type: 'dateTime',
      headerName: 'Submitted at',
      width: 200,
      valueGetter: (value, row: Job) => new Date(row.submissionTime)
    },
    {
      field: 'submitterDID',
      headerName: 'Name',
      width: 150
    },
    {
      field: 'scriptURI',
      headerName: 'Script URI',
      width: 150
    },
    {
      field: 'command',
      headerName: 'Command',
      width: 200
    },
    {
      field: 'status',
      headerName: 'Status',
      width: 130,
      renderCell: (params: GridRenderCellParams<Job>) => {
        switch (params.row.status) {
          case 'SUCCESS': {
            return <Chip color="success" label="Success" />;
          }
          case 'ERROR': {
            return <Chip color="error" label="Error" />;
          }

          default: {
            return <Chip color="secondary" label={params.row.status} />;
          }
        }
      }
    },
    {
      field: 'actions',
      type: 'actions',
      width: 100,
      getActions: (params) => [
        <GridActionsCellItem
          icon={<ReplayIcon className="run-icon" />}
          label="Run"
          onClick={rerunJob(params)}
        />,
      ]
    }
  ];

  return <Styled>
    <DataGrid
      onRowClick={({ row }) => selectJob(row)}
      columns={columns}
      rows={jobsList}
      className="grid"
      disableRowSelectionOnClick
      aria-label="compute sessions list"
      pageSizeOptions={[5, 10, 25]}
    />
  </Styled>;
};