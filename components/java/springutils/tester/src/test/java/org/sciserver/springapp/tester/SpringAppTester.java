package org.sciserver.springapp.tester;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.testng.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import org.sciserver.authentication.client.AuthenticatedUser;
import org.sciserver.authentication.client.CachingAuthenticationClient;
import org.sciserver.authentication.client.UnauthenticatedException;
import org.sciserver.clientutils.SciServerClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.web.servlet.MockMvc;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import sciserver.logging.Logger;


/**
 tester class.
*/
@SpringBootTest
@AutoConfigureMockMvc
public class SpringAppTester extends AbstractTestNGSpringContextTests {

    private String faketoken = "faketoken";
    private AuthenticatedUser mockuser = mock(AuthenticatedUser.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Logger logger;
    private String logfile = "build/tmp/test/tester.log";

    @MockBean @Autowired
    private CachingAuthenticationClient authClient;

    @BeforeClass
    private void setUp() throws Exception {
        logger.enableConsoleOutput();
        logger.enableFileOutput(logfile);
    }

    @AfterMethod
    public void resetMocks() {
        reset(authClient);
    }

    @BeforeMethod
    public void setupMocks() throws SciServerClientException, UnauthenticatedException {
        when(authClient.getAuthenticatedUser("invalid")).thenThrow(UnauthenticatedException.class);
        when(authClient.getAuthenticatedUser(faketoken)).thenReturn(mockuser);
    }

    @Test
    private void bootstrapTest() {
    }

    @Test
    private void testAuthenticationGoodUser() throws Exception {
        mockMvc.perform(get("/testauth").header("X-Auth-Token", faketoken))
            .andExpect(status().isOk());
    }

    @Test
    private void testAuthenticationBadUser() throws Exception {
        mockMvc.perform(get("/testauth").header("X-Auth-Token", "invalid"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    private void testLogLine() throws Exception {
        mockMvc.perform(get("/testlogline"))
            .andExpect(status().isOk());
        assertTrue(
            (new BufferedReader(new FileReader(logfile)))
            .lines()
            .filter(l -> l.contains("SERVICELOG"))
            .anyMatch(l -> l.contains("org.sciserver.springapp.tester.TesterApp.testLogLine")));
    }


}
