/****** Object:  Schema [racm]    Script Date: 8/7/2018 3:31:56 PM ******/
CREATE SCHEMA [racm]
GO
/****** Object:  UserDefinedFunction [racm].[nextJobs]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE function [racm].[nextJobs](@compmUUID varchar(64), @timeout real, @interval real, @maxNum integer) 
returns @rt table(
	  jobId bigint,submitterId bigint,
	  computeDomainId bigint, submitTime datetime,
	  usageWeight float,numQueued integer, numStarted integer, ranking integer) 
as

begin
insert into @rt
select * from racm.nextJobsRestricted(@compmUUID, @timeout,@interval, @maxNum,20)
  return 
end

GO
/****** Object:  UserDefinedFunction [racm].[nextJobsNEW]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO


CREATE function [racm].[nextJobsNEW](@compmUUID varchar(64), @timeout real, @interval real, @maxNum integer) 
returns @rt table(
	  jobId bigint,submitterId bigint,
	  computeDomainId bigint, submitTime datetime,
	  usageWeight float,numQueued integer, numStarted integer, ranking integer) 
as
begin
insert into @rt
select * from racm.nextJobsRestricted(@compmUUID, @timeout,@interval, @maxNum,25)
  return 
end

GO
/****** Object:  UserDefinedFunction [racm].[nextJobsOLD]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO


CREATE function [racm].[nextJobsOLD](@compmUUID varchar(64), @timeout real, @interval real, @maxNum integer) 
returns @rt table(
	  jobId bigint,submitterId bigint,
	  computeDomainId bigint, submitTime datetime,
	  usageWeight float,numQueued integer, numStarted integer, ranking integer) 
as
begin

declare @computeDomainId bigint, @currentDate datetime = getDate()

select @computeDomainId = computeDomainId
  from COMPM
 where uuid=@compmUUID
;
with prev as (
select dj.submitterId
,      count(*) as numJobs
,      sum(case when dj.status = 2 then 1 else 0 end) as numQueued
,      sum(case when dj.status = 8 then 1 else 0 end) as numStarted
,      sum(case when dj.finishedTime is null then @timeout -- should really be cast(datediff(second,startedTime, getDate()) as float)
                else cast(datediff(second,dj.startedTime, dj.finishedtime) as float)
				end ) as totTime
,      sum((case when dj.finishedTime is null then @timeout -- should really be cast(datediff(second,startedTime, getDate()) as float)
                else cast(datediff_big(millisecond,dj.startedTime, dj.finishedtime) as float)
				end )/datediff_big(millisecond,dj.startedTime,@currentDate)) as usageWeight
  from compm c , job dj 
 where c.uuid = @compmUUID
   and dj.computeDomainId=c.computeDomainId
   and dj.status > 1 -- PENDING
   and dj.startedTime >= dateadd(second,-@interval,getDate())
 group by submitterid
),
pending as (
select dj.*
,      rank() over (partition by submitterId order by submitTime) as rankSubmitted
  from compm c , job dj 
 where c.uuid = @compmUUID
   and dj.computeDomainId=c.computeDomainId
   and dj.status=1 -- PENDING
),
final as (
select j.Id as jobId,j.submitterId, j.computeDomainId, j.submitTime  -- without TOP cannot do an order by in a function
,      isnull(p.usageWeight,0) as usageWeight
,      isnull(p.numQueued,0) as numQueued
,      isnull(p.numStarted,0) as numStarted
,      rank() over (order by rankSubmitted,usageWeight,submitTime) as ranking
  from pending j left outer join prev p
  on p.submitterId = j.submitterid
  where j.rankSubmitted <=@maxNum
)
insert into @rt
select * from final
 where ranking <= @maxNum -- this could be a parameter

  return 
end

GO
/****** Object:  UserDefinedFunction [racm].[nextJobsRestricted]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO



CREATE function [racm].[nextJobsRestricted](@compmUUID varchar(64), @timeout real, @interval real, @maxNum integer,@maxPerUser integer) 
returns @rt table(
	  jobId bigint,submitterId bigint,
	  computeDomainId bigint, submitTime datetime,
	  usageWeight float,numQueued integer, numStarted integer, ranking integer) 
as
begin

declare @computeDomainId bigint, @currentDate datetime = getDate()

select @computeDomainId = computeDomainId
  from COMPM
 where uuid=@compmUUID
;
with prev as (
select dj.submitterId
,      count(*) as numJobs
,      sum(case when dj.status between 2 and 4 then 1 else 0 end) as numQueued
,      sum(case when dj.status = 8 then 1 else 0 end) as numStarted
,      sum(case when dj.finishedTime is null then @timeout -- should really be cast(datediff(second,startedTime, getDate()) as float)
                else cast(datediff(second,dj.startedTime, dj.finishedtime) as float)
				end ) as totTime
,      sum((case when dj.finishedTime is null then @timeout -- should really be cast(datediff(second,startedTime, getDate()) as float)
                else cast(datediff_big(millisecond,dj.startedTime, dj.finishedtime) as float)
				end )/datediff_big(millisecond,dj.startedTime,@currentDate)) as usageWeight
  from compm c , job dj 
 where c.uuid = @compmUUID
   and dj.computeDomainId=c.computeDomainId
   and dj.status > 1 -- PENDING
   and dj.startedTime >= dateadd(second,-@interval,getDate())
 group by submitterid
),
pending as (
select dj.*
,      rank() over (partition by submitterId order by submitTime) as rankSubmitted
  from compm c , job dj 
 where c.uuid = @compmUUID
   and dj.computeDomainId=c.computeDomainId
   and dj.status=1 -- PENDING
),
final as (
select j.Id as jobId,j.submitterId, j.computeDomainId, j.submitTime  -- without TOP cannot do an order by in a function
,      isnull(p.usageWeight,0) as usageWeight
,      isnull(p.numQueued,0) as numQueued
,      isnull(p.numStarted,0) as numStarted
,      rank() over (order by rankSubmitted,usageWeight,submitTime) as ranking
  from pending j left outer join prev p
  on p.submitterId = j.submitterid
  where j.rankSubmitted <=@maxNum
    and j.rankSubmitted+isnull(p.numQueued,0)+isnull(p.numStarted,0)<=@maxPerUser
)
insert into @rt
select * -- jobId,submitterId, computeDomainId, submitTime , usageWeight, numQueued, numStarted, ranking
  from final
 where ranking <= @maxNum -- this could be a parameter
--   and rankSubmitted+numQueued+numStarted<=@maxPerUser -- to counter possibility COMPM gets single user too many times in internal queue

  return 
end

GO
/****** Object:  Table [dbo].[roles]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[roles](
	[rolename] [varchar](128) NOT NULL
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_AccessControl]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_AccessControl](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[containerId] [bigint] NOT NULL,
	[scisEntityId] [bigint] NOT NULL,
 CONSTRAINT [pk_t_AccessControl_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_Action]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_Action](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[containerId] [bigint] NOT NULL,
	[name] [varchar](255) NOT NULL,
	[description] [varchar](max) NULL,
	[category] [varchar](255) NULL,
 CONSTRAINT [pk_t_Action_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_ActionExecution]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_ActionExecution](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[containerId] [bigint] NOT NULL,
	[executionDate] [datetime] NOT NULL,
	[status] [varchar](255) NOT NULL,
	[actionId] [bigint] NOT NULL,
	[userId] [bigint] NOT NULL,
	[resourceId] [bigint] NOT NULL,
 CONSTRAINT [pk_t_ActionExecution_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_COMPM]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_COMPM](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[uuid] [varchar](255) NOT NULL,
	[description] [varchar](max) NULL,
	[label] [varchar](255) NULL,
	[creatorUserid] [varchar](255) NOT NULL,
	[computeDomainId] [bigint] NULL,
	[creationDate] [datetime] NULL,
	[modificationDate] [datetime] NULL,
	[defaultJobTimeout] [int] NOT NULL DEFAULT ((28800)),
 CONSTRAINT [pk_t_COMPM_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_ComputeDomain]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_ComputeDomain](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[name] [varchar](255) NOT NULL,
	[description] [varchar](max) NULL,
	[apiEndpoint] [varchar](255) NOT NULL,
	[resourceContextId] [bigint] NULL,
	[creationDate] [datetime] NULL,
	[modificationDate] [datetime] NULL,
 CONSTRAINT [pk_t_ComputeDomain_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_ComputeResource]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_ComputeResource](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[containerId] [bigint] NOT NULL,
	[name] [varchar](255) NOT NULL,
	[description] [varchar](255) NULL,
	[resourceId] [bigint] NOT NULL,
 CONSTRAINT [pk_t_ComputeResource_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_ContextClass]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_ContextClass](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[name] [varchar](255) NOT NULL,
	[description] [varchar](max) NULL,
	[release] [varchar](255) NULL,
	[creatorId] [bigint] NULL,
	[creationDate] [datetime] NULL,
	[modificationDate] [datetime] NULL,
 CONSTRAINT [pk_t_ContextClass_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_DatabaseContext]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_DatabaseContext](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[containerId] [bigint] NOT NULL,
	[name] [varchar](255) NOT NULL,
	[description] [varchar](max) NULL,
	[resourceId] [bigint] NOT NULL,
 CONSTRAINT [pk_t_DatabaseContext_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_Dataset]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_Dataset](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[creationDate] [datetime] NULL,
	[modificationDate] [datetime] NULL,
 CONSTRAINT [pk_t_Dataset_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_DockerComputeDomain]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[t_DockerComputeDomain](
	[ID] [bigint] NOT NULL,
 CONSTRAINT [pk_t_DockerComputeDomain_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
/****** Object:  Table [dbo].[t_DockerImage]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[t_DockerImage](
	[ID] [bigint] NOT NULL,
 CONSTRAINT [pk_t_DockerImage_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
/****** Object:  Table [dbo].[t_DockerJob]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_DockerJob](
	[ID] [bigint] NOT NULL,
	[command] [varchar](max) NOT NULL,
	[scriptURI] [varchar](255) NULL,
	[fullDockerCommand] [varchar](max) NULL,
	[imageId] [bigint] NOT NULL,
 CONSTRAINT [pk_t_DockerJob_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_FileService]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_FileService](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[name] [varchar](255) NOT NULL,
	[description] [varchar](max) NULL,
	[apiEndpoint] [varchar](255) NOT NULL,
	[serviceToken] [varchar](255) NOT NULL,
	[resourceContextId] [bigint] NOT NULL,
	[creationDate] [datetime] NULL,
	[modificationDate] [datetime] NULL,
 CONSTRAINT [pk_t_FileService_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_History]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_History](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[creationDate] [datetime] NULL,
	[modificationDate] [datetime] NULL,
 CONSTRAINT [pk_t_History_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_Job]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_Job](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[submitTime] [datetime] NOT NULL,
	[expectedTime] [varchar](255) NULL,
	[status] [int] NOT NULL,
	[excutorDID] [varchar](255) NULL,
	[submitterDID] [varchar](255) NULL,
	[startedTime] [datetime] NULL,
	[finishedTime] [datetime] NULL,
	[duration] [float] NULL,
	[resultsFolderURI] [varchar](255) NULL,
	[runById] [bigint] NULL,
	[computeDomainId] [bigint] NOT NULL,
	[submitterId] [bigint] NOT NULL,
	[creationDate] [datetime] NULL,
	[modificationDate] [datetime] NULL,
	[timeout] [int] NULL,
 CONSTRAINT [pk_t_Job_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_JobMessage]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_JobMessage](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[containerId] [bigint] NOT NULL,
	[content] [varchar](max) NULL,
	[label] [varchar](255) NULL,
 CONSTRAINT [pk_t_JobMessage_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_Member]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_Member](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[containerId] [bigint] NOT NULL,
	[memberRole] [varchar](255) NULL,
	[status] [varchar](255) NOT NULL,
	[scisEntityId] [bigint] NOT NULL,
 CONSTRAINT [pk_t_Member_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_Party]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_Party](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[fullName] [varchar](255) NOT NULL,
	[affiliation] [varchar](255) NULL,
	[creationDate] [datetime] NULL,
	[modificationDate] [datetime] NULL,
 CONSTRAINT [pk_t_Party_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_Privilege]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[t_Privilege](
	[ID] [bigint] NOT NULL,
	[actionId] [bigint] NOT NULL,
 CONSTRAINT [pk_t_Privilege_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
/****** Object:  Table [dbo].[t_RDBComputeDomain]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_RDBComputeDomain](
	[ID] [bigint] NOT NULL,
	[vendor] [varchar](255) NOT NULL,
 CONSTRAINT [pk_t_RDBComputeDomain_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_RDBJob]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_RDBJob](
	[ID] [bigint] NOT NULL,
	[inputSQL] [varchar](max) NOT NULL,
	[sql] [varchar](max) NULL,
	[databaseContextId] [bigint] NOT NULL,
 CONSTRAINT [pk_t_RDBJob_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_RDBJobTarget]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_RDBJobTarget](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[containerId] [bigint] NOT NULL,
	[location] [varchar](255) NOT NULL,
	[targetType] [varchar](255) NOT NULL,
	[resultNumber] [smallint] NOT NULL,
 CONSTRAINT [pk_t_RDBJobTarget_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_RequiredUserVolume]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_RequiredUserVolume](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[containerId] [bigint] NOT NULL,
	[pathOnComputeDomain] [varchar](255) NULL,
	[needsWriteAccess] [bit] NULL,
	[userVolumeId] [bigint] NOT NULL,
 CONSTRAINT [pk_t_RequiredUserVolume_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_RequiredVolume]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_RequiredVolume](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[containerId] [bigint] NOT NULL,
	[volumeId] [bigint] NOT NULL,
	[needsWriteAccess] [bit] NULL DEFAULT ((0)),
 CONSTRAINT [pk_t_RequiredVolume_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_Resource]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_Resource](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[containerId] [bigint] NOT NULL,
	[name] [varchar](255) NULL,
	[description] [varchar](max) NULL,
	[uuid] [varchar](255) NOT NULL,
	[resourceTypeId] [bigint] NOT NULL,
 CONSTRAINT [pk_t_Resource_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_ResourceContext]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_ResourceContext](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[racmEndpoint] [varchar](255) NULL,
	[uuid] [varchar](255) NOT NULL,
	[label] [varchar](255) NULL,
	[description] [varchar](max) NULL,
	[contextClassId] [bigint] NOT NULL,
	[creationDate] [datetime] NULL,
	[modificationDate] [datetime] NULL,
 CONSTRAINT [pk_t_ResourceContext_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_ResourceType]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_ResourceType](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[containerId] [bigint] NOT NULL,
	[name] [varchar](255) NOT NULL,
	[description] [varchar](max) NULL,
 CONSTRAINT [pk_t_ResourceType_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_Response]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_Response](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[containerId] [bigint] NOT NULL,
	[messageId] [bigint] NOT NULL,
 CONSTRAINT [pk_t_Response_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_Role]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_Role](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[containerId] [bigint] NOT NULL,
	[name] [varchar](255) NOT NULL,
	[description] [varchar](max) NULL,
 CONSTRAINT [pk_t_Role_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_RoleAction]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_RoleAction](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[containerId] [bigint] NOT NULL,
	[actionId] [bigint] NOT NULL,
 CONSTRAINT [pk_t_RoleAction_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_RoleAssignment]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[t_RoleAssignment](
	[ID] [bigint] NOT NULL,
	[roleId] [bigint] NOT NULL,
 CONSTRAINT [pk_t_RoleAssignment_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
/****** Object:  Table [dbo].[t_RootVolume]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_RootVolume](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[containerId] [bigint] NOT NULL,
	[name] [varchar](255) NOT NULL,
	[description] [varchar](255) NULL,
	[pathOnFileSystem] [varchar](255) NOT NULL,
	[containsSharedVolumes] [bit] NOT NULL,
	[resourceId] [bigint] NOT NULL,
 CONSTRAINT [pk_t_RootVolume_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_RootVolumeOnComputeDomain]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_RootVolumeOnComputeDomain](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[containerId] [bigint] NOT NULL,
	[path] [varchar](255) NOT NULL,
	[displayName] [varchar](255) NULL,
	[rootVolumeId] [bigint] NOT NULL,
 CONSTRAINT [pk_t_RootVolumeOnComputeDomain_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_SciserverEntity]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_SciserverEntity](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[creationDate] [datetime] NULL,
	[modificationDate] [datetime] NULL,
 CONSTRAINT [pk_t_SciserverEntity_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_User]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_User](
	[ID] [bigint] NOT NULL,
	[userId] [varchar](128) NOT NULL,
	[username] [varchar](255) NOT NULL,
	[contactEmail] [varchar](255) NULL,
	[trustId] [varchar](255) NULL,
	[visibility] [varchar](255) NOT NULL,
	[preferences] [varchar](max) NULL,
	[partyId] [bigint] NULL,
 CONSTRAINT [pk_t_User_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_UserGroup]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_UserGroup](
	[ID] [bigint] NOT NULL,
	[name] [varchar](255) NOT NULL,
	[description] [varchar](max) NULL,
	[ownerId] [bigint] NOT NULL,
 CONSTRAINT [pk_t_UserGroup_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_UserVolume]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_UserVolume](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[containerId] [bigint] NOT NULL,
	[relativePath] [varchar](255) NOT NULL,
	[description] [varchar](255) NULL,
	[name] [varchar](255) NULL,
	[resourceId] [bigint] NOT NULL,
	[rootVolumeId] [bigint] NOT NULL,
	[ownerId] [bigint] NOT NULL,
 CONSTRAINT [pk_t_UserVolume_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_VolumeContainer]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[t_VolumeContainer](
	[ID] [bigint] NOT NULL,
 CONSTRAINT [pk_t_VolumeContainer_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
/****** Object:  Table [dbo].[t_Workspace]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_Workspace](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[name] [varchar](max) NOT NULL,
	[description] [varchar](255) NULL,
	[resourceId] [bigint] NOT NULL,
	[membersId] [bigint] NOT NULL,
	[creationDate] [datetime] NULL,
	[modificationDate] [datetime] NULL,
 CONSTRAINT [pk_t_Workspace_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[t_WorkspaceUserVolume]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[t_WorkspaceUserVolume](
	[ID] [bigint] IDENTITY(1,1) NOT NULL,
	[DTYPE] [varchar](32) NULL,
	[OPTLOCK] [int] NULL,
	[publisherDID] [varchar](256) NULL,
	[containerId] [bigint] NOT NULL,
	[userVolumeId] [bigint] NOT NULL,
 CONSTRAINT [pk_t_WorkspaceUserVolume_ID] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  View [dbo].[Member]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[Member] AS SELECT ID ,      publisherDID ,      DTYPE ,      containerId ,      [memberRole] ,      [status] ,      scisEntityId FROM t_Member 
GO
/****** Object:  View [racm].[TrueMember]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
   create view [racm].[TrueMember] as select * from member where status in ('ACCEPTED','OWNER')  
GO
/****** Object:  Table [dbo].[user_roles]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[user_roles](
	[username] [varchar](128) NOT NULL,
	[rolename] [varchar](128) NOT NULL
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[users]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[users](
	[username] [varchar](128) NOT NULL,
	[password] [varchar](128) NULL,
	[md5password] [varchar](128) NULL,
	[createDate] [datetime] NULL
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  View [dbo].[AccessControl]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[AccessControl] AS SELECT ID ,      publisherDID ,      DTYPE ,      containerId ,      scisEntityId FROM t_AccessControl 
GO
/****** Object:  View [dbo].[Action]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[Action] AS SELECT ID ,      publisherDID ,      DTYPE ,      containerId ,      [name] ,      [description] ,      [category] FROM t_Action 
GO
/****** Object:  View [dbo].[ActionExecution]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[ActionExecution] AS SELECT ID ,      publisherDID ,      DTYPE ,      containerId ,      [executionDate] ,      [status] ,      actionId ,      userId ,      resourceId FROM t_ActionExecution 
GO
/****** Object:  View [dbo].[COMPM]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[COMPM] AS SELECT ID ,      publisherDID ,      DTYPE ,      [uuid] ,      [description] ,      
  [label] ,      [creatorUserid] ,      computeDomainId ,defaultJobTimeout FROM t_COMPM 

GO
/****** Object:  View [dbo].[ComputeDomain]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[ComputeDomain] AS SELECT ID ,      publisherDID ,      DTYPE ,      [name] ,      [description] ,      [apiEndpoint] ,      resourceContextId FROM t_ComputeDomain 
GO
/****** Object:  View [dbo].[ComputeResource]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[ComputeResource] AS SELECT ID ,      publisherDID ,      DTYPE ,      containerId ,      [name] ,      [description] ,      resourceId FROM t_ComputeResource 
GO
/****** Object:  View [dbo].[ContextClass]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[ContextClass] AS SELECT ID ,      publisherDID ,      DTYPE ,      [name] ,      [description] ,      [release] ,      creatorId FROM t_ContextClass 
GO
/****** Object:  View [dbo].[DatabaseContext]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[DatabaseContext] AS SELECT ID ,      publisherDID ,      DTYPE ,      containerId ,      [name] ,      [description] ,      resourceId FROM t_DatabaseContext 
GO
/****** Object:  View [dbo].[Dataset]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[Dataset] AS SELECT ID ,      publisherDID ,      DTYPE FROM t_Dataset 
GO
/****** Object:  View [dbo].[DockerComputeDomain]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[DockerComputeDomain] AS SELECT b.* FROM t_DockerComputeDomain t ,    [ComputeDomain] b WHERE b.ID = t.ID 
GO
/****** Object:  View [dbo].[DockerImage]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[DockerImage] AS SELECT b.* FROM t_DockerImage t ,    [ComputeResource] b WHERE b.ID = t.ID 
GO
/****** Object:  View [dbo].[Job]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

  CREATE VIEW [dbo].[Job] AS SELECT ID ,      publisherDID ,      DTYPE ,      [submitTime] ,      [expectedTime] ,      [status] ,      [excutorDID] ,      [submitterDID] ,      [startedTime] ,      [finishedTime] ,      [duration] ,      [resultsFolderURI] ,      runById ,      computeDomainId ,      submitterId, timeout FROM t_Job 

GO
/****** Object:  View [dbo].[DockerJob]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[DockerJob] AS SELECT b.* ,      t.[command] ,      t.[scriptURI] ,      t.[fullDockerCommand] ,      t.imageId FROM t_DockerJob t ,    [Job] b WHERE b.ID = t.ID 
GO
/****** Object:  View [dbo].[FileService]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[FileService] AS SELECT ID ,      publisherDID ,      DTYPE ,      [name] ,      [description] ,      [apiEndpoint] ,      [serviceToken] ,      resourceContextId FROM t_FileService 
GO
/****** Object:  View [dbo].[History]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[History] AS SELECT ID ,      publisherDID ,      DTYPE FROM t_History 
GO
/****** Object:  View [dbo].[JobMessage]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[JobMessage] AS SELECT ID ,      publisherDID ,      DTYPE ,      containerId ,      [content] ,      [label] FROM t_JobMessage 
GO
/****** Object:  View [dbo].[Party]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[Party] AS SELECT ID ,      publisherDID ,      DTYPE ,      [fullName] ,      [affiliation] FROM t_Party 
GO
/****** Object:  View [dbo].[Privilege]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[Privilege] AS SELECT b.* ,      t.actionId FROM t_Privilege t ,    [AccessControl] b WHERE b.ID = t.ID 
GO
/****** Object:  View [dbo].[RDBComputeDomain]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[RDBComputeDomain] AS SELECT b.* ,      t.[vendor] FROM t_RDBComputeDomain t ,    [ComputeDomain] b WHERE b.ID = t.ID 
GO
/****** Object:  View [dbo].[RDBJob]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[RDBJob] AS SELECT b.* ,      t.[inputSQL] ,      t.[sql] ,      t.databaseContextId FROM t_RDBJob t ,    [Job] b WHERE b.ID = t.ID 
GO
/****** Object:  View [dbo].[RDBJobTarget]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[RDBJobTarget] AS SELECT ID ,      publisherDID ,      DTYPE ,      containerId ,      [location] ,      [targetType] ,      [resultNumber] FROM t_RDBJobTarget 
GO
/****** Object:  View [dbo].[RequiredUserVolume]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[RequiredUserVolume] AS SELECT ID ,      publisherDID ,      DTYPE ,      containerId ,      [pathOnComputeDomain] ,      [needsWriteAccess] ,      userVolumeId FROM t_RequiredUserVolume 
GO
/****** Object:  View [dbo].[RequiredVolume]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[RequiredVolume] AS SELECT ID ,      publisherDID ,      DTYPE ,      containerId ,      volumeId FROM t_RequiredVolume 
GO
/****** Object:  View [dbo].[Resource]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[Resource] AS SELECT ID ,      publisherDID ,      DTYPE ,      containerId ,      [name] ,      [description] ,      [uuid] ,      resourceTypeId FROM t_Resource 
GO
/****** Object:  View [dbo].[ResourceContext]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[ResourceContext] AS SELECT ID ,      publisherDID ,      DTYPE ,      [racmEndpoint] ,      [uuid] ,      [label] ,      [description] ,      contextClassId FROM t_ResourceContext 
GO
/****** Object:  View [dbo].[ResourceType]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[ResourceType] AS SELECT ID ,      publisherDID ,      DTYPE ,      containerId ,      [name] ,      [description] FROM t_ResourceType 
GO
/****** Object:  View [dbo].[Response]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[Response] AS SELECT ID ,      publisherDID ,      DTYPE ,      containerId ,      messageId FROM t_Response 
GO
/****** Object:  View [dbo].[Role]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[Role] AS SELECT ID ,      publisherDID ,      DTYPE ,      containerId ,      [name] ,      [description] FROM t_Role 
GO
/****** Object:  View [dbo].[RoleAction]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[RoleAction] AS SELECT ID ,      publisherDID ,      DTYPE ,      containerId ,      actionId FROM t_RoleAction 
GO
/****** Object:  View [dbo].[RoleAssignment]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[RoleAssignment] AS SELECT b.* ,      t.roleId FROM t_RoleAssignment t ,    [AccessControl] b WHERE b.ID = t.ID 
GO
/****** Object:  View [dbo].[RootVolume]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[RootVolume] AS SELECT ID ,      publisherDID ,      DTYPE ,      containerId ,      [name] ,      [description] ,      [pathOnFileSystem] ,      [containsSharedVolumes] ,      resourceId FROM t_RootVolume 
GO
/****** Object:  View [dbo].[RootVolumeOnComputeDomain]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[RootVolumeOnComputeDomain] AS SELECT ID ,      publisherDID ,      DTYPE ,      containerId ,      [path] ,      [displayName] ,      rootVolumeId FROM t_RootVolumeOnComputeDomain 
GO
/****** Object:  View [dbo].[SciserverEntity]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[SciserverEntity] AS SELECT ID ,      publisherDID ,      DTYPE FROM t_SciserverEntity 
GO
/****** Object:  View [dbo].[User]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[User] AS SELECT b.* ,      t.[userId] ,      t.[username] ,      t.[contactEmail] ,      t.[trustId] ,      t.[visibility] ,      t.[preferences] ,      t.partyId FROM t_User t ,    [SciserverEntity] b WHERE b.ID = t.ID 
GO
/****** Object:  View [dbo].[UserGroup]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[UserGroup] AS SELECT b.* ,      t.[name] ,      t.[description] ,      t.ownerId FROM t_UserGroup t ,    [SciserverEntity] b WHERE b.ID = t.ID 
GO
/****** Object:  View [dbo].[UserVolume]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[UserVolume] AS SELECT ID ,      publisherDID ,      DTYPE ,      containerId ,      [relativePath] ,      [description] ,      [name] ,      resourceId ,      rootVolumeId ,      ownerId FROM t_UserVolume 
GO
/****** Object:  View [dbo].[VolumeContainer]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[VolumeContainer] AS SELECT b.* FROM t_VolumeContainer t ,    [ComputeResource] b WHERE b.ID = t.ID 
GO
/****** Object:  View [dbo].[Workspace]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[Workspace] AS SELECT ID ,      publisherDID ,      DTYPE ,      [name] ,      [description] ,      resourceId ,      membersId FROM t_Workspace 
GO
/****** Object:  View [dbo].[WorkspaceUserVolume]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  CREATE VIEW [dbo].[WorkspaceUserVolume] AS SELECT ID ,      publisherDID ,      DTYPE ,      containerId ,      userVolumeId FROM t_WorkspaceUserVolume 
GO
/****** Object:  View [racm].[ActionAssignments]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  create view [racm].[ActionAssignments] as select rc.UUID as resourceContextUUID ,      rc.racmEndpoint as resourceContextAPIEndpoint ,      r.publisherDID as resourcePubDID ,      r.id as resourceId ,      r.uuid as resourceUUID ,      r.name as resourceName ,      cc.name as contextClass ,      rt.name as resourceType ,      ro.name as role ,      a.name as action ,      a.id as actionId ,      a.category as actionCategory ,      ras.scisEntityId as scisId from resourcecontext rc inner join resource r on r.containerId = rc.id inner join roleassignment ras on ras.containerId = r.id inner join role ro on ro.id=ras.roleid inner join roleaction ra on ro.id=ra.containerId inner join action a on ra.actionId=a.id inner join resourcetype rt on rt.id=a.containerId inner join contextclass cc on cc.id=rt.containerId union select rc.UUID as resourceContextUUID ,      rc.racmEndpoint ,      r.publisherDID ,      r.id ,      r.uuid ,      r.name ,      cc.name ,      rt.name ,      null ,      a.name ,      a.id ,      a.category ,      p.scisEntityId from resourcecontext rc inner join resource r on r.containerId = rc.id inner join privilege p on p.containerId = r.id inner join action a on p.actionId=a.id inner join resourcetype rt on rt.id=a.containerId inner join contextclass cc on cc.id=rt.containerId  
GO
/****** Object:  View [racm].[GroupMembers]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  create view [racm].[GroupMembers] as select ug.id as groupid, ug.name as groupname, o.username as [owner], u.username , m.memberRole from usergroup ug ,    racm.truemember m ,    [user] u ,    [user] o where m.containerId = ug.id and u.id = m.scisentityid and o.id=ug.ownerId 
GO
/****** Object:  View [racm].[myDockerJobs]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
   create view [racm].[myDockerJobs] as select j.id,status,j.submitterDID ,   j.submitTime,j.startedTime,j.finishedTime,j.duration ,   j.resultsFolderURI ,   cd.name as computeDomainName, cd.apiEndpoint, rc.uuid as resourceContextUUID ,   j.submitterId,u.username, u.trustId ,   j.command,j.fullDockerCommand as fullCommand, j.scriptURI ,   j.imageId, im.name as dockerImage from DockerJob j inner join DockerComputeDomain cd on j.computeDomainId=cd.id inner join ResourceContext rc on rc.id=cd.resourceContextId inner join [User] u on u.id=j.submitterId inner join DockerImage im on im.id=j.imageId 
GO
/****** Object:  View [racm].[RoleAssignments]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  create view [racm].[RoleAssignments] as select rc.UUID as resourceContextUUID ,      rc.racmEndpoint as resourceContextAPIEndpoint ,      r.publisherDID as resourcePubDID ,      r.uuid as resourceUUID ,      r.name as resourceName ,      cc.name as contextClass ,      rt.name as resourceType ,      ro.name as role ,      ras.scisEntityId as scisId from resourcecontext rc inner join resource r on r.containerId = rc.id inner join roleassignment ras on ras.containerId = r.id inner join role ro on ro.id=ras.roleid inner join resourcetype rt on rt.id=ro.containerId inner join contextclass cc on cc.id=rt.containerId  
GO
/****** Object:  View [racm].[RootActions]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  create view [racm].[RootActions] as select a.name as action ,      a.id as actionId from ContextClass cc ,    resourcetype rt ,    [action] a where cc.name='System' and cc.id=rt.containerId and rt.name='__rootcontext__' and a.containerId=rt.id  
GO
/****** Object:  View [racm].[UserRDBJobs]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
   create view [racm].[UserRDBJobs] as select j.ID as jobId,j.publisherDID,submitTime,expectedTime,status,submitterDID ,	startedTime,finishedTime,duration,resultsFolderURI,c.label, c.uuid ,	cd.apiEndpoint, cd.description, cd.vendor,u.username,inputSQL,sql ,   db.name from RDBJob j inner join [User] u on u.id=j.submitterId inner join RDBComputeDomain cd on cd.id=j.computeDomainId inner join DatabaseContext db on db.id=j.databaseContextId left outer join COMPM c on c.id=j.runById  
GO
/****** Object:  UserDefinedFunction [racm].[userActions]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
   create function [racm].[userActions](@username varchar(32)) returns table as return with entities as ( select id, username, Sentinel = CAST('' AS VARCHAR(MAX)), 'U' as privEntity from [user] u where u.username=@username union all select g.id, g.name , Sentinel + convert(varchar,g.id)+'|', 'G' from entities e ,    racm.truemember m ,    usergroup g where m.scisEntityId=e.id and g.id=m.containerid and CHARINDEX(convert(varchar,g.id),Sentinel)=0 ) select a.*, e.username, e.id, e.privEntity from entities e inner join racm.actionassignments a on a.scisId=e.id  
GO
/****** Object:  UserDefinedFunction [racm].[AccessibleDockerImages]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  create function [racm].[AccessibleDockerImages](@username varchar(32)) returns table as return select di.name as imageName,a.* from racm.userActions(@username) a ,    dockerimage di where di.resourceId=a.resourceId  
GO
/****** Object:  UserDefinedFunction [racm].[AccessibleVolumeContainers]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  create function [racm].[AccessibleVolumeContainers](@username varchar(32)) returns table as return select vc.name as containerName,a.* from racm.userActions(@username) a ,    VolumeContainer vc where vc.resourceId=a.resourceId  
GO
/****** Object:  UserDefinedFunction [racm].[CanceledCOMPMJobs]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
   /* Return all jobs for a COMPM that have not yet finished but have received a CANCEL message. May need a special index to execute efficiently. */ create function [racm].[CanceledCOMPMJobs] (@uuid varchar(255)) returns table as return select distinct j.id as jobId, c.uuid as compmUUID from COMPM c inner loop join Job j on j.runById=c.id and j.status<16 inner loop join jobmessage jm on jm.containerId=j.id and jm.label='CANCEL' where c.uuid=@uuid   
