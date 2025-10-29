import { FC, useMemo, useState } from 'react';
import { useRouter } from 'next/router';
import { useQuery, ApolloError } from '@apollo/client';
import { Chip, IconButton } from '@mui/material';
import { Close as CloseIcon } from '@mui/icons-material';
import styled from 'styled-components';

import { DataVolume, Domain, Image, UserVolume } from 'src/graphql/typings';
import { GET_DOMAINS } from 'src/graphql/domains';

import { NewResource, NewSessionType } from 'components/content/newResource/newResource';

const Styled = styled.div`
  .header {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-bottom: 1.5rem;

    .alert {
      color: ${({ theme }) => theme.palette.warning.dark};
    }
  }
`;

export const NewSession: FC = () => {

  const router = useRouter();

  const [loadingSubmit, setLoadingSubmit] = useState<boolean>(false);

  const { loading: loadingData, data } = useQuery(GET_DOMAINS,
    {
      onError: (error: ApolloError) => {
        if (error.message.includes('Unauthorized')) {
          router.push('/login?callbackURL=/compute');
        }
      }
    }
  );

  const [domainChoice, setDomainChoice] = useState<Domain>();
  const [imageChoice, setImageChoice] = useState<Image>();
  const [dataVolumesChoice, setDataVolumesChoice] = useState<DataVolume[]>([]);
  const [userVolumesChoice, setUserVolumesChoice] = useState<UserVolume[]>([]);

  const domainList = useMemo<Domain[]>(() => {
    if (data && data.getDomains) {
      setDomainChoice((data.getDomains as Domain[]).find(d => d.name == process.env.NEXT_PUBLIC_NEW_SESSION_DOMAIN_NAME_DEFAULT));
      return data.getDomains;
    }
    return [];
  }, [data]);

  const imageList = useMemo<Image[]>(() => {
    if (domainChoice) {
      setImageChoice(domainChoice.images.find(d => d.name == process.env.NEXT_PUBLIC_NEW_SESSION_IMAGE_NAME_DEFAULT));
      return domainChoice.images;
    }
    return [];
  }, [domainChoice]);

  const dataVolumeList = useMemo<DataVolume[]>(() => {
    if (domainChoice) {
      return domainChoice.dataVolumes;
    }
    return [];
  }, [domainChoice]);

  const userVolumeList = useMemo<UserVolume[]>(() => {
    if (domainChoice) {
      return domainChoice.userVolumes;
    }
    return [];
  }, [domainChoice]);

  const submit = () => {
    if (domainChoice == undefined || imageChoice == undefined) {
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

  return <Styled>
    <div className="header">
      <IconButton onClick={() => router.push('/compute')} >
        <CloseIcon />
      </IconButton>
      <h1>New Compute Session</h1>
      <Chip color="warning" label="BETA" />
    </div>
    <NewResource
      sessionType={NewSessionType.INTERACTIVE}
      domainList={domainList}
      domainChoice={domainChoice}
      setDomainChoice={setDomainChoice}
      imageList={imageList}
      imageChoice={imageChoice}
      setImageChoice={setImageChoice}
      dataVolumeList={dataVolumeList}
      dataVolumesChoice={dataVolumesChoice}
      setDataVolumesChoice={setDataVolumesChoice}
      userVolumeList={userVolumeList}
      userVolumesChoice={userVolumesChoice}
      setUserVolumesChoice={setUserVolumesChoice}
      submit={submit}
      loadingSubmit={loadingSubmit}
      loadingData={loadingData}
    />
  </Styled>;
};