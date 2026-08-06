/**
 * Formats a Date object into a string in the format: YYYY-MM-DD HH:mm:ss.SSS
 * @param dt - The Date object to format
 * @returns A formatted date string with zero-padded components
 */
export const formatDate = (dt: Date) => {
  const pad = (n: number, z = 2) => String(n).padStart(z, '0');
  const year = dt.getFullYear();
  const month = pad(dt.getMonth() + 1);
  const day = pad(dt.getDate());
  const hours = pad(dt.getHours());
  const minutes = pad(dt.getMinutes());
  const seconds = pad(dt.getSeconds());
  const ms = String(dt.getMilliseconds()).padStart(3, '0');
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}.${ms}`;
};