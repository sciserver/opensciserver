import { FC, useContext, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import styled from 'styled-components';
import { Alert } from '@mui/material';

import { AppContext } from 'context';
import { DrawerNav, useRailWidth } from 'components/common/drawer';

export type DrawerOption = {
  name: string
  value: string
  onClick: () => void
  icon: JSX.Element
}

type Props = {
  children: any
}

type StyleProps = {
  railWidth: number
  compact: boolean
}

export const netMargin = 50;
// Gap between the rail and the page content. Compact screens have no rail, so only keep room for the menu button.
const compactTopOffset = 56;
const compactMargin = 16;

// CSS variable holding the rendered height of the banner (0px when hidden). Views that need
// to fill the viewport (e.g. the compute session iframe) subtract it from their height.
export const bannerHeightVar = '--banner-height';

export const railWidthVar = '--rail-width';
export const contentMarginVar = '--content-margin';

const bannerDismissedKey = 'betaBannerDismissed';

const Styled = styled.div`
  /* Published for full-height views (e.g. the compute session iframe) */
  ${railWidthVar}: ${(props: StyleProps) => props.railWidth}px;
  ${contentMarginVar}: ${(props: StyleProps) => props.compact ? compactMargin : props.railWidth + netMargin}px;

  padding-top: ${(props: StyleProps) => props.compact ? compactTopOffset : 0}px;

  main {
    margin-left: var(${contentMarginVar});
  }

  .alert {
    margin-left: var(${railWidthVar});
  }
`;

export const Layout: FC<Props> = ({ children }) => {

  const { showBanner } = useContext(AppContext);
  const { compact, width: railWidth } = useRailWidth();

  const containerRef = useRef<HTMLDivElement>(null);
  const bannerRef = useRef<HTMLDivElement>(null);
  const [dismissed, setDismissed] = useState<boolean>(false);

  // Remember the dismissal across visits. Storage may be unavailable (private mode, blocked site data).
  useEffect(() => {
    try {
      setDismissed(window.localStorage.getItem(bannerDismissedKey) === 'true');
    }
    catch {
      // keep banner visible
    }
  }, []);

  const handleClose = () => {
    setDismissed(true);
    try {
      window.localStorage.setItem(bannerDismissedKey, 'true');
    }
    catch {
      // dismissal only lasts for this page view
    }
  };

  const bannerVisible = showBanner && !dismissed;

  // Publish the banner's height (excluding the app bar offset) so full-height views can size
  // themselves with CSS. The banner wraps on narrow screens, so observe it.
  useEffect(() => {
    const container = containerRef.current;
    if (!container) {
      return;
    }
    const banner = bannerRef.current;
    if (!bannerVisible || !banner) {
      container.style.setProperty(bannerHeightVar, '0px');
      return;
    }
    const update = () => container.style.setProperty(bannerHeightVar, `${banner.offsetHeight}px`);
    update();
    const observer = new ResizeObserver(update);
    observer.observe(banner);
    return () => observer.disconnect();
  }, [bannerVisible]);

  return (
    <Styled {...{ railWidth, compact }} ref={containerRef}>
      <DrawerNav />
      {bannerVisible &&
        <Alert ref={bannerRef} className="alert" severity="warning" onClose={handleClose}>
          This is a beta release of SciServer V3.0. We're actively improving it and adding new features.
          You can still access the current version <Link href={process.env.NEXT_PUBLIC_NOTEBOOKS_URL || ''} target="_blank">here</Link>. If you have any feedback, please let us know by adding an issue to our <Link rel="noopener noreferrer" target="_blank" href="https://github.com/sciserver/opensciserver/issues">Github Repo</Link> or
          contacting us at <Link href={`mailto:${process.env.NEXT_PUBLIC_HELPDESK_EMAIL}`}>{process.env.NEXT_PUBLIC_HELPDESK_EMAIL}</Link>.
        </Alert>
      }
      <main>{children}</main>
    </Styled>
  );
};
