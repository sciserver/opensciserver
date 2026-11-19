package org.sciserver.springapp.sciquery.model;

/**
 * Defines JSON views for controlling the serialization of model objects in API responses.
 */
public class JsonModelViews {

    /**
     * View for public API responses, exposing only fields intended for external use.
     */
    public static class Public {
    }

    /**
     * View for internal API responses, exposing all fields including those intended for internal use only.
     */
    public static class Internal extends Public {
    }
}
