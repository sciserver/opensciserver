package org.sciserver.testservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sciserver.clientutils.SciServerClientException;
import org.sciserver.racm.client.RACMClient;
import org.sciserver.racm.ugm.model.PersonalUserInfo;
import org.springframework.web.server.ResponseStatusException;

/** Authorization checks independent of runner orchestration. */
class ServiceTest {
    private final RACMClient client = mock(RACMClient.class);
    private final AdminAccess access = new AdminAccess(client);

    @Test
    void racmErrorsPreserveDenialAndHideUpstreamDetails() throws Exception {
        for (int code : new int[] {401, 403, 302, 500, 502}) {
            doThrow(new SciServerClientException("sensitive upstream detail", code))
                    .when(client).getGroupsUserIsMemberOf("token");
            ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> access.require("token"));
            assertEquals(code == 401 || code == 403 ? code : 503, error.getStatus().value());
            assertFalse(error.getReason().contains("sensitive"));
        }
        doThrow(new IllegalArgumentException("malformed payload")).when(client).getGroupsUserIsMemberOf("token");
        assertEquals(503, assertThrows(ResponseStatusException.class,
                () -> access.require("token")).getStatus().value());
    }

    @Test
    void validAdminGetsTrustedIdentity() throws Exception {
        when(client.getGroupsUserIsMemberOf("token")).thenReturn(List.of("public", "admin"));
        when(client.getUser("token")).thenReturn(new PersonalUserInfo(
                1L, "administrator", null, null, null, null, "PUBLIC"));
        assertEquals("administrator", access.require("token"));
    }

    @Test
    void ordinaryMembershipCannotAuthorizeAndMissingIdentityFailsClosed() throws Exception {
        when(client.getGroupsUserIsMemberOf("token")).thenReturn(List.of("public", "administrator"));
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> access.require("token")).getStatus().value());
        verify(client, never()).getUser(any());
        when(client.getGroupsUserIsMemberOf("token")).thenReturn(List.of("admin"));
        assertEquals(503, assertThrows(ResponseStatusException.class,
                () -> access.require("token")).getStatus().value());
    }

}
