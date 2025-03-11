CREATE TABLE t_DataVolume (
  ID BIGINT IDENTITY NOT NULL
, DTYPE  VARCHAR(32)
, OPTLOCK INTEGER
, publisherDID VARCHAR(256)
, [name] VARCHAR(255) NOT NULL
, [description] VARCHAR(MAX)
, [path] VARCHAR(255) NOT NULL
, [url] VARCHAR(255) NOT NULL
, resourceId BIGINT NOT NULL-- Resource
, fileServiceId BIGINT NOT NULL-- FileService
, creationDate DATETIME
, modificationDate DATETIME
);

ALTER TABLE t_DataVolume ADD CONSTRAINT pk_t_DataVolume_ID PRIMARY KEY(ID);
CREATE INDEX ix_t_DataVolume_resource ON t_DataVolume(resourceId);
CREATE INDEX ix_t_DataVolume_fileService ON t_DataVolume(fileServiceId);
GO

ALTER TABLE [dbo].[t_VolumeContainer] ADD [datavolumeId] BIGINT NULL;
GO
CREATE INDEX ix_t_VolumeContainer_datavolume ON t_VolumeContainer(datavolumeId);
GO

ALTER TABLE t_DataVolume ADD CONSTRAINT fk_t_DataVolume_fileService
	FOREIGN KEY (fileServiceId)
	REFERENCES t_FileService(ID)
	ON DELETE CASCADE;
ALTER TABLE t_VolumeContainer ADD CONSTRAINT fk_t_VolumeContainer_datavolume
	FOREIGN KEY (datavolumeId)
	REFERENCES t_DataVolume(ID)
	ON DELETE CASCADE;
GO

CREATE VIEW [DataVolume] AS
  SELECT ID
  ,      publisherDID
  ,      DTYPE
  ,      [name]
  ,      [description]
  ,      [path]
  ,      [url]
  ,      resourceId
  ,      fileServiceId
    FROM t_DataVolume
GO

ALTER VIEW [VolumeContainer] AS
  SELECT b.*
  ,      t.datavolumeId
    FROM t_VolumeContainer t
    ,    [ComputeResource] b
   WHERE b.ID = t.ID
GO