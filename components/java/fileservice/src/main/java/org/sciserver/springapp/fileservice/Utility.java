/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under a modified Apache 2.0 license.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.springapp.fileservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.sciserver.authentication.client.User;
import org.sciserver.springapp.loginterceptor.Log;
import sciserver.logging.Message;

public class Utility {


    public static void ensureSuccessStatusCode(CloseableHttpResponse res) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        int code = res.getStatusLine().getStatusCode();
        if (code < 200 || code >= 300) {
            throw new Exception(
                res.getStatusLine().toString() + " " + mapper.readTree(res.getEntity().getContent()).toString()
            );
        }
    }


    public static void fillLoggingMessage(Message message, User user, HttpServletRequest request, String taskName) {

        //setting user info
        try {
            //getting client IP address
            String clientIP;
            if (request.getHeader("X-FORWARDED-FOR") != null) {
                clientIP = request.getHeader("X-FORWARDED-FOR");
            } else if (request.getHeader("X-Forwarded-For") != null){
                clientIP = request.getHeader("X-Forwarded-For");
            } else if (request.getHeader("HTTP_CLIENT_IP") != null){
                clientIP = request.getHeader("HTTP_CLIENT_IP");
            } else {
                clientIP = request.getRemoteAddr();
            }
            message.ClientIP = clientIP.split(",")[0];

            //setting TaskName
            message.TaskName = request.getParameter("TaskName") != null ? request.getParameter("TaskName") : taskName;

            //user info
            message.UserToken = request.getHeader("X-Auth-Token");
            message.UserId = user.getUserId();
            message.UserName = user.getUserName();

            int numLevelsUp = 3;
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            Log.getLogger().setMethod(
                stackTraceElements[numLevelsUp].getClassName() + "." + stackTraceElements[numLevelsUp].getMethodName()
            );

        } catch (Exception ignored) { }
    }

    public static String getRandomUUID() throws Exception {
        return UUID.randomUUID().toString().replaceAll("\\-", "");
    }
}
