package org.sciserver.springapp.racm.jobm.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import javax.persistence.Query;

import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.TransientObjectManager;
import org.junit.Before;
import org.junit.Test;
import org.sciserver.racm.jobm.model.RootVolumeOnComputeDomainModel;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.controller.ResourceNotFoundException;

import edu.jhu.file.RootVolume;
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

    /** Builds a mock existing entry on the domain. Any argument may be null. */
    private RootVolumeOnComputeDomain existingEntry(Long rootVolumeId, String path, String displayName) {
        RootVolumeOnComputeDomain rv = mock(RootVolumeOnComputeDomain.class);
        if (rootVolumeId != null) {
            RootVolume target = mock(RootVolume.class);
            when(target.getId()).thenReturn(rootVolumeId);
            when(rv.getRootVolume()).thenReturn(target);
        }
        when(rv.getPath()).thenReturn(path);
        when(rv.getDisplayName()).thenReturn(displayName);
        return rv;
    }

    private void withExistingEntries(DockerComputeDomain dcd, RootVolumeOnComputeDomain... entries) {
        List<RootVolumeOnComputeDomain> list = Arrays.asList(entries);
        when(dcd.getRootVolume()).thenReturn(list);
    }

    /** Asserts that the call fails with ILLEGAL_ARGUMENT and that nothing was created. */
    private void assertRejected(RootVolumeOnComputeDomainModel model) throws Exception {
        try {
            manager.addRootVolume(RACM_UUID, model, up);
            fail("expected VOURPException(ILLEGAL_ARGUMENT)");
        } catch (VOURPException expected) {
            assertEquals(VOURPException.ILLEGAL_ARGUMENT, expected.getErrorCode());
        }
        verify(modelFactory, never()).newRootVolumeOnComputeDomain(any(), any());
    }

    @Test
    public void suppliedIdIsRejected() throws Exception {
        authorizedDomain();
        RootVolumeOnComputeDomainModel model = validModel();
        model.setId(99L);

        assertRejected(model);
    }

    @Test
    public void missingRootVolumeIdIsRejected() throws Exception {
        authorizedDomain();
        RootVolumeOnComputeDomainModel model = validModel();
        model.setRootVolumeId(null);

        assertRejected(model);
    }

    @Test
    public void blankPathIsRejected() throws Exception {
        authorizedDomain();
        RootVolumeOnComputeDomainModel model = validModel();
        model.setPathOnCD("   ");

        assertRejected(model);
    }

    @Test
    public void missingDisplayNameIsRejected() throws Exception {
        authorizedDomain();
        RootVolumeOnComputeDomainModel model = validModel();
        model.setDisplayName(null);

        assertRejected(model);
    }

    @Test
    public void mountingTheSameRootVolumeTwiceIsRejected() throws Exception {
        DockerComputeDomain dcd = authorizedDomain();
        withExistingEntries(dcd, existingEntry(ROOT_VOLUME_ID, "/somewhere/else", "Other"));

        assertRejected(validModel());
    }

    @Test
    public void duplicatePathIsRejected() throws Exception {
        DockerComputeDomain dcd = authorizedDomain();
        withExistingEntries(dcd,
                existingEntry(999L, "/home/idies/workspace/Storage", "Other"));

        assertRejected(validModel());
    }

    @Test
    public void duplicateDisplayNameDifferingOnlyInCaseIsRejected() throws Exception {
        DockerComputeDomain dcd = authorizedDomain();
        withExistingEntries(dcd, existingEntry(999L, "/somewhere/else", "sTORAGE"));

        assertRejected(validModel());
    }

    /**
     * Deliberate absence of validation. publisherDID belongs to the publisher: nothing in RACM
     * looks this entity up by it, and duplicates may be intentional. If you are here because you
     * "fixed" a missing uniqueness check, read design v5 D12 first -- this test failing means the
     * decision was reversed, not that a bug was found.
     */
    @Test
    public void duplicatePublisherDIDIsAllowed() throws Exception {
        DockerComputeDomain dcd = authorizedDomain();
        RootVolumeOnComputeDomain existing = existingEntry(999L, "/somewhere/else", "Other");
        when(existing.getPublisherDID()).thenReturn("ivo://example.org/thing");
        withExistingEntries(dcd, existing);

        RootVolumeOnComputeDomainModel model = validModel();
        model.setPublisherDID("ivo://example.org/thing");
        RootVolumeOnComputeDomain created = mock(RootVolumeOnComputeDomain.class);
        when(modelFactory.newRootVolumeOnComputeDomain(model, dcd)).thenReturn(created);

        assertNotNull(manager.addRootVolume(RACM_UUID, model, up));
    }

    /** getRootVolume() returns null, not an empty list, until something has been attached. */
    @Test
    public void domainWithNoRootVolumesIsAccepted() throws Exception {
        DockerComputeDomain dcd = authorizedDomain();
        when(dcd.getRootVolume()).thenReturn(null);

        RootVolumeOnComputeDomainModel model = validModel();
        RootVolumeOnComputeDomain created = mock(RootVolumeOnComputeDomain.class);
        when(modelFactory.newRootVolumeOnComputeDomain(model, dcd)).thenReturn(created);

        assertNotNull(manager.addRootVolume(RACM_UUID, model, up));
    }

    /** The new invariants do not hold retroactively: legacy rows may have null fields. */
    @Test
    public void existingEntryWithNullDisplayNameDoesNotBreakValidation() throws Exception {
        DockerComputeDomain dcd = authorizedDomain();
        withExistingEntries(dcd, existingEntry(999L, null, null));

        RootVolumeOnComputeDomainModel model = validModel();
        RootVolumeOnComputeDomain created = mock(RootVolumeOnComputeDomain.class);
        when(modelFactory.newRootVolumeOnComputeDomain(model, dcd)).thenReturn(created);

        assertNotNull(manager.addRootVolume(RACM_UUID, model, up));
    }
}
