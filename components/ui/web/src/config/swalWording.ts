export const ReRunJobModalWording = {
  title: 'Rerun job',
  text: `Do you want to run this job again as is, or would you like to review and modify the job parameters before submitting?`,
  icon: 'question',
  showCancelButton: true,
  showDenyButton: true,
  confirmButtonText: 'Rerun unmodified',
  denyButtonText: 'Review and modify',
  cancelButtonText: 'Cancel'
};

export const UnableToAddJobModalWording = {
  title: 'Unable to add job',
  text: `Please try again. If the problem persists, contact us at <a href=\"mailto:${process.env.NEXT_PUBLIC_HELPDESK_EMAIL}\">${process.env.NEXT_PUBLIC_HELPDESK_EMAIL}</a> for more assistance.`,
  icon: 'error',
  confirmButtonText: 'OK'
};

export const JobCreatedModalWording = {
  title: 'Job created successfully',
  text: 'Your job has been created and is now queued.',
  icon: 'success',
  confirmButtonText: 'OK'
};