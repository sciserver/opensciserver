import { FC } from 'react';
import styled from 'styled-components';
import { ButtonBase } from '@mui/material';
import { VolumeType } from 'src/graphql/typings';

import { BORDER, MUTED } from './filesStyles';

type Props = {
  value: VolumeType;
  counts: Record<VolumeType, number | undefined>;
  onChange: (type: VolumeType) => void;
};

const TABS = [
  { type: VolumeType.Uservolume, label: 'User volumes' },
  { type: VolumeType.Datavolume, label: 'Data volumes' }
];

const Styled = styled.div`
  display: flex;
  gap: 4px;
  padding: 0 12px;
  border-bottom: 1px solid ${BORDER};
  flex: none;
`;

const Tab = styled(ButtonBase)<{ $selected: boolean }>`
  && {
    gap: 8px;
    padding: 12px 14px 10px;
    font-size: 14px;
    font-weight: 500;
    border-radius: 0;
    color: ${({ theme, $selected }) => ($selected ? theme.palette.secondary.main : 'rgba(0,0,0,0.7)')};
    border-bottom: 2px solid ${({ theme, $selected }) => ($selected ? theme.palette.primary.main : 'transparent')};
  }

  .count {
    font-size: 12px;
    color: ${MUTED};
    background: #eef1f4;
    border-radius: 10px;
    padding: 1px 7px;
  }
`;

export const VolumeTabs: FC<Props> = ({ value, counts, onChange }) => (
  <Styled role="tablist">
    {TABS.map(({ type, label }) => (
      <Tab key={type} role="tab" aria-selected={value === type} $selected={value === type} onClick={() => onChange(type)}>
        {label}
        {counts[type] !== undefined && <span className="count">{counts[type]}</span>}
      </Tab>
    ))}
  </Styled>
);
