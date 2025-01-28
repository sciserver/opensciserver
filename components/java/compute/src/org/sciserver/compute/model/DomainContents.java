/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.compute.model;

public class DomainContents {
	private Iterable<SelectionInfo> images;
	private Iterable<SelectionInfo> publicVolumes;
	private Iterable<SelectionInfo> userVolumes;
	
	public Iterable<SelectionInfo> getImages() {
		return images;
	}
	public void setImages(Iterable<SelectionInfo> images) {
		this.images = images;
	}
	public Iterable<SelectionInfo> getPublicVolumes() {
		return publicVolumes;
	}
	public void setPublicVolumes(Iterable<SelectionInfo> publicVolumes) {
		this.publicVolumes = publicVolumes;
	}
	public Iterable<SelectionInfo> getUserVolumes() {
		return userVolumes;
	}
	public void setUserVolumes(Iterable<SelectionInfo> userVolumes) {
		this.userVolumes = userVolumes;
	}
	
}
