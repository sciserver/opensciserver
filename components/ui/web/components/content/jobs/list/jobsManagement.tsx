import { FC, useState } from 'react';
import styled from 'styled-components';
import { Typography } from '@mui/material';

import { Job } from 'src/graphql/typings';
import { JobsList } from './jobsList';


const Styled = styled.div`
`;

export const JobsManagement: FC = ({ }) => {

  const [jobSelected, setJobSelected] = useState<Job | null>(null);

  return <Styled>
    <Typography variant="h3">Jobs</Typography>
    <JobsList selectJob={setJobSelected} />
  </Styled>;
};