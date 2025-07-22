import { IconButton, InputBase } from '@mui/material';
import { Search as SearchIcon, Clear as ClearIcon } from '@mui/icons-material';
import styled from 'styled-components';
import { FC, useRef, useState } from 'react';

const Search = styled.div`
  background-color: #fff;
  box-shadow: 0px 8px 20px rgba(0,0,0,0.06);
  height: 64px;
  border-radius: 8px;
  display: flex;
  margin-right: 60px;
  padding-right: 20px;
  
  .left-container { 
    display: flex;
    position: relative;
    flex: 1;
    align-items: center;
  }

  &:hover {
    background-color: rgb(142, 202, 230, 0.25);
  },
`;

const SearchIconWrapper = styled.div`
  padding-left: 28px;
`;

const StyledInputBase = styled(InputBase)`
  height: 100%;
  font-size: 16px;
  color: inherit;
  padding-left: 20px;
  .MuiInputBase-input: {
    width: inherit;
  }`;

type Props = {
  className?: string
  onChangeParam?: (input: string) => void
  placeholder?: string
}

export const SearchBar: FC<Props> = ({ className = '', onChangeParam, placeholder }) => {

  const [searchStr, setSearchStr] = useState<string>('');

  const searchInput = useRef<HTMLInputElement>(null);

  const onChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const str = e.target.value;
    e.preventDefault();
    setSearchStr(str);

    if (onChangeParam) {
      onChangeParam(str);
    }
  };

  const onClear = () => {
    setSearchStr('');

    if (onChangeParam) {
      onChangeParam('');
    }
  };

  return (
    <Search className={className} onClick={() => searchInput.current?.focus()}>
      <div className="left-container">
        <SearchIconWrapper>
          <SearchIcon />
        </SearchIconWrapper>
        <StyledInputBase
          id="search-bar"
          placeholder={placeholder || 'Search…'}
          onChange={onChange}
          value={searchStr}
          inputProps={{ 'aria-label': 'search' }}
          ref={searchInput}
        />
      </div>
      <IconButton
        aria-label="clear search"
        onClick={onClear}
      >
        <ClearIcon />
      </IconButton>
    </Search>);
};

