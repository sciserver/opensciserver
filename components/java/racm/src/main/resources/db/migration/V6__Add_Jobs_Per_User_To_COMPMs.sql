ALTER TABLE [dbo].[t_COMPM]
	ADD [defaultJobsPerUser] INTEGER NOT NULL DEFAULT(1)
GO

ALTER VIEW [COMPM] AS
  SELECT ID
  ,      publisherDID
  ,      DTYPE
  ,      [uuid]
  ,      [description]
  ,      [label]
  ,      [creatorUserid]
  ,      [defaultJobTimeout]
  ,      [defaultJobsPerUser]
  ,      computeDomainId
    FROM t_COMPM
GO