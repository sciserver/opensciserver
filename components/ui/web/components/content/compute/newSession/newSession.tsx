import { FC, useMemo, useState } from 'react';
import { useRouter } from 'next/router';
import styled from 'styled-components';
import {
  Button,
  CircularProgress,
  IconButton,
  TextField,
  Typography
} from '@mui/material';
import { ArrowBackIos as ArrowBackIcon } from '@mui/icons-material';
import { useQuery, ApolloError } from '@apollo/client';

import { DomainAccordionSummary } from 'components/content/compute/newSession/domainAccordion';
import { ImageAccordionSummary } from 'components/content/compute/newSession/imageAccordion';
import { DataVolAccordionSummary } from 'components/content/compute/newSession/dataVolumeAccordion';
import { UserVolAccordionSummary } from 'components/content/compute/newSession/userVolumeAccordion';
import { DataVolume, Domain, Image, UserVolume } from 'src/graphql/typings';
import { GET_DOMAINS } from 'src/graphql/domains';
import { LoadingAnimation } from 'components/common/loadingAnimation';

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
  
  .form {
    display: flex;
    flex-direction: column;
    gap: 2rem;
    margin: 2rem 0.5rem;
    width: 90%;
    
    .accordion-header {
      display: flex;
      align-items: center;
      gap: 2rem;
      
      .accordion-title { 
        display: flex;
        width: 120px;
        align-items: center;

        .title {
          text-transform: capitalize;
        }
      }
    }

    .submit-button {
      margin: 2rem 0.1rem;
    }
  }
`;


export const NewSession: FC = ({ }) => {

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
        <ArrowBackIcon />
      </IconButton>
      <Typography variant="h3">New Compute Session</Typography>
      <Typography className="alert" variant="h4">(BETA version)</Typography>
    </div>
    <div className="form">
      <TextField id="standard-basic" label="Session Name" variant="standard" />
      <DomainAccordionSummary domainList={domainList} domainChoice={domainChoice} setDomainChoice={setDomainChoice} />
      <ImageAccordionSummary imageList={imageList} imageChoice={imageChoice} setImageChoice={setImageChoice} />
      <DataVolAccordionSummary dataVolumeList={dataVolumeList} dataVolumesChoice={dataVolumesChoice} setDataVolumesChoice={setDataVolumesChoice} />
      <UserVolAccordionSummary userVolumeList={userVolumeList} userVolumesChoice={userVolumesChoice} setUserVolumesChoice={setUserVolumesChoice} />
      <Button className="submit-button" type="submit" onClick={submit} variant="contained">
        {loadingSubmit ?
          <CircularProgress color="secondary" />
          :
          'CREATE'
        }
      </Button>
    </div>
    {loadingData &&
      <LoadingAnimation backDropIsOpen={loadingData} />
    }
  </Styled >;
};