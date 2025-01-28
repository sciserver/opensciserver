-- insert an action  to the root context on System for accepting registration requests
declare @rtid bigint
select @rtid=rt.id 
  from t_contextclass cc join resourcetype rt on rt.containerId=cc.id and rt.name='__rootcontext__'
 where cc.[name]='System'

insert into t_action(DTYPE,containerID,name,category)
select 'Action',@rtid,'acceptRegistrationRequest','X'
  where not exists (select * from t_action where containerid=@rtid and name ='acceptRegistrationRequest')
