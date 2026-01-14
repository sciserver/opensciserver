import { FC, useMemo, useState } from 'react';
import styled from 'styled-components';
import { Accordion, AccordionDetails, AccordionSummary, Divider } from '@mui/material';
import { ExpandMore as ExpandMoreIcon } from '@mui/icons-material';

import { Image } from 'src/graphql/typings';
import { textSearch } from 'src/utils/search';

import { OptionCard } from 'components/common/optionCard';
import { SearchBar } from 'components/common/search';

const Styled = styled.div`

  .search-bar {
    position: sticky;
    top: 0;
    margin-bottom: 1.5rem;
    z-index: 10;
    background-color: white;
  }

  .options-content {
    max-height: 600px;
    overflow-y: auto;
  }
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
    <SearchBar className="search-bar" placeholder="Search images" onChangeParam={onSearch} />
    <div className="options-content">
      {filteredEssentials.length > 0 &&
        <>
          <Accordion defaultExpanded disableGutters sx={{ '&.MuiAccordion-root:before': { display: 'none' } }}>
            <AccordionSummary
              expandIcon={<ExpandMoreIcon />}
              aria-controls="panel1-content"
              id="panel1-header"
            >
              <h3>Default</h3>
            </AccordionSummary>
            <AccordionDetails className="option-items">
              {filteredEssentials.map(i =>
                <OptionCard
                  key={i.id}
                  selected={imageChoice?.id === i.id}
                  title={i.name}
                  description={i.description || 'No description available'}
                  action={() => setImageChoice(i)}
                />
              )}
            </AccordionDetails>
          </Accordion>
          <Divider />
        </>

      }
      {filteredAstro.length > 0 &&
        <>
          <Accordion defaultExpanded disableGutters sx={{ '&.MuiAccordion-root:before': { display: 'none' } }}>
            <AccordionSummary
              expandIcon={<ExpandMoreIcon />}
              aria-controls="panel1-content"
              id="panel1-header"
            >
              <h3>Astronomy</h3>
            </AccordionSummary>
            <AccordionDetails className="option-items">
              {filteredAstro.map(i =>
                <OptionCard
                  key={i.id}
                  selected={imageChoice?.id === i.id}
                  title={i.name}
                  description={i.description || 'No description available'}
                  action={() => setImageChoice(i)}
                />
              )}
            </AccordionDetails>
          </Accordion>
          <Divider />
        </>
      }
      {filteredCosmo.length > 0 &&
        <>
          <Accordion defaultExpanded disableGutters sx={{ '&.MuiAccordion-root:before': { display: 'none' } }}>
            <AccordionSummary
              expandIcon={<ExpandMoreIcon />}
              aria-controls="panel1-content"
              id="panel1-header"
            >
              <h3>Cosmology</h3>
            </AccordionSummary>
            <AccordionDetails className="option-items">
              {filteredCosmo.map(i =>
                <OptionCard
                  key={i.id}
                  selected={imageChoice?.id === i.id}
                  title={i.name}
                  description={i.description || 'No description available'}
                  action={() => setImageChoice(i)}
                />
              )}
            </AccordionDetails>
          </Accordion>
          <Divider />
        </>
      }
      {filteredHeasarc.length > 0 &&
        <>
          <Accordion defaultExpanded disableGutters sx={{ '&.MuiAccordion-root:before': { display: 'none' } }}>
            <AccordionSummary
              expandIcon={<ExpandMoreIcon />}
              aria-controls="panel1-content"
              id="panel1-header"
            >
              <h3>HEASARC</h3>
            </AccordionSummary>
            <AccordionDetails className="option-items">
              {filteredHeasarc.map(i =>
                <OptionCard
                  key={i.id}
                  selected={imageChoice?.id === i.id}
                  title={i.name}
                  description={i.description || 'No description available'}
                  action={() => setImageChoice(i)}
                />
              )}
            </AccordionDetails>
          </Accordion>
          <Divider />
        </>
      }
      {filteredSimulations.length > 0 &&
        <>
          <Accordion defaultExpanded disableGutters sx={{ '&.MuiAccordion-root:before': { display: 'none' } }}>
            <AccordionSummary
              expandIcon={<ExpandMoreIcon />}
              aria-controls="panel1-content"
              id="panel1-header"
            >
              <h3>Simulations</h3>
            </AccordionSummary>
            <AccordionDetails className="option-items">
              {filteredSimulations.map(i =>
                <OptionCard
                  key={i.id}
                  selected={imageChoice?.id === i.id}
                  title={i.name}
                  description={i.description || 'No description available'}
                  action={() => setImageChoice(i)}
                />
              )}
            </AccordionDetails>
          </Accordion>
          <Divider />
        </>
      }
      {filteredTurbulence.length > 0 &&
        <>
          <Accordion defaultExpanded disableGutters sx={{ '&.MuiAccordion-root:before': { display: 'none' } }}>
            <AccordionSummary
              expandIcon={<ExpandMoreIcon />}
              aria-controls="panel1-content"
              id="panel1-header"
            >
              <h3>Turbulence</h3>
            </AccordionSummary>
            <AccordionDetails className="option-items">
              {filteredTurbulence.map(i =>
                <OptionCard
                  key={i.id}
                  selected={imageChoice?.id === i.id}
                  title={i.name}
                  description={i.description || 'No description available'}
                  action={() => setImageChoice(i)}
                />
              )}
            </AccordionDetails>
          </Accordion>
          <Divider />
        </>
      }
      <Accordion defaultExpanded disableGutters sx={{ '&.MuiAccordion-root:before': { display: 'none' } }}>
        <AccordionSummary
          expandIcon={<ExpandMoreIcon />}
          aria-controls="panel2-content"
          id="panel2-header"
        >
          <h3>All</h3>
        </AccordionSummary>
        <AccordionDetails className="option-items">
          {filteredImages.map(i =>
            <OptionCard
              key={i.id}
              selected={imageChoice?.id === i.id}
              title={i.name}
              description={i.description || 'No description available'}
              action={() => setImageChoice(i)}
            />
          )}
        </AccordionDetails>
      </Accordion>
    </div>
  </Styled>;
};