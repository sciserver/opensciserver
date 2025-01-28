package org.sciserver.authentication.client;

import java.io.IOException;
import static org.testng.Assert.*;
import org.testng.annotations.*;

import org.sciserver.clientutils.SciServerClientException;


public class AuthenticationClientTest extends AuthenticationTestBase {

    private AuthenticationClient authClient;

    @BeforeClass
    public void setUpClient() {
        authClient = new AuthenticationClient(mockServer.url("/").toString());
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
        authClient.getAuthenticatedUser("a-valid-token");
    }

    @Test(expectedExceptions = SciServerClientException.class)
    public void getAuthenticationNon500ExceptsGeneric() throws IOException, SciServerClientException, UnauthenticatedException {
        setupResponseFromString("", 404);
        authClient.getAuthenticatedUser("a-valid-token");
    }

    @Test(expectedExceptions = UnauthenticatedException.class)
    public void tokenEmptyStringReturnsUnauthorized() throws SciServerClientException, UnauthenticatedException {
        clearRequest = false; // empty string is short circuited before request
        authClient.getAuthenticatedUser("");
    }

    @Test(expectedExceptions = UnauthenticatedException.class)
    public void tokenNullReturnsUnauthorized() throws SciServerClientException, UnauthenticatedException {
        clearRequest = false; // null is short circuited before request
        authClient.getAuthenticatedUser(null);
    }

    @Test
    public void getUserByName() throws IOException, SciServerClientException {
        setupResponseFromFile("src/test/data/userInArray.json");
        User user = authClient.getUserByName("username");
        assertEquals(user.getUserName(), "username");
        assertEquals(user.getUserId(), "4053123153531241231231232e9e3a01");
    }

    @Test
    public void getUserById() throws IOException, SciServerClientException {
        setupResponseFromFile("src/test/data/user.json");
        User user = authClient.getUserById("userid");
        assertEquals(user.getUserName(), "username");
        assertEquals(user.getUserId(), "4053123153531241231231232e9e3a01");
    }

}
