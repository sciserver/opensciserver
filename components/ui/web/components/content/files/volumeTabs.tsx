import { FC } from 'react';
import { Box, ButtonBase } from '@mui/material';
import { VolumeType } from 'src/graphql/typings';

type Props = {
  value: VolumeType;
  counts: Record<VolumeType, number | undefined>;
  onChange: (type: VolumeType) => void;
};

const TABS = [
  { type: VolumeType.Uservolume, label: 'User volumes' },
  { type: VolumeType.Datavolume, label: 'Data volumes' }
];

export const VolumeTabs: FC<Props> = ({ value, counts, onChange }) => (
  <Box role="tablist" sx={{ display: 'flex', gap: 0.5, px: 1.5, borderBottom: '1px solid #e6e9ed', flex: 'none' }}>
    {TABS.map(({ type, label }) => {
      const selected = value === type;
      return (
        <ButtonBase
          key={type}
          role="tab"
          aria-selected={selected}
          onClick={() => onChange(type)}
          sx={{
            gap: 1, px: 1.75, pt: 1.5, pb: 1.25, fontSize: 14, fontWeight: 500, borderRadius: 0,
            color: selected ? 'secondary.main' : 'rgba(0,0,0,0.7)',
            borderBottom: `2px solid ${selected ? '#0E3659' : 'transparent'}`
          }}
        >
          {label}
          {counts[type] !== undefined && (
            <Box component="span" sx={{ fontSize: 12, color: 'rgba(0,0,0,0.6)', bgcolor: '#eef1f4', borderRadius: '10px', px: '7px', py: '1px' }}>
              {counts[type]}
            </Box>
          )}
        </ButtonBase>
      );
    })}
  </Box>
);
