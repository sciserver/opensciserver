package org.sciserver.authentication.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.testng.annotations.*;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;


public class AuthenticationTestBase {

    protected MockWebServer mockServer;
    protected boolean clearRequest = true;

    protected void setupResponseFromFile(String filename) throws IOException {
        String data = new String(Files.readAllBytes(Paths.get(filename)));
        setupResponseFromString(data);
    }

    protected void setupResponseFromFile(String filename, int code) throws IOException {
        String data = new String(Files.readAllBytes(Paths.get(filename)));
        setupResponseFromString(data, code);
    }

    protected void setupResponseFromString(String string) {
        mockServer.enqueue(new MockResponse().
                           setResponseCode(200).
                           setBody(string));
    }

    protected void setupResponseFromString(String string, int code) {
        mockServer.enqueue(new MockResponse().
                           setResponseCode(code).
                           setBody(string));
    }

    @BeforeClass
    public void setUpServer() {
        mockServer = new MockWebServer();
    }

    @AfterMethod
    public void popRequest() throws InterruptedException, IOException {
        if (clearRequest)
            mockServer.takeRequest();
        clearRequest = true;
    }

}
