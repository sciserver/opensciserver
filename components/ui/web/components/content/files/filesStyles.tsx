import styled, { css } from 'styled-components';
import { ButtonBase } from '@mui/material';

/** Colours and small building blocks shared by the Files components. Theme colours come from the styled-components theme. */
export const BORDER = '#e6e9ed';
export const ROW_BORDER = '#f0f2f4';
export const MUTED = 'rgba(0,0,0,0.6)';
export const ROW_HOVER = 'rgba(57,140,191,0.08)';
export const ROW_SELECTED = 'rgba(57,140,191,0.16)';
export const DANGER = '#C62828';

type IconProps = { $size?: number; $color?: string; $filled?: boolean };

/** A Material Symbols icon. Pass the icon name as the child. */
export const Icon = styled.span.attrs({ className: 'material-symbols-outlined' })<IconProps>`
  /* Raised specificity: MUI sizes the icon inside a Button's startIcon with a selector that would otherwise win. */
  &&& {
    font-size: ${({ $size }) => $size ?? 20}px;
  }
  ${({ $color }) => $color && `color: ${$color};`}
  ${({ $filled }) => $filled && 'font-variation-settings: \'FILL\' 1;'}
`;

/** A round icon-only button. */
export const CircleButton = styled(ButtonBase)<{ $size?: number }>`
  && {
    width: ${({ $size }) => $size ?? 32}px;
    height: ${({ $size }) => $size ?? 32}px;
    border-radius: 50%;
    color: ${MUTED};
  }
`;

/** The small grey tag next to a name ("Shared", "Read-only"). */
export const Tag = styled.span`
  flex: none;
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 3px;
  background: #eef1f4;
  color: rgba(0, 0, 0, 0.62);
`;

/**
 * Layout shared by the volume list and the folder list: a header row and rows on the same grid.
 * Use inside a styled wrapper: `${listStyles('36px 1fr 36px')}`.
 */
export const listStyles = (columns: string) => css`
  .list-header {
    display: grid;
    grid-template-columns: ${columns};
    align-items: center;
    padding: 0 8px;
    height: 34px;
    border-bottom: 1px solid ${BORDER};
    flex: none;
  }

  .list-body {
    flex: 1;
    min-height: 0;
    overflow: auto;
  }

  .list-row {
    position: relative;
    display: grid;
    grid-template-columns: ${columns};
    align-items: center;
    padding: 0 8px;
    min-height: 38px;
    border-bottom: 1px solid ${ROW_BORDER};
    user-select: none;
  }

  .list-row.clickable {
    cursor: pointer;
  }

  .list-row.clickable:hover {
    background: ${ROW_HOVER};
  }

  .list-row.checked {
    background: rgba(57, 140, 191, 0.07);
  }

  .list-row.selected {
    background: ${ROW_SELECTED};
  }

  .name-cell {
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 0;
    font-size: 14px;
  }

  .name-text {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .text-cell {
    font-size: 13px;
    color: rgba(0, 0, 0, 0.62);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .dim {
    opacity: 0.42;
  }
`;
