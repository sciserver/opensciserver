import { FC, useEffect, useMemo, useState } from 'react';
import styled from 'styled-components';

import { Domain } from 'src/graphql/typings';
import { OptionCard } from 'components/common/optionCard';
import { Divider } from '@mui/material';
import { SearchBar } from 'components/common/search';
import { textSearch } from 'src/utils/search';

const Styled = styled.div`
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
    <SearchBar placeholder="Search domains" onChangeParam={onSearch} />
    {filteredEssentials.length > 0 &&
      <>
        <h3>Default</h3>
        <section>
          {filteredEssentials.map(d =>
            <article key={d.id}>
              <OptionCard
                selected={domainChoice?.id === d.id}
                title={d.name}
                description={d.description || 'No description available'}
                action={() => setDomainChoice(d)}
              />
            </article>
          )}
        </section>
        <Divider />
      </>
    }
    {filteredGPU.length > 0 &&
      <>
        <h3>GPU</h3>
        <section>
          {filteredGPU.map(d =>
            <article key={d.id}>
              <OptionCard
                selected={domainChoice?.id === d.id}
                title={d.name}
                description={d.description || 'No description available'}
                action={() => setDomainChoice(d)}
              />
            </article>
          )}
        </section>
        <Divider />
      </>
    }
    <h3>All</h3>
    <section>
      {filteredDomains.map(d =>
        <article key={d.id}>
          <OptionCard
            selected={domainChoice?.id === d.id}
            title={d.name}
            description={d.description || 'No description available'}
            action={() => setDomainChoice(d)}
          />
        </article>
      )}
    </section>
  </Styled>;
};