export type UserVolumeCategory = {
  key: string;
  title: string;
  filter: 'owned' | 'shared';
};

export const userVolumeCategories: UserVolumeCategory[] = [
  {
    key: 'owned',
    title: 'Owned by me',
    filter: 'owned'
  },
  {
    key: 'shared',
    title: 'Shared',
    filter: 'shared'
  },
];
