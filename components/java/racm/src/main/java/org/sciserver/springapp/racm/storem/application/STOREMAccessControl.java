package org.sciserver.springapp.racm.storem.application;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_DATAVOLUME_DELETE;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_DATAVOLUME_EDIT;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_DATAVOLUME_GRANT;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_DEFINE_DATAVOLUME;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_EDIT;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_REGISTER;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_ROOTVOLUME_CREATE;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_USERVOLUME_DELETE;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_USERVOLUME_GRANT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import javax.persistence.Query;

import org.ivoa.dm.model.TransientObjectManager;
import org.sciserver.racm.utils.model.NativeQueryResult;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.RACM;
import org.sciserver.springapp.racm.utils.RACMUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.jhu.file.DataVolume;
import edu.jhu.file.FileService;
import edu.jhu.file.RootVolume;
import edu.jhu.file.UserVolume;
import edu.jhu.rac.AccessControl;
import edu.jhu.rac.Action;
import edu.jhu.rac.Privilege;
import edu.jhu.rac.Resource;
import edu.jhu.rac.ResourceContext;
import edu.jhu.rac.ResourceType;
import edu.jhu.rac.RoleAssignment;
import edu.jhu.user.SciserverEntity;

/**
 * Class implementing methods to perform permission-related queries to the
 * database for STOREM.
 *
 */
@Component
public class STOREMAccessControl {
    private final RACM racm;

    @Autowired
    public STOREMAccessControl(RACM racm) {
        this.racm = racm;
    }

    boolean canRegisterFileService(UserProfile up) {
        return racm.canUserDoRootAction(up.getUsername(), A_FILESERVICE_REGISTER);
    }

    boolean canEditFileService(UserProfile up, FileService fs) {
        String rootResourceUUID = RACMUtil.getRootResource(fs.getResourceContext()).getUuid();
        return racm.canUserDoActionOnResource(up.getUsername(), rootResourceUUID, A_FILESERVICE_EDIT);
    }

    boolean canCreateUserVolume(UserProfile up, RootVolume rv) {
        return racm.canUserDoActionOnResource(up.getUsername(), rv.getResource().getUuid(),
                A_FILESERVICE_ROOTVOLUME_CREATE);
    }

    boolean canCreateServiceVolume(String serviceToken, RootVolume rv) {
        return racm.canServiceDoActionOnResource(rv.getResource().getUuid(), A_FILESERVICE_ROOTVOLUME_CREATE,
                serviceToken);
    }

    boolean canServiceDeleteUserVolume(String serviceToken, UserVolume uv) {
        return racm.canServiceDoActionOnResource(uv.getResource().getUuid(), A_FILESERVICE_USERVOLUME_DELETE,
                serviceToken);
    }

    boolean canCreateDataVolume(UserProfile up, FileService fs) {
        String rootResourceUUID = RACMUtil.getRootResource(fs.getResourceContext()).getUuid();
        return racm.canUserDoActionOnResource(up.getUsername(), rootResourceUUID, A_FILESERVICE_DEFINE_DATAVOLUME);
    }

    boolean canUserShareUserVolume(UserProfile up, UserVolume uv) {
        return racm.canUserDoActionOnResource(up.getUsername(), uv.getResource().getUuid(),
                A_FILESERVICE_USERVOLUME_GRANT);
    }

    boolean canUserEditDataVolume(UserProfile up, DataVolume dv) {
        return racm.canUserDoActionOnResource(up.getUsername(), dv.getResource().getUuid(),
                A_FILESERVICE_DATAVOLUME_EDIT);
    }

    boolean canUserDeleteDataVolume(UserProfile up, DataVolume dv) {
        return racm.canUserDoActionOnResource(up.getUsername(), dv.getResource().getUuid(),
                A_FILESERVICE_DATAVOLUME_DELETE);
    }

    boolean canUserShareDataVolume(UserProfile up, DataVolume dv) {
        return racm.canUserDoActionOnResource(up.getUsername(), dv.getResource().getUuid(),
                A_FILESERVICE_DATAVOLUME_GRANT);
    }

    boolean canUserDeleteUserVolume(UserProfile up, UserVolume uv) {
        return racm.canUserDoActionOnResource(up.getUsername(), uv.getResource().getUuid(),
                A_FILESERVICE_USERVOLUME_DELETE);
    }

    boolean isUserVolumeOwned(UserVolume uv) {
        return racm.isResourceOwnedByAnotherResource(uv.getResource().getUuid());
    }

