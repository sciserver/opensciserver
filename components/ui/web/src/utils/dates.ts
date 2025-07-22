const millisecsToSecs = 0.001;
const secsToMin = 60;

export const getExpireTime = (createdAt: Date, maxSecs: number) => {
  if (maxSecs > 0) {
    const createdAtSecs = createdAt.getTime() * millisecsToSecs;
    const expireAtSecs = createdAtSecs + maxSecs;
    const nowInSecs = (Date.now() * millisecsToSecs);
    const minutesTotal = Math.round((nowInSecs - expireAtSecs) / secsToMin);
    return `${minutesTotal} min`;
  }
  return '---';
};