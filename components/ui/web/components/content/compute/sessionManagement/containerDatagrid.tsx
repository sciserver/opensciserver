import { FC, useCallback } from 'react';
import { useRouter } from 'next/router';
import styled from 'styled-components';
import { DataGrid, GridActionsCellItem, GridColDef, GridRowId, GridRowParams } from '@mui/x-data-grid';
import { Delete as DeleteIcon, PlayArrow as PlayArrowIcon } from '@mui/icons-material';

import { Container } from 'src/graphql/typings';

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
  containerList: Container[];
  selectContainer: (container: Container) => void;
}

export const ContainerDataGrid: FC<Props> = ({ containerList, selectContainer }) => {
  const router = useRouter();

  // TODO: implement delete container mutation and logic
  const deleteContainer = useCallback(
    (id: GridRowId) => () => {
      // Delete logic goes here
    },
    []);

  const runContainer = useCallback(
    (params: GridRowParams<Container>) => () => {
      const domainName = params.row.domainName;
      const imageName = params.row.imageName;
      const dataVolumes = params.row.dataVolumes;
      const userVolumes = params.row.userVolumes;
      let url = `/compute/run?dom=${domainName}&img=${imageName}`;
      if (dataVolumes.length) {
        url += `&dvs=${dataVolumes.map(dv => dv.publisherDID)}`;
      }
      if (userVolumes.length) {
        url += `&uvs=${userVolumes.map(uv => uv)}`;
      }
      router.push(url);
    }, []);

  const columns: GridColDef<Container>[] = [
    {
      field: 'id',
      headerName: 'ID',
      width: 100
    },
    {
      field: 'imageName',
      headerName: 'Image',
      flex: 1
    },
    {
      field: 'domainName',
      headerName: 'Domain',
      width: 150
    },
    {
      field: 'dataVolumes',
      headerName: 'DataVols',
      flex: 0.5,
      valueGetter: (value, row: Container) => row.dataVolumes.length
    },
    {
      field: 'userVolumes',
      headerName: 'UserVols',
      flex: 0.5,
      valueGetter: (value, row: Container) => row.userVolumes.length
    },
    {
      field: 'accessedAt',
      headerName: 'Last Accessed',
      type: 'dateTime',
      width: 150,
      valueGetter: (value) => new Date(value)
    },
    {
      field: 'createdAt',
      headerName: 'Created',
      type: 'date',
      flex: 0.6,
      valueGetter: (value) => new Date(value)
    },
    {
      field: 'actions',
      type: 'actions',
      flex: 0.8,
      getActions: (params) => [
        <GridActionsCellItem
          icon={<PlayArrowIcon className="run-icon" />}
          label="Run"
          onClick={runContainer(params)}
        />,
        <GridActionsCellItem
          icon={<DeleteIcon className="delete-icon" />}
          label="Delete"
          onClick={deleteContainer(params.id)}
        />
      ]
    }
  ];

  return <Styled>
    <h2>Active sessions</h2>
    <DataGrid
      onRowClick={({ row }) => selectContainer(row)}
      columns={columns}
      rows={containerList}
      className="grid"
      disableRowSelectionOnClick
      aria-label="compute sessions list"
      pageSizeOptions={[5, 10, 25]}
    />
  </Styled>;
};