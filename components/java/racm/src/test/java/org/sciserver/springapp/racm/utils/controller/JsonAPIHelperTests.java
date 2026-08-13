package org.sciserver.springapp.racm.utils.controller;

import static org.junit.Assert.assertEquals;

import org.ivoa.dm.VOURPException;
import org.junit.Test;
import org.sciserver.springapp.racm.login.InsufficientPermissionsException;
import org.sciserver.springapp.racm.utils.RACMException;
import org.springframework.http.HttpStatus;

/**
 * Pins the exception-to-HTTP-status mapping. This mapping has silently drifted once
 * already: VOURPException.UNAUTHORIZED used to fall through to 500.
 */
public class JsonAPIHelperTests {

    @Test
    public void illegalArgumentMapsToBadRequest() {
        assertEquals(HttpStatus.BAD_REQUEST,
                JsonAPIHelper.statusFor(new VOURPException(VOURPException.ILLEGAL_ARGUMENT, "bad input")));
    }

    @Test
    public void unauthorizedMapsToForbidden() {
        assertEquals(HttpStatus.FORBIDDEN,
                JsonAPIHelper.statusFor(new VOURPException(VOURPException.UNAUTHORIZED, "not allowed")));
    }

    @Test
    public void illegalStateMapsToInternalServerError() {
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                JsonAPIHelper.statusFor(new VOURPException(VOURPException.ILLEGAL_STATE, "invariant violated")));
    }

    @Test
    public void racmExceptionMapsToUnauthorized() {
        assertEquals(HttpStatus.UNAUTHORIZED,
                JsonAPIHelper.statusFor(new RACMException("someuser", "some-resource-uuid", "someAction")));
    }

    @Test
    public void insufficientPermissionsMapsToForbidden() {
        assertEquals(HttpStatus.FORBIDDEN,
                JsonAPIHelper.statusFor(new InsufficientPermissionsException("do something")));
    }

    @Test
    public void unrecognisedExceptionMapsToInternalServerError() {
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                JsonAPIHelper.statusFor(new IllegalStateException("boom")));
    }
}
