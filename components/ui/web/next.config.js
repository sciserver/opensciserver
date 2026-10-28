const path = require('path');

/** @type {import('next').NextConfig} */

// MUI is rendered with styled-components instead of emotion. The alias has to be
// declared for both bundlers: Turbopack (default for dev/build) and webpack (`--webpack`).
module.exports = {
  reactStrictMode: true,
  agentRules: false,
  basePath: process.env.NEXT_PUBLIC_BASE_PATH,
  output: 'standalone',
  transpilePackages: ['@mui/material', '@mui/system', '@mui/icons-material'],
  turbopack: {
    resolveAlias: {
      '@mui/styled-engine': './src/utils/muiStyledEngine.js'
    }
  },
  webpack: (config) => {
    config.resolve.alias = {
      ...config.resolve.alias,
      '@mui/styled-engine': path.resolve(__dirname, 'src/utils/muiStyledEngine.js')
    };
    return config;
  },
  compiler: {
    styledComponents: true
  },
  images: {
    remotePatterns: [{
      protocol: 'https',
      hostname: 'www.globus.org',
      pathname: '/assets/images/**'
    }]
  }
};