GO
/****** Object:  UserDefinedFunction [racm].[canUserDoAction]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
   create function [racm].[canUserDoAction] (@resourceuuid varchar(128), @action varchar(32), @username varchar(32)) returns table as return with entities as ( select id, username, Sentinel = CAST('' AS VARCHAR(MAX)), 'U' as privEntity from [user] u where u.username=@username union all select g.id, g.name , Sentinel + convert(varchar,g.id)+'|', 'G' from entities e ,    racm.truemember m ,    usergroup g where m.scisEntityId=e.id and g.id=m.containerid and CHARINDEX(convert(varchar,g.id),Sentinel)=0 )select a.*, e.username, e.id, e.privEntity from entities e inner join racm.actionassignments a on a.scisId = e.id and a.resourceUUID=@resourceuuid and a.action=@action   
GO
/****** Object:  UserDefinedFunction [racm].[canUserDoActionDEPRECATED]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  create function [racm].[canUserDoActionDEPRECATED] (@cid varchar(128), @rid varchar(128), @action varchar(32), @username varchar(32)) returns table as return with entities as ( select id, username, Sentinel = CAST('' AS VARCHAR(MAX)), 'U' as privEntity from [user] u where u.username=@username union all select g.id, g.name , Sentinel + convert(varchar,g.id)+'|', 'G' from entities e ,    racm.truemember m ,    usergroup g where m.scisEntityId=e.id and g.id=m.containerid and CHARINDEX(convert(varchar,g.id),Sentinel)=0 )select a.*, e.username, e.id, e.privEntity from entities e inner join racm.actionassignments a on a.scisId = e.id and a.resourceContextUUID=@cid and a.resourcePubDID=@rid and a.action=@action   
GO
/****** Object:  UserDefinedFunction [racm].[canUserDoRootAction]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
   create function [racm].[canUserDoRootAction] (@username varchar(32), @action varchar(32)) returns table as return with entities as ( select id, username, Sentinel = CAST('' AS VARCHAR(MAX)), 'U' as privEntity from [user] u where u.username=@username union all select g.id, g.name , Sentinel + convert(varchar,g.id)+'|', 'G' from entities e ,    racm.truemember m ,    usergroup g where m.scisEntityId=e.id and g.id=m.containerid and CHARINDEX(convert(varchar,g.id),Sentinel)=0 )select a.*, e.username, e.id, e.privEntity from entities e inner join racm.actionassignments a on a.scisId = e.id inner join racm.RootActions ra on a.actionId=ra.actionId and ra.action=@action   
GO
/****** Object:  UserDefinedFunction [racm].[canUserGrant]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
   create function [racm].[canUserGrant] (@cid varchar(128), @rid varchar(128), @username varchar(32)) returns table as return with entities as ( select id, username, Sentinel = CAST('' AS VARCHAR(MAX)), 'U' as privEntity from [user] u where u.username=@username union all select g.id, g.name , Sentinel + convert(varchar,g.id)+'|', 'G' from entities e ,    racm.truemember m ,    usergroup g where m.scisEntityId=e.id and g.id=m.containerid and CHARINDEX(convert(varchar,g.id),Sentinel)=0 )select a.*, e.username, e.id, e.privEntity from entities e inner join racm.actionassignments a on a.scisId = e.id and a.resourceContextUUID=@cid and a.resourcePubDID=@rid and a.actionCategory='G' 
GO
/****** Object:  UserDefinedFunction [racm].[doesUserHaveRole]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
    create function [racm].[doesUserHaveRole] (@cid varchar(128), @rid varchar(128), @role varchar(32), @username varchar(32)) returns table as return with entities as ( select id, username, Sentinel = CAST('' AS VARCHAR(MAX)), 'U' as privEntity from [user] u where u.username=@username union all select g.id, g.name , Sentinel + convert(varchar,g.id)+'|', 'G' from entities e ,    racm.truemember m ,    usergroup g where m.scisEntityId=e.id and g.id=m.containerid and CHARINDEX(convert(varchar,g.id),Sentinel)=0 )select ra.*, e.username, e.id, e.privEntity from entities e inner join racm.RoleAssignments ra on ra.scisId = e.id and ra.resourceContextUUID=@cid and ra.resourcePubDID=@rid and ra.role=@role   
GO
/****** Object:  UserDefinedFunction [racm].[groupActions]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
   create function [racm].[groupActions](@groupid bigint) returns table as return with entities as ( select id, name, Sentinel = CAST('' AS VARCHAR(MAX)) from [usergroup] where id=@groupid union all select g.id, g.name , Sentinel + convert(varchar,g.id)+'|' from entities e ,    racm.truemember m ,    usergroup g where m.scisEntityId=e.id and g.id=m.containerid and CHARINDEX(convert(varchar,g.id),Sentinel)=0 ) select a.*, e.name, e.id from entities e inner join racm.actionassignments a on a.scisId=e.id  
GO
/****** Object:  UserDefinedFunction [racm].[groupFriends]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  create function [racm].[groupFriends](@username varchar(64)) returns table as return select ug.id as groupId ,      mo.memberRole ,      o.id as memberuserid ,      o.username as memberName ,      o.contactEmail as memberEmail ,      p.fullName ,      p.affiliation from [user] u inner join racm.truemember m on m.scisEntityId=u.id inner join UserGroup ug on m.containerId = ug.id inner join racm.TrueMember mo on mo.containerId=ug.id inner join [User] o on o.id=mo.scisEntityId left outer join Party p on p.id=o.partyId where u.username=@username and ug.name != 'public' 
GO
/****** Object:  UserDefinedFunction [racm].[groupResourcesForUser]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
   create function [racm].[groupResourcesForUser](@username varchar(64)) returns table as return select distinct ug.id as groupid ,      ug.name as groupName ,      ga.contextClass ,      ga.resourceContextAPIEndpoint ,      ga.resourceType ,      ga.resourceName ,      ga.resourcePubDID ,      ga.resourceId ,      ga.action from [User] u inner join Member m on m.scisEntityId=u.id inner join UserGroup ug on ug.id=m.containerId and ug.name != 'public' cross apply racm.groupActions(ug.id) ga join racm.userActions(@username) ua on ua.resourceId=ga.resourceid where u.username=@username   
GO
/****** Object:  UserDefinedFunction [racm].[mountableUserVolumes]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  create function [racm].[mountableUserVolumes](@username varchar(32) ) returns table as return  select rvcd.containerId as computeDomainId ,      uv.id as userVolumeId ,      a.resourceUUID ,      rv.containsSharedVolumes as isShareable ,      rvcd.path as rvPath ,      uv.relativePath as uvRelativePath ,      uv.name as displayName ,      rvcd.publisherDID ,      u.username as owner ,      u.userId as ownerId ,      uv.description ,      fs.apiEndpoint as fileServiceAPIEndpoint ,      rv.name as rootVolumeName ,      a.action as action from racm.useractions(@username) a inner join UserVolume uv on uv.resourceId = a.resourceId inner join [User] u on u.id=uv.ownerId inner join RootVolume rv on rv.id = uv.rootVolumeId inner join FileService fs on rv.containerId=fs.id inner join RootVolumeOnComputeDomain rvcd on rvcd.rootVolumeId=rv.id 
GO
/****** Object:  UserDefinedFunction [racm].[userActionsOnContext]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
   create function [racm].[userActionsOnContext] (@cid varchar(128), @username varchar(32)) returns table as return with entities as ( select id, username, Sentinel = CAST('' AS VARCHAR(MAX)), 'U' as privEntity from [user] u where u.username=@username union all select g.id, g.name , Sentinel + convert(varchar,g.id)+'|', 'G' from entities e ,    racm.truemember m ,    usergroup g where m.scisEntityId=e.id and g.id=m.containerid and CHARINDEX(convert(varchar,g.id),Sentinel)=0 ) select a.*, e.username, e.id, e.privEntity from entities e inner join racm.actionassignments a on a.scisId = e.id and a.resourceContextUUID=@cid  
GO
/****** Object:  UserDefinedFunction [racm].[userGroups]    Script Date: 8/7/2018 3:31:56 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
  create function [racm].[userGroups](@username varchar(64)) returns table as return with gs as ( select distinct ug.id from [user] u inner join racm.TrueMember m on m.scisEntityId = u.id inner join usergroup ug on m.containerId=ug.id and ug.name != 'public' where u.username=@username ) select ug.id,ug.name, ug.description from gs, usergroup ug where ug.id=gs.id 
GO
SET IDENTITY_INSERT [dbo].[t_AccessControl] ON 

GO
INSERT [dbo].[t_AccessControl] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [scisEntityId]) VALUES (1, N'Privilege', 1, NULL, 1, 4)
GO
INSERT [dbo].[t_AccessControl] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [scisEntityId]) VALUES (2, N'Privilege', 1, NULL, 1, 1)
GO
INSERT [dbo].[t_AccessControl] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [scisEntityId]) VALUES (3, N'Privilege', 1, NULL, 1, 4)
GO
INSERT [dbo].[t_AccessControl] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [scisEntityId]) VALUES (4, N'RoleAssignment', 1, NULL, 1, 3)
GO
INSERT [dbo].[t_AccessControl] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [scisEntityId]) VALUES (5, N'RoleAssignment', 1, NULL, 1, 1)
GO
INSERT [dbo].[t_AccessControl] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [scisEntityId]) VALUES (10171, N'RoleAssignment', 1, NULL, 1, 5)
GO
SET IDENTITY_INSERT [dbo].[t_AccessControl] OFF
GO
SET IDENTITY_INSERT [dbo].[t_Action] ON 

GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (1, N'Action', 1, NULL, 10, N'grant', N'Action of granting the right to create a container for this image.', N'G')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (2, N'Action', 1, NULL, 9, N'registerDatabaseContext', N'Action to register a new context', N'C')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (3, N'Action', 1, NULL, 8, N'use', N'Action to use a certain jobtype', N'R')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (4, N'Action', 1, NULL, 4, N'delete', N'Action of deleting the container.', N'D')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (5, N'Action', 1, NULL, 7, N'read', N'Action of attaching a shared volume container when creating a docker container', N'R')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (6, N'Action', 1, NULL, 1, N'registerDockerImage', N'Action of creating a docker image.', N'C')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (7, N'Action', 1, NULL, 5, N'createContextClass', N'This action represents the creation of a ContextClass on the System', N'C')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (8, N'Action', 1, NULL, 10, N'createContainer', N'Action of creating a container for this image.', N'X')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (9, N'Action', 1, NULL, 7, N'grant', N'Action of granting access to a shared volume container.', N'G')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (10, N'Action', 1, NULL, 4, N'stop', N'Action of stopping the container.', N'X')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (11, N'Action', 1, NULL, 6, N'submitQuery', N'Action corresponding to submitting a query to a database context', NULL)
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (12, N'Action', 1, NULL, 1, N'registerVolumeContainer', N'Action of creating a public volume container.', N'C')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (13, N'Action', 1, NULL, 8, N'update', N'Action to update or delete a jobtype', N'U')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (14, N'Action', 1, NULL, 5, N'createContext', N'This action represents the creation of a Context on the System', N'C')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (15, N'Action', 1, NULL, 2, N'view', NULL, N'R')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (16, N'Action', 1, NULL, 5, N'grantRootPrivilege', N'Grant privileges at the ''context'' level to a user. I.e. provides the ''admin'' role.', N'G')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (17, N'Action', 1, NULL, 5, N'createGroup', N'This action represents the creation of a UserGroup on the System', N'C')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (18, N'Action', 1, NULL, 4, N'grant', N'Action of granting access to a container.', N'G')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (19, N'Action', 1, NULL, 4, N'write', N'Action of accessing the contents of the container in read-write mode.', N'U')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (20, N'Action', 1, NULL, 6, N'viewSchema', N'Action corresponding to viewing the schema, i.e. contents of the database', N'R')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (21, N'Action', 1, NULL, 5, N'registerComputeDomain', N'This action represents the registration of a Compute Domain on the System', N'C')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (22, N'Action', 1, NULL, 3, N'submitJob', NULL, N'C')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (23, N'Action', 1, NULL, 5, N'queryJOQL', N'This action represents submitting a JOQL query to the system.', N'X')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (24, N'Action', 1, NULL, 4, N'start', N'Action of stopping the container.', N'X')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (25, N'Action', 1, NULL, 5, N'registerCOMPM', N'This action represents the registration of a running COMPM instance on the System', N'C')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (26, N'Action', 1, NULL, 4, N'read', N'Action of accessing the contents of the container in read-only mode.', N'R')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (27, N'Action', 1, NULL, 7, N'write', N'Action of updating a shared volume container.', N'U')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (28, N'Action', 1, NULL, 3, N'defineJobType', NULL, N'C')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (29, N'Action', 1, NULL, 10, N'unregister', N'Action of unregistering the docker image.', N'D')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (30, N'Action', 1, NULL, 14, N'GRANT', N'Action of granting access to a database context.', N'G')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (31, N'Action', 1, NULL, 13, N'grant', N'Grant access to a user volume', N'G')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (32, N'Action', 1, NULL, 14, N'QUERY', N'Action of connecting to and sending a query to a database context.', N'R')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (33, N'Action', 1, NULL, 11, N'registerDatabaseContext', N'Action of creating a database context.', N'C')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (34, N'Action', 1, NULL, 13, N'read', N'Read a User Volume', N'R')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (35, N'Action', 1, NULL, 12, N'edit', N'Edit the metadata for a File Service', N'U')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (36, N'Action', 1, NULL, 15, N'grant', N'Grant access to a root volume', N'G')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (37, N'Action', 1, NULL, 12, N'registerUserVolume', N'Create a new User Volume', N'C')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (38, N'Action', 1, NULL, 14, N'UPDATE', N'Action of updating a database context, creating/deleting tables/views/indexes/functions; inserting, updating, deleting from tables; etc.', N'U')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (39, N'Action', 1, NULL, 13, N'write', N'Write to a User Volume', N'U')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (40, N'Action', 1, NULL, 15, N'create', N'Create a new User Volume', N'C')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (41, N'Action', 1, NULL, 13, N'delete', N'Delete a User volume', N'D')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (42, N'Action', 1, NULL, 5, N'registerFileService', N'register a file service', N'C')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (43, N'Action', 1, NULL, 1, N'grant', N'grant', N'G')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (44, N'Action', 1, NULL, 11, N'grant', N'grant', N'G')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (45, N'Action', 1, NULL, 6, N'grant', N'grant', N'G')
GO
INSERT [dbo].[t_Action] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category]) VALUES (46, N'Action', 1, NULL, 9, N'grant', N'grant', N'G')
GO
SET IDENTITY_INSERT [dbo].[t_Action] OFF
GO
SET IDENTITY_INSERT [dbo].[t_ContextClass] ON 

GO
INSERT [dbo].[t_ContextClass] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [name], [description], [release], [creatorId], [creationDate], [modificationDate]) VALUES (1, N'ContextClass', 2, NULL, N'DockerComputeDomain', N'Represents SciServer Compute Domains.', N'alpha01', 1, GETDATE(), NULL)
GO
INSERT [dbo].[t_ContextClass] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [name], [description], [release], [creatorId], [creationDate], [modificationDate]) VALUES (2, N'ContextClass', 1, NULL, N'JOBM', N'Context class representing the Job submission component', N'v0.1-20160804', 2, GETDATE(), NULL)
GO
INSERT [dbo].[t_ContextClass] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [name], [description], [release], [creatorId], [creationDate], [modificationDate]) VALUES (3, N'ContextClass', 1, NULL, N'CasJobs', N'Context class representing the CasJobs  component', N'v1.0-20160804', 1, GETDATE(), NULL)
GO
INSERT [dbo].[t_ContextClass] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [name], [description], [release], [creatorId], [creationDate], [modificationDate]) VALUES (4, N'ContextClass', 1, NULL, N'System', N'This is the context ContextClass. It is required so that actions can be defined at the system level. Examples are createContextClass, createContext.', N'0.x', 1, GETDATE(), NULL)
GO
INSERT [dbo].[t_ContextClass] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [name], [description], [release], [creatorId], [creationDate], [modificationDate]) VALUES (5, N'ContextClass', 1, NULL, N'FileService', N'Context class representing File Services', N'v0.1-20170911', 1, GETDATE(), NULL)
GO
INSERT [dbo].[t_ContextClass] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [name], [description], [release], [creatorId], [creationDate], [modificationDate]) VALUES (6, N'ContextClass', 2, NULL, N'RDBComputeDomain', N'Represents SciServer RDB Compute Domains.', N'', 1, GETDATE(), NULL)
GO
SET IDENTITY_INSERT [dbo].[t_ContextClass] OFF
GO
SET IDENTITY_INSERT [dbo].[t_Member] ON 

GO
INSERT [dbo].[t_Member] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [memberRole], [status], [scisEntityId]) VALUES (1, N'Member', 1, NULL, 3, N'MEMBER', N'ACCEPTED', 5)
GO
INSERT [dbo].[t_Member] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [memberRole], [status], [scisEntityId]) VALUES (2, N'Member', 1, NULL, 5, N'OWNER', N'ACCEPTED', 1)
GO
INSERT [dbo].[t_Member] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [memberRole], [status], [scisEntityId]) VALUES (55, N'Member', 1, NULL, 4, N'OWNER', N'OWNER', 1)
GO
INSERT [dbo].[t_Member] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [memberRole], [status], [scisEntityId]) VALUES (56, N'Member', 1, NULL, 3, N'OWNER', N'OWNER', 1)
GO
SET IDENTITY_INSERT [dbo].[t_Member] OFF
GO
INSERT [dbo].[t_Privilege] ([ID], [actionId]) VALUES (1, 21)
GO
INSERT [dbo].[t_Privilege] ([ID], [actionId]) VALUES (2, 16)
GO
INSERT [dbo].[t_Privilege] ([ID], [actionId]) VALUES (3, 25)
GO
SET IDENTITY_INSERT [dbo].[t_Resource] ON 

GO
INSERT [dbo].[t_Resource] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [uuid], [resourceTypeId]) VALUES (1, N'Resource', 1, N'__rootcontext__', 1, N'__rootcontext__', N'The root Resource for its container ResourceContext, corresponding to root ResourceType of its ContainerClass. Used for associating privileges having to do with ResourceContext-level actions such as those to create other Resources', N'505190c6-0ffd-4e16-8e24-04796e44e59b', 5)
GO
SET IDENTITY_INSERT [dbo].[t_Resource] OFF
GO
SET IDENTITY_INSERT [dbo].[t_ResourceContext] ON 

GO
INSERT [dbo].[t_ResourceContext] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [racmEndpoint], [uuid], [label], [description], [contextClassId], [creationDate], [modificationDate]) VALUES (1, N'ResourceContext', 1, NULL, N'https://alpha02.sciserver.org/racm/', N'00cdea23-09de-4eb3-bd73-b2b459cf5a4d', NULL, NULL, 4, GETDATE(), NULL)
GO
SET IDENTITY_INSERT [dbo].[t_ResourceContext] OFF
GO
SET IDENTITY_INSERT [dbo].[t_ResourceType] ON 

GO
INSERT [dbo].[t_ResourceType] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (1, N'ResourceType', 1, NULL, 1, N'__rootcontext__', N'The root context resource type that every contextclass must have.')
GO
INSERT [dbo].[t_ResourceType] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (2, N'ResourceType', 1, NULL, 2, N'JOBM.Job', NULL)
GO
INSERT [dbo].[t_ResourceType] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (3, N'ResourceType', 1, NULL, 2, N'__rootcontext__', N'The root context resource type that every contextclass must have.')
GO
INSERT [dbo].[t_ResourceType] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (4, N'ResourceType', 1, NULL, 1, N'ExecutableContainer', N'Represents a Docker Container.')
GO
INSERT [dbo].[t_ResourceType] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (5, N'ResourceType', 1, NULL, 4, N'__rootcontext__', N'The root context resource type that every contextclass must have.')
GO
INSERT [dbo].[t_ResourceType] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (6, N'ResourceType', 1, NULL, 3, N'Casjobs.DatabaseContext', NULL)
GO
INSERT [dbo].[t_ResourceType] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (7, N'ResourceType', 1, NULL, 1, N'VolumeContainer', N'Represents public, shared Docker volume containers.')
GO
INSERT [dbo].[t_ResourceType] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (8, N'ResourceType', 1, NULL, 2, N'JobType', NULL)
GO
INSERT [dbo].[t_ResourceType] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (9, N'ResourceType', 1, NULL, 3, N'__rootcontext__', N'The root context resource type that every contextclass must have.')
GO
INSERT [dbo].[t_ResourceType] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (10, N'ResourceType', 1, NULL, 1, N'DockerImage', N'Represents a Docker Image with which one can create a docker container.')
GO
INSERT [dbo].[t_ResourceType] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (11, N'ResourceType', 1, NULL, 6, N'__rootcontext__', N'The root context resource type that every contextclass must have.')
GO
INSERT [dbo].[t_ResourceType] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (12, N'ResourceType', 1, NULL, 5, N'__rootcontext__', N'The root context resource type that every contextclass must have.')
GO
INSERT [dbo].[t_ResourceType] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (13, N'ResourceType', 1, NULL, 5, N'FileService.UserVolume', NULL)
GO
INSERT [dbo].[t_ResourceType] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (14, N'ResourceType', 1, NULL, 6, N'DatabaseContext', N'Represents database contexts that queries can be sent to.')
GO
INSERT [dbo].[t_ResourceType] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (15, N'ResourceType', 1, NULL, 5, N'FileService.RootVolume', NULL)
GO
SET IDENTITY_INSERT [dbo].[t_ResourceType] OFF
GO
SET IDENTITY_INSERT [dbo].[t_Role] ON 

GO
INSERT [dbo].[t_Role] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (1, N'Role', 1, NULL, 5, N'admin', N'The ''admin'' role provides privilege to execute any action on the context resource.')
GO
INSERT [dbo].[t_Role] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (2, N'Role', 1, NULL, 10, N'user', N'Allow usage of this docker image')
GO
INSERT [dbo].[t_Role] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (3, N'Role', 1, NULL, 4, N'writer', NULL)
GO
INSERT [dbo].[t_Role] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (4, N'Role', 1, NULL, 4, N'reader', NULL)
GO
INSERT [dbo].[t_Role] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (5, N'Role', 1, NULL, 4, N'owner', NULL)
GO
INSERT [dbo].[t_Role] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (6, N'Role', 1, NULL, 5, N'public', N'Public role on context context.')
GO
INSERT [dbo].[t_Role] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (7, N'Role', 1, NULL, 10, N'admin', N'Allow reading/writing of this volume container')
GO
INSERT [dbo].[t_Role] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (8, N'Role', 1, NULL, 7, N'admin', N'Allow reading/writing of this volume container')
GO
INSERT [dbo].[t_Role] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (9, N'Role', 2, NULL, 1, N'admin', N'')
GO
INSERT [dbo].[t_Role] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (10, N'Role', 1, NULL, 7, N'user', N'Allow usage of this volume container')
GO
INSERT [dbo].[t_Role] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (11, N'Role', 1, NULL, 14, N'writer', N'Allow updating of this database context')
GO
INSERT [dbo].[t_Role] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (12, N'Role', 2, NULL, 11, N'admin', N'')
GO
INSERT [dbo].[t_Role] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (13, N'Role', 1, NULL, 14, N'reader', N'Allow querying of this database context')
GO
INSERT [dbo].[t_Role] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (14, N'Role', 1, NULL, 14, N'admin', N'Allow reading/writing of this database context')
GO
INSERT [dbo].[t_Role] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description]) VALUES (15, N'Role', 1, NULL, 12, N'fs_admin', N'An administrator of a File Service')
GO
SET IDENTITY_INSERT [dbo].[t_Role] OFF
GO
SET IDENTITY_INSERT [dbo].[t_RoleAction] ON 

GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (1, N'RoleAction', 1, NULL, 5, 4)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (2, N'RoleAction', 1, NULL, 8, 9)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (3, N'RoleAction', 1, NULL, 1, 14)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (4, N'RoleAction', 1, NULL, 5, 18)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (5, N'RoleAction', 1, NULL, 3, 19)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (6, N'RoleAction', 1, NULL, 9, 6)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (7, N'RoleAction', 1, NULL, 4, 26)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (8, N'RoleAction', 1, NULL, 2, 8)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (9, N'RoleAction', 1, NULL, 1, 23)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (10, N'RoleAction', 1, NULL, 1, 7)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (11, N'RoleAction', 1, NULL, 7, 8)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (12, N'RoleAction', 1, NULL, 7, 1)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (13, N'RoleAction', 1, NULL, 1, 21)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (14, N'RoleAction', 1, NULL, 8, 5)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (15, N'RoleAction', 1, NULL, 9, 12)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (16, N'RoleAction', 1, NULL, 3, 26)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (17, N'RoleAction', 1, NULL, 5, 24)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (18, N'RoleAction', 1, NULL, 6, 17)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (19, N'RoleAction', 1, NULL, 1, 25)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (21, N'RoleAction', 1, NULL, 10, 5)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (22, N'RoleAction', 1, NULL, 5, 10)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (23, N'RoleAction', 1, NULL, 5, 26)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (24, N'RoleAction', 1, NULL, 5, 19)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (25, N'RoleAction', 1, NULL, 1, 16)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (26, N'RoleAction', 1, NULL, 12, 33)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (27, N'RoleAction', 1, NULL, 13, 32)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (28, N'RoleAction', 1, NULL, 14, 38)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (29, N'RoleAction', 1, NULL, 15, 37)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (30, N'RoleAction', 1, NULL, 14, 30)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (31, N'RoleAction', 1, NULL, 11, 32)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (32, N'RoleAction', 1, NULL, 15, 35)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (33, N'RoleAction', 1, NULL, 11, 38)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (34, N'RoleAction', 1, NULL, 14, 32)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (35, N'RoleAction', 1, NULL, 1, 42)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (36, N'RoleAction', 1, NULL, 9, 43)
GO
INSERT [dbo].[t_RoleAction] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId]) VALUES (37, N'RoleAction', 1, NULL, 12, 44)
GO
SET IDENTITY_INSERT [dbo].[t_RoleAction] OFF
GO
INSERT [dbo].[t_RoleAssignment] ([ID], [roleId]) VALUES (5, 1)
GO
INSERT [dbo].[t_RoleAssignment] ([ID], [roleId]) VALUES (10171, 1)
GO
INSERT [dbo].[t_RoleAssignment] ([ID], [roleId]) VALUES (4, 6)
GO
SET IDENTITY_INSERT [dbo].[t_SciserverEntity] ON 

GO
INSERT [dbo].[t_SciserverEntity] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [creationDate], [modificationDate]) VALUES (1, N'User', 1, NULL, GETDATE(), NULL)
GO
INSERT [dbo].[t_SciserverEntity] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [creationDate], [modificationDate]) VALUES (2, N'User', 1, NULL, GETDATE(), NULL)
GO
INSERT [dbo].[t_SciserverEntity] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [creationDate], [modificationDate]) VALUES (3, N'UserGroup', 640, NULL, GETDATE(), NULL)
GO
INSERT [dbo].[t_SciserverEntity] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [creationDate], [modificationDate]) VALUES (4, N'UserGroup', 5, NULL, GETDATE(), NULL)
GO
INSERT [dbo].[t_SciserverEntity] ([ID], [DTYPE], [OPTLOCK], [publisherDID], [creationDate], [modificationDate]) VALUES (5, N'UserGroup', 9, NULL, GETDATE(), NULL)
GO
SET IDENTITY_INSERT [dbo].[t_SciserverEntity] OFF
GO
INSERT [dbo].[t_User] ([ID], [userId], [username], [contactEmail], [trustId], [visibility], [preferences], [partyId]) VALUES (1, N'${admin-keystone-id}', N'${admin-user}', N'${admin-email}', NULL, N'SYSTEM', NULL, NULL)
GO
INSERT [dbo].[t_User] ([ID], [userId], [username], [contactEmail], [trustId], [visibility], [preferences], [partyId]) VALUES (2, N'${jobm-keystone-id}', N'${jobm-user}', N'${jobm-email}', NULL, N'SYSTEM', NULL, NULL)
GO
INSERT [dbo].[t_UserGroup] ([ID], [name], [description], [ownerId]) VALUES (3, N'public', N'''public'' group that all users are automatically a member of. If you want to make resources public, assign an appropriate privilege or role to this group.', 1)
GO
INSERT [dbo].[t_UserGroup] ([ID], [name], [description], [ownerId]) VALUES (4, N'COMPMAdmin', N'Group with right to register COMPMs', 1)
GO
INSERT [dbo].[t_UserGroup] ([ID], [name], [description], [ownerId]) VALUES (5, N'admin', N'', 1)
GO
/****** Object:  Index [ix_t_AccessControl___CONTAINER]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_AccessControl___CONTAINER] ON [dbo].[t_AccessControl]
(
	[containerId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_AccessControl_scisEntity]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_AccessControl_scisEntity] ON [dbo].[t_AccessControl]
(
	[scisEntityId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_Action___CONTAINER]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_Action___CONTAINER] ON [dbo].[t_Action]
(
	[containerId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_ActionExecution___CONTAINER]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_ActionExecution___CONTAINER] ON [dbo].[t_ActionExecution]
(
	[containerId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_ActionExecution_action]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_ActionExecution_action] ON [dbo].[t_ActionExecution]
(
	[actionId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_ActionExecution_resource]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_ActionExecution_resource] ON [dbo].[t_ActionExecution]
(
	[resourceId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_ActionExecution_user]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_ActionExecution_user] ON [dbo].[t_ActionExecution]
(
	[userId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_COMPM_computeDomain]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_COMPM_computeDomain] ON [dbo].[t_COMPM]
(
	[computeDomainId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_ComputeDomain_resourceContext]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_ComputeDomain_resourceContext] ON [dbo].[t_ComputeDomain]
(
	[resourceContextId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_ComputeResource___CONTAINER]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_ComputeResource___CONTAINER] ON [dbo].[t_ComputeResource]
(
	[containerId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_ComputeResource_resource]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_ComputeResource_resource] ON [dbo].[t_ComputeResource]
(
	[resourceId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_ContextClass_creator]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_ContextClass_creator] ON [dbo].[t_ContextClass]
(
	[creatorId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_DatabaseContext___CONTAINER]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_DatabaseContext___CONTAINER] ON [dbo].[t_DatabaseContext]
(
	[containerId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_DatabaseContext_resource]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_DatabaseContext_resource] ON [dbo].[t_DatabaseContext]
(
	[resourceId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_DockerJob_image]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_DockerJob_image] ON [dbo].[t_DockerJob]
(
	[imageId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_FileService_resourceContext]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_FileService_resourceContext] ON [dbo].[t_FileService]
(
	[resourceContextId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_Job_computeDomain]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_Job_computeDomain] ON [dbo].[t_Job]
(
	[computeDomainId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_Job_runBy]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_Job_runBy] ON [dbo].[t_Job]
(
	[runById] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_Job_submitter]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_Job_submitter] ON [dbo].[t_Job]
(
	[submitterId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_JobMessage___CONTAINER]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_JobMessage___CONTAINER] ON [dbo].[t_JobMessage]
(
	[containerId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_Member___CONTAINER]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_Member___CONTAINER] ON [dbo].[t_Member]
(
	[containerId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_Member_scisEntity]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_Member_scisEntity] ON [dbo].[t_Member]
(
	[scisEntityId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_Privilege_action]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_Privilege_action] ON [dbo].[t_Privilege]
(
	[actionId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_RDBJob_databaseContext]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_RDBJob_databaseContext] ON [dbo].[t_RDBJob]
(
	[databaseContextId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_RDBJobTarget___CONTAINER]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_RDBJobTarget___CONTAINER] ON [dbo].[t_RDBJobTarget]
(
	[containerId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_RequiredUserVolume___CONTAINER]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_RequiredUserVolume___CONTAINER] ON [dbo].[t_RequiredUserVolume]
(
	[containerId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_RequiredUserVolume_userVolume]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_RequiredUserVolume_userVolume] ON [dbo].[t_RequiredUserVolume]
(
	[userVolumeId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_RequiredVolume___CONTAINER]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_RequiredVolume___CONTAINER] ON [dbo].[t_RequiredVolume]
(
	[containerId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_RequiredVolume_volume]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_RequiredVolume_volume] ON [dbo].[t_RequiredVolume]
(
	[volumeId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_Resource___CONTAINER]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_Resource___CONTAINER] ON [dbo].[t_Resource]
(
	[containerId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_Resource_resourceType]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_Resource_resourceType] ON [dbo].[t_Resource]
(
	[resourceTypeId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
SET ANSI_PADDING ON

GO
/****** Object:  Index [uix_t_Resource_UUID]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE UNIQUE NONCLUSTERED INDEX [uix_t_Resource_UUID] ON [dbo].[t_Resource]
(
	[uuid] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_ResourceContext_contextClass]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_ResourceContext_contextClass] ON [dbo].[t_ResourceContext]
(
	[contextClassId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_ResourceType___CONTAINER]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_ResourceType___CONTAINER] ON [dbo].[t_ResourceType]
(
	[containerId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_Response___CONTAINER]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_Response___CONTAINER] ON [dbo].[t_Response]
(
	[containerId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_Response_message]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_Response_message] ON [dbo].[t_Response]
(
	[messageId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_Role___CONTAINER]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_Role___CONTAINER] ON [dbo].[t_Role]
(
	[containerId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_RoleAction___CONTAINER]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_RoleAction___CONTAINER] ON [dbo].[t_RoleAction]
(
	[containerId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_RoleAction_action]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_RoleAction_action] ON [dbo].[t_RoleAction]
(
	[actionId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_RoleAssignment_role]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_RoleAssignment_role] ON [dbo].[t_RoleAssignment]
(
	[roleId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_RootVolume___CONTAINER]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_RootVolume___CONTAINER] ON [dbo].[t_RootVolume]
(
	[containerId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_RootVolume_resource]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_RootVolume_resource] ON [dbo].[t_RootVolume]
(
	[resourceId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_RootVolumeOnComputeDomain___CONTAINER]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_RootVolumeOnComputeDomain___CONTAINER] ON [dbo].[t_RootVolumeOnComputeDomain]
(
	[containerId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_RootVolumeOnComputeDomain_rootVolume]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_RootVolumeOnComputeDomain_rootVolume] ON [dbo].[t_RootVolumeOnComputeDomain]
(
	[rootVolumeId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
SET ANSI_PADDING ON

GO
/****** Object:  Index [t_unique_t_User_username]    Script Date: 8/7/2018 3:31:57 PM ******/
ALTER TABLE [dbo].[t_User] ADD  CONSTRAINT [t_unique_t_User_username] UNIQUE NONCLUSTERED 
(
	[username] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_User_party]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_User_party] ON [dbo].[t_User]
