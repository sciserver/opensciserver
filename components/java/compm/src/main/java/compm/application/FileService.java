package compm.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import org.sciserver.racm.storem.model.MinimalFileServiceModel;
import org.springframework.web.util.UriUtils;

/**
 * The `FileService` class contains methods for interacting with the SciServer filesystem by means of
 * HTTP requests to the SciServer FileService REST API.
 */
public class FileService {

    HttpRequest request;

    public FileService() {
        request = new HttpRequest("NOT_NEEDED");
    }

    /**
     * Uploads a file into a specific path in the SciServer Filesystem.
     *
     * @param fileServiceRelativeUrl path in the SciServer filesystem where the file is uploadted into. 
     *     Starts with a rootVolume or dataVolume.
     * @param token the user's SciServer auth token.
     * @param fileContent string content of the file.
     */
    public void uploadContentToFile(String fileServiceRelativeUrl, String token, String fileContent) throws Exception {
        
        Hashtable<String, String> extraHeaderFields = new Hashtable<String, String>();
        extraHeaderFields.put("X-Auth-Token", token);
        extraHeaderFields.put("Content-Type", "text/plain; charset=UTF-8");

        List<String> urlComponents = Arrays.asList(fileServiceRelativeUrl.split("/"));
        for (int i = 0; i < urlComponents.size(); i++) {
            urlComponents.set(i, UriUtils.encodePathSegment(urlComponents.get(i), "UTF-8"));
        }

        String url = Config.getInstance().getFileServiceUrl() 
                   + "api/file/" + String.join("/", urlComponents) 
                   + "?doShowInUserHistory=false&TaskName=ComputeManager.UploadFile&quiet=true"; 
        HttpResponseResult result = request.executePut(url, extraHeaderFields, fileContent);
        if (result.responseCode < 200 || result.responseCode > 299) { //error
            String fileName = "'" + Config.getInstance().getFileServiceBasePath() + fileServiceRelativeUrl + "'";  
            throw new Exception(
                "Could not upload file " + fileName + " into file system.\n Request URL: " 
                + result.requestUrl.toString() + "\n HttpResponse code: " + result.responseCode + ".\n" + result.message
            );
        }
    }

    /**
     * Creates a user volume in the SciServer filesystem.
     *
     * @param fileServiceRelativeUrl relative URL for this file service endpoint, in the form 
     *     rootVolume/ownerName/userVolumeName
     * @param token the user's SciServer auth token.
     * @param fileContent request body content.
     */
    public void createUserVolume(String fileServiceRelativeUrl, String token, String fileContent) throws Exception {
        
        Hashtable<String, String> extraHeaderFields = new Hashtable<String, String>();
        extraHeaderFields.put("X-Auth-Token", token);
        List<String> urlComponents = Arrays.asList(fileServiceRelativeUrl.split("/"));
        for (int i = 0; i < urlComponents.size(); i++) {
            urlComponents.set(i, UriUtils.encodePathSegment(urlComponents.get(i), "UTF-8"));
        }
        String url = Config.getInstance().getFileServiceUrl() + "api/volume/" + String.join("/", urlComponents) 
                   + "?quiet=true&TaskName=ComputeManager.CreateUserVolume&doShowInUserHistory=false";
        HttpResponseResult result = request.executePut(url, extraHeaderFields, fileContent);
        if (result.responseCode < 200 || result.responseCode > 299) { //error
            throw new Exception(
                "Could not create user volume '" + fileServiceRelativeUrl + "'. Request URL: " 
                + result.requestUrl.toString() + "\n HttpResponse code: " + result.responseCode + ".\n" + result.message
            );
        }
    }

