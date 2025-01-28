EXEC sp_rename 't_DataVolume.path', 'pathOnFileSystem', 'COLUMN';
GO

ALTER VIEW [DataVolume] AS
  SELECT ID
  ,      publisherDID
  ,      DTYPE
  ,      [name]
  ,      [description]
  ,      [pathOnFileSystem]
  ,      [url]
  ,      resourceId
  ,      fileServiceId
    FROM t_DataVolume
GO