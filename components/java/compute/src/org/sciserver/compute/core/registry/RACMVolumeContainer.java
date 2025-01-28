package org.sciserver.compute.core.registry;

import java.sql.SQLException;

public class RACMVolumeContainer extends VolumeContainer {

	public RACMVolumeContainer(Registry registry) {
		super(registry);
	}

	@Override
	public void register() throws Exception {
		registry.registerRACMVolumeContainer(this);
	}
	
	@Override
	public void unregister() throws SQLException {
		registry.unregisterRACMVolumeContainer(this);
	}

	@Override
	public void update() throws SQLException {
		registry.updateRACMVolumeContainer(this);
	}
}