(
	[partyId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_UserGroup_owner]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_UserGroup_owner] ON [dbo].[t_UserGroup]
(
	[ownerId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_UserVolume___CONTAINER]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_UserVolume___CONTAINER] ON [dbo].[t_UserVolume]
(
	[containerId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_UserVolume_owner]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_UserVolume_owner] ON [dbo].[t_UserVolume]
(
	[ownerId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_UserVolume_resource]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_UserVolume_resource] ON [dbo].[t_UserVolume]
(
	[resourceId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_UserVolume_rootVolume]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_UserVolume_rootVolume] ON [dbo].[t_UserVolume]
(
	[rootVolumeId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_Workspace_members]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_Workspace_members] ON [dbo].[t_Workspace]
(
	[membersId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_Workspace_resource]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_Workspace_resource] ON [dbo].[t_Workspace]
(
	[resourceId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_WorkspaceUserVolume___CONTAINER]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_WorkspaceUserVolume___CONTAINER] ON [dbo].[t_WorkspaceUserVolume]
(
	[containerId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Index [ix_t_WorkspaceUserVolume_userVolume]    Script Date: 8/7/2018 3:31:57 PM ******/
CREATE NONCLUSTERED INDEX [ix_t_WorkspaceUserVolume_userVolume] ON [dbo].[t_WorkspaceUserVolume]
(
	[userVolumeId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
ALTER TABLE [dbo].[users] ADD  DEFAULT (getdate()) FOR [createDate]
GO
ALTER TABLE [dbo].[t_AccessControl]  WITH CHECK ADD  CONSTRAINT [fk_t_AccessControl_container] FOREIGN KEY([containerId])
REFERENCES [dbo].[t_Resource] ([ID])
GO
ALTER TABLE [dbo].[t_AccessControl] CHECK CONSTRAINT [fk_t_AccessControl_container]
GO
ALTER TABLE [dbo].[t_AccessControl]  WITH CHECK ADD  CONSTRAINT [fk_t_AccessControl_scisEntity] FOREIGN KEY([scisEntityId])
REFERENCES [dbo].[t_SciserverEntity] ([ID])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[t_AccessControl] CHECK CONSTRAINT [fk_t_AccessControl_scisEntity]
GO
ALTER TABLE [dbo].[t_Action]  WITH CHECK ADD  CONSTRAINT [fk_t_Action_container] FOREIGN KEY([containerId])
REFERENCES [dbo].[t_ResourceType] ([ID])
GO
ALTER TABLE [dbo].[t_Action] CHECK CONSTRAINT [fk_t_Action_container]
GO
ALTER TABLE [dbo].[t_ActionExecution]  WITH CHECK ADD  CONSTRAINT [fk_t_ActionExecution_action] FOREIGN KEY([actionId])
REFERENCES [dbo].[t_Action] ([ID])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[t_ActionExecution] CHECK CONSTRAINT [fk_t_ActionExecution_action]
GO
ALTER TABLE [dbo].[t_ActionExecution]  WITH CHECK ADD  CONSTRAINT [fk_t_ActionExecution_container] FOREIGN KEY([containerId])
REFERENCES [dbo].[t_History] ([ID])
GO
ALTER TABLE [dbo].[t_ActionExecution] CHECK CONSTRAINT [fk_t_ActionExecution_container]
GO
ALTER TABLE [dbo].[t_ActionExecution]  WITH CHECK ADD  CONSTRAINT [fk_t_ActionExecution_resource] FOREIGN KEY([resourceId])
REFERENCES [dbo].[t_Resource] ([ID])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[t_ActionExecution] CHECK CONSTRAINT [fk_t_ActionExecution_resource]
GO
ALTER TABLE [dbo].[t_ActionExecution]  WITH CHECK ADD  CONSTRAINT [fk_t_ActionExecution_user] FOREIGN KEY([userId])
REFERENCES [dbo].[t_SciserverEntity] ([ID])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[t_ActionExecution] CHECK CONSTRAINT [fk_t_ActionExecution_user]
GO
ALTER TABLE [dbo].[t_COMPM]  WITH CHECK ADD  CONSTRAINT [fk_t_COMPM_computeDomain] FOREIGN KEY([computeDomainId])
REFERENCES [dbo].[t_ComputeDomain] ([ID])
ON DELETE SET NULL
GO
ALTER TABLE [dbo].[t_COMPM] CHECK CONSTRAINT [fk_t_COMPM_computeDomain]
GO
ALTER TABLE [dbo].[t_ComputeResource]  WITH CHECK ADD  CONSTRAINT [fk_t_ComputeResource_container] FOREIGN KEY([containerId])
REFERENCES [dbo].[t_ComputeDomain] ([ID])
GO
ALTER TABLE [dbo].[t_ComputeResource] CHECK CONSTRAINT [fk_t_ComputeResource_container]
GO
ALTER TABLE [dbo].[t_ContextClass]  WITH CHECK ADD  CONSTRAINT [fk_t_ContextClass_creator] FOREIGN KEY([creatorId])
REFERENCES [dbo].[t_SciserverEntity] ([ID])
ON DELETE SET NULL
GO
ALTER TABLE [dbo].[t_ContextClass] CHECK CONSTRAINT [fk_t_ContextClass_creator]
GO
ALTER TABLE [dbo].[t_DatabaseContext]  WITH CHECK ADD  CONSTRAINT [fk_t_DatabaseContext_container] FOREIGN KEY([containerId])
REFERENCES [dbo].[t_ComputeDomain] ([ID])
GO
ALTER TABLE [dbo].[t_DatabaseContext] CHECK CONSTRAINT [fk_t_DatabaseContext_container]
GO
ALTER TABLE [dbo].[t_DockerComputeDomain]  WITH CHECK ADD  CONSTRAINT [fk_t_DockerComputeDomain_extends] FOREIGN KEY([ID])
REFERENCES [dbo].[t_ComputeDomain] ([ID])
GO
ALTER TABLE [dbo].[t_DockerComputeDomain] CHECK CONSTRAINT [fk_t_DockerComputeDomain_extends]
GO
ALTER TABLE [dbo].[t_DockerImage]  WITH CHECK ADD  CONSTRAINT [fk_t_DockerImage_extends] FOREIGN KEY([ID])
REFERENCES [dbo].[t_ComputeResource] ([ID])
GO
ALTER TABLE [dbo].[t_DockerImage] CHECK CONSTRAINT [fk_t_DockerImage_extends]
GO
ALTER TABLE [dbo].[t_DockerJob]  WITH CHECK ADD  CONSTRAINT [fk_t_DockerJob_extends] FOREIGN KEY([ID])
REFERENCES [dbo].[t_Job] ([ID])
GO
ALTER TABLE [dbo].[t_DockerJob] CHECK CONSTRAINT [fk_t_DockerJob_extends]
GO
ALTER TABLE [dbo].[t_DockerJob]  WITH CHECK ADD  CONSTRAINT [fk_t_DockerJob_image] FOREIGN KEY([imageId])
REFERENCES [dbo].[t_ComputeResource] ([ID])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[t_DockerJob] CHECK CONSTRAINT [fk_t_DockerJob_image]
GO
ALTER TABLE [dbo].[t_Job]  WITH CHECK ADD  CONSTRAINT [fk_t_Job_computeDomain] FOREIGN KEY([computeDomainId])
REFERENCES [dbo].[t_ComputeDomain] ([ID])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[t_Job] CHECK CONSTRAINT [fk_t_Job_computeDomain]
GO
ALTER TABLE [dbo].[t_Job]  WITH CHECK ADD  CONSTRAINT [fk_t_Job_runBy] FOREIGN KEY([runById])
REFERENCES [dbo].[t_COMPM] ([ID])
ON DELETE SET NULL
GO
ALTER TABLE [dbo].[t_Job] CHECK CONSTRAINT [fk_t_Job_runBy]
GO
ALTER TABLE [dbo].[t_Job]  WITH CHECK ADD  CONSTRAINT [fk_t_Job_submitter] FOREIGN KEY([submitterId])
REFERENCES [dbo].[t_SciserverEntity] ([ID])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[t_Job] CHECK CONSTRAINT [fk_t_Job_submitter]
GO
ALTER TABLE [dbo].[t_JobMessage]  WITH CHECK ADD  CONSTRAINT [fk_t_JobMessage_container] FOREIGN KEY([containerId])
REFERENCES [dbo].[t_Job] ([ID])
GO
ALTER TABLE [dbo].[t_JobMessage] CHECK CONSTRAINT [fk_t_JobMessage_container]
GO
ALTER TABLE [dbo].[t_Member]  WITH CHECK ADD  CONSTRAINT [fk_t_Member_container] FOREIGN KEY([containerId])
REFERENCES [dbo].[t_SciserverEntity] ([ID])
GO
ALTER TABLE [dbo].[t_Member] CHECK CONSTRAINT [fk_t_Member_container]
GO
ALTER TABLE [dbo].[t_Member]  WITH CHECK ADD  CONSTRAINT [fk_t_Member_scisEntity] FOREIGN KEY([scisEntityId])
REFERENCES [dbo].[t_SciserverEntity] ([ID])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[t_Member] CHECK CONSTRAINT [fk_t_Member_scisEntity]
GO
ALTER TABLE [dbo].[t_Privilege]  WITH CHECK ADD  CONSTRAINT [fk_t_Privilege_action] FOREIGN KEY([actionId])
REFERENCES [dbo].[t_Action] ([ID])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[t_Privilege] CHECK CONSTRAINT [fk_t_Privilege_action]
GO
ALTER TABLE [dbo].[t_Privilege]  WITH CHECK ADD  CONSTRAINT [fk_t_Privilege_extends] FOREIGN KEY([ID])
REFERENCES [dbo].[t_AccessControl] ([ID])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[t_Privilege] CHECK CONSTRAINT [fk_t_Privilege_extends]
GO
ALTER TABLE [dbo].[t_RDBComputeDomain]  WITH CHECK ADD  CONSTRAINT [fk_t_RDBComputeDomain_extends] FOREIGN KEY([ID])
REFERENCES [dbo].[t_ComputeDomain] ([ID])
GO
ALTER TABLE [dbo].[t_RDBComputeDomain] CHECK CONSTRAINT [fk_t_RDBComputeDomain_extends]
GO
ALTER TABLE [dbo].[t_RDBJob]  WITH CHECK ADD  CONSTRAINT [fk_t_RDBJob_databaseContext] FOREIGN KEY([databaseContextId])
REFERENCES [dbo].[t_DatabaseContext] ([ID])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[t_RDBJob] CHECK CONSTRAINT [fk_t_RDBJob_databaseContext]
GO
ALTER TABLE [dbo].[t_RDBJob]  WITH CHECK ADD  CONSTRAINT [fk_t_RDBJob_extends] FOREIGN KEY([ID])
REFERENCES [dbo].[t_Job] ([ID])
GO
ALTER TABLE [dbo].[t_RDBJob] CHECK CONSTRAINT [fk_t_RDBJob_extends]
GO
ALTER TABLE [dbo].[t_RDBJobTarget]  WITH CHECK ADD  CONSTRAINT [fk_t_RDBJobTarget_container] FOREIGN KEY([containerId])
REFERENCES [dbo].[t_Job] ([ID])
GO
ALTER TABLE [dbo].[t_RDBJobTarget] CHECK CONSTRAINT [fk_t_RDBJobTarget_container]
GO
ALTER TABLE [dbo].[t_RequiredUserVolume]  WITH CHECK ADD  CONSTRAINT [fk_t_RequiredUserVolume_container] FOREIGN KEY([containerId])
REFERENCES [dbo].[t_Job] ([ID])
GO
ALTER TABLE [dbo].[t_RequiredUserVolume] CHECK CONSTRAINT [fk_t_RequiredUserVolume_container]
GO
ALTER TABLE [dbo].[t_RequiredUserVolume]  WITH CHECK ADD  CONSTRAINT [fk_t_RequiredUserVolume_userVolume] FOREIGN KEY([userVolumeId])
REFERENCES [dbo].[t_UserVolume] ([ID])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[t_RequiredUserVolume] CHECK CONSTRAINT [fk_t_RequiredUserVolume_userVolume]
GO
ALTER TABLE [dbo].[t_RequiredVolume]  WITH CHECK ADD  CONSTRAINT [fk_t_RequiredVolume_container] FOREIGN KEY([containerId])
REFERENCES [dbo].[t_Job] ([ID])
GO
ALTER TABLE [dbo].[t_RequiredVolume] CHECK CONSTRAINT [fk_t_RequiredVolume_container]
GO
ALTER TABLE [dbo].[t_RequiredVolume]  WITH CHECK ADD  CONSTRAINT [fk_t_RequiredVolume_volume] FOREIGN KEY([volumeId])
REFERENCES [dbo].[t_ComputeResource] ([ID])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[t_RequiredVolume] CHECK CONSTRAINT [fk_t_RequiredVolume_volume]
GO
ALTER TABLE [dbo].[t_Resource]  WITH CHECK ADD  CONSTRAINT [fk_t_Resource_container] FOREIGN KEY([containerId])
REFERENCES [dbo].[t_ResourceContext] ([ID])
GO
ALTER TABLE [dbo].[t_Resource] CHECK CONSTRAINT [fk_t_Resource_container]
GO
ALTER TABLE [dbo].[t_Resource]  WITH CHECK ADD  CONSTRAINT [fk_t_Resource_resourceType] FOREIGN KEY([resourceTypeId])
REFERENCES [dbo].[t_ResourceType] ([ID])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[t_Resource] CHECK CONSTRAINT [fk_t_Resource_resourceType]
GO
ALTER TABLE [dbo].[t_ResourceContext]  WITH CHECK ADD  CONSTRAINT [fk_t_ResourceContext_contextClass] FOREIGN KEY([contextClassId])
REFERENCES [dbo].[t_ContextClass] ([ID])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[t_ResourceContext] CHECK CONSTRAINT [fk_t_ResourceContext_contextClass]
GO
ALTER TABLE [dbo].[t_ResourceType]  WITH CHECK ADD  CONSTRAINT [fk_t_ResourceType_container] FOREIGN KEY([containerId])
REFERENCES [dbo].[t_ContextClass] ([ID])
GO
ALTER TABLE [dbo].[t_ResourceType] CHECK CONSTRAINT [fk_t_ResourceType_container]
GO
ALTER TABLE [dbo].[t_Response]  WITH CHECK ADD  CONSTRAINT [fk_t_Response_container] FOREIGN KEY([containerId])
REFERENCES [dbo].[t_JobMessage] ([ID])
GO
ALTER TABLE [dbo].[t_Response] CHECK CONSTRAINT [fk_t_Response_container]
GO
ALTER TABLE [dbo].[t_Response]  WITH CHECK ADD  CONSTRAINT [fk_t_Response_message] FOREIGN KEY([messageId])
REFERENCES [dbo].[t_JobMessage] ([ID])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[t_Response] CHECK CONSTRAINT [fk_t_Response_message]
GO
ALTER TABLE [dbo].[t_Role]  WITH CHECK ADD  CONSTRAINT [fk_t_Role_container] FOREIGN KEY([containerId])
REFERENCES [dbo].[t_ResourceType] ([ID])
GO
ALTER TABLE [dbo].[t_Role] CHECK CONSTRAINT [fk_t_Role_container]
GO
ALTER TABLE [dbo].[t_RoleAction]  WITH CHECK ADD  CONSTRAINT [fk_t_RoleAction_action] FOREIGN KEY([actionId])
REFERENCES [dbo].[t_Action] ([ID])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[t_RoleAction] CHECK CONSTRAINT [fk_t_RoleAction_action]
GO
ALTER TABLE [dbo].[t_RoleAction]  WITH CHECK ADD  CONSTRAINT [fk_t_RoleAction_container] FOREIGN KEY([containerId])
REFERENCES [dbo].[t_Role] ([ID])
GO
ALTER TABLE [dbo].[t_RoleAction] CHECK CONSTRAINT [fk_t_RoleAction_container]
GO
ALTER TABLE [dbo].[t_RoleAssignment]  WITH CHECK ADD  CONSTRAINT [fk_t_RoleAssignment_extends] FOREIGN KEY([ID])
REFERENCES [dbo].[t_AccessControl] ([ID])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[t_RoleAssignment] CHECK CONSTRAINT [fk_t_RoleAssignment_extends]
GO
ALTER TABLE [dbo].[t_RoleAssignment]  WITH CHECK ADD  CONSTRAINT [fk_t_RoleAssignment_role] FOREIGN KEY([roleId])
REFERENCES [dbo].[t_Role] ([ID])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[t_RoleAssignment] CHECK CONSTRAINT [fk_t_RoleAssignment_role]
GO
ALTER TABLE [dbo].[t_RootVolume]  WITH CHECK ADD  CONSTRAINT [fk_t_RootVolume_container] FOREIGN KEY([containerId])
REFERENCES [dbo].[t_FileService] ([ID])
GO
ALTER TABLE [dbo].[t_RootVolume] CHECK CONSTRAINT [fk_t_RootVolume_container]
GO
ALTER TABLE [dbo].[t_RootVolumeOnComputeDomain]  WITH CHECK ADD  CONSTRAINT [fk_t_RootVolumeOnComputeDomain_container] FOREIGN KEY([containerId])
REFERENCES [dbo].[t_ComputeDomain] ([ID])
GO
ALTER TABLE [dbo].[t_RootVolumeOnComputeDomain] CHECK CONSTRAINT [fk_t_RootVolumeOnComputeDomain_container]
GO
ALTER TABLE [dbo].[t_RootVolumeOnComputeDomain]  WITH CHECK ADD  CONSTRAINT [fk_t_RootVolumeOnComputeDomain_rootVolume] FOREIGN KEY([rootVolumeId])
REFERENCES [dbo].[t_RootVolume] ([ID])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[t_RootVolumeOnComputeDomain] CHECK CONSTRAINT [fk_t_RootVolumeOnComputeDomain_rootVolume]
GO
ALTER TABLE [dbo].[t_User]  WITH CHECK ADD  CONSTRAINT [fk_t_User_extends] FOREIGN KEY([ID])
REFERENCES [dbo].[t_SciserverEntity] ([ID])
GO
ALTER TABLE [dbo].[t_User] CHECK CONSTRAINT [fk_t_User_extends]
GO
ALTER TABLE [dbo].[t_User]  WITH CHECK ADD  CONSTRAINT [fk_t_User_party] FOREIGN KEY([partyId])
REFERENCES [dbo].[t_Party] ([ID])
ON DELETE SET NULL
GO
ALTER TABLE [dbo].[t_User] CHECK CONSTRAINT [fk_t_User_party]
GO
ALTER TABLE [dbo].[t_UserGroup]  WITH CHECK ADD  CONSTRAINT [fk_t_UserGroup_extends] FOREIGN KEY([ID])
REFERENCES [dbo].[t_SciserverEntity] ([ID])
GO
ALTER TABLE [dbo].[t_UserGroup] CHECK CONSTRAINT [fk_t_UserGroup_extends]
GO
ALTER TABLE [dbo].[t_UserGroup]  WITH CHECK ADD  CONSTRAINT [fk_t_UserGroup_owner] FOREIGN KEY([ownerId])
REFERENCES [dbo].[t_SciserverEntity] ([ID])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[t_UserGroup] CHECK CONSTRAINT [fk_t_UserGroup_owner]
GO
ALTER TABLE [dbo].[t_UserVolume]  WITH CHECK ADD  CONSTRAINT [fk_t_UserVolume_container] FOREIGN KEY([containerId])
REFERENCES [dbo].[t_FileService] ([ID])
GO
ALTER TABLE [dbo].[t_UserVolume] CHECK CONSTRAINT [fk_t_UserVolume_container]
GO
ALTER TABLE [dbo].[t_UserVolume]  WITH CHECK ADD  CONSTRAINT [fk_t_UserVolume_owner] FOREIGN KEY([ownerId])
REFERENCES [dbo].[t_SciserverEntity] ([ID])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[t_UserVolume] CHECK CONSTRAINT [fk_t_UserVolume_owner]
GO
ALTER TABLE [dbo].[t_UserVolume]  WITH CHECK ADD  CONSTRAINT [fk_t_UserVolume_rootVolume] FOREIGN KEY([rootVolumeId])
REFERENCES [dbo].[t_RootVolume] ([ID])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[t_UserVolume] CHECK CONSTRAINT [fk_t_UserVolume_rootVolume]
GO
ALTER TABLE [dbo].[t_VolumeContainer]  WITH CHECK ADD  CONSTRAINT [fk_t_VolumeContainer_extends] FOREIGN KEY([ID])
REFERENCES [dbo].[t_ComputeResource] ([ID])
GO
ALTER TABLE [dbo].[t_VolumeContainer] CHECK CONSTRAINT [fk_t_VolumeContainer_extends]
GO
ALTER TABLE [dbo].[t_WorkspaceUserVolume]  WITH CHECK ADD  CONSTRAINT [fk_t_WorkspaceUserVolume_container] FOREIGN KEY([containerId])
REFERENCES [dbo].[t_Workspace] ([ID])
GO
ALTER TABLE [dbo].[t_WorkspaceUserVolume] CHECK CONSTRAINT [fk_t_WorkspaceUserVolume_container]
GO

