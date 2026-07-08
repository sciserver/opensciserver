import { FC, useState } from 'react';
import styled from 'styled-components';

import { Container } from 'src/graphql/typings';

import { ContainerDetail } from 'components/content/compute/sessionManagement/containerDetail';
import { ContainerList } from 'components/content/compute/sessionManagement/containerList';

const Styled = styled.div`
`;

export const SessionManagement: FC = ({ }) => {

  const [containerSelected, setContainerSelected] = useState<Container | null>(null);

  return <Styled>
    <h1>Compute Sessions</h1>
    {containerSelected ?
      <ContainerDetail row={containerSelected} back={() => setContainerSelected(null)} />
      :
      <ContainerList selectContainer={setContainerSelected} />
    }
  </Styled>;
};