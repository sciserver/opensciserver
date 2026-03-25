import { FC, useContext, useEffect, useMemo, useState } from 'react';
import styled from 'styled-components';
import { Checkbox, TextField } from '@mui/material';

import { UserContext } from 'context';
import { UserVolume } from 'src/graphql/typings';
import { OptionCard } from 'components/common/optionCard';

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

  .option-items {
    justify-content: center;
  }

  .path-to-file {
    overflow-wrap: break-word;
  }
`;

type Props = {
  resultsFolderURI: string;
  setResultsFolderURI: (uri: string) => void;
  userVolumesList: UserVolume[];
};
export const WorkingDirectoryForm: FC<Props> = ({
  resultsFolderURI,
  setResultsFolderURI,
  userVolumesList
}) => {

  const { user } = useContext(UserContext);
  const [useTemporaryVolume, setUseTemporaryVolume] = useState<boolean>(true);
  const [workingDirectoryUserVolumesChoice, setWorkingDirectoryUserVolumesChoice] = useState<UserVolume>();
  const [userSetResultFolderBasePath, setUserSetResultFolderBasePath] = useState<string>('');
  const [pathToFile, setPathToFile] = useState<string>('');

  const filteredOwned = useMemo<UserVolume[]>(() => {
    return userVolumesList.filter(i => i.owner === user?.userName);
  }, [userVolumesList, user]);

  useEffect(() => {
    const scratchVol = filteredOwned.find(uv => uv.name === 'scratch');
    setWorkingDirectoryUserVolumesChoice(scratchVol);
  }, [filteredOwned]);

  useEffect(() => {
    if (useTemporaryVolume) {
      setResultsFolderURI(`${process.env.NEXT_PUBLIC_JOB_WORKSPACE_PATH}Temporary/${user?.userName}/jobs/`);
      setPathToFile('');
      return;
    }

    const basepath = `${process.env.NEXT_PUBLIC_JOB_WORKSPACE_PATH}${workingDirectoryUserVolumesChoice!.rootVolumeName}/${workingDirectoryUserVolumesChoice!.owner}/${workingDirectoryUserVolumesChoice!.name}/`;
    setUserSetResultFolderBasePath(basepath);
    setResultsFolderURI(basepath);
  }, [useTemporaryVolume, workingDirectoryUserVolumesChoice, user]);

  useEffect(() => {
    setResultsFolderURI(userSetResultFolderBasePath + pathToFile);
  }, [pathToFile, userSetResultFolderBasePath]);

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
          • Relative paths will be resolved from this location.
        </p>
      </div>
      :
      <>
        <div className="option-items">
          {filteredOwned.map(uv =>
            <OptionCard
              key={uv.id}
              selected={workingDirectoryUserVolumesChoice?.id === uv.id}
              title={uv.name}
              description={uv.description || 'No description available'}
              action={() => setWorkingDirectoryUserVolumesChoice(uv)}
            />
          )}
        </div>
        <TextField
          id="path-to-file"
          label="Path within user volume (optional)"
          placeholder="Type the path to the file here."
          value={pathToFile}
          onChange={(e) => setPathToFile(e.target.value)}
        />
      </>
    }
    <p className="bullet path-to-file">
      • A copy of this command will be placed in a unique, nested subfolder of <i className="path">{resultsFolderURI}</i>.
    </p>
  </Styled>;
};