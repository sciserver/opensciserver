alter table t_UserGroup add accessibility varchar(10) not null  default 'PRIVATE' with values

GO 

ALTER VIEW [dbo].[UserGroup] AS 
  SELECT b.* 
  ,      t.[name] 
  ,      t.[description] 
  ,      t.ownerId, t.accessibility FROM t_UserGroup t ,    [SciserverEntity] b WHERE b.ID = t.ID 

  GO
