package org.sciserver.racm.storem.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;

public class UpdatedUserVolumeInfo {
    private final Optional<String> name;
    private final Optional<String> description;

    @JsonCreator
    public UpdatedUserVolumeInfo(@JsonProperty("name") Optional<String> name,
            @JsonProperty("description") Optional<String> description) {
        super();
        this.name = name;
        this.description = description;
    }

    public Optional<String> getName() {
        return name;
    }

    public Optional<String> getDescription() {
        return description;
    }
}
