import { FC } from 'react';
import { useRouter } from 'next/router';
import { useMutation } from '@apollo/client';
import { IconButton, Tooltip } from '@mui/material';
import { Replay as ReplayIcon } from '@mui/icons-material';
import Swal from 'sweetalert2';

import { CREATE_JOB } from 'src/graphql/jobs';
import { Job, JobStatus } from 'src/graphql/typings';

import { jobStatusPollingInterval } from 'components/content/jobs/list/jobsList';
import { JobCreatedModalWording, ReRunJobModalWording, UnableToAddJobModalWording } from 'src/utils/swalWording';

interface Props {
  job: Job;
  startPolling: (interval: number) => void;
}

export const jobStatusAllowRerun = new Set([JobStatus.Error, JobStatus.Success]);
export const RerunJobAction: FC<Props> = ({ job, startPolling }) => {
  const router = useRouter();
  const [createJob] = useMutation(CREATE_JOB, {
    onError: () => Swal.fire(UnableToAddJobModalWording as any).then(() => {
      return;
    }).catch(Error),
    onCompleted: () => Swal.fire(JobCreatedModalWording as any).then(() => {
      startPolling(jobStatusPollingInterval);
    })
  });

  const handleRerun = async () => {
    const result = await Swal.fire(ReRunJobModalWording as any);
    if (result.isConfirmed) {
      const resultsFolderURI = job.resultsFolderURI
        .split('/')
        // compm adds subdirs to the results folder, this indicates last non-dynamic index
        .slice(0, Number.parseInt(process.env.NEXT_PUBLIC_JOB_URI_CONSTANT_TERMINUS || '0'))
        .join('/');

      createJob({
        variables: {
          createJobParams: {
            dockerComputeEndpoint: job.dockerComputeEndpoint,
            dockerImageName: job.dockerImageName,
            resultsFolderURI,
            submitterDID: job.submitterDID,
            volumeContainers: job.dataVolumes.map(dv => {
              return { name: dv.name };
            }),
            userVolumes: job.userVolumes.map(uv => {
              return { userVolumeId: uv.id, needsWriteAccess: uv.needsWriteAccess };
            }),
            command: job.command,
            scriptURI: job.scriptURI || ''
          }
        }
      });
    }
    if (result.isDenied) {
      router.push({ pathname: '/jobs/new', query: { rerunFromJobId: job.id } });
    }
  };

  return (
    <Tooltip title="Re-run Job">
      <IconButton onClick={(e) => { e.stopPropagation(); handleRerun(); }} size="small">
        <ReplayIcon color="primary" className="replay-icon" />
      </IconButton>
    </Tooltip>
  );
};