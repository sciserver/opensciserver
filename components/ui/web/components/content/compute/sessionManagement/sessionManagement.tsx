import { FC, useState } from 'react';
import styled from 'styled-components';
import { Typography } from '@mui/material';

import { Container } from 'src/graphql/typings';

import { ContainerDetail } from 'components/content/compute/sessionManagement/containerDetail';
import { ContainerList } from 'components/content/compute/sessionManagement/containerList';

const Styled = styled.div`
`;

export const SessionManagement: FC = ({ }) => {

  const [containerSelected, setContainerSelected] = useState<Container | null>(null);

  return <Styled>
    <Typography variant="h3">Compute Sessions</Typography>
    {containerSelected ?
      <ContainerDetail row={containerSelected} back={() => setContainerSelected(null)} />
      :
      <ContainerList selectContainer={setContainerSelected} />
    }
  </Styled>;
};