import { FC } from 'react';
import { Box, ButtonBase, Menu, MenuItem } from '@mui/material';

/** Pieces shared by the volume list and the folder list. */

type SortHeaderProps = {
  label: string;
  /** Whether the list is sorted by this column. */
  active: boolean;
  direction: 1 | -1;
  onClick: () => void;
};

export const SortHeader: FC<SortHeaderProps> = ({ label, active, direction, onClick }) => (
  <ButtonBase
    onClick={onClick}
    aria-sort={active ? (direction === 1 ? 'ascending' : 'descending') : 'none'}
    sx={{ justifySelf: 'start', gap: 0.5, fontSize: 12, fontWeight: 600, color: 'rgba(0,0,0,0.6)' }}
  >
    {label}
    <span className="material-symbols-outlined" style={{ fontSize: 15 }}>
      {active ? (direction === 1 ? 'arrow_upward' : 'arrow_downward') : 'unfold_more'}
    </span>
  </ButtonBase>
);

export type MenuAction = { label: string; icon: string; color?: string; run: () => void };

type RowMenuProps = {
  anchor: HTMLElement | null;
  items: MenuAction[];
  onClose: () => void;
};

/** The ⋮ menu of a row. Closes before running the chosen action. */
export const RowMenu: FC<RowMenuProps> = ({ anchor, items, onClose }) => (
  <Menu anchorEl={anchor} open={!!anchor} onClose={onClose}>
    {items.map((item) => (
      <MenuItem
        key={item.label}
        onClick={() => {
          onClose();
          item.run();
        }}
        sx={{ gap: 1.5, fontSize: 14, color: item.color }}
      >
        <span className="material-symbols-outlined" style={{ fontSize: 18 }}>{item.icon}</span>
        {item.label}
      </MenuItem>
    ))}
  </Menu>
);

export const EmptyMessage: FC<{ icon: string; title: string; hint?: string }> = ({ icon, title, hint }) => (
  <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 0.75, py: 6, px: 2, color: 'rgba(0,0,0,0.6)', textAlign: 'center' }}>
    <span className="material-symbols-outlined" style={{ fontSize: 36, color: '#B0C1D9' }}>{icon}</span>
    <Box sx={{ fontWeight: 600, color: 'text.primary' }}>{title}</Box>
    {hint && <Box sx={{ fontSize: 13 }}>{hint}</Box>}
  </Box>
);
