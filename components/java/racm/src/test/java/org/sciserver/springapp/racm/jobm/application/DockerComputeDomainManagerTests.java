package org.sciserver.springapp.racm.jobm.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.persistence.Query;

import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.TransientObjectManager;
import org.junit.Before;
import org.junit.Test;
import org.sciserver.racm.jobm.model.RootVolumeOnComputeDomainModel;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.controller.ResourceNotFoundException;

import edu.jhu.job.DockerComputeDomain;
import edu.jhu.job.RootVolumeOnComputeDomain;

public class DockerComputeDomainManagerTests {
    private static final String RACM_UUID = "3f9a1c2e-8b04-4d17-9e55-1a2b3c4d5e6f";
    private static final Long ROOT_VOLUME_ID = 17L;

    private final JOBMAccessControl accessControl = mock(JOBMAccessControl.class);
    private final JOBMModelFactory modelFactory = mock(JOBMModelFactory.class);
    private final ComputeDomainManager computeDomainManager = mock(ComputeDomainManager.class);
    private final DockerComputeDomainManager manager =
            new DockerComputeDomainManager(accessControl, modelFactory, computeDomainManager);

    private final UserProfile up = mock(UserProfile.class);
    private final TransientObjectManager tom = mock(TransientObjectManager.class);
    private final Query query = mock(Query.class);

    @Before
    public void setUp() {
        when(up.getTom()).thenReturn(tom);
        when(up.getUsername()).thenReturn("someuser");
        when(tom.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    /** Registers a compute domain as the one the uuid lookup will find, and authorizes the user. */
    private DockerComputeDomain authorizedDomain() {
        DockerComputeDomain dcd = mock(DockerComputeDomain.class);
        when(dcd.getApiEndpoint()).thenReturn("https://example.org/racm");
        when(tom.queryOne(query, DockerComputeDomain.class)).thenReturn(dcd);
        when(accessControl.canAddRootVolume(up, dcd)).thenReturn(true);
        return dcd;
    }

    private RootVolumeOnComputeDomainModel validModel() {
        RootVolumeOnComputeDomainModel m = new RootVolumeOnComputeDomainModel();
        m.setRootVolumeId(ROOT_VOLUME_ID);
        m.setPathOnCD("/home/idies/workspace/Storage");
        m.setDisplayName("Storage");
        return m;
    }

    @Test
    public void unknownRacmUUIDIsNotFound() {
        when(tom.queryOne(query, DockerComputeDomain.class)).thenReturn(null);

        try {
            manager.addRootVolume(RACM_UUID, validModel(), up);
            fail("expected ResourceNotFoundException");
        } catch (ResourceNotFoundException expected) {
            // pass
        } catch (VOURPException e) {
            fail("expected ResourceNotFoundException, got " + e);
        }
    }

    @Test
    public void unauthorizedUserIsRejectedAndNothingIsCreated() throws Exception {
        DockerComputeDomain dcd = mock(DockerComputeDomain.class);
        when(dcd.getApiEndpoint()).thenReturn("https://example.org/racm");
        when(tom.queryOne(query, DockerComputeDomain.class)).thenReturn(dcd);
        when(accessControl.canAddRootVolume(up, dcd)).thenReturn(false);

        try {
            manager.addRootVolume(RACM_UUID, validModel(), up);
            fail("expected VOURPException");
        } catch (VOURPException expected) {
            assertEquals(VOURPException.UNAUTHORIZED, expected.getErrorCode());
        }

        verify(modelFactory, never()).newRootVolumeOnComputeDomain(any(), any());
    }

    @Test
    public void validRequestDelegatesToTheFactoryAndDoesNotPersist() throws Exception {
        DockerComputeDomain dcd = authorizedDomain();
        RootVolumeOnComputeDomain created = mock(RootVolumeOnComputeDomain.class);
        RootVolumeOnComputeDomainModel model = validModel();
        when(modelFactory.newRootVolumeOnComputeDomain(model, dcd)).thenReturn(created);

        RootVolumeOnComputeDomain result = manager.addRootVolume(RACM_UUID, model, up);

        assertSame(created, result);
        verify(modelFactory).newRootVolumeOnComputeDomain(model, dcd);
        // The transaction belongs to the controller. RACM uses no @Transactional; managers mutate
        // and the controller calls tom.persist(). Persisting here would break that contract.
        verify(tom, never()).persist();
    }
}
