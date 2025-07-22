import { FC } from 'react';
import { Autocomplete, TextField } from '@mui/material';
import styled from 'styled-components';

const Styled = styled.div`
  background-color: #fff;
  box-shadow: 0px 8px 20px rgba(0,0,0,0.06);
  height: 64px;
  border-radius: 8px;
  display: flex;
  margin-right: 60px;
  padding: 20px;
  
  &:hover {
    background-color: rgb(142, 202, 230, 0.25);
  }

  .MuiAutocomplete-root {
    width: 100%;
  }
`;

type Props = {
  options: string[]
  value: string[]
  handleFilter: (selected: string[]) => void
  placeholder?: string
}

export const AutoCompleteFilter: FC<Props> = ({ options, value, handleFilter, placeholder }) => {
  return <Styled>
    <Autocomplete
      value={value}
      multiple
      id="tags-standard"
      options={options}
      getOptionLabel={(option: string) => option.toUpperCase()}
      onChange={(_, selectedTags: string[]) => handleFilter(selectedTags)}
      renderInput={(params) => (
        <TextField
          {...params}
          variant="standard"
          placeholder={placeholder || 'Filter by...'}
        />
      )}
    />
  </Styled>;
};