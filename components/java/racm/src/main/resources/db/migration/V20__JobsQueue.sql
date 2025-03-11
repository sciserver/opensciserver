-- this function  shows the queue of current runnin and pending jobs for the specified COMPM and
-- it includes an entry where a new job submitted by the user would end up.
-- the returned username is a checksum of the usernames except for that of the user itself.
CREATE OR ALTER function [racm].jobsQueue(@userid bigint, @compmUUID varchar(64))
returns @rt table(
	  username varchar(255),ranking integer,jobId bigint, [status] integer, submitterId bigint,
	  computeDomainId bigint, submitTime datetime, startedTime datetime
	  ) 
as
begin
declare @computeDomainId bigint, @currentDate datetime = getDate()
declare @timeout real, @maxJobsPerUser integer, @interval real

select @computeDomainId = computeDomainId
,      @interval=defaultJobTimeout
,      @timeout=defaultJobTimeout
,      @maxJobsPerUser = defaultJobsPerUser
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
  from  job dj 
 where computeDomainId=@computeDomainId
   and status > 1 -- PENDING
   and startedTime >= dateadd(second,-@interval,getDate())
 group by submitterid
),
pending as (
select *
,      row_number() over (partition by submitterId order by submitTime) as rankSubmitted
  from  (
    select id, submitTime, status,submitterId,computeDomainId, startedTime
	  from job 
	 where computeDomainId=@computeDomainId
       and status=1 -- PENDING
   union all  -- add job for current user as if submitted now
    select -1 as id
    ,      GETDATE() as submitTime
    ,      0 as status
    ,      @userid as submitterId
    ,      @computeDomainId as computeDomainId 
	,     NULL as startedTIme
	) j
),
final as (
select row_number() over (order by rankSubmitted,usageWeight,submitTime) as ranking
,      j.Id as jobId,j.status,j.submitterId, j.computeDomainId, j.submitTime  
,      j.startedTime
  from pending j left outer join prev p
  on p.submitterId = j.submitterid
  union all
select -1*(row_number() over (order by [status], isnull(startedTime,submitTime) desc)) as [ranking]
,      id as jobId,status,submitterId, computeDomainId, submitTime, startedTime 
  from job where status between 2 and 16

)
insert into @rt
select case when f.submitterid=@userid then u.username
        else cast(checksum(u.username) as varchar(255)) end as username
		, f.*
  from final f join [user] u on u.id=f.submitterId

return 
end

GO
-- retrieve the queues for all computedomains
-- with similar treatent for assumed job submitted now
create or alter function racm.allJobsQueues(@userid bigint)
returns table
as
return 
select d.name as domainName, d.DTYPE as domainClass
,      c.id as queueId
,      q.*
  from computedomain d
  join compm c on c.computeDomainId=d.id
    cross apply racm.jobsQueue(@userid, c.uuid) q
	where q.computedomainid=d.id
GO
