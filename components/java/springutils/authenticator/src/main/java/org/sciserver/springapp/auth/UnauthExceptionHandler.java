package org.sciserver.springapp.auth;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import org.sciserver.authentication.client.UnauthenticatedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;


/**
 * Translates unathenticated exception to appropriate http code.
 */
@ControllerAdvice
public class UnauthExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(UnauthenticatedException.class)
    public void springHandleNotAuthorized(HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.UNAUTHORIZED.value());
    }

}
