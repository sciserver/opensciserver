package org.ivoa.dm.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.Transactional;

import org.eclipse.persistence.config.HintValues;
import org.eclipse.persistence.config.QueryHints;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.sessions.changesets.ObjectChangeSet;
import org.eclipse.persistence.sessions.changesets.UnitOfWorkChangeSet;
import org.ivoa.dm.VOURPException;

public class TransientObjectManager implements IMetadataObjectContainer {

	public class ChangeSet {
		private ArrayList<MetadataObject> added = new ArrayList<>();
		private ArrayList<MetadataObject> updated = new ArrayList<>();
		private ArrayList<MetadataObject> deleted = new ArrayList<>();

		/**
		 * Declare a new object to the changeset.<br/>
		 * 
		 * @param o
		 */
		public boolean add(MetadataObject o) {
			if (o.isPurelyTransient() && !added.contains(o))
				return added.add(o);
			else
				return false;
		}

		/**
		 * declare an updated object to the changeset.<br/>
		 * 
		 * @param o
		 * @return
		 */
		public boolean update(MetadataObject o) {
			if (o.isTransientCopy() && !updated.contains(o))
				return updated.add(o);
			else
				return false;
		}

		/**
		 * declare a deleted object to the changeset.<br/>
		 * 
		 * @param o
		 * @return
		 */
		public boolean delete(MetadataObject o) {
			if (o.isTransientCopy() && !deleted.contains(o))
				return deleted.add(o);
			else
				return false;
		}

		/**
		 * Validate all added and updated objects and return list of invalid objects if any.<br/>
		 * @return
		 */
		private ArrayList<MetadataObject> validate(){
			ArrayList<MetadataObject> invalidObjects = new ArrayList<>();
			for(MetadataObject o : added)
				if(!o.isValid())
					invalidObjects.add(o);
			for(MetadataObject o : updated)
				if(!o.isValid())
					invalidObjects.add(o);
			return invalidObjects;
		}

		/**
		 * Persist objects in this changeset to the database.<br/>
		 * 
		 * TODO check each object is actually known to the EMs UnitOfWorkChangeSet.
		 * TODO check that no other objects in the UnitOfWorkChangeSet were modified.
		 * 
		 * @param currentTimestamp
		 */
		private void persistChangeSet(Date currentTimestamp, boolean doValidate)  throws InvalidTOMException{
			if(doValidate) {
				ArrayList<MetadataObject> invalidObjects = validate();
				if (invalidObjects.size() > 0)
					throw new InvalidTOMException(invalidObjects);
			}
			UnitOfWorkChangeSet changeSet = getUnitOfWorkChangeSet();
			if (added != null) {
				for (MetadataObject o : added) {
					if (o instanceof MetadataRootEntityObject) {
						MetadataRootEntityObject root = (MetadataRootEntityObject) o;
						if (root.isPurelyTransient())
							root.setCreationDate(currentTimestamp);
						final ObjectChangeSet objectChangeSet = changeSet.getObjectChangeSetForClone(root);
						if (objectChangeSet != null && objectChangeSet.hasChanges())
							root.setModificationDate(currentTimestamp); //
					}
					em.persist(o);
				}
			}
			if (updated != null) {
				for (MetadataObject o : updated) {
					if (o instanceof MetadataRootEntityObject) {
						MetadataRootEntityObject root = (MetadataRootEntityObject) o;
						final ObjectChangeSet objectChangeSet = changeSet.getObjectChangeSetForClone(root);
						if (objectChangeSet != null && objectChangeSet.hasChanges()) {
							root.setModificationDate(currentTimestamp); //
							em.persist(o);
						}
					}
				}
			}
			if (deleted != null) {
				for (MetadataObject o : deleted)
					em.remove(o);
			}
		}
	}
	/**
	 * TODO ensure only one changeset active per session/transaction ??!!
	 * @return
	 */
	public ChangeSet newChangeSet() {
		return this.new ChangeSet(); 
	}

	private final List<MetadataRootEntityObject> entities = new ArrayList<>();
	private final List<MetadataRootEntityObject> removedEntities = new ArrayList<>();

	private final EntityManager em;

	public TransientObjectManager(EntityManager em) {
		this.em = em;
	}

	protected void addEntity(MetadataRootEntityObject entity) {
		if (!entities.contains(entity))
			entities.add(entity);
	}

	public List<MetadataObject> queryJPA(String query, boolean doRefresh) {
		return queryJPA(em.createQuery(query), doRefresh);
	}

	public List<MetadataObject> queryJPA(String query) {
		return queryJPA(em.createQuery(query));
	}

	public Query createQuery(String query) {
		return em.createQuery(query);
	}

	public Query createNamedQuery(String queryName) {
		return em.createNamedQuery(queryName);
	}

	public List<MetadataObject> queryJPA(Query q) {
		return queryJPA(q, true);
	}

	@Transactional
	public boolean persist() throws InvalidTOMException {
		ValidatorVisitor v = new ValidatorVisitor();
		if (isValid(v)) {
			persist(entities, removedEntities);
			return true;
		}
		throw new InvalidTOMException(v.getInvalidObjects());
	}

	@Transactional
	public boolean delete(final Collection<MetadataObject> removedObjects) {
		if (removedObjects != null) {
			for (MetadataObject o : removedObjects)
				em.remove(o);
		}
		return true;
	}

