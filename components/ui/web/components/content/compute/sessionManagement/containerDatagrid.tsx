import { FC, useCallback, useState } from 'react';
import { useRouter } from 'next/router';
import styled from 'styled-components';
import { useLazyQuery } from '@apollo/client';
import Swal from 'sweetalert2';
import { DataGrid, GridActionsCellItem, GridColDef, GridRowId, GridRowParams } from '@mui/x-data-grid';
import { Delete as DeleteIcon, PlayArrow as PlayArrowIcon } from '@mui/icons-material';

import { Container, UserVolume } from 'src/graphql/typings';
import { VOLUMES_CONTAINER_DETAIL_VIEW } from 'src/graphql/containers';
import { Tooltip } from '@mui/material';

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

  const [getContainerDetail] = useLazyQuery(VOLUMES_CONTAINER_DETAIL_VIEW);
  const [pendingContainerId, setPendingContainerId] = useState<GridRowId | null>(null);

  // TODO: implement delete container mutation and logic
  const deleteContainer = useCallback(
    (id: GridRowId) => () => {
      // Delete logic goes here
    },
    []);

  const runContainer = useCallback(
    (params: GridRowParams<Container>) => async () => {
      const imageName = params.row.imageName;
      const dataVolumes = params.row.dataVolumes;
      const userVolumes = params.row.userVolumes;

      setPendingContainerId(params.id);
      try {
        const { data } = await getContainerDetail({
          variables: {
            containerDetailParams: {
              domainId: params.row.domainID,
              dataVolumeIds: dataVolumes.map(dv => dv.publisherDID),
              userVolumeIds: userVolumes
            }
          }
        });

        let url = `/compute/run?dom=${params.row.domainID}&img=${imageName}`;
        if (dataVolumes.length) {
          url += `&dvs=${dataVolumes.map(dv => dv.publisherDID)}`;
        }
        if (userVolumes.length) {
          // The scratch and persistent User Volumes are auto-injected into every
          // container, so they're excluded here to match the graphql-side behavior.
          const nonDefaultUVs = ((data?.getContainerDetail?.userVolumes ?? []) as UserVolume[])
            .filter(uv => uv.name !== 'persistent' && uv.name !== 'scratch')
            .map(uv => uv.id);
          if (nonDefaultUVs.length) {
            url += `&uvs=${nonDefaultUVs}`;
          }
        }
        router.push(url);
      }
      catch (error) {
        Swal.fire({
          icon: 'error',
          title: 'Error running container',
          text: error instanceof Error ? error.message : 'Something went wrong while preparing this session.'
        });
      }
      finally {
        setPendingContainerId(null);
      }
    }, [router, getContainerDetail]);

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
      type: 'dateTime',
      flex: 0.6,
      valueGetter: (value) => new Date(value)
    },
    {
      field: 'actions',
      type: 'actions',
      flex: 0.8,
      getActions: (params) => [
        <GridActionsCellItem
          icon={
            <Tooltip title="Run Container">
              <PlayArrowIcon className="run-icon" />
            </Tooltip>
          }
          label="Run"
          disabled={pendingContainerId === params.id}
          onClick={runContainer(params)}
        />,
        // NOTE: Delete action is currently hidden until the delete container 
        // functionality works end-to-end. The code is left here for reference
        // and future implementation. There are permission issues that we haven't
        // dealt with yet around deleting containers that need to be resolved before 
        // this can be implemented.
        // <GridActionsCellItem
        //   icon={
        //     <Tooltip title="Delete Container">
        //       <DeleteIcon className="delete-icon" />
        //     </Tooltip>
        //   }
        //   label="Delete"
        //   onClick={deleteContainer(params.id)}
        // />
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