ALTER function [racm].[nextJobs](@compmUUID varchar(64), @timeout real, @interval real, @maxNum integer, @maxJobsPerUser integer)
returns @rt table(
	jobId bigint,submitterId bigint,
	computeDomainId bigint, submitTime datetime,
	usageWeight float,numQueued integer, numStarted integer, ranking integer) 
as

begin
insert into @rt
select * from racm.nextJobsRestricted(@compmUUID, @timeout,@interval, @maxNum, @maxJobsPerUser)
	return
end
GO