package org.sciserver.springapp.racm.resourcecontext.vourp;

import static java.util.Collections.emptySet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FlushModeType;
import javax.persistence.Persistence;

import org.ivoa.dm.model.TransientObjectManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sciserver.springapp.racm.login.NotAuthorizedException;
import org.sciserver.springapp.racm.resourcecontext.domain.Resource;
import org.sciserver.springapp.racm.resourcecontext.domain.AssociatedResource;
import org.sciserver.springapp.racm.resourcecontext.domain.AssociatedSciserverEntity;
import org.sciserver.springapp.racm.storem.application.RegistrationInvalidException;
import org.sciserver.springapp.racm.utils.VOURPContext;
import org.sciserver.springapp.racm.utils.controller.ResourceContextNotFoundException;
import org.sciserver.springapp.racm.utils.controller.ResourceNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import edu.jhu.rac.ContextClass;
import edu.jhu.rac.ResourceContext;
import edu.jhu.rac.ResourceType;
import edu.jhu.user.ServiceAccount;

/**
 * Real JPQL, entity mappings and TOM persistence; no external services or database mocks.
 * Transactions are explicit, without production's AspectJ transaction weaving. Assertions
 * about stored state reload after commit; returned pre-commit numeric IDs are not tested.
 */
public class ResourceRepositoryIntegrationTests {
    private static EntityManagerFactory factory;
    private EntityManager em;
    private ResourceRepository repository;
    private String contextA;
    private String contextB;
    private String contextWithoutAccount;
    private static final String TOKEN_A = "token-a";
    private static final String TOKEN_B = "token-b";

