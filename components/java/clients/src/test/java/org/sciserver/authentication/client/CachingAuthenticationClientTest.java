package org.sciserver.authentication.client;

import java.io.IOException;
import static org.testng.Assert.*;
import org.testng.annotations.*;

import org.sciserver.clientutils.SciServerClientException;


public class CachingAuthenticationClientTest extends AuthenticationTestBase {

    private CachingAuthenticationClient authClient;

    @BeforeClass
    public void setUpClient() {
        authClient = new CachingAuthenticationClient(mockServer.url("/").toString(), 10, 500);
    }

    @Test
    public void getAuthenticatedUserTest() throws IOException, SciServerClientException, UnauthenticatedException {
        setupResponseFromFile("src/test/data/good_auth_user_response.json");
        AuthenticatedUser user = authClient.getAuthenticatedUser("a-valid-token");
        assertEquals(user.getUserName(), "username");
        assertEquals(user.getUserId(), "4053123153531241231231232e9e3a01");
        assertEquals(user.getEmail(), "username@example.com");
        assertEquals(user.getToken(), "a-valid-token");
    }

    @Test(expectedExceptions = UnauthenticatedException.class)
    public void getAuthentication500ExceptsUnauthenticated() throws IOException, SciServerClientException, UnauthenticatedException {
        setupResponseFromString("", 500);
        authClient.getAuthenticatedUser("an-invalid-token");
    }

    @Test(expectedExceptions = SciServerClientException.class)
    public void getAuthenticationNon500ExceptsGeneric() throws IOException, SciServerClientException, UnauthenticatedException {
        setupResponseFromString("", 404);
        authClient.getAuthenticatedUser("a-possibly-valid-token");
    }

    @Test
    public void getAuthenticationFromCacheEntry() throws IOException, SciServerClientException, UnauthenticatedException {
        setupResponseFromFile("src/test/data/good_auth_user_response.json");
        authClient.getAuthenticatedUser("a-new-valid-token");
        // same token, so we will read from cache
        authClient.getAuthenticatedUser("a-new-valid-token");
    }

    @Test(expectedExceptions = SciServerClientException.class)
    public void getAuthenticationNoCacheMakesRequest() throws IOException, SciServerClientException, UnauthenticatedException {
        setupResponseFromFile("src/test/data/good_auth_user_response.json");
        // this request eats the above http response
        authClient.getAuthenticatedUser("a-new-new-valid-token");
        setupResponseFromString("", 404);
        authClient.getAuthenticatedUser("a-new-valid-token-no-response");
    }

    @Test
    public void getAuthenticationOrNullReturnsNullOnInvalidToken() throws IOException, SciServerClientException, UnauthenticatedException {
        setupResponseFromString("", 500);
        AuthenticatedUser user = authClient.getAuthenticatedUserOrNull("a-new-invalid-token");
        assertNull(user);
    }

    @Test
    public void getAuthenticationOrNullReturnsNullOnBadResponse() throws IOException, SciServerClientException, UnauthenticatedException {
        setupResponseFromString("", 404);
        AuthenticatedUser user = authClient.getAuthenticatedUserOrNull("a-new-new-invalid-token");
        assertNull(user);
    }

    @Test
    public void noCacheOnNonUnauthResponse() throws IOException, SciServerClientException, UnauthenticatedException {
        String token = "a-new-new-valid-token-2";
        setupResponseFromString("", 404);
        assertNull(authClient.getAuthenticatedUserOrNull(token));
        setupResponseFromFile("src/test/data/good_auth_user_response.json");
        assertNotNull(authClient.getAuthenticatedUserOrNull(token));
    }

    @Test
    public void doCacheUnauthResponse() throws IOException, SciServerClientException, UnauthenticatedException {
        String token = "a-new-new-invalid-token-2";
        setupResponseFromString("", 500);
        assertNull(authClient.getAuthenticatedUserOrNull(token));
        setupResponseFromFile("src/test/data/good_auth_user_response.json");
        assertNull(authClient.getAuthenticatedUserOrNull(token));
        // the previous request should not use that response. Now we
        // can pop it off. If previous did read, this would timeout
        // and except
        authClient.getAuthenticatedUserOrNull("a-completely-different-token");
    }

    @Test
    public void cacheEntryExpiresOnTime() throws IOException, SciServerClientException, UnauthenticatedException, InterruptedException {
        String token = "a-new-new-valid-token-3";
        setupResponseFromFile("src/test/data/good_auth_user_response.json");
        assertNotNull(authClient.getAuthenticatedUserOrNull(token));
        Thread.sleep(600);
        setupResponseFromString("", 500);
        assertNull(authClient.getAuthenticatedUserOrNull(token));
    }

}
