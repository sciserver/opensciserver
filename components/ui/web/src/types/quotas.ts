export type QuotaItem = {
  key: string;
  /** "ana's user volumes" for a per-user quota, otherwise the volume's name. */
  label: string;
  used: number;
  quota: number;
  /** 0-1, capped. A quota of 0 counts as full. */
  fraction: number;
  full: boolean;
};

export type QuotaGroup = { rootVolumeId: string; rootVolumeName: string; items: QuotaItem[] };
