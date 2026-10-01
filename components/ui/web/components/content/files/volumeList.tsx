import { FC } from 'react';
import { Box, ButtonBase } from '@mui/material';
import { VolumeType } from 'src/graphql/typings';
import { SortKey, VolumeRow } from 'src/utils/fileVolumes';

type Props = {
  volumeType: VolumeType;
  rows: VolumeRow[];
  sortKey: SortKey;
  direction: 1 | -1;
  onSort: (key: SortKey) => void;
  onOpen: (row: VolumeRow) => void;
  /** Pick mode: single click selects, double click or the arrow opens. */
  pick?: { selectedKey: string | null; isDisabled: (row: VolumeRow) => boolean; onSelect: (row: VolumeRow) => void };
};

const COLUMNS = '36px minmax(0,2fr) minmax(0,1fr) minmax(0,1.5fr) 36px';
const ROW_HEIGHT = 38;

const Tag: FC<{ label: string }> = ({ label }) => (
  <Box component="span" sx={{ flex: 'none', fontSize: 11, px: 0.75, py: '1px', borderRadius: '3px', bgcolor: '#eef1f4', color: 'rgba(0,0,0,0.62)' }}>{label}</Box>
);

export const VolumeList: FC<Props> = ({ volumeType, rows, sortKey, direction, onSort, onOpen, pick }) => {
  const isData = volumeType === VolumeType.Datavolume;
  const headers: { key: SortKey; label: string }[] = [
    { key: 'name', label: 'Name' },
    { key: 'rootVolume', label: isData ? 'Access' : 'Root volume' },
    { key: 'detail', label: isData ? 'Description' : 'Owner' }
  ];

  return (
    <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
      <Box role="row" sx={{ display: 'grid', gridTemplateColumns: COLUMNS, alignItems: 'center', px: 1, height: 34, borderBottom: '1px solid #e6e9ed', flex: 'none' }}>
        <span />
        {headers.map(({ key, label }) => (
          <ButtonBase
            key={key}
            onClick={() => onSort(key)}
            aria-sort={sortKey === key ? (direction === 1 ? 'ascending' : 'descending') : 'none'}
            sx={{ justifySelf: 'start', gap: 0.5, fontSize: 12, fontWeight: 600, color: 'rgba(0,0,0,0.6)' }}
          >
            {label}
            <span className="material-symbols-outlined" style={{ fontSize: 15 }}>
              {sortKey === key ? (direction === 1 ? 'arrow_upward' : 'arrow_downward') : 'unfold_more'}
            </span>
          </ButtonBase>
        ))}
        <span />
      </Box>
      <Box sx={{ flex: 1, minHeight: 0, overflow: 'auto' }}>
        {rows.map((row) => {
          const disabled = !!pick?.isDisabled(row);
          const selected = pick?.selectedKey === row.key;
          return (
            <Box
              key={row.key}
              role="row"
              title={disabled ? 'Read-only: not available here' : undefined}
              onClick={() => (pick ? !disabled && pick.onSelect(row) : onOpen(row))}
              onDoubleClick={() => pick && !disabled && onOpen(row)}
              sx={{ display: 'grid', gridTemplateColumns: COLUMNS, alignItems: 'center', px: 1, minHeight: ROW_HEIGHT, borderBottom: '1px solid #f0f2f4', cursor: disabled ? 'default' : 'pointer', userSelect: 'none', bgcolor: selected ? 'rgba(57,140,191,0.16)' : 'transparent', '&:hover': { bgcolor: disabled ? undefined : 'rgba(57,140,191,0.08)' } }}
            >
              <Box sx={{ opacity: disabled ? 0.42 : 1, display: 'flex' }}>
                {pick ? (
                  <span className="material-symbols-outlined" style={{ fontSize: 20, color: selected ? '#398CBF' : 'rgba(0,0,0,0.5)' }}>
                    {disabled ? '' : selected ? 'radio_button_checked' : 'radio_button_unchecked'}
                  </span>
                ) : (
                  <span className="material-symbols-outlined" style={{ fontSize: 22, color: '#398CBF', fontVariationSettings: '\'FILL\' 1' }}>
                    {isData ? 'database' : row.shared ? 'folder_shared' : 'folder'}
                  </span>
                )}
              </Box>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25, minWidth: 0, fontSize: 14, fontWeight: 500, opacity: disabled ? 0.42 : 1 }}>
                {pick && (
                  <span className="material-symbols-outlined" style={{ fontSize: 22, color: '#398CBF', fontVariationSettings: '\'FILL\' 1' }}>
                    {isData ? 'database' : row.shared ? 'folder_shared' : 'folder'}
                  </span>
                )}
                <Box component="span" sx={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{row.name}</Box>
                {!row.writable && !isData && <Tag label="Read-only" />}
                {row.shared && <Tag label="Shared" />}
              </Box>
              <Box component="span" sx={{ fontSize: 13, color: 'rgba(0,0,0,0.62)', opacity: disabled ? 0.42 : 1 }}>{row.rootVolume}</Box>
              <Box component="span" sx={{ fontSize: 13, color: 'rgba(0,0,0,0.62)', opacity: disabled ? 0.42 : 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{row.detail}</Box>
              {pick && !disabled ? (
                <ButtonBase aria-label={`Open ${row.name}`} onClick={(event) => {
                  event.stopPropagation(); onOpen(row); 
                }} sx={{ width: 32, height: 32, borderRadius: '50%', color: 'rgba(0,0,0,0.6)' }}>
                  <span className="material-symbols-outlined" style={{ fontSize: 20 }}>chevron_right</span>
                </ButtonBase>
              ) : <span />}
            </Box>
          );
        })}
        {rows.length === 0 && (
          <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 0.75, py: 6, px: 2, color: 'rgba(0,0,0,0.6)', textAlign: 'center' }}>
            <span className="material-symbols-outlined" style={{ fontSize: 36, color: '#B0C1D9' }}>folder_open</span>
            <Box sx={{ fontWeight: 600, color: 'text.primary' }}>No volumes found</Box>
            <Box sx={{ fontSize: 13 }}>Try a different filter.</Box>
          </Box>
        )}
      </Box>
    </Box>
  );
};
