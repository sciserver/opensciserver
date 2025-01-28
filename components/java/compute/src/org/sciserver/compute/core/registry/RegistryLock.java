package org.sciserver.compute.core.registry;

public class RegistryLock implements AutoCloseable {

	private Registry registry;
	
	@Override
	public void close() throws Exception {
		registry.releaseLock();
	}

	public RegistryLock(Registry registry) throws Exception {
		this.registry = registry;
		this.registry.acquireLock();
	}
}
