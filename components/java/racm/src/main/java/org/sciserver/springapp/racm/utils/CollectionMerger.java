package org.sciserver.springapp.racm.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ivoa.dm.model.MetadataObject;
import org.sciserver.racm.utils.model.RACMBaseModel;

/**
 * This class implements the common pattern where a collection of VO-URP MetadataObject must be merged with a collection of corresponding model objects.
 * The class builds a data structure consisting of
 * - an insert list, consisting of model objects that should give rise to new metadataobjects;
 * - a remove collection, consisting of metadataobjects that should be removed
 * - two aligned update lists, consisting of metadataobject/modelobject pairs, where the former should be updated with the latter.
 *
 *
 * @author gerard
 *
 * @param <D>
 * @param <M>
 */
public class CollectionMerger<D extends MetadataObject, M extends RACMBaseModel> {

	private Collection<D> remove = null;
	private List<M> insert = new ArrayList<>();
	private List<D> updateD = new ArrayList<>();
	private List<M> updateM = new ArrayList<>();
	/**
	 * If addOnly = true, existing elements in the collection will not be removed if their is no counterpart in the model's collection
	 */
	public CollectionMerger(List<D> ds, List<M> ms, boolean addOnly){
		Map<Long, D> dd=new HashMap<>();
		for(D d:ds)
			dd.put(d.getId(), d);

		if(ms != null){
			for(M m: ms){
				if(m.getId() == null)
					insert.add(m);
				else {
					D d = dd.get(m.getId());
					if(d == null)
						throw new IllegalStateException("Cannot find Object with id=%d from input collection");
					updateD.add(d);
					updateM.add(m);
					dd.remove(m.getId());
				}
			}
		}
		if(addOnly)
			remove = new ArrayList<>();
		else
			remove=dd.values();
	}

	public Collection<D> getRemove() {
		return remove;
	}

	public List<M> getInsert() {
		return insert;
	}

	public List<D> getUpdateD() {
		return updateD;
	}

	public List<M> getUpdateM() {
		return updateM;
	}
}
