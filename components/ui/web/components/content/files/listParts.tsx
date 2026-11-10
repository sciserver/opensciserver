import { FC } from 'react';
import styled, { css } from 'styled-components';
import { ButtonBase, Menu, MenuItem } from '@mui/material';

import { VOLUME_ACTION_DISPLAY } from 'src/config/files';
import { VolumeAction } from 'src/types/sharing';

import { DANGER, Icon, MUTED, SoftIcon } from './filesStyles';

/** Pieces shared by the volume list and the folder list. */

type SortHeaderProps = {
  label: string;
  /** Whether the list is sorted by this column. */
  active: boolean;
  direction: 1 | -1;
  onClick: () => void;
};

const SortButton = styled(ButtonBase)`
  && {
    justify-self: start;
    gap: 4px;
    font-size: 12px;
    font-weight: 600;
    color: ${MUTED};
  }
`;

export const SortHeader: FC<SortHeaderProps> = ({ label, active, direction, onClick }) => (
  <SortButton onClick={onClick} aria-sort={active ? (direction === 1 ? 'ascending' : 'descending') : 'none'}>
    {label}
    <Icon $size={15}>{active ? (direction === 1 ? 'arrow_upward' : 'arrow_downward') : 'unfold_more'}</Icon>
  </SortButton>
);

export type MenuAction = { label: string; icon: string; danger?: boolean; run: () => void };

type RowMenuProps = {
  anchor: HTMLElement | null;
  items: MenuAction[];
  onClose: () => void;
};

// The menu is rendered in a portal, so it is styled directly rather than from a parent wrapper.
const Item = styled(MenuItem)<{ $danger?: boolean }>`
  && {
    gap: 12px;
    font-size: 14px;
    ${({ $danger }) => $danger && css`color: ${DANGER};`}
  }
`;

/** The ⋮ menu of a row. Closes before running the chosen action. */
export const RowMenu: FC<RowMenuProps> = ({ anchor, items, onClose }) => (
  <Menu anchorEl={anchor} open={!!anchor} onClose={onClose}>
    {items.map((item) => (
      <Item
        key={item.label}
        $danger={item.danger}
        onClick={() => {
          onClose();
          item.run();
        }}
      >
        <Icon $size={18}>{item.icon}</Icon>
        {item.label}
      </Item>
    ))}
  </Menu>
);

const Empty = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 48px 16px;
  color: ${MUTED};
  text-align: center;

  .title {
    font-weight: 600;
    color: ${({ theme }) => theme.palette.text.primary};
  }

  .hint {
    font-size: 13px;
  }
`;

export const EmptyMessage: FC<{ icon: string; title: string; hint?: string }> = ({ icon, title, hint }) => (
  <Empty>
    <SoftIcon $size={36}>{icon}</SoftIcon>
    <div className="title">{title}</div>
    {hint && <div className="hint">{hint}</div>}
  </Empty>
);

const Access = styled.span`
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: ${MUTED};
`;

/** What the user may do with a volume, as small icons (Read, Write, Grant, Delete) that name themselves on hover. */
export const AccessIcons: FC<{ actions: VolumeAction[] }> = ({ actions }) => (
  <Access role="img" aria-label={actions.length ? `Access: ${actions.map((action) => VOLUME_ACTION_DISPLAY[action].label).join(', ')}` : 'No access'}>
    {actions.map((action) => (
      <Icon key={action} $size={18} title={VOLUME_ACTION_DISPLAY[action].label}>{VOLUME_ACTION_DISPLAY[action].icon}</Icon>
    ))}
  </Access>
);
