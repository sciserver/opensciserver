import { FC, useState } from 'react';
import { useRouter } from 'next/router';
import { useMutation } from '@apollo/client';
import Swal from 'sweetalert2';

import { DataVolume, Domain, Image, UserVolume } from 'src/graphql/typings';
import { CREATE_JOB } from 'src/graphql/jobs';

import { NewComputeSession } from 'components/content/newComputeSession/newComputeSession';


export const NewJob: FC = () => {

  const router = useRouter();

  const [loadingSubmit, setLoadingSubmit] = useState<boolean>(false);

  const [sessionName, setSessionName] = useState<string>('');
  const [domainChoice, setDomainChoice] = useState<Domain>();
  const [imageChoice, setImageChoice] = useState<Image>();
  const [dataVolumesChoice, setDataVolumesChoice] = useState<DataVolume[]>([]);
  const [userVolumesChoice, setUserVolumesChoice] = useState<UserVolume[]>([]);
  const [command, setCommand] = useState<string>('');
  const [useTemporaryVolume, setUseTemporaryVolume] = useState<boolean>(true);
  const [workingDirectoryUserVolumesChoice, setWorkingDirectoryUserVolumesChoice] = useState<UserVolume>();

  const [commandError, setCommandError] = useState<boolean>(false);


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


  // TODO: get userName from user profile
  const temporaryWorkingDirPath = `/home/idies/workspace/Temporary/\${userName}/jobs/`;

  // eslint-disable-next-line unicorn/consistent-function-scoping
  const submit = () => {
    if (!command.length) {
      setCommandError(true);
      return;
    }

    if (!useTemporaryVolume && !workingDirectoryUserVolumesChoice) {
      return;
    }
    setLoadingSubmit(true);

    const resultsFolderURI = useTemporaryVolume ?
      temporaryWorkingDirPath :
      `/home/idies/workspace/${workingDirectoryUserVolumesChoice!.rootVolumeName}/${workingDirectoryUserVolumesChoice!.owner}/${workingDirectoryUserVolumesChoice!.name}/`;

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


  return <NewComputeSession
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
    submit={submit}
    loadingSubmit={loadingSubmit}
    isJob
  />;
};