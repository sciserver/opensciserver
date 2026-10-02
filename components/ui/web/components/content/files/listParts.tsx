import { FC } from 'react';
import styled from 'styled-components';
import { ButtonBase, Menu, MenuItem } from '@mui/material';

import { Icon, MUTED } from './filesStyles';

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

export type MenuAction = { label: string; icon: string; color?: string; run: () => void };

type RowMenuProps = {
  anchor: HTMLElement | null;
  items: MenuAction[];
  onClose: () => void;
};

// The menu is rendered in a portal, so it is styled directly rather than from a parent wrapper.
const Item = styled(MenuItem)<{ $color?: string }>`
  && {
    gap: 12px;
    font-size: 14px;
    ${({ $color }) => $color && `color: ${$color};`}
  }
`;

/** The ⋮ menu of a row. Closes before running the chosen action. */
export const RowMenu: FC<RowMenuProps> = ({ anchor, items, onClose }) => (
  <Menu anchorEl={anchor} open={!!anchor} onClose={onClose}>
    {items.map((item) => (
      <Item
        key={item.label}
        $color={item.color}
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
    <Icon $size={36} $color="#B0C1D9">{icon}</Icon>
    <div className="title">{title}</div>
    {hint && <div className="hint">{hint}</div>}
  </Empty>
);