    /**
     * Moves a file from one path in the SciServer filesystem to another.
     *
     * @param fileServiceRelativeUrl relative URL for this file service endpoint, in the form 
     *     rootVolume/ownerName/userVolumeName
     * @param token the user's SciServer auth token.
     * @param destinationRootFolder The rootVolume where the file will be moved under.
     * @param destinationOwner The owner of the destinationUserVolume,
     * @param destinationUserVolume The userVolume where the file will be moved under.
     * @param destinationPath The relative path under where the file will be moved to.
     */
    public void moveFile(String fileServiceRelativeUrl, String token, String destinationRootFolder, 
                         String destinationOwner, String destinationUserVolume, String destinationPath
    ) throws Exception {
        
        ObjectMapper mapper = new ObjectMapper(); 
        ObjectNode node = mapper.createObjectNode();
        node.put("destinationRootVolume", destinationRootFolder);
        node.put("destinationOwnerName", destinationOwner);
        node.put("destinationUserVolume", destinationUserVolume);
        node.put("destinationPath", destinationPath);
        String jsonString = node.toString();
        
        Hashtable<String, String> extraHeaderFields = new Hashtable<String, String>();
        extraHeaderFields.put("X-Auth-Token", token);

        List<String> urlComponents = Arrays.asList(fileServiceRelativeUrl.split("/"));
        for (int i = 0; i < urlComponents.size(); i++) {
            urlComponents.set(i, UriUtils.encodePathSegment(urlComponents.get(i), "UTF-8"));
        }
        
        String url = Config.getInstance().getFileServiceUrl() + "api/data/" + String.join("/", urlComponents) 
                   + "?replaceExisting=true&doCopy=true&doShowInUserHistory=false" 
                   + "&TaskName=ComputeManager.MoveFile&quiet=true";
        HttpResponseResult result = request.executePut(url, extraHeaderFields, jsonString);
        if (result.responseCode < 200 || result.responseCode > 299) { //error
            String source = Config.getInstance().getFileServiceBasePath() + fileServiceRelativeUrl; 
            String destination = Config.getInstance().getFileServiceBasePath() + destinationRootFolder 
                               + "/" + destinationOwner + "/" + destinationUserVolume + "/" + destinationPath;
            throw new Exception(
                "Could not move '" + source + "' to '" + destination + "'. Request URL: " 
                + result.requestUrl.toString() + "\n HttpResponse code: " + result.responseCode + ".\n" + result.message
            );
        }
    }

    /**
     * Gets the list of volumes visible to a user in the SciServer filesystem.
     *
     * @param token the user's SciServer auth token.
     * @return A JsonNode object with a the volumes and associated metadata.
     */
    public JsonNode getVolumes(String token) throws Exception {
        
        Hashtable<String, String> extraHeaderFields = new Hashtable<String, String>();
        extraHeaderFields.put("X-Auth-Token", token);
        String url = Config.getInstance().getFileServiceUrl() + "api/volumes";
        HttpResponseResult result = request.executeGet(url, extraHeaderFields);
        if (result.responseCode < 200 || result.responseCode > 299) { //error
            throw new Exception(
                "Could not get user volumes. HttpResponse code:" + result.responseCode + ".\n" + result.message
            );
        }
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(result.message);
    }

    /**
     * Gets the URL of a FileService's REST API, based on the FileService UUID.
     *
     * @param fileServiceUUID FileService UUID, as registered in STOREM (RACM).
     * @param token valid SciServer auth token of the user.
     * @return url as a string.
     */    
    public String getFileServiceUrl(String fileServiceUUID, String token) throws Exception {
        Hashtable<String, String> extraHeaderFields = new Hashtable<String, String>();
        extraHeaderFields.put("X-Auth-Token", token);
        HttpResponseResult result = new HttpRequest("").executeGet(Config.getInstance().getStoremUrl() 
                                                                          + "fileservices", extraHeaderFields);
        if (result.responseCode < 200 || result.responseCode > 299) {
            throw new Exception("Could not get filervices from storem. HttpResponse code:" 
                                + result.responseCode + ".\n" + result.message);
        }
        
        MinimalFileServiceModel[] fileServices = new ObjectMapper().readValue(result.message, 
                                                                    MinimalFileServiceModel[].class);
        for (MinimalFileServiceModel fileService : fileServices) {
            if (fileService.getIdentifier().equals(fileServiceUUID)) {
                return fileService.getApiEndpoint();
            }
        }
        throw new Exception("There is no available fileservice with uuid = " + fileServiceUUID);
    }

    /**
     * Extracts and returns the FileService UUID, as part of the results folder URI,  
     * which points to a location in the Sciserver File system.
     *
     * @param resultsFolderUri results folder URI.
     * @return the FileService UUID
     */
    public String getFileServiceUUID(String resultsFolderUri) throws Exception {
        //for now
        return resultsFolderUri.split(":")[0];
    }

    /**
     * Gets the relative path pointing to the results folder in the SciServer file system, 
     * given the location provided by the user in the job definition.
     *
     * @param resultsFolderUri The `resultsFolderUri` (or location) provided by the user in the job definition).
     * 
     * @return the relative path pointing to the results folder in the SciServer file system.
     */
    public String getResultsFolderRelativePath(String resultsFolderUri) throws Exception {
        //for now
        String uuid = getFileServiceUUID(resultsFolderUri);
        String path = resultsFolderUri.replace(uuid + ":", "");
        if (!path.equals("")) {
            path = "/" + path.replaceAll("^/+", "").replaceAll("/+$", "") + "/";
            // remove /home/idies/workspace/
            if (path.startsWith(Config.getInstance().getFileServiceBasePath())) {
                path = path.replaceFirst(Config.getInstance().getFileServiceBasePath(), "");
            }
        }
        return path.replaceFirst("^/+", "");
    }
}