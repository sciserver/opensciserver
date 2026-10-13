import { afterEach, describe, expect, it, vi } from 'vitest';
import { VolumeType } from '../graphql/typings';
import { errorMessage, fileServiceUrl, fileUrl } from './fileTransfer';

const base = 'https://host/fileservice/api/';

describe('fileUrl', () => {
  const user = { volumeType: VolumeType.Uservolume, volumeName: 'FESS', rootVolumeName: 'Storage', owner: 'jjaime' };

  it('matches the dashboard download link for a user volume', () => {
    expect(fileUrl(base, user, '/20251103-test/test tes', 'command.txt'))
      .toBe('https://host/fileservice/api/file/Storage/jjaime/FESS/20251103-test/test%20tes/command.txt');
  });

  it('addresses data volumes by name and the volume root without a path', () => {
    expect(fileUrl(base, { volumeType: VolumeType.Datavolume, volumeName: 'SDSS DAS' }, '', 'a#b.txt'))
      .toBe('https://host/fileservice/api/file/SDSS%20DAS/a%23b.txt');
  });

  it('builds a folder URL when there is no file name', () => {
    expect(fileUrl(base, user, '/a')).toBe('https://host/fileservice/api/file/Storage/jjaime/FESS/a');
  });
});

describe('errorMessage', () => {
  it('reads the fileservice error body and tolerates anything else', () => {
    expect(errorMessage('{"status":"error","error":"Missing permissions"}')).toBe('Missing permissions');
    expect(errorMessage('<html>')).toBe('');
  });
});

describe('fileServiceUrl', () => {
  afterEach(() => vi.unstubAllEnvs());

  it('adds the trailing slash', () => {
    vi.stubEnv('NEXT_PUBLIC_FILE_SERVICE_URL', 'https://host/fileservice/api');
    expect(fileServiceUrl()).toBe('https://host/fileservice/api/');
  });

  it('is empty, not a bare slash, when the variable is missing', () => {
    vi.stubEnv('NEXT_PUBLIC_FILE_SERVICE_URL', '');
    expect(fileServiceUrl()).toBe('');
  });
});
