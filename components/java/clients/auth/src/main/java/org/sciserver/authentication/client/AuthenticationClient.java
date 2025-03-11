package org.sciserver.authentication.client;

import java.util.List;
import org.sciserver.clientutils.Client;
import org.sciserver.clientutils.SciServerClientException;
import retrofit2.Call;

public class AuthenticationClient extends Client<AuthenticationClientInterface> {

    public AuthenticationClient(String authEndpoint) {
        super(authEndpoint, AuthenticationClientInterface.class);
    }

    public AuthenticatedUser getAuthenticatedUser(String userToken) throws SciServerClientException,
            UnauthenticatedException {
        if (userToken == null || userToken.equals("")) {
            throw new UnauthenticatedException();
        }

        Call<AuthenticatedUser> call = retrofitAdapter.getAuthenticatedUser(userToken);
        try {
            AuthenticatedUser user = getSyncResponse(call);
            user.setToken(userToken);
            return user;
        } catch (SciServerClientException e) {
            if (e.httpCode() == 500 || e.httpCode() == 401) {
                throw new UnauthenticatedException();
            }
            throw e;
        }
    }

    public User getUserById(String userid) throws SciServerClientException {
        Call<User> call = retrofitAdapter.getUserById(userid);
        return getSyncResponse(call);
    }

    public User getUserByName(String username) throws SciServerClientException {
        Call<List<User>> call = retrofitAdapter.getUserByName(username);
        // for reasons unknown to me, the by-name endpoint returns a list - while we still have the expectation of
        // unique user names, in this client we return just user.
        List<User> userList = getSyncResponse(call);
        if (userList == null || userList.isEmpty()) {
            throw new SciServerClientException("Unable to retrieve user '" + username + "'!", 400);
        }
        return userList.get(0);
    }

}
