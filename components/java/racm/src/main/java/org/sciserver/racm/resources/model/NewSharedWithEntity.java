package org.sciserver.racm.resources.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NewSharedWithEntity {
    private final String name;
    private final SciServerEntityType type;
    private List<String> allowedActions;

    public NewSharedWithEntity(@JsonProperty("name") String name, @JsonProperty("type") SciServerEntityType type,
            @JsonProperty("allowedActions") List<String> allowedActions) {
        this(name, type);
        if (allowedActions != null)
            this.allowedActions.addAll(allowedActions);
    }

    public NewSharedWithEntity(String name, SciServerEntityType type) {
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
        this.allowedActions = new ArrayList<String>();
    }

    public String getName() {
        return name;
    }

    public SciServerEntityType getType() {
        return type;
    }

    public List<String> getAllowedActions() {
        return allowedActions;
    }

    public void addAllowedAction(String action) {
        this.allowedActions.add(action);
    }
}
