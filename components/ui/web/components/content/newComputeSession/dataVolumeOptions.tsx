import { FC, Fragment, useMemo, useState } from 'react';
import styled from 'styled-components';
import { Accordion, AccordionDetails, AccordionSummary, Divider } from '@mui/material';
import { ExpandMore as ExpandMoreIcon } from '@mui/icons-material';
import { remove } from 'lodash';

import { DataVolume } from 'src/graphql/typings';
import { textSearch } from 'src/utils/search';
import { dataVolumeCategories } from 'src/config/dataVolumeCategories';

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
  dataVolumeList: DataVolume[];
  dataVolumesChoice: DataVolume[];
  setDataVolumesChoice: (dataVols: DataVolume[]) => void;
};

export const DataVolumeOptions: FC<Props> = ({ dataVolumeList, dataVolumesChoice, setDataVolumesChoice }) => {

  const [filteredDatavols, setFilteredDatavols] = useState<DataVolume[]>(dataVolumeList || []);

  const categorizedDatavols = useMemo(() => {
    return dataVolumeCategories.map(category => {
      const items = filteredDatavols.filter(dv => {
        const name = dv.description?.toLowerCase() || '';
        return category.keywords.some(keyword => name.includes(keyword.toLowerCase()));
      });

      return {
        ...category,
        items
      };
    });
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
    <SearchBar className="search-bar" placeholder="Search data volumes" onChangeParam={onSearch} />
    <div className="options-content">
      {categorizedDatavols
        .filter(category => category.items.length > 0)
        .map(category => (
          <Fragment key={category.key}>
            <Accordion defaultExpanded disableGutters sx={{ '&.MuiAccordion-root:before': { display: 'none' } }}>
              <AccordionSummary
                expandIcon={<ExpandMoreIcon />}
                aria-controls="panel2-content"
                id="panel2-header"
              >
                <h3>{category.title}</h3>
              </AccordionSummary>
              <AccordionDetails className="option-items">
                {category.items.map(dv =>
                  <OptionCard
                    key={dv.id}
                    selected={dataVolumesChoice.some(dvc => dvc.id === dv.id)}
                    title={dv.name}
                    description={dv.description || 'No description available'}
                    action={() => handleOnClickOption(dv)}
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
          {filteredDatavols.map(dv =>
            <OptionCard
              key={dv.id}
              selected={dataVolumesChoice.some(dvc => dvc.id === dv.id)}
              title={dv.name}
              description={dv.description || 'No description available'}
              action={() => handleOnClickOption(dv)}
            />
          )}
        </AccordionDetails>
      </Accordion>
    </div>
  </Styled>;
};