import { FC, useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/router';
import { useLazyQuery, useMutation } from '@apollo/client';
import { sanitize } from 'dompurify';
import Swal from 'sweetalert2';

import { DataVolume, Domain, Image, Job, UserVolume } from 'src/graphql/typings';
import { CREATE_JOB, JOB_DETAIL_VIEW } from 'src/graphql/jobs';

import { NewComputeSession } from 'components/content/newComputeSession/newComputeSession';
import { LoadingAnimation } from 'components/common/loadingAnimation';


export const NewJob: FC = () => {

  const router = useRouter();

  const [loadingSubmit, setLoadingSubmit] = useState<boolean>(false);

  const [sessionName, setSessionName] = useState<string>('');
  const [domainChoice, setDomainChoice] = useState<Domain>();
  const [imageChoice, setImageChoice] = useState<Image>();
  const [dataVolumesChoice, setDataVolumesChoice] = useState<DataVolume[]>([]);
  const [userVolumesChoice, setUserVolumesChoice] = useState<UserVolume[]>([]);
  const [command, setCommand] = useState<string>('');
  const [resultsFolderURI, setResultsFolderURI] = useState<string>('');

  const [createJob] = useMutation(CREATE_JOB, {
    onError: () => Swal.fire({
      title: 'Unable to add job',
      text: `Please try again. If the problem persists, contact us at <a href=\"mailto:${process.env.NEXT_PUBLIC_HELPDESK_EMAIL}\">${process.env.NEXT_PUBLIC_HELPDESK_EMAIL}</a> for more assistance.`,
      icon: 'error',
      confirmButtonText: 'OK'
    }).then(() => {
      setLoadingSubmit(false);
      return;
    }).catch(Error),
    onCompleted: () => Swal.fire({
      title: 'Job created successfully',
      text: 'Your job has been created and is now queued.',
      icon: 'success',
      confirmButtonText: 'OK'
    }).then(() => {
      setLoadingSubmit(false);
      router.push('/jobs');
    })
  });

  const [getEditJob, { loading: loadingEditJob, data: dataEditJob }] =
    useLazyQuery(
      JOB_DETAIL_VIEW,
      {
        onError: () => Swal.fire({
          title: 'Unable to load job for editing',
          text: `Please try again.`,
          icon: 'error',
          confirmButtonText: 'OK'
        }).then(() => {
          router.push('/jobs');
        }).catch(Error)
      }
    );

  // eslint-disable-next-line unicorn/consistent-function-scoping
  const submit = () => {

    setLoadingSubmit(true);

    createJob({
      variables: {
        createJobParams: {
          dockerComputeEndpoint: domainChoice!.apiEndpoint,
          dockerImageName: imageChoice!.name,
          resultsFolderURI,
          submitterDID: sessionName,
          scriptURI: '',
          volumeContainers: dataVolumesChoice.map(dv => dv.publisherDID),
          userVolumes: userVolumesChoice.map(uv => uv.id),
          command
        }
      }
    });
  };

  // ON MOUNT: if rerunFromJobId is present, load the job details and pre-populate the form
  useEffect(() => {
    if (!router.isReady) {
      return;
    }

    let { rerunFromJobId } = router.query;

    rerunFromJobId = sanitize(rerunFromJobId as string);

    if (rerunFromJobId) {
      getEditJob({ variables: { jobId: rerunFromJobId } });
    }
  }, [router]);

  const editJob = useMemo<Job | undefined>(() => {
    if (dataEditJob && dataEditJob.getJobDetails) {
      return dataEditJob.getJobDetails.job;
    }
    return undefined;
  }, [dataEditJob]);

  return <>
    <NewComputeSession
      isJob
      editJob={editJob}
      sessionName={sessionName}
      setSessionName={setSessionName}
      domainChoice={domainChoice}
      setDomainChoice={setDomainChoice}
      imageChoice={imageChoice}
      setImageChoice={setImageChoice}
      dataVolumesChoice={dataVolumesChoice}
      setDataVolumesChoice={setDataVolumesChoice}
      userVolumesChoice={userVolumesChoice}
      setUserVolumesChoice={setUserVolumesChoice}
      command={command}
      setCommand={setCommand}
      resultsFolderURI={resultsFolderURI}
      setResultsFolderURI={setResultsFolderURI}
      submit={submit}
      loadingSubmit={loadingSubmit}
    />
    <LoadingAnimation backDropIsOpen={loadingEditJob} />
  </>;
};