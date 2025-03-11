package org.sciserver.springapp.auth;

import org.sciserver.authentication.client.CachingAuthenticationClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
Configuration for authentication client. expiryms is the cache expiration in milliseconds and size is the maximum number
of entries.
 */
@Component
public class AuthConfig {

    public static String tokenHeader = "X-Auth-Token";
    public static String reqAttrBase = "sciserver.auth.";
    public static String tokenCookie = "portalCookie";

    @Bean
    public CachingAuthenticationClient cachingAuthClient(
        @Value("${auth.portal.url:https://apps.sciserver.org/login-portal/}") String portalUrl,
        @Value("${auth.cache.expiryms:60000}") String cacheTimeout,
        @Value("${auth.cache.size:1024}") String cacheSize
    ) {
        return new CachingAuthenticationClient(portalUrl, Integer.valueOf(cacheSize), Integer.valueOf(cacheTimeout));
    }

}
