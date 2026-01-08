import { FC, useMemo, useState } from 'react';
import styled from 'styled-components';

import { Image } from 'src/graphql/typings';
import { OptionCard } from 'components/common/optionCard';
import { Divider } from '@mui/material';
import { SearchBar } from 'components/common/search';
import { textSearch } from 'src/utils/search';

const Styled = styled.div`
`;

type Props = {
  imageList: Image[]
  imageChoice?: Image
  setImageChoice: (image: Image) => void;
};

export const ImageOptions: FC<Props> = ({ imageList, imageChoice, setImageChoice }) => {

  const [filteredImages, setFilteredImages] = useState<Image[]>(imageList);

  const filteredEssentials = useMemo<Image[]>(() => {
    return filteredImages.filter(i => i.name.toLowerCase().includes('essentials'));
  }, [filteredImages]);

  const filteredAstro = useMemo<Image[]>(() => {
    return filteredImages.filter(i => i.name.toLowerCase().includes('astro'));
  }, [filteredImages]);
  const filteredCosmo = useMemo<Image[]>(() => {
    return filteredImages.filter(i => i.name.toLowerCase().includes('cosmo'));
  }, [filteredImages]);
  const filteredHeasarc = useMemo<Image[]>(() => {
    return filteredImages.filter(i => i.name.toLowerCase().includes('heasarc'));
  }, [filteredImages]);
  const filteredSimulations = useMemo<Image[]>(() => {
    return filteredImages.filter(i => i.name.toLowerCase().includes('simulation'));
  }, [filteredImages]);
  const filteredTurbulence = useMemo<Image[]>(() => {
    return filteredImages.filter(i => i.name.toLowerCase().includes('turbulence'));
  }, [filteredImages]);

  const searchDatasetParams = (image: Image, input: string) => {
    return textSearch(image.description || '', input)
      || textSearch(image.name || '', input);
  };

  const onSearch = (input: string) => {
    const tempImages = imageList.filter(d => searchDatasetParams(d, input));
    setFilteredImages(tempImages);
  };

  return <Styled>
    <SearchBar placeholder="Search images" onChangeParam={onSearch} />
    {filteredEssentials.length > 0 &&
      <>
        <h3>Essentials</h3>
        <section>
          {filteredEssentials.map(i =>
            <article key={i.id}>
              <OptionCard
                selected={imageChoice?.id === i.id}
                title={i.name}
                description={i.description || 'No description available'}
                action={() => setImageChoice(i)}
              />
            </article>
          )}
        </section>
        <Divider />
      </>
    }
    {filteredAstro.length > 0 &&
      <>
        <h3>Astronomy</h3>
        <section>
          {filteredAstro.map(i =>
            <article key={i.id}>
              <OptionCard
                selected={imageChoice?.id === i.id}
                title={i.name}
                description={i.description || 'No description available'}
                action={() => setImageChoice(i)}
              />
            </article>
          )}
        </section>
        <Divider />
      </>
    }
    {filteredCosmo.length > 0 &&
      <>
        <h3>Cosmology</h3>
        <section>
          {filteredCosmo.map(i =>
            <article key={i.id}>
              <OptionCard
                selected={imageChoice?.id === i.id}
                title={i.name}
                description={i.description || 'No description available'}
                action={() => setImageChoice(i)}
              />
            </article>
          )}
        </section>
        <Divider />
      </>
    }
    {filteredHeasarc.length > 0 &&
      <>
        <h3>HEASARC</h3>
        <section>
          {filteredHeasarc.map(i =>
            <article key={i.id}>
              <OptionCard
                selected={imageChoice?.id === i.id}
                title={i.name}
                description={i.description || 'No description available'}
                action={() => setImageChoice(i)}
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
          {filteredSimulations.map(i =>
            <article key={i.id}>
              <OptionCard
                selected={imageChoice?.id === i.id}
                title={i.name}
                description={i.description || 'No description available'}
                action={() => setImageChoice(i)}
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
          {filteredTurbulence.map(i =>
            <article key={i.id}>
              <OptionCard
                selected={imageChoice?.id === i.id}
                title={i.name}
                description={i.description || 'No description available'}
                action={() => setImageChoice(i)}
              />
            </article>
          )}
        </section>
        <Divider />
      </>
    }
    <h3>All</h3>
    <section>
      {filteredImages.map(i =>
        <article key={i.id}>
          <OptionCard
            selected={imageChoice?.id === i.id}
            title={i.name}
            description={i.description || 'No description available'}
            action={() => setImageChoice(i)}
          />
        </article>
      )}
    </section>
  </Styled>;
};