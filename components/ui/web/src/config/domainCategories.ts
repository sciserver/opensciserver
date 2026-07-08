export type DomainCategory = {
  key: string;
  title: string;
  keywords: string[];
  excludeKeywords?: string[];
};

export const domainCategories: DomainCategory[] = [
  {
    key: 'default',
    title: 'Default',
    keywords: ['domain'],
    excludeKeywords: ['deprecated']
  },
  {
    key: 'gpu',
    title: 'GPU',
    keywords: ['gpu']
  },
];
