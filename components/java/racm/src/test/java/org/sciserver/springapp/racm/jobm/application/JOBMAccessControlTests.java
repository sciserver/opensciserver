package org.sciserver.springapp.racm.jobm.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.RACM;
import org.sciserver.springapp.racm.utils.RACMNames;

import edu.jhu.job.DockerComputeDomain;
import edu.jhu.rac.ResourceContext;

public class JOBMAccessControlTests {
    private static final String CONTEXT_UUID = "3f9a1c2e-8b04-4d17-9e55-1a2b3c4d5e6f";

    private final RACM racm = mock(RACM.class);
    private final JOBMAccessControl accessControl = new JOBMAccessControl(racm);

    private DockerComputeDomain domainWithContext() {
        ResourceContext rc = mock(ResourceContext.class);
        when(rc.getUuid()).thenReturn(CONTEXT_UUID);
        DockerComputeDomain dcd = mock(DockerComputeDomain.class);
        when(dcd.getResourceContext()).thenReturn(rc);
        return dcd;
    }

    private UserProfile user() {
        UserProfile up = mock(UserProfile.class);
        when(up.getUsername()).thenReturn("someuser");
        return up;
    }

    @Test
    public void canAddRootVolumeChecksTheAddRootVolumeActionOnTheDomainsRootContext() {
        when(racm.canUserDoActionOnRootContext("someuser", CONTEXT_UUID, RACMNames.A_ADD_ROOT_VOLUME))
                .thenReturn(true);

        assertTrue(accessControl.canAddRootVolume(user(), domainWithContext()));
    }

    @Test
    public void canAddRootVolumeIsFalseWhenTheActionIsNotGranted() {
        when(racm.canUserDoActionOnRootContext("someuser", CONTEXT_UUID, RACMNames.A_ADD_ROOT_VOLUME))
                .thenReturn(false);

        assertFalse(accessControl.canAddRootVolume(user(), domainWithContext()));
    }

    @Test
    public void actionNameIsAddRootVolume() {
        // The V35 migration inserts this literal into t_Action; the two must agree.
        assertEquals("addRootVolume", RACMNames.A_ADD_ROOT_VOLUME);
    }
}
