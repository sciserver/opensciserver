package org.sciserver.casjobs.client;

import org.sciserver.authentication.client.AuthenticationClient;
import org.sciserver.authentication.client.UnauthenticatedException;
import org.sciserver.clientutils.Client;
import org.sciserver.clientutils.SciServerClientException;

import com.fasterxml.jackson.databind.JsonNode;

import retrofit2.Call;

public class CasJobsClient extends Client<CasJobsClientInterface> {

    private AuthenticationClient auth;

    public CasJobsClient(String endpoint) {
        super(endpoint, CasJobsClientInterface.class);
    }

    private JsonNode getCasJobsUserid(String id, String usertoken) throws SciServerClientException, UnauthenticatedException {
        Call<JsonNode> call = retrofitAdapter.getCasJobsUserInfo(id, usertoken);
        JsonNode js= getSyncResponse(call);
        return js;
    }

    public String getMyDB(String id, String usertoken) throws SciServerClientException, UnauthenticatedException {
        JsonNode js = getCasJobsUserid(id, usertoken);
        return js == null?null:js.get("MyDBName").textValue();
    }
}