	/**
	 * Protected so othe classes in this model can access this method. E.g.
	 * PersistObjectPreProcessor
	 * 
	 * @return
	 */
	protected UnitOfWorkChangeSet getUnitOfWorkChangeSet() {
		JpaEntityManager jpaEntityManager = (JpaEntityManager) em.getDelegate();
		UnitOfWorkChangeSet changeSet = jpaEntityManager.getUnitOfWork().getCurrentChanges();
		return changeSet;
	}

	/**
	 * ONLY persist the specified objects. This to avoid possible useless validation
	 * of objects that have not changed.<br/>
	 * 
	 * TODO if the em's changeset contains objects apart from these, an error should
	 * be thrown.
	 * 
	 * @param objectsToBePersisted
	 * @return
	 */
	@Transactional
	public boolean persistChangeSet(final ChangeSet changeSet, boolean doValidate) throws InvalidTOMException{
		final Timestamp currentTimestamp = new Timestamp(Calendar.getInstance().getTimeInMillis());
		changeSet.persistChangeSet(currentTimestamp, doValidate);
		return true;
	}

	/**
	 * Persist (aka flush) all specified objects (and their children) to the
	 * database.<br/>
	 * For now only root entity objects can be persisted as a whole. TODO decide
	 * whether this can be generalised.
	 *
	 * @param objects
	 */
	private void persist(final Collection<MetadataRootEntityObject> objects,
	    final Collection<MetadataRootEntityObject> removedObjects) {
		// TODO here we could(should?) get the current timestamp from the database,
		// which is not necessarily in synch with the web server.
		// For now a simpler solution ...
		final Timestamp currentTimestamp = new Timestamp(Calendar.getInstance().getTimeInMillis());
		final PersistObjectPreProcessor preProcessor = new PersistObjectPreProcessor(currentTimestamp, this);
		for (MetadataRootEntityObject o : objects) {
			o.accept(preProcessor);
		}

		for (MetadataRootEntityObject o : objects) {
			if (o.isPurelyTransient() && !em.contains(o)) {
				em.persist(o);
			}
		}

		// now remove objects
		if (removedObjects != null) {
			for (MetadataRootEntityObject o : removedObjects)
				em.remove(o);
		}
	}

	public List<MetadataObject> queryJPA(Query q, boolean doRefresh) {
		if (doRefresh)
			q.setHint(QueryHints.REFRESH, HintValues.TRUE);
		List<?> list = q.getResultList();
		ArrayList<MetadataObject> r = new ArrayList<>();
		for (Object o : list)
			r.add((MetadataObject) o);
		add(r);
		return r;
	}

	/**
	 * Unknown result type.
	 * 
	 * @param q
	 * @param doRefresh
	 * @return
	 */
	public List<?> customJPQL(Query q, boolean doRefresh) {
		if (doRefresh)
			q.setHint(QueryHints.REFRESH, HintValues.TRUE);
		return q.getResultList();
	}

	@SuppressWarnings("unchecked")
	public <T extends MetadataObject> T queryOne(Query q, Class<T> t) {
		q.setMaxResults(1);
		List<MetadataObject> l = this.queryJPA(q, false);
		if (l.size() == 1)
			return (T) l.get(0);
		return null;
	}

	@SuppressWarnings("unchecked")
	public <T extends MetadataObject> List<T> queryJPA(Query q, Class<T> t) throws VOURPException {
		List<MetadataObject> l = this.queryJPA(q, false);
		List<T> ts = new ArrayList<>();
		for (MetadataObject o : l) {
			try {
				ts.add((T) o);
			} catch (ClassCastException e) {
				throw new VOURPException(e);
			}
		}
		return ts;
	}

	public <T extends MetadataObject> T find(Class<T> t, Object o) {
		T mo = em.find(t, o);
		add(mo);
		return mo;
	}

	private void add(List<MetadataObject> l) {
		for (MetadataObject o : l)
			add(o);
	}

	private void add(MetadataObject o) {
		if (o.getTom() == null)
			o.setTom(this);
		else if (o.getTom() != this) {
			throw new IllegalStateException();
		}

	}

	public boolean isValid() {
		return isValid(new ValidatorVisitor());
	}

	private boolean isValid(ValidatorVisitor v) {
		visit(v);
		return !v.foundErrors();
	}

	public void visit(MetaDataObjectVisitor visitor) {
		for (MetadataRootEntityObject o : this.entities)
			o.accept(visitor);
	}

	@Override
	public TransientObjectManager getTom() {
		return this;
	}

	@Override
	public IMetadataObjectContainer getContainer() {
		return null;
	}

	public void refresh(MetadataObject o) {
		em.refresh(o);
		o.setTom(this);

	}

	public Query createNativeQuery(String sql) {
		return em.createNativeQuery(sql);
	}

	/**
	 * Execute the specified native query.<br/>
	 * TODO deal with timeout
	 * 
	 * @param q
	 * @return
	 */
	public List<?> executeNativeQuery(Query q) {
		return q.getResultList();
	}

	public void remove(MetadataRootEntityObject o) {
		if (entities.contains(o)) {
			entities.remove(o);
			removedEntities.add(o);
		} else {
			throw new IllegalArgumentException(
			    "Tried to remove an object that was not " + "part of the TransientObjectManager to begin with");
		}
	}

	public EntityManager getEntityManager() {
		return em;
	}
}
