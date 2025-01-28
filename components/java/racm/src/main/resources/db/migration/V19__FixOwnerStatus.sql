-- ensure __racm__'s group status is OWNER in admin group.
UPDATE [dbo].[t_Member] set status = 'OWNER' where id=2

GO