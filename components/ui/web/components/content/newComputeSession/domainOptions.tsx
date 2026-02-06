import { FC, Fragment, useEffect, useMemo, useState } from 'react';
import { Accordion, AccordionDetails, AccordionSummary, Divider } from '@mui/material';
import { ExpandMore as ExpandMoreIcon } from '@mui/icons-material';
import styled from 'styled-components';

import { Domain } from 'src/graphql/typings';
import { textSearch } from 'src/utils/search';
import { domainCategories } from 'src/config/domainCategories';

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
  domainList: Domain[];
  domainChoice?: Domain;
  setDomainChoice: (domain: Domain) => void;
};

export const DomainOptions: FC<Props> = ({ domainList, domainChoice, setDomainChoice }) => {

  const [filteredDomains, setFilteredDomains] = useState<Domain[]>(domainList || []);

  useEffect(() => {
    setFilteredDomains(domainList);
  }, [domainList]);

  const categorizedDomains = useMemo(() => {
    return domainCategories.map(category => {
      const items = filteredDomains.filter(domain => {
        const name = domain.name?.toLowerCase() || '';
        const matchesInclude = category.keywords.some(keyword => name.includes(keyword.toLowerCase()));
        const matchesExclude = category.excludeKeywords?.some(keyword => name.includes(keyword.toLowerCase())) || false;
        return matchesInclude && !matchesExclude;
      });

      return {
        ...category,
        items
      };
    });
  }, [filteredDomains]);

  const searchDatasetParams = (domain: Domain, input: string) => {
    return textSearch(domain.description || '', input)
      || textSearch(domain.name || '', input);
  };

  const onSearch = (input: string) => {
    const tempDomains = domainList.filter(d => searchDatasetParams(d, input));
    setFilteredDomains(tempDomains);
  };

  return <Styled>
    <SearchBar className="search-bar" placeholder="Search domains" onChangeParam={onSearch} />
    <div className="options-content">
      {categorizedDomains
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
                {category.items.map(d =>
                  <OptionCard
                    key={d.id}
                    selected={domainChoice?.id === d.id}
                    title={d.name}
                    description={d.description || 'No description available'}
                    action={() => setDomainChoice(d)}
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
          {filteredDomains.map(d =>
            <OptionCard
              key={d.id}
              selected={domainChoice?.id === d.id}
              title={d.name}
              description={d.description || 'No description available'}
              action={() => setDomainChoice(d)}
            />
          )}
        </AccordionDetails>
      </Accordion>
    </div>
  </Styled>;
};