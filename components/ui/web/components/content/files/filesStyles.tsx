import styled, { css } from 'styled-components';
import { ButtonBase } from '@mui/material';

type Themed = { theme: { palette: Record<string, Record<string, string>> } };

/** A colour from the theme (theme.json). Use it in a styled template: `color: ${MUTED};`. */
const token = (group: string, key: string) => ({ theme }: Themed): string => theme.palette[group][key];

/** Colours of the Files screens, all from the `neutral` group of the theme except the ones named after a palette entry. */
export const BORDER = token('neutral', 'border');
export const ROW_BORDER = token('neutral', 'borderLight');
export const MUTED = token('neutral', 'muted');
export const MUTED_LIGHT = token('neutral', 'mutedLight');
export const FAINT = token('neutral', 'faint');
export const SURFACE = token('neutral', 'surface');
export const SURFACE_ALT = token('neutral', 'surfaceAlt');
export const SURFACE_SUBTLE = token('neutral', 'surfaceSubtle');
export const CHIP = token('neutral', 'chip');
export const SELECTION_BAR = token('neutral', 'selectionBar');
export const DROP_ZONE = token('neutral', 'dropZone');
export const INPUT_BORDER = token('neutral', 'inputBorder');
export const CARD_BORDER = token('neutral', 'borderStrong');
export const ROW_HOVER = token('neutral', 'rowHover');
export const ROW_SELECTED = token('neutral', 'rowSelected');
export const ACCENT_WASH = token('neutral', 'accentWash');
export const FOCUS_RING = token('neutral', 'focusRing');
export const DANGER = token('error', 'dark');
export const ACCENT = token('secondary', 'main');
export const PRIMARY = token('primary', 'main');
export const SUCCESS = token('contrast2', 'main');
export const PAPER = token('background', 'paper');
export const TEXT = token('text', 'primary');
export const SOFT = token('contrast1', 'main');

type IconProps = { $size?: number; $filled?: boolean };

/** A Material Symbols icon. Pass the icon name as the child. */
export const Icon = styled.span.attrs({ className: 'material-symbols-outlined' })<IconProps>`
  /* Raised specificity: MUI sizes the icon inside a Button's startIcon with a selector that would otherwise win. */
  &&& {
    font-size: ${({ $size }) => $size ?? 20}px;
    /* One em square, clipped: before the icon font has loaded (or if it never does) the icon name would otherwise
       print as text and spill over its neighbours. */
    width: 1em;
    height: 1em;
    overflow: hidden;
  }
  ${({ $filled }) => $filled && 'font-variation-settings: \'FILL\' 1;'}
`;

/** Icons in the colours the Files screens use. */
export const AccentIcon = styled(Icon)`
  color: ${ACCENT};
`;
export const MutedIcon = styled(Icon)`
  color: ${MUTED};
`;
export const MutedLightIcon = styled(Icon)`
  color: ${MUTED_LIGHT};
`;
export const FaintIcon = styled(Icon)`
  color: ${FAINT};
`;
export const SoftIcon = styled(Icon)`
  color: ${SOFT};
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
  background: ${CHIP};
  color: ${MUTED};
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
    height: 40px;
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
    background: ${ROW_HOVER};
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
    color: ${MUTED};
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .dim {
    opacity: 0.42;
  }

  .header-label {
    justify-self: start;
    font-size: 12px;
    font-weight: 600;
    color: ${MUTED};
  }
`;