    @BeforeClass
    public static void openDatabase() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("javax.persistence.jdbc.driver", "org.h2.Driver");
        properties.put("javax.persistence.jdbc.url", "jdbc:h2:mem:racm-" + UUID.randomUUID()
                + ";MODE=LEGACY;DB_CLOSE_DELAY=-1");
        properties.put("javax.persistence.jdbc.user", "sa");
        properties.put("javax.persistence.jdbc.password", "");
        properties.put("eclipselink.target-database", "org.eclipse.persistence.platform.database.H2Platform");
        properties.put("eclipselink.weaving", "false");
        properties.put("eclipselink.ddl-generation", "create-tables");
        properties.put("eclipselink.ddl-generation.output-mode", "database");
        properties.put("eclipselink.logging.level", "SEVERE");
        factory = Persistence.createEntityManagerFactory("RACM-PU", properties);
    }

    @AfterClass
    public static void closeDatabase() {
        if (factory != null) factory.close();
    }

    @Before
    public void createFixture() throws Exception {
        openRequest();
        TransientObjectManager tom = new TransientObjectManager(em);
        ContextClass contextClass = new ContextClass(tom);
        contextClass.setName("test-" + UUID.randomUUID());
        ResourceType type = new ResourceType(contextClass);
        type.setName("object");
        ResourceType otherType = new ResourceType(contextClass);
        otherType.setName("bucket");
        contextA = context(tom, contextClass, TOKEN_A);
        contextB = context(tom, contextClass, TOKEN_B);
        contextWithoutAccount = context(tom, contextClass, null);
        tom.persist();
        commitAndReopen();
    }

    private String context(TransientObjectManager tom, ContextClass contextClass, String token) {
        ResourceContext context = new ResourceContext(tom);
        context.setUuid(UUID.randomUUID().toString());
        context.setContextClass(contextClass);
        if (token != null) {
            ServiceAccount account = new ServiceAccount(tom);
            account.setServiceToken(token);
            context.setAccount(account);
        }
        return context.getUuid();
    }

    private void openRequest() {
        em = factory.createEntityManager();
        // Match RACMDatabaseConfiguration: queries must not flush partially built associations.
        em.setFlushMode(FlushModeType.COMMIT);
        em.getTransaction().begin();
        VOURPContext context = new VOURPContext();
        ReflectionTestUtils.setField(context, "em", em);
        repository = new ResourceRepository(context);
    }

    private void commitAndReopen() {
        em.getTransaction().commit();
        em.close();
        openRequest();
    }

    @After
    public void closeRequest() {
        if (em != null && em.isOpen()) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.close();
        }
    }

    private Resource createResource() {
        Resource result = repository.add(Resource.createNew(contextA, "pubdid", "original", "description", "object"), TOKEN_A);
        commitAndReopen();
        return repository.get(result.uuid());
    }

    private Resource edited(Resource resource, String context, String type) {
        return Resource.createFromExisting(resource.id(), resource.uuid(), context,
                "new-pubdid", "changed", "new-description", type, emptySet(), emptySet());
    }

    private void expect(Class<? extends Throwable> type, Runnable operation) {
        try {
            operation.run();
            fail("Expected " + type.getSimpleName());
        } catch (RuntimeException ex) {
            if (!type.isInstance(ex)) throw ex;
        }
    }

    @Test
    public void createsAndUpdatesPersistedResource() {
        Resource input = Resource.createNew(contextA, "pubdid", "original", "description", "object");
        Resource saved = repository.add(input, TOKEN_A);
        commitAndReopen();
        assertTrue(input.isTransient());
        assertFalse(saved.isTransient());
        assertNotNull(saved.uuid());
        Resource persisted = repository.get(saved.uuid());
        assertEquals("original", persisted.name());
        repository.update(edited(persisted, contextA, "object"), TOKEN_A);
        commitAndReopen();
        Resource reloaded = repository.get(saved.uuid());
        assertEquals(persisted.id(), reloaded.id());
        assertEquals(contextA, reloaded.resourceContextUUID());
        assertEquals("changed", reloaded.name());
        assertEquals("new-pubdid", reloaded.publisherDID());
        assertEquals("new-description", reloaded.description());
        assertEquals("object", reloaded.resourceTypeName());
    }

    @Test
    public void addRejectsExistingResource() {
        Resource saved = createResource();
        expect(IllegalArgumentException.class, () -> repository.add(saved, TOKEN_A));
    }

    @Test
    public void persistsBothAssociationKindsThroughUpdate() {
        Resource parent = createResource();
        Resource child = createResource();
        long accountId = em.createQuery("SELECT rc FROM ResourceContext rc WHERE rc.uuid = :uuid",
                ResourceContext.class).setParameter("uuid", contextA).getSingleResult().getAccount().getId();
        Resource input = repository.get(parent.uuid());
        input.addAssociationWithResource(new AssociatedResource(child.uuid(), "child", "description", false));
        input.addAssociationWithSciserverEntity(new AssociatedSciserverEntity(accountId, "SERVICE", "operator", false));
        repository.update(input, TOKEN_A);
        commitAndReopen();
        Resource reloaded = repository.get(parent.uuid());
        assertEquals(1, reloaded.associatedResources().size());
        assertEquals(child.uuid(), reloaded.associatedResources().iterator().next().resourceUUID());
        assertEquals(1, reloaded.associatedSciserverEntities().size());
        assertEquals(accountId, reloaded.associatedSciserverEntities().iterator().next().entityId());
    }

    @Test
    public void rejectsCrossContextUpdateAndDeleteWithoutChangingRow() {
        Resource saved = createResource();
        Resource forged = edited(saved, contextB, "object");
        expect(ResourceNotFoundException.class, () -> repository.update(forged, TOKEN_B));
        expect(ResourceNotFoundException.class, () -> repository.delete(forged, TOKEN_B));
        commitAndReopen();
        assertEquals("original", repository.get(saved.uuid()).name());
        assertEquals(contextA, repository.get(saved.uuid()).resourceContextUUID());
    }

    @Test
    public void rejectsWrongTokenForEveryMutation() {
        Resource saved = createResource();
        expect(NotAuthorizedException.class, () -> repository.add(
                Resource.createNew(contextA, "other", "other", "description", "object"), TOKEN_B));
        expect(NotAuthorizedException.class, () -> repository.update(edited(saved, contextA, "object"), TOKEN_B));
        expect(NotAuthorizedException.class, () -> repository.delete(saved, TOKEN_B));
        commitAndReopen();
        assertEquals("original", repository.get(saved.uuid()).name());
    }

    @Test
    public void rejectsTypeChangeBeforeCopyingFields() {
        Resource saved = createResource();
        expect(RegistrationInvalidException.class, () -> repository.update(edited(saved, contextA, "bucket"), TOKEN_A));
        commitAndReopen();
        assertEquals("object", repository.get(saved.uuid()).resourceTypeName());
        assertEquals("original", repository.get(saved.uuid()).name());
    }

    @Test
    public void deletesAuthorizedResourceAndReportsMissingRow() {
        Resource saved = createResource();
        repository.delete(saved, TOKEN_A);
        commitAndReopen();
        assertNull(repository.get(saved.uuid()));
        expect(ResourceNotFoundException.class, () -> repository.delete(saved, TOKEN_A));
        expect(ResourceNotFoundException.class, () -> repository.update(saved, TOKEN_A));
    }

    @Test
    public void distinguishesMissingAndAccountlessContexts() {
        Resource missing = Resource.createNew("missing-context", "pubdid", "name", "description", "object");
        expect(ResourceContextNotFoundException.class, () -> repository.add(missing, TOKEN_A));
        Resource accountless = Resource.createNew(contextWithoutAccount, "pubdid", "name", "description", "object");
        expect(NotAuthorizedException.class, () -> repository.add(accountless, TOKEN_A));
        Resource saved = createResource();
        expect(ResourceContextNotFoundException.class, () -> repository.update(edited(saved, "missing-context", "object"), TOKEN_A));
        expect(NotAuthorizedException.class, () -> repository.delete(edited(saved, contextWithoutAccount, "object"), TOKEN_A));
    }

    @Test
    public void rejectsNullInputsExplicitly() {
        expect(ResourceNotFoundException.class, () -> repository.add(null, TOKEN_A));
        expect(ResourceNotFoundException.class, () -> repository.update(null, TOKEN_A));
        expect(ResourceNotFoundException.class, () -> repository.delete(null, TOKEN_A));
    }
}
