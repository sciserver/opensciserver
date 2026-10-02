import { FC, useContext, useEffect, useMemo, useState } from 'react';
import styled from 'styled-components';
import { Alert, Checkbox } from '@mui/material';

import { UserContext } from 'context';
import { UserVolume, VolumeType } from 'src/graphql/typings';
import { DEFAULT_FILES_ROUTE, FilesRoute, sameVolume, workspacePath } from 'src/utils/fileVolumes';
import { getDefaultUserVolumeIds } from 'src/utils/userVolumes';
import { FileBrowser, FileBrowserSelection } from 'components/content/files/fileBrowser';

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

  .picker {
    height: 540px;
  }

  .picker-off {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 0.4rem;
    height: 200px;
    padding: 1.5rem;
    text-align: center;
    border: 1px dashed ${({ theme }) => theme.palette.contrast1.main};
    border-radius: 6px;
    background: rgba(250, 250, 250, 0.86);
  }

  .picker-off .material-symbols-outlined {
    font-size: 32px;
    color: ${({ theme }) => theme.palette.secondary.main};
  }

  .picker-off .path {
    font: 13px ui-monospace, Menlo, monospace;
  }

  .path-to-file {
    overflow-wrap: break-word;
  }
`;

type Props = {
  resultsFolderURI: string;
  setResultsFolderURI: (uri: string) => void;
  userVolumesList: UserVolume[];
  /** The user volumes chosen for the job, to warn when the working directory is on one that is not mounted. */
  userVolumesChoice?: UserVolume[];
};

const workspaceRoot = () => process.env.NEXT_PUBLIC_JOB_WORKSPACE_PATH || undefined;

export const WorkingDirectoryForm: FC<Props> = ({
  resultsFolderURI,
  setResultsFolderURI,
  userVolumesList,
  userVolumesChoice = []
}) => {

  const { user } = useContext(UserContext);
  const [useTemporaryVolume, setUseTemporaryVolume] = useState<boolean>(true);
  const [picked, setPicked] = useState<FilesRoute>();

  // Start the picker in the user's own persistent volume, when they have one.
  const initialLocation = useMemo<FilesRoute>(() => {
    const persistent = userVolumesList.find(uv => uv.name === 'persistent' && uv.rootVolumeName === 'Storage' && uv.owner === user?.userName);
    return persistent
      ? { volumeType: VolumeType.Uservolume, volume: { volumeName: persistent.name, rootVolumeName: persistent.rootVolumeName, owner: persistent.owner }, path: '' }
      : DEFAULT_FILES_ROUTE;
  }, [userVolumesList, user]);

  // The automatic "jobs" folder replaces whatever was picked; picking replaces it back.
  useEffect(() => {
    if (useTemporaryVolume) {
      setResultsFolderURI(`${process.env.NEXT_PUBLIC_JOB_WORKSPACE_PATH}Temporary/${user?.userName}/jobs/`);
    }
  }, [useTemporaryVolume, user]);

  const onSelect = ({ route }: FileBrowserSelection) => {
    setPicked(route);
    setResultsFolderURI(`${workspacePath(route, workspaceRoot())}/`);
  };

  // A job can only write to volumes mounted in its container: the user's persistent and scratch, plus the chosen ones.
  const notMounted = useMemo(() => {
    if (!picked?.volume || picked.volumeType !== VolumeType.Uservolume) {
      return false;
    }
    const volume = userVolumesList.find(uv => sameVolume({ volumeName: uv.name, rootVolumeName: uv.rootVolumeName, owner: uv.owner }, picked.volume!));
    if (!volume) {
      return false;
    }
    const mounted = new Set([...userVolumesChoice.map(uv => uv.id.toString()), ...getDefaultUserVolumeIds(userVolumesList, user?.userName || '')]);
    return !mounted.has(volume.id.toString());
  }, [picked, userVolumesList, userVolumesChoice, user]);

  return <Styled>
    <h4>Working Directory</h4>
    <p>
      Pick a folder to store standard input/output logs, which will also serve as the current working directory for this job.
      Any subfolder of a volume you can write to works. <strong>Do not use relative paths in the command.</strong>
    </p>

    <div className="checkbox-container">
      <Checkbox checked={useTemporaryVolume} onChange={() => setUseTemporaryVolume(!useTemporaryVolume)} />
      <span className="caption">
        Create and use a new folder in the “jobs” temporary volume. The folder will be created automatically.
      </span>
    </div>
    {useTemporaryVolume ?
      <>
        <div className="picker-off">
          <span className="material-symbols-outlined">create_new_folder</span>
          <strong>A new folder will be created in jobs/</strong>
          <span className="path">{process.env.NEXT_PUBLIC_JOB_WORKSPACE_PATH}Temporary/{user?.userName}/jobs/&lt;timestamp&gt;</span>
          <span className="caption">Uncheck the box above to pick your own folder.</span>
        </div>
        <p className="bullet">
          • Relative paths will be resolved from this location.
        </p>
      </>
      :
      <>
        <FileBrowser className="picker" mode="pick" initialLocation={initialLocation} pickLabel="Use as working directory" onSelect={onSelect} />
        {notMounted && (
          <Alert severity="warning">
            This volume is not selected for the job. Add it in the User vols tab, or the job will not be able to write there.
          </Alert>
        )}
        {!picked && (
          <p className="bullet">
            • Choose a folder and press “Use as working directory”. Until you do, the job keeps using the folder below.
          </p>
        )}
      </>
    }
    <p className="bullet path-to-file">
      • A copy of this command will be placed in a unique, nested subfolder of <i className="path">{resultsFolderURI}</i>.
    </p>
  </Styled>;
};
