CREATE or alter function [racm].dataVolumeAllowedActions (@username varchar(255), @dvname varchar(128), @contextid varchar(128))
returns table as
return 

with a as (
SELECT DISTINCT ua.actions,  dv.pathOnFileSystem FROM racm.userResourceActionsOnContext(@contextid, @username) ua  
  inner join datavolume dv on dv.resourceId=ua.resourceId 
WHERE dv.name=@dvname 
)
select aa.value as action, a.pathOnFileSystem
  from a cross apply string_split(a.actions,',') aa 

GO
