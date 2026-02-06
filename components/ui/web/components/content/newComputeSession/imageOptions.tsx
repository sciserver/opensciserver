import { FC, Fragment, useMemo, useState } from 'react';
import styled from 'styled-components';
import { Accordion, AccordionDetails, AccordionSummary, Divider } from '@mui/material';
import { ExpandMore as ExpandMoreIcon } from '@mui/icons-material';

import { Image } from 'src/graphql/typings';
import { textSearch } from 'src/utils/search';
import { imageCategories } from 'src/config/imageCategories';

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

  const [filteredImages, setFilteredImages] = useState<Image[]>(imageList || []);

  const categorizedImages = useMemo(() => {
    return imageCategories.map(category => {
      const items = filteredImages.filter(image => {
        const name = image.name?.toLowerCase() || '';
        return category.keywords.some(keyword => name.includes(keyword.toLowerCase()));
      });

      return {
        ...category,
        items
      };
    });
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
      {categorizedImages
        .filter(category => category.items.length > 0)
        .map(category => (
          <Fragment key={category.key}>
            <Accordion defaultExpanded disableGutters sx={{ '&.MuiAccordion-root:before': { display: 'none' } }}>
              <AccordionSummary
                expandIcon={<ExpandMoreIcon />}
                aria-controls="panel1-content"
                id="panel1-header"
              >
                <h3>{category.title}</h3>
              </AccordionSummary>
              <AccordionDetails className="option-items">
                {category.items.map(i =>
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
          </Fragment>
        ))}
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