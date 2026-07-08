export type ImageCategory = {
  key: string;
  title: string;
  keywords: string[];
};

export const imageCategories: ImageCategory[] = [
  {
    key: 'default',
    title: 'Default',
    keywords: ['essentials']
  },
  {
    key: 'astronomy',
    title: 'Astronomy',
    keywords: ['astro']
  },
  {
    key: 'cosmology',
    title: 'Cosmology',
    keywords: ['cosmo']
  },
  {
    key: 'heasarc',
    title: 'HEASARC',
    keywords: ['heasarc']
  },
  {
    key: 'simulations',
    title: 'Simulations',
    keywords: ['simulation']
  },
  {
    key: 'turbulence',
    title: 'Turbulence',
    keywords: ['turbulence']
  },
];
