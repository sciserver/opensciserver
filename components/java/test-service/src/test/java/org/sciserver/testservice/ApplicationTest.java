package org.sciserver.testservice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/** Verify actual Boot wiring, configuration binding, probes and authentication boundary. */
@SpringBootTest(properties = {
    "test.namespace=sciserver", "test.image=registry/tests:example",
    "test.base-url=http://example.test/sciserver", "test.racm-url=http://racm/sciserver/racm",
    "test.callback-url=http://test-service/reporting",
    "test.report-signing-key=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE="
})
@AutoConfigureMockMvc
class ApplicationTest {
    @MockBean
    Kubernetes kubernetes;
    @Autowired
    MockMvc mvc;

    @Test
    void probesArePublicButApiRequiresExplicitAuthentication() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/api/runs")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/runs").header("Authorization", "Bearer reporting-token"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/runs").cookie(new javax.servlet.http.Cookie("portalCookie", "token")))
                .andExpect(status().isUnauthorized());
    }
}
