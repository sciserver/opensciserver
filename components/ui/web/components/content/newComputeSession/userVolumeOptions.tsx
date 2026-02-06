import { FC, Fragment, useContext, useMemo, useState } from 'react';
import styled from 'styled-components';
import { Accordion, AccordionDetails, AccordionSummary, Divider } from '@mui/material';
import { ExpandMore as ExpandMoreIcon } from '@mui/icons-material';
import { remove } from 'lodash';

import { UserContext } from 'context';
import { UserVolume } from 'src/graphql/typings';
import { textSearch } from 'src/utils/search';
import { userVolumeCategories } from 'src/config/userVolumeCategories';

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

  const { user } = useContext(UserContext);

  const [filteredUserVols, setFilteredUserVols] = useState<UserVolume[]>(userVolumeList || []);

  const categorizedUserVols = useMemo(() => {
    const userName = user?.userName;

    return userVolumeCategories.map(category => {
      const items = filteredUserVols.filter(uv => {
        if (category.filter === 'owned') {
          return userName ? uv.owner === userName : false;
        }

        if (category.filter === 'shared') {
          return userName ? uv.owner !== userName : true;
        }

        return true;
      });

      return {
        ...category,
        items
      };
    });
  }, [filteredUserVols, user]);

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
      {categorizedUserVols
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
                {category.items.map(uv =>
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