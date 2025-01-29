package org.sciserver.racm.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A set of actions on a volume along with the path on filesystem under which it exists.
 */
public class VolumeActions {
    private String relativePath;
    private List<String> allowedActions;

    public VolumeActions(String relativePath, List<String> allowedActions) {
        this.relativePath = relativePath;
        this.allowedActions = allowedActions;
    }

    /**
     * Construct a volume actions object from a JSON structure representing a query.
     *
     * @param json the query result structure, must contain an list field called "rows" which itself is a list of
     *     [{action}, {relativePath}] pairs.
     */
    public VolumeActions(JsonNode json) {
        List<String> allowedActions = new ArrayList<String>();
        String relativePath = null;
        for (int i = 0; i < json.get("rows").size(); i++) {
            allowedActions.add(json.get("rows").get(i).get(0).asText());
            relativePath = json.get("rows").get(i).get(1).asText();
        }
        this.relativePath = relativePath;
        this.allowedActions = allowedActions;
    }

    /**
     * Check if the uservolume exists, that is if it has a path.
     *
     * @return true if exsits, false otherwise
     */
    public boolean exists() {
        return this.relativePath != null;
    }

    /**
     * Return true/false if all the specified actions are allowed on the volume.
     *
     * @param actions a list of action strings
     * @return true if all actions are allowed, false otherwise
     */
    public boolean hasActions(List<String> actions) {
        return allowedActions.containsAll(actions);
    }

    /**
     * Return true/false if all the specified actions are allowed on the volume.
     *
     * @param actions a number of actions as strings
     * @return true if all actions are allowed, false otherwise
     */
    public boolean hasActions(String ...actions) {
        return hasActions(Arrays.asList(actions));
    }

    /**
     * Throw an exception if not all specified actions are allowed on the volume.
     *
     * @param actions a list of action strings
     * @return itself, can be called as e.g. volume.requireActions("read").getRelativePath()
     *
     * @throws Exception if not exists or if not all actions are alowed
     */
    public VolumeActions requireActions(List<String> actions) throws Exception {
        if (!exists()) {
            throw new Exception("Volume does not exist");
        }
        if (!hasActions(actions)) {
            throw new Exception("Missing required permissions on volume");
        }
        return this;
    }

    /**
     * Throw an exception if not all specified actions are allowed on the volume.
     *
     * @param actions a number of actions as strings
     * @return itself, can be called as e.g. volume.requireActions("read").getRelativePath()
     *
     * @throws Exception if not exists or all actions are alowed
     */
    public VolumeActions requireActions(String ...actions) throws Exception {
        return requireActions(Arrays.asList(actions));
    }

    /**
     * relativePath getter.
     *
     * @return the relativePath
     */
    public String getRelativePath() {
        return relativePath;
    }

    /**
     * relativePath setter.
     *
     * @param relativePath the relativePath to set
     */
    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    /**
     * allowedActions getter.
     *
     * @return the allowedActions
     */
    public List<String> getAllowedActions() {
        return allowedActions;
    }

    /**
     * allowedActions setter.
     *
     * @param allowedActions the allowedActions to set
     */
    public void setAllowedActions(List<String> allowedActions) {
        this.allowedActions = allowedActions;
    }


}
