import { useRef, useState } from 'react';

import { FILE_SERVICE_NOT_CONFIGURED, fileServiceUrl, fileUrl, uploadFile } from 'src/utils/fileTransfer';
import { FileVolumeRoute } from 'src/utils/files';

import { UploadItem } from './uploadsPanel';

const DONE_UPLOAD_MS = 4000;

type Options = {
  token: string;
  /** Where the files go; undefined when no writable folder is open. */
  target?: { volume: FileVolumeRoute; path: string };
  notifyError: (message: string) => void;
  /** Called once when a whole batch of uploads has finished, successfully or not. */
  onBatchDone: () => void;
};

/** Runs uploads in parallel and keeps the list shown in the uploads panel. */
export const useUploads = ({ token, target, notifyError, onBatchDone }: Options) => {
  const [uploads, setUploads] = useState<UploadItem[]>([]);
  const nextId = useRef(0);
  const active = useRef(0);

  const patch = (id: number, change: Partial<UploadItem>) => setUploads((current) => current.map((item) => (item.id === id ? { ...item, ...change } : item)));
  const dismiss = (id: number) => setUploads((current) => current.filter((item) => item.id !== id));

  const upload = (files: File[]) => {
    if (!target) {
      notifyError('Open a writable folder to upload');
      return;
    }
    const base = fileServiceUrl();
    if (!base) {
      notifyError(FILE_SERVICE_NOT_CONFIGURED);
      return;
    }
    const where = target.path.split('/').filter(Boolean).pop() || target.volume.volumeName;
    for (const file of files) {
      nextId.current += 1;
      active.current += 1;
      const id = nextId.current;
      const handle = uploadFile(fileUrl(base, target.volume, target.path, file.name), file, token, (progress) => patch(id, { progress }));
      setUploads((current) => [...current, { id, name: file.name, where, progress: 0, status: 'uploading', abort: handle.abort }]);
      handle.promise
        .then(() => {
          patch(id, { status: 'done', progress: 1 });
          // A finished upload clears itself; failures stay until dismissed.
          setTimeout(() => dismiss(id), DONE_UPLOAD_MS);
        })
        .finally(() => {
          // One reload once the whole batch is done, not one per file.
          active.current -= 1;
          if (active.current === 0) {
            onBatchDone();
          }
        })
        .catch((error: Error) => patch(id, { status: 'error', error: error.message }));
    }
  };

  return { uploads, upload, dismiss };
};