    boolean isUserVolumeOwnedByService(String serviceToken, UserVolume uv) {
        return racm.isResourceOwnedByThisService(serviceToken, uv.getResource().getUuid());
    }

    Map<Long, List<String>> getAllowedActions(UserProfile up, ResourceContext... resourceContexts) {
        List<String> resourceContextUUIDsToSearch = Arrays.asList(resourceContexts).stream()
                .map(ResourceContext::getUuid).collect(toList());

        /*
         * According to https://stackoverflow.com/a/21494273/239003 , collections are
         * not allowed in native queries. This must be native since we use a table
         * function, so we do some manual looping.
         */
        String sql = "SELECT DISTINCT resourceId, action FROM racm.userActions(?) WHERE resourceContextUUID in ("
                + String.join(",", Collections.nCopies(resourceContextUUIDsToSearch.size(), "?")) + ")";

        TransientObjectManager tom = up.getTom();
        Query nativeQuery = tom.createNativeQuery(sql).setParameter(1, up.getUsername());
        for (int i = 0; i < resourceContextUUIDsToSearch.size(); ++i) {
            nativeQuery.setParameter(i + 2, resourceContextUUIDsToSearch.get(i));
        }

        @SuppressWarnings("unchecked")
        Collection<Object[]> rawResults = (Collection<Object[]>) tom.executeNativeQuery(nativeQuery);

        return rawResults.stream()
                .collect(groupingBy(x -> Long.valueOf(x[0].toString()), mapping(x -> (String) x[1], toList())));
    }

    Map<Long, List<String>> getAllowedResourceActionsOnContext(UserProfile up, String resourceContextsUUID) {
        String sql = "SELECT resourceId, action FROM racm.userResourceActionsOnContext(?,?)";

        TransientObjectManager tom = up.getTom();
        Query nativeQuery = tom.createNativeQuery(sql).setParameter(1,resourceContextsUUID).setParameter(2, up.getUsername());
        
        @SuppressWarnings("unchecked")
        Collection<Object[]> rawResults = (Collection<Object[]>) tom.executeNativeQuery(nativeQuery);

        return rawResults.stream()
                .collect(groupingBy(x -> Long.valueOf(x[0].toString()), mapping(x -> (String) x[1], toList())));
    }

    Set<String> getAllowedActions(UserProfile up, String resourceContextUUID, String pubDID) {
        String sql = "SELECT DISTINCT action FROM racm.userActions(?) WHERE resourcePubDID = (?) and resourceContextUUID = (?)";
        TransientObjectManager tom = up.getTom();
        Query nativeQuery = tom.createNativeQuery(sql).setParameter(1, up.getUsername()).setParameter(2, pubDID)
                .setParameter(3, resourceContextUUID);

        @SuppressWarnings("unchecked")
        Collection<String> rawResults = (Collection<String>) tom.executeNativeQuery(nativeQuery);

        return new HashSet<>(rawResults);
    }

    NativeQueryResult getAllowedUserVolumeActions(UserProfile up, String resourceContextUUID, String pubDID) {
        String sql = "SELECT DISTINCT ua.action, uv.relativePath FROM racm.userActions(?) ua "
                + "inner join  uservolume uv on uv.resourceId=ua.resourceId "
                + " WHERE ua.resourcePubDID = (?) and ua.resourceContextUUID = (?)";
        TransientObjectManager tom = up.getTom();
        Query nativeQuery = tom.createNativeQuery(sql).setParameter(1, up.getUsername()).setParameter(2, pubDID)
                .setParameter(3, resourceContextUUID);

        @SuppressWarnings("unchecked")
        List<?> rawResults = tom.executeNativeQuery(nativeQuery);
        NativeQueryResult r = new NativeQueryResult();
        r.setColumns("action,relativePath");
        r.setRows(rawResults);

        return r;
    }

    NativeQueryResult getAllowedDataVolumeActions(UserProfile up, String resourceContextUUID, String name) {
        String sql = "SELECT DISTINCT action, pathOnFileSystem FROM racm.dataVolumeAllowedActions(?,?,?)";

        TransientObjectManager tom = up.getTom();
        Query nativeQuery = tom.createNativeQuery(sql).setParameter(1, up.getUsername()).setParameter(2, name)
                .setParameter(3, resourceContextUUID);

        @SuppressWarnings("unchecked")
        List<?> rawResults = tom.executeNativeQuery(nativeQuery);
        NativeQueryResult r = new NativeQueryResult();
        r.setColumns("action,pathOnFileSystem");
        r.setRows(rawResults);

        return r;
    }

