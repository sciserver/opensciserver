import { FC } from 'react';
import styled from 'styled-components';
import { Checkbox } from '@mui/material';

import { UserVolume } from 'src/graphql/typings';
import { UserVolumeOptions } from 'components/content/newComputeSession/userVolumeOptions';

const Styled = styled.div`
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin: 2rem 0.5rem;

  p {
    margin: 0;

    .path {
      font-family: monospace;
      color: ${({ theme }) => theme.palette.error.light};
    }
  }

  .bullet {
    margin-left: 1.5rem;
  }

  .checkbox-container {
    display: flex;
    align-items: center;
    gap: 0.2rem;

  }
`;

type Props = {
  useTemporaryVolume: boolean;
  setUseTemporaryVolume: (use: boolean) => void;
  temporaryWorkingDirPath: string;
  workingDirectoryUserVolumesChoice?: UserVolume;
  setWorkingDirectoryUserVolumesChoice: (workingDirectory: UserVolume) => void;
  userVolumesList: UserVolume[];
  userVolumesChoice: UserVolume[];
};
export const WorkingDirectoryForm: FC<Props> = ({
  useTemporaryVolume,
  setUseTemporaryVolume,
  temporaryWorkingDirPath,
  workingDirectoryUserVolumesChoice,
  setWorkingDirectoryUserVolumesChoice,
  userVolumesList,
  userVolumesChoice
}) => {
  return <Styled>
    <h4>Working Directory</h4>
    <p>
      Select a location to store standard input/output logs, which will also serve as the current working directory for this job.
      To use other writable user volumes, enable them in the Files tab. <strong>Do not use relative paths in the command.</strong>
    </p>

    <div className="checkbox-container">
      <Checkbox checked={useTemporaryVolume} onChange={() => setUseTemporaryVolume(!useTemporaryVolume)} />
      <span className="caption">
        Create and use a new folder in the “jobs” temporary volume. The folder will be created automatically.
      </span>
    </div>
    {useTemporaryVolume ?
      <div>
        <p className="bullet">
          • A copy of this command will be placed in a unique, nested subfolder of <i className="path">{temporaryWorkingDirPath}</i>.
        </p>
        <p className="bullet">
          • Relative paths will be resolved from this location.
        </p>
      </div>
      :
      <UserVolumeOptions userVolumeList={userVolumesList} userVolumesChoice={userVolumesChoice} setUserVolumesChoice={() => { }} />
    }
  </Styled>;
};