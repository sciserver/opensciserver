import { FC } from 'react';
import styled from 'styled-components';
import { TextField } from '@mui/material';

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
  command?: string;
  setCommand?: (command: string) => void;
  commandError?: boolean;
  setCommandError?: (error: boolean) => void;
};

export const CommandForm: FC<Props> = ({
  command,
  setCommand,
  commandError,
  setCommandError
}) => {
  return <Styled>
    <TextField
      id="command-multiline"
      label="Command"
      multiline
      placeholder="Type your command here. This will be run as a bash shell command."
      rows={10}
      value={command}
      onChange={(e) => {
        setCommand?.(e.target.value);
        setCommandError?.(false);
      }}
      error={commandError}
      helperText={commandError ? 'Command cannot be empty' : ''}
    />
  </Styled>;
};