    NativeQueryResult getAllowedServiceVolumeActions(String serviceToken, UserProfile up, String resourceContextUUID,
            String pubDID) {
        String sql = "SELECT DISTINCT ua.name, uv.relativePath FROM racm.serviceActions(?,?,?) ua "
                + "inner join  uservolume uv on uv.resourceId=ua.resourceId ";

        TransientObjectManager tom = up.getTom();
        Query nativeQuery = tom.createNativeQuery(sql).setParameter(1, resourceContextUUID).setParameter(2, pubDID)
                .setParameter(3, serviceToken);

        @SuppressWarnings("unchecked")
        List<?> rawResults = tom.executeNativeQuery(nativeQuery);
        NativeQueryResult r = new NativeQueryResult();
        r.setColumns("action,pathOnFileSystem");
        r.setRows(rawResults);

        return r;
    }

    Map<VOURPEntityWithResource, Map<SciserverEntity, List<Privilege>>> getSharedPrivileges(
            List<VOURPEntityWithResource> userVolumes, TransientObjectManager tom) {
        return userVolumes.stream()
                .collect(groupingBy(Function.identity(),
                        flatMapping(
                                item -> getAllEntitiesWithPrivilege(tom, item.getResource()).stream()
                                        .filter(p -> item.showPrivilegesForUser(p.getScisEntity())),
                                groupingBy(Privilege::getScisEntity))));
    }

    @SuppressWarnings("unchecked")
    private List<Privilege> getAllEntitiesWithPrivilege(TransientObjectManager tom, Resource resource) {
        Query privileges = tom
                .createQuery("SELECT p FROM Privilege p join fetch p.scisEntity WHERE p.container = :resource")
                .setParameter("resource", resource);
        List<Privilege> privs = privileges.getResultList();
        return privs;
    }

    void assignPrivilages(SciserverEntity entity, Resource resource, String... actionNames) {
        Arrays.asList(actionNames).stream().map(name -> findAction(entity.getTom(), name, resource.getResourceType()))
                .forEach(action -> {
                    Privilege priv = new Privilege(resource);
                    priv.setAction(action);
                    priv.setScisEntity(entity);
                });
    }

    void deleteAllPrivileges(TransientObjectManager tom, long entityId, Resource resource) {
        List<Privilege> privilegesToDelete = new ArrayList<>();
        for (AccessControl ac : resource.getAccesControl()) {
            if (ac instanceof Privilege && ac.getScisEntity().getId() == entityId) {
                privilegesToDelete.add((Privilege) ac);
            }
        }
        for (Privilege p : privilegesToDelete) {
            resource.getAccesControl().remove(p);
        }
    }
    void deleteAllRoles(TransientObjectManager tom, long entityId, Resource resource) {
        List<RoleAssignment> rolesToDelete = new ArrayList<>();
        for (AccessControl ac : resource.getAccesControl()) {
            if (ac instanceof RoleAssignment && ac.getScisEntity().getId() == entityId) {
                rolesToDelete.add((RoleAssignment) ac);
            }
        }
        for (RoleAssignment p : rolesToDelete) {
            resource.getAccesControl().remove(p);
        }
    }
    
    private Action findAction(TransientObjectManager tom, String name, ResourceType rt) {
        Query actionQuery = tom
                .createQuery("SELECT a FROM Action a WHERE a.name = :name AND a.container = :resourcetype")
                .setParameter("name", name).setParameter("resourcetype", rt);
        return tom.queryOne(actionQuery, Action.class);
    }

    /*
     * from https://stackoverflow.com/a/39131049/239003 This is needed until Java 9
     */
    private static <T, U, A, R> Collector<T, ?, R> flatMapping(
            Function<? super T, ? extends Stream<? extends U>> mapper, Collector<? super U, A, R> downstream) {

        BiConsumer<A, ? super U> acc = downstream.accumulator();
        return Collector.of(downstream.supplier(), (a, t) -> {
            try (Stream<? extends U> s = mapper.apply(t)) {
                if (s != null)
                    s.forEachOrdered(u -> acc.accept(a, u));
            }
        }, downstream.combiner(), downstream.finisher(),
                downstream.characteristics().stream().toArray(Collector.Characteristics[]::new));
    }
}
