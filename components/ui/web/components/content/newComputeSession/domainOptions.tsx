import { FC, useEffect, useMemo, useState } from 'react';
import { Accordion, AccordionDetails, AccordionSummary, Divider } from '@mui/material';
import { ExpandMore as ExpandMoreIcon } from '@mui/icons-material';
import styled from 'styled-components';

import { Domain } from 'src/graphql/typings';
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
  domainList: Domain[];
  domainChoice?: Domain;
  setDomainChoice: (domain: Domain) => void;
};

export const DomainOptions: FC<Props> = ({ domainList, domainChoice, setDomainChoice }) => {

  const [filteredDomains, setFilteredDomains] = useState<Domain[]>([]);

  useEffect(() => {
    setFilteredDomains(domainList);
  }, [domainList]);

  const filteredEssentials = useMemo<Domain[]>(() => {
    return filteredDomains.filter(d => d.name.toLowerCase().includes('domain') && !d.name.toLowerCase().includes('deprecated'));
  }, [filteredDomains]);
  const filteredGPU = useMemo<Domain[]>(() => {
    return filteredDomains.filter(d => d.name.toLowerCase().includes('gpu'));
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
              {filteredEssentials.map(d =>
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
        </>

      }
      {filteredGPU.length > 0 &&
        <>
          <Accordion defaultExpanded disableGutters sx={{ '&.MuiAccordion-root:before': { display: 'none' } }}>
            <AccordionSummary
              expandIcon={<ExpandMoreIcon />}
              aria-controls="panel2-content"
              id="panel2-header"
            >
              <h3>GPU</h3>
            </AccordionSummary>
            <AccordionDetails className="option-items">
              {filteredGPU.map(d =>
                <OptionCard
                  selected={domainChoice?.id === d.id}
                  title={d.name}
                  description={d.description || 'No description available'}
                  action={() => setDomainChoice(d)}
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