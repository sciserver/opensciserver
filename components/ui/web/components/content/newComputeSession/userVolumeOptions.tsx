import { FC, useMemo, useState } from 'react';
import styled from 'styled-components';

import { UserVolume } from 'src/graphql/typings';
import { OptionCard } from 'components/common/optionCard';
import { Divider } from '@mui/material';
import { SearchBar } from 'components/common/search';
import { textSearch } from 'src/utils/search';
import { remove } from 'lodash';

const Styled = styled.div`
`;

type Props = {
  userVolumeList: UserVolume[];
  userVolumesChoice: UserVolume[];
  setUserVolumesChoice: (userVols: UserVolume[]) => void;
};

export const UserVolumeOptions: FC<Props> = ({ userVolumeList, userVolumesChoice, setUserVolumesChoice }) => {
  const [filteredUserVols, setFilteredUserVols] = useState<UserVolume[]>(userVolumeList);

  const filteredOwned = useMemo<UserVolume[]>(() => {
    return filteredUserVols.filter(i => i.owner === 'jjaime');
  }, [filteredUserVols]);
  const filteredShared = useMemo<UserVolume[]>(() => {
    return filteredUserVols.filter(i => i.owner !== 'jjaime');
  }, [filteredUserVols]);

  const searchDatasetParams = (uv: UserVolume, input: string) => {
    return textSearch(uv.description || '', input)
      || textSearch(uv.name || '', input);
  };

  const onSearch = (input: string) => {
    const tempDVs = userVolumeList.filter(uv => searchDatasetParams(uv, input));
    setFilteredUserVols(tempDVs);
  };

  const handleOnClickOption = (uv: UserVolume) => {
    const uvs = userVolumesChoice;
    const tempUv = uvs.find(i => i && i.id === uv.id);

    if (tempUv) {
      remove(uvs, (i) => i.id === uv.id);
      setUserVolumesChoice([...uvs]);
      return;
    }
    uvs.push(uv);
    setUserVolumesChoice([...uvs]);
  };

  return <Styled>
    <SearchBar placeholder="Search user volumes" onChangeParam={onSearch} />
    {filteredOwned.length > 0 &&
      <>
        <h3>Owned by me</h3>
        <section>
          {filteredOwned.map(uv =>
            <article key={uv.id}>
              <OptionCard
                selected={userVolumesChoice.some(dvc => dvc.id === uv.id)}
                title={`${uv.name} (${uv.owner})`}
                description={uv.description || 'No description available'}
                action={() => handleOnClickOption(uv)}
              />
            </article>
          )}
        </section>
        <Divider />
      </>
    }
    {filteredShared.length > 0 &&
      <>
        <h3>Shared</h3>
        <section>
          {filteredShared.map(uv =>
            <article key={uv.id}>
              <OptionCard
                selected={userVolumesChoice.some(dvc => dvc.id === uv.id)}
                title={`${uv.name} (${uv.owner})`}
                description={uv.description || 'No description available'}
                action={() => handleOnClickOption(uv)}
              />
            </article>
          )}
        </section>
        <Divider />
      </>
    }
    <h3>All</h3>
    <section>
      {filteredUserVols.map(uv =>
        <article key={uv.id}>
          <OptionCard
            title={`${uv.name} (${uv.owner})`}
            description={uv.description || 'No description available'}
            action={() => handleOnClickOption(uv)}
          />
        </article>
      )}
    </section>
  </Styled>;
};