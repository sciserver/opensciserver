package org.sciserver.sciquery.metadata.model;

public class ConnectionAuthData {
    private String url;
    private String username;
    private String password;
    
    public ConnectionAuthData(String url, String user, String password) {
        this.url=url;
        this.username=user;
        this.password=password;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
    
}
