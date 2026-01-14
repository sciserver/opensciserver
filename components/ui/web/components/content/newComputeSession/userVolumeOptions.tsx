import { FC, useMemo, useState } from 'react';
import styled from 'styled-components';
import { Accordion, AccordionDetails, AccordionSummary, Divider } from '@mui/material';
import { ExpandMore as ExpandMoreIcon } from '@mui/icons-material';
import { remove } from 'lodash';

import { UserVolume } from 'src/graphql/typings';
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
    <SearchBar className="search-bar" placeholder="Search user volumes" onChangeParam={onSearch} />
    <div className="options-content">
      {filteredOwned.length > 0 &&
        <>
          <Accordion defaultExpanded disableGutters sx={{ '&.MuiAccordion-root:before': { display: 'none' } }}>
            <AccordionSummary
              expandIcon={<ExpandMoreIcon />}
              aria-controls="panel2-content"
              id="panel2-header"
            >
              <h3>Owned by me</h3>
            </AccordionSummary>
            <AccordionDetails className="option-items">
              {filteredOwned.map(uv =>
                <OptionCard
                  key={uv.id}
                  selected={userVolumesChoice.some(uvc => uvc.id === uv.id)}
                  title={uv.name}
                  description={uv.description || 'No description available'}
                  action={() => handleOnClickOption(uv)}
                />
              )}
            </AccordionDetails>
          </Accordion>
          <Divider />
        </>
      }
      {filteredShared.length > 0 &&
        <>
          <Accordion defaultExpanded disableGutters sx={{ '&.MuiAccordion-root:before': { display: 'none' } }}>
            <AccordionSummary
              expandIcon={<ExpandMoreIcon />}
              aria-controls="panel2-content"
              id="panel2-header"
            >
              <h3>Shared</h3>
            </AccordionSummary>
            <AccordionDetails className="option-items">
              {filteredShared.map(uv =>
                <OptionCard
                  key={uv.id}
                  selected={userVolumesChoice.some(uvc => uvc.id === uv.id)}
                  title={uv.name}
                  description={uv.description || 'No description available'}
                  action={() => handleOnClickOption(uv)}
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
          {filteredUserVols.map(uv =>
            <OptionCard
              key={uv.id}
              selected={userVolumesChoice.some(uvc => uvc.id === uv.id)}
              title={uv.name}
              description={uv.description || 'No description available'}
              action={() => handleOnClickOption(uv)}
            />
          )}
        </AccordionDetails>
      </Accordion>
    </div>
  </Styled>;
};