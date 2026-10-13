import { FC, useState } from 'react';
import styled from 'styled-components';
import { VolumeType } from 'src/graphql/typings';
import { SortKey, VolumeRow } from 'src/utils/fileVolumes';

import { CircleButton, Icon, listStyles, Tag } from './filesStyles';
import { EmptyMessage, MenuAction, RowMenu, SortHeader } from './listParts';

type Props = {
  volumeType: VolumeType;
  rows: VolumeRow[];
  sortKey: SortKey;
  direction: 1 | -1;
  onSort: (key: SortKey) => void;
  onOpen: (row: VolumeRow) => void;
  /** Row menu (manage mode). Rows with no items get no menu button. */
  menuItems?: (row: VolumeRow) => MenuAction[];
  /** Pick mode: single click selects, double click or the arrow opens. */
  pick?: { selectedKey: string | null; isDisabled: (row: VolumeRow) => boolean; onSelect: (row: VolumeRow) => void };
};

const Styled = styled.div`
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;

  ${listStyles('36px minmax(0,2fr) minmax(0,1fr) minmax(0,1.5fr) 36px')}

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

const VolumeIcon = styled(Icon)`
  color: ${({ theme }) => theme.palette.secondary.main};
`;

const PickIcon = styled(Icon)<{ $selected: boolean }>`
  color: ${({ theme, $selected }) => ($selected ? theme.palette.secondary.main : 'rgba(0,0,0,0.5)')};
`;

export const VolumeList: FC<Props> = ({ volumeType, rows, sortKey, direction, onSort, onOpen, menuItems, pick }) => {
  const [menu, setMenu] = useState<{ anchor: HTMLElement; row: VolumeRow } | null>(null);
  const isData = volumeType === VolumeType.Datavolume;
  const headers: { key: SortKey; label: string }[] = [
    { key: 'name', label: 'Name' },
    { key: 'rootVolume', label: isData ? 'Access' : 'Root volume' },
    { key: 'detail', label: isData ? 'Description' : 'Owner' }
  ];

  return (
    <Styled>
      <div className="list-header" role="row">
        <span />
        {headers.map(({ key, label }) => (
          <SortHeader key={key} label={label} active={sortKey === key} direction={direction} onClick={() => onSort(key)} />
        ))}
        <span />
      </div>
      <div className="list-body">
        {rows.map((row) => {
          const disabled = !!pick?.isDisabled(row);
          const selected = pick?.selectedKey === row.key;
          const dim = disabled ? ' dim' : '';
          const activate = () => (pick ? !disabled && pick.onSelect(row) : onOpen(row));
          const volumeIcon = <VolumeIcon $size={22} $filled>{isData ? 'database' : row.shared ? 'folder_shared' : 'folder'}</VolumeIcon>;
          return (
            <div
              key={row.key}
              role="row"
              className={`list-row${disabled ? ' disabled' : ' clickable'}${selected ? ' selected' : ''}`}
              title={disabled ? 'Read-only: not available here' : undefined}
              tabIndex={disabled ? -1 : 0}
              onClick={activate}
              onKeyDown={(event) => event.key === 'Enter' && event.target === event.currentTarget && activate()}
              onDoubleClick={() => pick && !disabled && onOpen(row)}
            >
              <div className={`icon-cell${dim}`}>
                {pick ? <PickIcon $selected={!!selected}>{disabled ? '' : selected ? 'radio_button_checked' : 'radio_button_unchecked'}</PickIcon> : volumeIcon}
              </div>
              <div className={`name-cell${dim}`}>
                {pick && volumeIcon}
                <span className="name-text">{row.name}</span>
                {!row.writable && !isData && <Tag>Read-only</Tag>}
                {row.shared && <Tag>Shared</Tag>}
              </div>
              <span className={`text-cell${dim}`}>{row.rootVolume}</span>
              <span className={`text-cell${dim}`}>{row.detail}</span>
              {pick && !disabled ? (
                <CircleButton
                  aria-label={`Open ${row.name}`}
                  onClick={(event) => {
                    event.stopPropagation();
                    onOpen(row);
                  }}
                >
                  <Icon>chevron_right</Icon>
                </CircleButton>
              ) : null}
              {!pick && menuItems && menuItems(row).length > 0 && (
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
          );
        })}
        {rows.length === 0 && <EmptyMessage icon="folder_open" title="No volumes found" hint="Try a different filter." />}
      </div>
      <RowMenu anchor={menu?.anchor ?? null} items={menu && menuItems ? menuItems(menu.row) : []} onClose={() => setMenu(null)} />
    </Styled>
  );
};
