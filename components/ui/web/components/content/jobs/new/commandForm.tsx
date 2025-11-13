import { FC } from 'react';
import styled from 'styled-components';
import { TextField, Checkbox } from '@mui/material';

import { WorkingDirectoryAccordionSummary } from 'components/content/newResource/workingDirectoryAccordion';
import { UserVolume } from 'src/graphql/typings';

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
  command: string;
  setCommand: (command: string) => void;
  commandError: boolean;
  setCommandError: (error: boolean) => void;
  useTemporaryVolume: boolean;
  setUseTemporaryVolume: (use: boolean) => void;
  temporaryWorkingDirPath: string;
  workingDirectoryUserVolumesChoice?: UserVolume;
  setWorkingDirectoryUserVolumesChoice: (workingDirectory: UserVolume) => void;
  userVolumesChoice: UserVolume[];
  setActiveStep: (step: number) => void;
  submit: () => void;
};
export const CommandForm: FC<Props> = ({
  command,
  setCommand,
  commandError,
  setCommandError,
  useTemporaryVolume,
  setUseTemporaryVolume,
  temporaryWorkingDirPath,
  workingDirectoryUserVolumesChoice,
  setWorkingDirectoryUserVolumesChoice,
  userVolumesChoice
}) => {
  return <Styled>
    <TextField
      id="command-multiline"
      label="Command"
      multiline
      rows={10}
      value={command}
      onChange={(e) => {
        setCommand(e.target.value);
        setCommandError(false);
      }}
      error={commandError}
      helperText={commandError ? 'Command cannot be empty' : ''}
    />
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
      <WorkingDirectoryAccordionSummary userVolumeList={userVolumesChoice} userVolumeChoice={workingDirectoryUserVolumesChoice} setUserVolumeChoice={setWorkingDirectoryUserVolumesChoice} />
    }
  </Styled>;
};