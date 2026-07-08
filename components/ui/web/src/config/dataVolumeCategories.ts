export type DataVolumeCategory = {
  key: string;
  title: string;
  keywords: string[];
};

export const dataVolumeCategories: DataVolumeCategory[] = [
  {
    key: 'essentials',
    title: 'Essentials',
    keywords: ['started']
  },
  {
    key: 'astronomy',
    title: 'Astronomy',
    keywords: ['astronomy']
  },
  {
    key: 'astropath',
    title: 'Astropath',
    keywords: ['astropath']
  },
  {
    key: 'simulations',
    title: 'Simulations',
    keywords: ['simulation']
  },
  {
    key: 'sdss',
    title: 'SDSS',
    keywords: ['sdss']
  },
  {
    key: 'turbulence',
    title: 'Turbulence',
    keywords: ['turbulence']
  },
];
