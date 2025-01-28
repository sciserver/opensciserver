-- due to update of the view racm.ActionAssignments, the following functions need refreshing, in the order indicated

-- due to direct dependence on racm.ActionAssignments
exec sp_refreshsqlmodule 'racm.canUserDoAction'
exec sp_refreshsqlmodule 'racm.userActionsOnContext'
exec sp_refreshsqlmodule 'racm.useractions'
exec sp_refreshsqlmodule 'racm.groupActions'
exec sp_refreshsqlmodule 'racm.canUserGrant'
exec sp_refreshsqlmodule 'racm.canUserDoRootAction'

-- due to dependence on racm.userActions:
exec sp_refreshsqlmodule 'racm.accessiblevolumecontainers'
exec sp_refreshsqlmodule 'racm.mountableuservolumes'
exec sp_refreshsqlmodule 'racm.userResourceActionsOnContext'
exec sp_refreshsqlmodule 'racm.associatedResourceActions'
exec sp_refreshsqlmodule 'racm.serviceOwnedUserResources'
exec sp_refreshsqlmodule 'racm.accessibledockerimages'
exec sp_refreshsqlmodule 'racm.groupResourcesForUser'

-- due to dependence on racm.userResourceActionsOnContext:
exec sp_refreshsqlmodule 'racm.userfileserviceresources'
exec sp_refreshsqlmodule 'racm.usercontextresourceshares'

GO