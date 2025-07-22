// eslint-disable-next-line eslint-comments/disable-enable-pair
/* eslint-disable import/no-extraneous-dependencies */
import { FC, useContext, useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/router';
import { useLazyQuery } from '@apollo/client';
import { compact } from 'lodash';
import { sanitize } from 'dompurify';
import { useInterval } from 'usehooks-ts';

import styled from 'styled-components';
import Swal from 'sweetalert2';

import { AppContext, UserContext } from 'context';
import { GET_CONTAINER_ID, PING_CONTAINER } from 'src/graphql/containers';

import { appBarHeight, drawerClosedWidth, drawerOpenWidth } from 'components/common/drawer';
import { LoadingAnimation } from 'components/common/loadingAnimation';
import { netMargin } from 'components/common/layout';


const Styled = styled.div`
  margin-top: -20px;
  margin-left: -${netMargin}px;

  iframe {
    border: none;
  }

  .MuiBackdrop-root {
    z-index: ${({ theme }) => theme.zIndex.drawer + 1};
    background-color: rgb(0,0,0,0.3);
  }

  .loading-div {
    margin-top: -150px;
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .loading {
    font-weight: bold;
    display:inline-block;
    font-family: monospace;
    font-size: 30px;
    clip-path: inset(0 3ch 0 0);
    animation: l 1s steps(4) infinite;
  }
  
  @keyframes l {
    to {
      clip-path: inset(0 -1ch 0 0)
    }
  }
`;


export const ContainerRun: FC = ({ }) => {

  const router = useRouter();

  const query = router.query;

  const [containerId, setContainerId] = useState<string>();

  const userInactivityTimeOut = Number.parseInt(process.env.NEXT_PUBLIC_COMPUTE_USER_INACTIVITY_TIMEOUT || '2400000'); // User inactivity 40 minutes
  const [pingInterval, setPingInterval] = useState<number | null>(Number.parseInt(process.env.NEXT_PUBLIC_COMPUTE_PING_INTERVAL || '300000')); //Try pinging container every 5 minutes

  const [userLastActivity, setUserLastActivity] = useState<number>(Date.now());

  const [getContainerID, { loading: loadingContainerID, data: dataContainerID, error: errorContainerID }] =
    useLazyQuery(GET_CONTAINER_ID);

  const [pingContainer] = useLazyQuery(PING_CONTAINER, { fetchPolicy: 'network-only', nextFetchPolicy: 'network-only' });

  const { token } = useContext(UserContext);
  const { drawerOpen, setMenuOption } = useContext(AppContext);

  const [path, setPath] = useState<string>('/');
  const [backDropIsOpen, setBackDropIsOpen] = useState<boolean>(false);
  const [loadingIframe, setLoadingIframe] = useState<boolean>(false);

  const [iframeWidth, setIframeWidth] = useState<number>(0);
  const [iframeHeight, setIframeHeight] = useState<number>(0);


  const handleWindowResize = () => {
    setIframeWidth(window.innerWidth - (drawerOpen ? drawerOpenWidth : drawerClosedWidth));
    setIframeHeight(window.innerHeight - appBarHeight);
  };

  // ON MOUNT: UI config
  useEffect(() => {
    window.addEventListener('resize', handleWindowResize);
    setMenuOption('compute');
    setIframeWidth(window.innerWidth - (drawerOpen ? drawerOpenWidth : drawerClosedWidth));
    setIframeHeight(window.innerHeight - appBarHeight);
  }, []);

  // Listens for drawerOpen state variable and adjusts width when it changes
  useEffect(() => {
    handleWindowResize();
  }, [drawerOpen]);

  // ON MOUNT: API calling
  useEffect(() => {
    if (!router.isReady) {
      return;
    }

    setBackDropIsOpen(true);
    let { img, dom, dvs, uvs, p } = query;

    img = sanitize(img as string);
    dom = sanitize(dom as string);
    dvs = compact(sanitize((dvs as string))?.split(',') || []);
    uvs = compact(sanitize((uvs as string))?.split(',') || []);

    if (p) {
      p = sanitize(p as string);
      setPath(p);
    }

    getContainerID({
      variables: {
        containerParams:
        {
          imageName: img,
          domainName: dom,
          dataVolumeIds: dvs,
          userVolumeIds: uvs
        }
      }
    });


  }, [router]);

  const showReloadModal = () => {
    Swal.fire({
      title: 'Session Expired',
      text: 'Your session has expired. In order to continue, you need to reload this page. Please refresh the page to resume your session.',
      icon: 'warning',
      confirmButtonText: 'Reload',
      allowOutsideClick: false,
      allowEscapeKey: false
    }).then(() => {
      router.reload();
      return;
    }).catch(Error);
  };

  // Listening for errors in the getContainerID query
  useEffect(() => {
    if (errorContainerID) {
      Swal.fire({
        icon: 'error',
        title: 'Error creating container',
        text: errorContainerID.message
      });
    }
  }, [errorContainerID]);

  const checkTimeSinceLastActive = async () => {

    console.log(`Checking activity @${new Date()}`);
    const timeSinceLastActivity = Date.now() - userLastActivity;

    if (pingInterval && timeSinceLastActivity < pingInterval) {
      console.log(`Recent activity detected for container ${containerId}: last active ${timeSinceLastActivity} ms ago, interval ${pingInterval} ms`);
      pingContainer({ variables: { containerId } });
      return;
    }
    console.log(`No user activity detected in the past ${pingInterval! / 60_000} minutes`);
    if (timeSinceLastActivity >= userInactivityTimeOut) {
      // pingInterval set to null to stop interval 
      setPingInterval(null);
      showReloadModal();
    }
  };

  // Javascript's built in setInterval function doesn't work with React's 
  // rendering strategies. useInterval React hook is used instead.
  useInterval(() => checkTimeSinceLastActive(), pingInterval);

  const trackUserActivity = () => {
    const iframe: HTMLIFrameElement | null = document.querySelector('#container-iframe');
    if (iframe) {
      iframe.contentWindow!.addEventListener('mousemove', () => setUserLastActivity(Date.now()));
      iframe.contentWindow!.addEventListener('keydown', () => setUserLastActivity(Date.now()));
    }
    addEventListener('mousemove', () => setUserLastActivity(Date.now()));
    addEventListener('keydown', () => setUserLastActivity(Date.now()));
  };

  const containerURL = useMemo<string>(() => {
    if (dataContainerID) {
      setContainerId(dataContainerID.getContainerID);
      setLoadingIframe(true);

      const url = new URL(`${process.env.NEXT_PUBLIC_NOTEBOOKS_URL}/go`);
      url.searchParams.append('id', dataContainerID.getContainerID);
      url.searchParams.append('path', path);
      url.searchParams.append('token', token);

      return decodeURIComponent(url.toString());
    }

    return '';
  }, [dataContainerID]);

  useEffect(() => {
    if (containerURL) {
      trackUserActivity();
    }
  }, [containerURL]);

  return <Styled>
    <iframe
      id="container-iframe"
      src={containerURL}
      title="Container rendering"
      width={iframeWidth}
      height={iframeHeight}
      onLoad={() => setLoadingIframe(false)}
    />
    {loadingContainerID || loadingIframe &&
      <LoadingAnimation backDropIsOpen={backDropIsOpen} />
    }
  </Styled>;
};