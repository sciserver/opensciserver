package org.sciserver.springapp.tester;

import org.sciserver.authentication.client.AuthenticatedUser;
import org.sciserver.authentication.client.UnauthenticatedException;
import org.sciserver.clientutils.SciServerClientException;
import org.sciserver.springapp.auth.Auth;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


/**
  tester app.
*/
@RestController
public class TesterApp {

    @GetMapping("/testauth")
    public void testAuth() throws UnauthenticatedException, SciServerClientException {
        Auth.get();
    }

    @GetMapping("/testlogline")
    public void testLogLine() {
    }

}
