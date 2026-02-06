import { FC, useState } from 'react';
import { useRouter } from 'next/router';

import { DataVolume, Domain, Image, UserVolume } from 'src/graphql/typings';
import { NewComputeSession } from 'components/content/newComputeSession/newComputeSession';

export const NewSession: FC = () => {

  const router = useRouter();

  const [loadingSubmit, setLoadingSubmit] = useState<boolean>(false);

  const [sessionName, setSessionName] = useState<string>('');
  const [domainChoice, setDomainChoice] = useState<Domain>();
  const [imageChoice, setImageChoice] = useState<Image>();
  const [dataVolumesChoice, setDataVolumesChoice] = useState<DataVolume[]>([]);
  const [userVolumesChoice, setUserVolumesChoice] = useState<UserVolume[]>([]);

  const submit = () => {
    if (domainChoice === undefined || imageChoice === undefined) {
      return;
    }

    setLoadingSubmit(true);
    let url = `/compute/run?dom=${domainChoice.name}&img=${imageChoice.name}`;
    if (dataVolumesChoice.length) {
      url += `&dvs=${dataVolumesChoice.map(dv => dv.publisherDID)}`;
    }
    if (userVolumesChoice.length) {
      url += `&uvs=${userVolumesChoice.map(uv => uv.id)}`;
    }

    router.push(url);
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
    submit={submit}
    loadingSubmit={loadingSubmit}
  />;
};