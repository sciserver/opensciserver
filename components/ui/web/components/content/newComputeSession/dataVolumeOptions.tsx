import { FC, useMemo, useState } from 'react';
import styled from 'styled-components';

import { DataVolume } from 'src/graphql/typings';
import { OptionCard } from 'components/common/optionCard';
import { Divider } from '@mui/material';
import { SearchBar } from 'components/common/search';
import { textSearch } from 'src/utils/search';
import { remove } from 'lodash';

const Styled = styled.div`
`;

type Props = {
  dataVolumeList: DataVolume[];
  dataVolumesChoice: DataVolume[];
  setDataVolumesChoice: (dataVols: DataVolume[]) => void;
};

export const DataVolumeOptions: FC<Props> = ({ dataVolumeList, dataVolumesChoice, setDataVolumesChoice }) => {

  const [filteredDatavols, setFilteredDatavols] = useState<DataVolume[]>(dataVolumeList);

  const filteredEssentials = useMemo<DataVolume[]>(() => {
    return filteredDatavols.filter(i => i.name.toLowerCase().includes('started'));
  }, [filteredDatavols]);
  const filteredAstronomy = useMemo<DataVolume[]>(() => {
    return filteredDatavols.filter(i => i.name.toLowerCase().includes('astronomy'));
  }, [filteredDatavols]);
  const filteredAstropath = useMemo<DataVolume[]>(() => {
    return filteredDatavols.filter(i => i.name.toLowerCase().includes('astropath'));
  }, [filteredDatavols]);
  const filteredSimulations = useMemo<DataVolume[]>(() => {
    return filteredDatavols.filter(i => i.name.toLowerCase().includes('simulation'));
  }, [filteredDatavols]);
  const filteredSDSS = useMemo<DataVolume[]>(() => {
    return filteredDatavols.filter(i => i.name.toLowerCase().includes('sdss'));
  }, [filteredDatavols]);
  const filteredTurbulence = useMemo<DataVolume[]>(() => {
    return filteredDatavols.filter(i => i.name.toLowerCase().includes('turbulence'));
  }, [filteredDatavols]);

  const searchDatasetParams = (dv: DataVolume, input: string) => {
    return textSearch(dv.description || '', input)
      || textSearch(dv.name || '', input);
  };

  const onSearch = (input: string) => {
    const tempDVs = dataVolumeList.filter(dv => searchDatasetParams(dv, input));
    setFilteredDatavols(tempDVs);
  };

  const handleOnClickOption = (dv: DataVolume) => {
    const dvs = dataVolumesChoice;
    const tempDv = dvs.find(i => i.name === dv.name);

    if (tempDv) {
      remove(dvs, (i) => i.id === tempDv.id);
      setDataVolumesChoice([...dvs]);
      return;
    }

    dvs.push(dv);
    setDataVolumesChoice([...dvs]);
  };

  return <Styled>
    <SearchBar placeholder="Search data volumes" onChangeParam={onSearch} />
    {filteredEssentials.length > 0 &&
      <>
        <h3>Essentials</h3>
        <section>
          {filteredEssentials.map(dv =>
            <article key={dv.id}>
              <OptionCard
                selected={dataVolumesChoice.some(dvc => dvc.id === dv.id)}
                title={dv.name}
                description={dv.description || 'No description available'}
                action={() => handleOnClickOption(dv)}
              />
            </article>
          )}
        </section>
        <Divider />
      </>
    }
    {filteredAstronomy.length > 0 &&
      <>
        <h3>Astronomy</h3>
        <section>
          {filteredAstronomy.map(dv =>
            <article key={dv.id}>
              <OptionCard
                selected={dataVolumesChoice.some(dvc => dvc.id === dv.id)}
                title={dv.name}
                description={dv.description || 'No description available'}
                action={() => handleOnClickOption(dv)}
              />
            </article>
          )}
        </section>
        <Divider />
      </>
    }
    {filteredAstropath.length > 0 &&
      <>
        <h3>Astropath</h3>
        <section>
          {filteredAstropath.map(dv =>
            <article key={dv.id}>
              <OptionCard
                selected={dataVolumesChoice.some(dvc => dvc.id === dv.id)}
                title={dv.name}
                description={dv.description || 'No description available'}
                action={() => handleOnClickOption(dv)}
              />
            </article>
          )}
        </section>
        <Divider />
      </>
    }
    {filteredSimulations.length > 0 &&
      <>
        <h3>Simulations</h3>
        <section>
          {filteredSimulations.map(dv =>
            <article key={dv.id}>
              <OptionCard
                selected={dataVolumesChoice.some(dvc => dvc.id === dv.id)}
                title={dv.name}
                description={dv.description || 'No description available'}
                action={() => handleOnClickOption(dv)}
              />
            </article>
          )}
        </section>
        <Divider />
      </>
    }
    {filteredSDSS.length > 0 &&
      <>
        <h3>SDSS</h3>
        <section>
          {filteredSDSS.map(dv =>
            <article key={dv.id}>
              <OptionCard
                selected={dataVolumesChoice.some(dvc => dvc.id === dv.id)}
                title={dv.name}
                description={dv.description || 'No description available'}
                action={() => handleOnClickOption(dv)}
              />
            </article>
          )}
        </section>
        <Divider />
      </>
    }
    {filteredTurbulence.length > 0 &&
      <>
        <h3>Turbulence</h3>
        <section>
          {filteredTurbulence.map(dv =>
            <article key={dv.id}>
              <OptionCard
                selected={dataVolumesChoice.some(dvc => dvc.id === dv.id)}
                title={dv.name}
                description={dv.description || 'No description available'}
                action={() => handleOnClickOption(dv)}
              />
            </article>
          )}
        </section>
        <Divider />
      </>
    }
    <h3>All</h3>
    <section>
      {filteredDatavols.map(dv =>
        <article key={dv.id}>
          <OptionCard

            title={dv.name}
            description={dv.description || 'No description available'}
            action={() => handleOnClickOption(dv)}
          />
        </article>
      )}
    </section>
  </Styled>;
};