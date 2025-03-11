package org.sciserver.springapp.racm.ugm.domain;

import java.util.List;

import org.ivoa.dm.model.MetadataObject;

import edu.jhu.file.DataVolume;
import edu.jhu.file.UserVolume;
import edu.jhu.job.DatabaseContext;
import edu.jhu.job.DockerImage;
import edu.jhu.job.VolumeContainer;
import edu.jhu.rac.Resource;

public class ActionsOnResource {
	private final long entityId;
	private final List<String> actions;
	private final TYPE type;

	public ActionsOnResource(long entityId, List<String> actions, TYPE type) {
		super();
		this.entityId = entityId;
		this.actions = actions;
		this.type = type;
	}

	public long getEntityId() {
		return entityId;
	}

	public List<String> getActions() {
		return actions;
	}

	public TYPE getType() {
		return type;
	}

	public enum TYPE {
		USERVOLUME(UserVolume.class),
		VOLUMECONTAINER(VolumeContainer.class),
		DATAVOLUME(DataVolume.class),
		DATABASE(DatabaseContext.class),
		DOCKERIMAGE(DockerImage.class),
		RESOURCE(Resource.class);

		private final Class<? extends MetadataObject> clazz;
		TYPE(Class<? extends MetadataObject> clazz) {
			this.clazz = clazz;
		}
		public Class<? extends MetadataObject> getVOURPClass() {
			return clazz;
		}
	}
}
