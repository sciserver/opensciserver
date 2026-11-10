import { FC, useState } from 'react';
import styled from 'styled-components';
import { ButtonBase } from '@mui/material';
import { VolumeType } from 'src/graphql/typings';
import { SortKey, VolumeRow } from 'src/types/files';

import { ACCENT, CircleButton, Icon, listStyles, MUTED_LIGHT, SUCCESS, Tag } from './filesStyles';
import { AccessIcons, EmptyMessage, MenuAction, RowMenu, SortHeader } from './listParts';

type Props = {
  volumeType: VolumeType;
  rows: VolumeRow[];
  sortKey: SortKey;
  direction: 1 | -1;
  onSort: (key: SortKey) => void;
  /** Clicking a volume opens it, in both modes. */
  onOpen: (row: VolumeRow) => void;
  /** Row menu (manage mode). Rows with no items get no menu button. */
  menuItems?: (row: VolumeRow) => MenuAction[];
  /** Pick mode: the circle in front of a volume selects it; clicking the volume still opens it. */
  pick?: { selectedKey: string | null; isDisabled: (row: VolumeRow) => boolean; onSelect: (row: VolumeRow) => void };
};

// Controls (select, actions) come first, on the left next to where the pointer starts; information follows on the right.
const columnsFor = (isData: boolean, isPick: boolean): string => {
  const controls = isPick ? '36px' : '36px 36px';
  const info = isData ? 'minmax(0,2fr) minmax(0,3fr) 112px' : 'minmax(0,2fr) minmax(0,1fr) minmax(0,1fr) minmax(0,2fr) 112px';
  return `${controls} ${info}`;
};

const Styled = styled.div<{ $columns: string }>`
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;

  ${({ $columns }) => listStyles($columns)}

  .list-row.disabled {
    cursor: default;
  }

  .icon-cell {
    display: flex;
  }

  .name-cell {
    font-weight: 500;
    gap: 10px;
  }
`;

const VolumeIcon = styled(Icon)<{ $sharedWithMe?: boolean }>`
  color: ${({ $sharedWithMe }) => ($sharedWithMe ? SUCCESS : ACCENT)};
`;

const Radio = styled(ButtonBase)<{ $selected: boolean }>`
  && {
    color: ${({ $selected }) => ($selected ? ACCENT : MUTED_LIGHT)};
  }
`;

export const VolumeList: FC<Props> = ({ volumeType, rows, sortKey, direction, onSort, onOpen, menuItems, pick }) => {
  const [menu, setMenu] = useState<{ anchor: HTMLElement; row: VolumeRow } | null>(null);
  const isData = volumeType === VolumeType.Datavolume;
  const sortable: { key: SortKey; label: string }[] = isData
    ? [{ key: 'name', label: 'Name' }, { key: 'description', label: 'Description' }]
    : [{ key: 'name', label: 'Name' }, { key: 'rootVolume', label: 'Root volume' }, { key: 'owner', label: 'Owner' }, { key: 'description', label: 'Description' }];

  return (
    <Styled $columns={columnsFor(isData, !!pick)}>
      <div className="list-header" role="row">
        <span />
        {!pick && <span />}
        {sortable.map(({ key, label }) => (
          <SortHeader key={key} label={label} active={sortKey === key} direction={direction} onClick={() => onSort(key)} />
        ))}
        <span className="header-label">Access</span>
      </div>
      <div className="list-body">
        {rows.map((row) => {
          const disabled = !!pick?.isDisabled(row);
          const selected = pick?.selectedKey === row.key;
          const dim = disabled ? ' dim' : '';
          const icon = isData ? 'database' : row.shared || row.sharedWithMe ? 'folder_shared' : 'folder';
          const volumeIcon = <VolumeIcon $size={22} $filled $sharedWithMe={row.sharedWithMe}>{icon}</VolumeIcon>;
          const open = () => !disabled && onOpen(row);
          const items = !pick && menuItems ? menuItems(row) : [];
          return (
            <div
              key={row.key}
              role="row"
              className={`list-row${disabled ? ' disabled' : ' clickable'}${selected ? ' selected' : ''}`}
              title={disabled ? 'Read-only: not available here' : undefined}
              tabIndex={disabled ? -1 : 0}
              onClick={open}
              onKeyDown={(event) => event.key === 'Enter' && event.target === event.currentTarget && open()}
            >
              {pick ? (
                <div className={`icon-cell${dim}`}>
                  {!disabled && (
                    <Radio
                      $selected={!!selected}
                      aria-label={`Select ${row.name}`}
                      aria-pressed={!!selected}
                      onClick={(event) => {
                        event.stopPropagation();
                        pick.onSelect(row);
                      }}
                    >
                      <Icon>{selected ? 'radio_button_checked' : 'radio_button_unchecked'}</Icon>
                    </Radio>
                  )}
                </div>
              ) : (
                <>
                  <div className="icon-cell">{volumeIcon}</div>
                  <div className="icon-cell">
                    {items.length > 0 && (
                      <CircleButton
                        aria-label={`More actions for ${row.name}`}
                        onClick={(event) => {
                          event.stopPropagation();
                          setMenu({ anchor: event.currentTarget, row });
                        }}
                      >
                        <Icon>more_vert</Icon>
                      </CircleButton>
                    )}
                  </div>
                </>
              )}
              <div className={`name-cell${dim}`}>
                {pick && volumeIcon}
                <span className="name-text">{row.name}</span>
                {row.shared && <Tag>Shared</Tag>}
                {row.sharedWithMe && <Tag>Shared with you</Tag>}
              </div>
              {!isData && <span className={`text-cell${dim}`}>{row.rootVolume}</span>}
              {!isData && <span className={`text-cell${dim}`}>{row.owner}</span>}
              <span className={`text-cell${dim}`} title={row.description}>{row.description}</span>
              <span className={dim.trim()}><AccessIcons actions={row.actions} /></span>
            </div>
          );
        })}
        {rows.length === 0 && <EmptyMessage icon="folder_open" title="No volumes found" hint="Try a different filter." />}
      </div>
      <RowMenu anchor={menu?.anchor ?? null} items={menu ? menuItems?.(menu.row) ?? [] : []} onClose={() => setMenu(null)} />
    </Styled>
  );
};
