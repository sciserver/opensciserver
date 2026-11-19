// `@mui/styled-engine` is aliased to this file (see next.config.js) so MUI renders
// through styled-components. @mui/styled-engine-sc stopped at 5.x behind @mui/system, and
// @mui/system imports `internal_serializeStyles`, which the sc engine doesn't export.
// MUI only calls it when CSS layers are enabled, which this app doesn't use; without the
// export Turbopack fails the build, since it checks imports statically.
export {
  default,
  internal_processStyles,
  ThemeContext,
  keyframes,
  css,
  StyledEngineProvider,
  GlobalStyles
} from '@mui/styled-engine-sc';

export const internal_serializeStyles = (styles) => styles;
