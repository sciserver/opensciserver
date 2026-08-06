import { FC } from 'react';
import { useMutation } from '@apollo/client';
import { IconButton, Tooltip } from '@mui/material';
import { Cancel as CancelIcon } from '@mui/icons-material';
import Swal from 'sweetalert2';

import { CANCEL_JOB } from 'src/graphql/jobs';
import { Job, JobStatus } from 'src/graphql/typings';

interface Props {
  job: Job;
  refetch: () => void;
}

export const jobStatusAllowCancel = new Set([JobStatus.Pending, JobStatus.Accepted, JobStatus.Queued, JobStatus.Started]);
export const CancelJobAction: FC<Props> = ({ job, refetch }) => {

  const [cancelJob] = useMutation(CANCEL_JOB, {
    onError: () => Swal.fire({
      title: 'Unable to cancel job',
      text: `Please try again. If the problem persists, contact us at <a href=\"mailto:${process.env.NEXT_PUBLIC_HELPDESK_EMAIL}\">${process.env.NEXT_PUBLIC_HELPDESK_EMAIL}</a> for more assistance.`,
      icon: 'error',
      confirmButtonText: 'OK'
    }).then(() => refetch()),
    onCompleted: () => Swal.fire({
      title: 'Job cancelled',
      text: 'The job has been successfully cancelled.',
      icon: 'success',
      confirmButtonText: 'OK'
    }).then(() => refetch())
  });

  return (
    <Tooltip title="Cancel Job">
      <IconButton onClick={(e) => {
        e.stopPropagation();
        cancelJob({ variables: { jobId: job.id } });
      }} size="medium">
        <CancelIcon color="error" className="delete-icon" />
      </IconButton>
    </Tooltip>
  );
};