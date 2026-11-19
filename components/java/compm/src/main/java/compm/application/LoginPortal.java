package compm.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Hashtable;

/**
 * Used for making HTTP requests to the Login Portal REST API.
 */
public class LoginPortal {

    static HttpRequest request = new HttpRequest("NOT_NEEDED");
    static ObjectMapper mapper = new ObjectMapper();

    public LoginPortal() {
        request = new HttpRequest("NOT_NEEDED");
    }

    /**
     * Gets the SciServer user Id.
     *
     * @param token a valid auth token if the user.
     * @return th euser ID as a string.
     */
    public static String getUserId(String token) throws Exception {

        String url = Config.getInstance().getLoginPortalApiBaseUrl() + token
                   + "?TaskName=ComputeManager.LoginPortal.getUser";
        Hashtable<String, String> extraHeaderFields = new Hashtable<String, String>();
        extraHeaderFields.put("X-Auth-Token", token);

        HttpResponseResult result = request.executeGet(url, extraHeaderFields);
        if (result.responseCode < 200 || result.responseCode > 299) {
            throw new Exception("Could not get user information from LoginPortal.\n HttpResponse code: "
                               + result.responseCode + ".\n" + result.message);
        } else {

            JsonNode response = mapper.readTree(result.message);
            JsonNode userId = response.get("token").get("user").get("id");
            return userId.asText();

        }
    }

}
