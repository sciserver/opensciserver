CREATE or ALTER function [racm].[nextJobsRestricted](@compmUUID varchar(64), @timeout real, 
        @interval real, @maxNum integer,@maxPerUser integer) 
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

declare @prev table (submitterId bigint, numJobs integer, numQueued integer
   ,  numStarted integer, totTime float, usageWeight float)

insert into @prev
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

declare @pending table (id bigint, submitterId bigint, computeDomainId bigint
       , submitTime datetime,rankSubmitted integer)

insert into @pending
select dj.id, dj.submitterId, dj.computeDomainId, submitTime
,      rank() over (partition by submitterId order by submitTime) as rankSubmitted
  from compm c , job dj 
 where c.uuid = @compmUUID
   and dj.computeDomainId=c.computeDomainId
   and dj.status=1 -- PENDING

;
with final as (
select j.Id as jobId,j.submitterId, j.computeDomainId, j.submitTime  -- without TOP cannot do an order by in a function
,      isnull(p.usageWeight,0) as usageWeight
,      isnull(p.numQueued,0) as numQueued
,      isnull(p.numStarted,0) as numStarted
,      rank() over (order by rankSubmitted,usageWeight,submitTime) as ranking
  from @pending j left outer join @prev p
  on p.submitterId = j.submitterid
  where j.rankSubmitted <=@maxNum
    and j.rankSubmitted+isnull(p.numQueued,0)+isnull(p.numStarted,0)<=@maxPerUser
)
insert into @rt
select * -- jobId,submitterId, computeDomainId, submitTime , usageWeight, numQueued, numStarted, ranking
  from final
 where ranking <= @maxNum 
--   and rankSubmitted+numQueued+numStarted<=@maxPerUser -- to counter possibility COMPM gets single user too many times in internal queue

  return 
end



