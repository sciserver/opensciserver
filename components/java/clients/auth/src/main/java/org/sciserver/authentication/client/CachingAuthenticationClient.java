package org.sciserver.authentication.client;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.sciserver.clientutils.SciServerClientException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.CacheLoader;


public class CachingAuthenticationClient extends AuthenticationClient {

    private LoadingCache<String, CacheEntry> authCache;

    public CachingAuthenticationClient(String authEndpoint, int size, int timeout) {
        super(authEndpoint);
        initialize(authEndpoint, size, timeout);
    }

    public CachingAuthenticationClient(String authEndpoint) {
        super(authEndpoint);
        initialize(authEndpoint, 1024, 60000);
    }

    private void initialize(String authEndpoint, int size, int timeout) {
        authCache = CacheBuilder.newBuilder().
            maximumSize(size).
            expireAfterWrite(timeout, TimeUnit.MILLISECONDS).
            build(new CacheLoader<String, CacheEntry>() {
                    public CacheEntry load(String key) throws SciServerClientException {
                        return loadCache(key);
                    }
                }
            );
    }

    private class CacheEntry {
        protected boolean authenticated = false;
        protected AuthenticatedUser authenticatedUser = null;
    }

    private CacheEntry loadCache(String token) throws SciServerClientException {
        CacheEntry entry = new CacheEntry();
        try {
            entry.authenticatedUser = super.getAuthenticatedUser(token);
            entry.authenticated = true;
        } catch (UnauthenticatedException e) {
        }
        return entry;
    }

    public AuthenticatedUser getAuthenticatedUser(String token) throws UnauthenticatedException, SciServerClientException {
        if (token == null) {
            throw new UnauthenticatedException("no token provided!");
        }
        try {
            CacheEntry entry = authCache.get(token);
            if (!entry.authenticated)
                throw new UnauthenticatedException("user not authenticated!");
            return entry.authenticatedUser;
        } catch (ExecutionException e) {
            throw new SciServerClientException("bad call to client: " + e, 500);
        }
    }

    public AuthenticatedUser getAuthenticatedUserOrNull(String token) {
        try {
            CacheEntry entry = authCache.get(token);
            return entry.authenticatedUser;
        } catch (Exception e) {
        }
        return null;
    }

}
