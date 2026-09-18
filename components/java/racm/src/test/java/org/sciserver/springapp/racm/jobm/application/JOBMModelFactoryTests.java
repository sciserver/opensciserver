package org.sciserver.springapp.racm.jobm.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.persistence.Query;

import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.TransientObjectManager;
import org.junit.Before;
import org.junit.Test;
import org.sciserver.racm.jobm.model.RootVolumeOnComputeDomainModel;
import org.sciserver.springapp.racm.utils.VOURPContext;

import edu.jhu.file.RootVolume;
import edu.jhu.job.DockerComputeDomain;
import edu.jhu.job.RootVolumeOnComputeDomain;

/**
 * Pins the publisherDID round-trip for RootVolumeOnComputeDomain. This pair was the only
 * entity/model pair in the factory that silently dropped publisherDID in both directions.
 */
public class JOBMModelFactoryTests {
    private static final Long ROOT_VOLUME_ID = 17L;
    private static final String PUBLISHER_DID = "ivo://sciserver.org/computedomain/1#storage";

    private final JOBMAccessControl accessControl = mock(JOBMAccessControl.class);
    private final VOURPContext vourpContext = mock(VOURPContext.class);
    private final JOBMModelFactory factory = new JOBMModelFactory(accessControl, vourpContext);

    private final TransientObjectManager tom = mock(TransientObjectManager.class);
    private final Query query = mock(Query.class);
    private DockerComputeDomain domain;

    @Before
    public void setUp() {
        when(tom.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);

        RootVolume rootVolume = mock(RootVolume.class);
        when(rootVolume.getId()).thenReturn(ROOT_VOLUME_ID);
        when(tom.queryOne(query, RootVolume.class)).thenReturn(rootVolume);

        domain = mock(DockerComputeDomain.class);
        when(domain.getTom()).thenReturn(tom);
    }

    private RootVolumeOnComputeDomainModel model(String publisherDID) {
        RootVolumeOnComputeDomainModel m = new RootVolumeOnComputeDomainModel();
        m.setRootVolumeId(ROOT_VOLUME_ID);
        m.setPathOnCD("/home/idies/workspace/Storage");
        m.setDisplayName("Storage");
        m.setPublisherDID(publisherDID);
        return m;
    }

    @Test
    public void publisherDIDIsCarriedOntoTheEntity() throws VOURPException {
        RootVolumeOnComputeDomain entity = factory.newRootVolumeOnComputeDomain(model(PUBLISHER_DID), domain);

        assertEquals(PUBLISHER_DID, entity.getPublisherDID());
    }

    @Test
    public void absentPublisherDIDLeavesTheEntityNull() throws VOURPException {
        RootVolumeOnComputeDomain entity = factory.newRootVolumeOnComputeDomain(model(null), domain);

        assertNull(entity.getPublisherDID());
    }

    @Test
    public void publisherDIDIsReadBackIntoTheModel() throws VOURPException {
        RootVolumeOnComputeDomain entity = factory.newRootVolumeOnComputeDomain(model(PUBLISHER_DID), domain);

        RootVolumeOnComputeDomainModel rendered = factory.newRootVolumeOnComputeDomainModel(entity);

        assertEquals(PUBLISHER_DID, rendered.getPublisherDID());
        assertEquals("/home/idies/workspace/Storage", rendered.getPathOnCD());
        assertEquals("Storage", rendered.getDisplayName());
        assertEquals(ROOT_VOLUME_ID, rendered.getRootVolumeId());
    }
}
