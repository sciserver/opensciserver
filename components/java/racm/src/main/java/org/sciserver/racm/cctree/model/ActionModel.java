package org.sciserver.racm.cctree.model;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sciserver.racm.utils.model.RACMMVCBaseModel;

import edu.jhu.rac.ActionCategory;

public class ActionModel extends RACMMVCBaseModel{
	private long resourceTypeId;
	private String name;
	private String description;
	private String category;
	private List<String> allCategories;
	public ActionModel(){
		init();
	}
	
	public ActionModel(String actionId){
		super(actionId);
		init();
	}
	private void init(){
		this.allCategories = Stream.of(ActionCategory.values())
                .map(Enum::name)
                .collect(Collectors.toList());

	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
 	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public long getResourceTypeId() {
		return this.resourceTypeId;
	}
	public void setResourceTypeId(long resourceTypeId) {
		this.resourceTypeId = resourceTypeId;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public List<String> getAllCategories() {
		return allCategories;
	}

	public void setAllCategories(List<String> allCategories) {
		init();
	}
}
