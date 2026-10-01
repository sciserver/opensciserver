import { VolumeType } from '../graphql/typings';
import { FileVolumeRoute, normalizePath } from './files';

/** Base URL of the fileservice API, with a trailing slash, e.g. https://host/fileservice/api/. */
export const fileServiceUrl = (): string => {
  const base = process.env.NEXT_PUBLIC_FILE_SERVICE_URL || '';
  return base.endsWith('/') ? base : `${base}/`;
};

/**
 * URL of a file or folder on the fileservice. Port of the dashboard's joinURLWithFileName: user volumes are
 * addressed as root/owner/volume, data volumes by name, and every segment is URL-encoded.
 */
export const fileUrl = (base: string, volume: FileVolumeRoute, path: string, name?: string): string => {
  const head = volume.volumeType === VolumeType.Uservolume
    ? [volume.rootVolumeName, volume.owner, volume.volumeName]
    : [volume.volumeName];
  const segments = [...head, ...normalizePath(path).split('/').filter(Boolean), ...(name ? [name] : [])];
  return `${base}file/${segments.map((segment) => encodeURIComponent(segment || '')).join('/')}`;
};

/** The fileservice answers errors as {"status":"error","error":"..."}. */
export const errorMessage = (body: string): string => {
  try {
    const parsed = JSON.parse(body);
    return typeof parsed?.error === 'string' ? parsed.error : '';
  }
  catch {
    return '';
  }
};

export type UploadHandle = { promise: Promise<void>; abort: () => void };

/**
 * PUTs a file to the fileservice with the user's token, reporting progress as a 0-1 fraction.
 * Like the dashboard, the body is sent raw as application/octet-stream.
 */
export const uploadFile = (url: string, file: File, token: string, onProgress: (fraction: number) => void): UploadHandle => {
  const request = new XMLHttpRequest();
  const promise = new Promise<void>((resolve, reject) => {
    request.open('PUT', url);
    request.setRequestHeader('X-Auth-Token', token);
    request.setRequestHeader('Content-Type', 'application/octet-stream');
    request.upload.addEventListener('progress', (event) => event.lengthComputable && onProgress(event.loaded / event.total));
    request.addEventListener('load', () => {
      if (request.status >= 200 && request.status < 300) {
        resolve();
        return;
      }
      reject(new Error(errorMessage(request.responseText) || `Upload failed (${request.status})`));
    });
    request.addEventListener('error', () => reject(new Error('Network error. The file service may be unreachable.')));
    request.addEventListener('abort', () => reject(new Error('Upload canceled')));
    request.send(file);
  });
  return { promise, abort: () => request.abort() };
};

/** Downloads are plain navigations, so the browser authenticates them with its cookie. */
export const startDownload = (url: string, name: string): void => {
  const link = document.createElement('a');
  link.href = url;
  link.download = name;
  link.rel = 'noopener';
  document.body.append(link);
  link.click();
  link.remove();
};

/** Fetches a text file (used for README.md); resolves to '' when it can't be read. */
export const fetchText = async (url: string, token: string): Promise<string> => {
  try {
    const response = await fetch(url, { headers: { 'X-Auth-Token': token } });
    return response.ok ? await response.text() : '';
  }
  catch {
    return '';
  }
};

/** Array.from for DOM lists (FileList, DOMStringList); the project targets ES5, where spreading them doesn't compile. */
export const toArray = <T>(list: ArrayLike<T> | Iterable<T>): T[] => (
  // eslint-disable-next-line unicorn/prefer-spread
  Array.from(list)
);
