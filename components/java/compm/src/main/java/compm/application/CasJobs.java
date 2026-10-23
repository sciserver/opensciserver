package compm.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Hashtable;


/**
 * Used for communicating with the CasJobs REST API.
 */
public class CasJobs {

    static HttpRequest request = new HttpRequest("NOT_NEEDED");
    static ObjectMapper mapper = new ObjectMapper();

    public CasJobs() {
        request = new HttpRequest("NOT_NEEDED");
    }

    /**
     * Calls the CasJobs REST APi and gets the name of a particular user's MyDB database.
     *
     * @param token a valid SciServer auth token for the user.
     * @return the name of the user's MyDB.
     */
    public static String getMyDB(String token) throws Exception {

        String userId = LoginPortal.getUserId(token);

        String url = Config.getInstance().getCasjobsApiBaseUrl() + "users/" + userId
                                                                 + "?TaskName=ComputeManager.CasJobs.getMyDB";
        Hashtable<String, String> extraHeaderFields = new Hashtable<String, String>();
        extraHeaderFields.put("X-Auth-Token", token);
        extraHeaderFields.put("Content-Type", "application/json");

        HttpResponseResult result = request.executeGet(url, extraHeaderFields);
        if (result.responseCode < 200 || result.responseCode > 299) {
            throw new Exception("Could not get schema name from CasJobs.\n HttpResponse code: "
                               + result.responseCode + ".\n" + result.message);
        } else {
            JsonNode response = mapper.readTree(result.message);
            JsonNode name = response.get("MyDBName");
            return name.textValue();
        }
    }

}
