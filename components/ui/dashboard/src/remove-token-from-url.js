import queryString from 'query-string';

export default () => {
  const parsed = queryString.parse(location.search);
  if (parsed.token) {
    delete parsed.token;
    const newPath = `${location.pathname}${queryString.stringify(parsed)}${location.hash}`;

    history.replaceState({}, '', newPath);
  }
};
