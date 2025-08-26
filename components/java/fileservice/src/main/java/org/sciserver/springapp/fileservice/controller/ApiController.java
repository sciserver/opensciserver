/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under a modified Apache 2.0 license.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.springapp.fileservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.Tika;
import org.sciserver.authentication.client.AuthenticatedUser;
import org.sciserver.authentication.client.UnauthenticatedException;
import org.sciserver.authentication.client.User;
import org.sciserver.clientutils.SciServerClientException;
import org.sciserver.racm.client.RACMClient;
import org.sciserver.racm.storem.model.FileServiceModel;
import org.sciserver.racm.storem.model.RegisterNewServiceVolumeModel;
import org.sciserver.racm.storem.model.RegisterNewUserVolumeModel;
import org.sciserver.racm.storem.model.RegisteredDataVolumeModel;
import org.sciserver.racm.storem.model.RegisteredFileServiceModel;
import org.sciserver.racm.storem.model.RegisteredRootVolumeModel;
import org.sciserver.racm.storem.model.RegisteredServiceVolumeModel;
import org.sciserver.racm.storem.model.UpdateSharedWithEntry;
import org.sciserver.racm.storem.model.UpdatedUserVolumeInfo;
import org.sciserver.springapp.auth.Auth;
import org.sciserver.springapp.fileservice.Config;
import org.sciserver.springapp.fileservice.Quartet;
import org.sciserver.springapp.fileservice.Quintet;
import org.sciserver.springapp.fileservice.Triplet;
import org.sciserver.springapp.fileservice.UsageInfoProvider;
import org.sciserver.springapp.fileservice.Utility;
import org.sciserver.springapp.fileservice.VolumesManager;
import org.sciserver.springapp.fileservice.dao.ManagerVolumeDTO;
import org.sciserver.springapp.fileservice.dao.QuotaFromManager;
import org.sciserver.springapp.fileservice.dao.QuotaManagerService;
import org.sciserver.springapp.fileservice.dao.QuotaPerUser;
import org.sciserver.springapp.fileservice.dao.QuotaPerVolume;
import org.sciserver.springapp.fileservice.model.CreateServiceVolumeRequestBody;
import org.sciserver.springapp.fileservice.model.CreateUserVolumeRequestBody;
import org.sciserver.springapp.fileservice.model.DownloadMultipleFilesRequest;
import org.sciserver.springapp.fileservice.model.DownloadMultipleFilesResponse;
import org.sciserver.springapp.fileservice.model.FileDataResponse;
import org.sciserver.springapp.fileservice.model.FileTree;
import org.sciserver.springapp.fileservice.model.MoveDataRequestBody;
import org.sciserver.springapp.fileservice.model.dirlist.DirectoryListing;
import org.sciserver.springapp.fileservice.model.dirlist.DirectoryProperties;
import org.sciserver.springapp.fileservice.model.dirlist.FileProperties;
import org.sciserver.springapp.fileservice.model.dirlist.FolderProperties;
import org.sciserver.springapp.loginterceptor.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriTemplate;
import org.springframework.web.util.UriUtils;
import retrofit2.Response;
import sciserver.logging.Message;
import sciserver.logging.ServiceLogTimer;

/**
 * Rules:
 * - Can write files to persistent/ and scratch/, but not to volumes/.
 * Must create a root-subfolder under volumes/ and can only write to that.
 * - can share root-subfolders of volumes/
 * - reference to a shared folder (a root-subfolder in volumes/) should be
 * '~<username>/<folder-name>'
 *
 *
 *
 *
 * @author gerard, mtaghiza
 *
 */
/**
 * @author mtaghiza
 *
 */
@Tag(name = "SciServer-FileService REST API ", description = "Supports the interaction with the SciServer file system.")
@CrossOrigin
@RestController
public class ApiController {
    private static final Logger LOG = LogManager.getLogger(ApiController.class);
    private final Config config;
    private final ObjectMapper mapper = new ObjectMapper();
    private final VolumesManager volumesManager;

    private static final int MAX_FILE_SIZE = 4000000; // 4MB
    private static final int MAX_FILES = 100;

    @Autowired(required = false)
    private QuotaManagerService quotaManagerService;
    @Autowired
    private RACMClient racmClient;
    @Value("${RACM.resourcecontext.uuid}")
    private String fileServiceIdentifier;

    private final UsageInfoProvider usageInfoProvider;
    private RegisteredFileServiceModel fileService;
    private Tika tika;

    @Autowired
    ApiController(VolumesManager volumesManager, UsageInfoProvider usageInfoProvider, Config config) {
        this.config = config;
        this.volumesManager = volumesManager;
        this.usageInfoProvider = usageInfoProvider;
        this.fileService = config.getFileService();
        this.tika = new Tika();
    }

    @Scheduled(fixedDelay = 60 * 1 * 1000, initialDelay = 10 * 1000)
    private void getFileServiceDefinitionPerisodically() {
        try {
            RegisteredFileServiceModel updatedFileService = config.getFileServiceDefinition(config.getProperties());
            synchronized (this.fileService) {
                this.fileService = updatedFileService;
            }
            logSimpleMessage(String.format("FileService %s Refreshed Succesfully", this.fileService.getApiEndpoint()));
        } catch (Exception e) {
            logException("PeriodicFileServiceRefresh", null, null, null, null, null, null, null, e,
                    HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
    }

    /**
     * Method for chekcing if volume is Root Volume type.
     *
     * @param topVolumeName
     * @throws Exception
     */
    public Boolean checkIfTopVolumeIsRootVolume(String topVolumeName) throws Exception {
        for (RegisteredRootVolumeModel rootVol : fileService.getRootVolumes()) {
            if (rootVol.getName().equals(topVolumeName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method for checking if volume is Data Volume type.
     *
     * @param topVolumeName
     * @throws Exception
     */
    public Boolean checkIfTopVolumeIsDataVolume(String topVolumeName) throws Exception {
        for (RegisteredDataVolumeModel rootVol : fileService.getDataVolumes()) {
            if (rootVol.getName().equals(topVolumeName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Endpoint for pingin service.
     */
    @Operation(summary = "Ping this API", description = "Verifies that this API is up and running.")
    @ApiResponses({@ApiResponse(responseCode = "200")})
    @GetMapping("/api/ping")
    public ResponseEntity<String> getPing() {
        return new ResponseEntity<>("Service is alive", HttpStatus.OK);
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @Operation(summary = "Get health status report of this API",
               description = "Verifies that a file can be written in all registered dataVolumes and rootVolumes.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "System is healthy"),
        @ApiResponse(responseCode = "500",
                     description = "Health problem with registered dataVolume(s) and/or rootVolume(s)")
    })
    @GetMapping("/api/health")
    public ResponseEntity<JsonNode> getHealthReport(HttpServletRequest request, HttpServletResponse response) {

        StringBuilder exceptionsb = new StringBuilder();

        /* check all data volumes */
        for (RegisteredDataVolumeModel dataVolume : fileService.getDataVolumes()) {
            try {
                File baseDirectory = new File(dataVolume.getPathOnFileSystem());
                Boolean canAccess = baseDirectory.isDirectory() & baseDirectory.canRead();
                if (!canAccess) {
                    throw new Exception("Path on file system can't be accessed. ");
                }
            } catch (Exception e) {
                exceptionsb.append("Volume " + dataVolume.getName() + ": " + e.getMessage());
            }
        }

        /* iterate over all root volumes */
        for (RegisteredRootVolumeModel rootVolume : fileService.getRootVolumes()) {
            try {
                File baseDirectory = new File(rootVolume.getPathOnFileSystem());
                String testFileName = "testFileForFileServiceHealth" + config.getTestFileNameEnding();
                String testFilePath = rootVolume.getPathOnFileSystem().endsWith("/")
                        ? rootVolume.getPathOnFileSystem() + testFileName
                        : rootVolume.getPathOnFileSystem() + "/" + testFileName;
                File testFile = new File(testFilePath);
                Boolean testFileExists = false;
                Boolean wasTestFileCreated = false;
                Boolean wasTestFileDeleted = false;
                Boolean isDirectory = false;
                try {
                    isDirectory = baseDirectory.isDirectory();
                } catch (Exception e) {
                    throw new Exception("Unable to assess if path is directory. ");
                }
                if (!isDirectory) {
                    throw new Exception("Path is not a directory. ");
                } else {
                    try {
                        testFileExists = testFile.isFile();
                    } catch (Exception e) {
                        throw new Exception("Unable to assess existance of preexisting test file. ");
                    }
                    if (testFileExists) {
                        try {
                            wasTestFileDeleted = testFile.delete();
                        } catch (Exception e) {
                            throw new Exception("Unable to try deletion of preexisting test file. ");
                        }
                        if (!wasTestFileDeleted) {
                            throw new Exception("Cannot delete preexisting test file. ");
                        }
                    }

                    try {
                        wasTestFileCreated = testFile.createNewFile();
                    } catch (Exception e) {
                        throw new Exception("Unable to try creation of new test file. ");
                    }
                    if (!wasTestFileCreated) {
                        throw new Exception("Cannot create new test file. ");
                    }

                    try {
                        wasTestFileDeleted = testFile.delete();
                    } catch (Exception e) {
                        throw new Exception("Unable to try deletion of new test file. ");
                    }
                    if (!wasTestFileDeleted) {
                        throw new Exception("Cannot delete new test file. ");
                    }
                }
            } catch (Exception ex) {
                exceptionsb.append("Volume " + rootVolume.getName() + ": " + ex.getMessage());
            }
        }
        if (exceptionsb.length() == 0) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            String message = exceptionsb.toString();
            return logException("CheckHealth", null, null, null, null, null, null, request, new Exception(message),
                    HttpStatus.INTERNAL_SERVER_ERROR, null, false);
        }

    }

    /**
     * TODO should also query RACM for folders shared with the user if volumes/ is
     * requested.
     *
     * @throws SciServerClientException
     * @throws UnauthenticatedException
     *
     */
    @Operation(summary = "Lists the contents of a particular directory path.",
               description = "Lists the contents of a directory (and its subdirectories) under a topVolume, "
                           + "given a number of depth levels. Metadata of files or directories is also provided, "
                           + "such as size and creation/modification dates.",
               parameters = {
                   @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", description = "User's auth token.",
                              required = true, schema = @Schema(type = "string")),
                   @Parameter(in = ParameterIn.PATH, name = "**", description = "Wild card", required = false,
                              schema = @Schema(type = "string"), example = "dwdw/dwdw")
               }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "401",
                     description = "User's auth token is missing or invalid."),
        @ApiResponse(responseCode = "500",
                     description = "Internal error when listing contents, permissions issue, "
                                 + "or wrong value for 'level' input parameter")
    })
    @GetMapping({ "/api/jsontree/{topVolume}/**" })
    public ResponseEntity<JsonNode> listDirectory(
        @Parameter(description = "Name of the top volume, which can be a rootVolume or a dataVolume.",
                   required = true) @PathVariable String topVolume,
        @Parameter(description = "Glob pattern that matches the names of listed files or directories.",
                       required = false) @RequestParam(required = false) String options,
        @Parameter(description = "Maximum depth level of listed subdirectories (greater than 0).",
                       example = "1", required = false) @RequestParam(value = "level", required = false) Short level,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws UnauthenticatedException, SciServerClientException {

        String ownerName = null;
        String userVolume = null;
        String path = null;
        String rootVolume = null;
        String dataVolume = null;
        User ownerUser = null;

        AuthenticatedUser user = Auth.get();

        try {

            short ilevel = Short.MAX_VALUE;
            if (level != null) {
                if (level < 1) {
                    throw new Exception("Parameter 'level' must be an integer greater than 0.");
                }
                ilevel = level;
            }

            // checking permissions and create folders if needed. Throws an exception if
            // checks are not met.
            ArrayList<String> permissions = new ArrayList<>(Collections.singletonList("read"));
            String baseUri = "/api/jsontree/";
            Quintet<Boolean, String, String, String, String> info = generalCheck(user, topVolume, permissions, baseUri,
                    request);

            Boolean isTopVolumeADataVolume = info.x1;
            rootVolume = !isTopVolumeADataVolume ? topVolume : null;
            dataVolume = isTopVolumeADataVolume ? topVolume : null;

            String topVolumePath = info.x2;// this is relative path in case of user vol and base path in case of data
                                           // vol
            ownerName = info.x3;
            userVolume = info.x4;
            path = info.x5;

            String pathForLogMessage = getPathForLogMessage(topVolume, ownerName, userVolume, path,
                    isTopVolumeADataVolume,
                    dataVolume);

            Object root = null;
            String pathUnderTopVolume = null;
            String replaceTopDirName = null;
            String pathForShowing = null;

            if (isTopVolumeADataVolume) {
                pathUnderTopVolume = path;
                replaceTopDirName = path.equals("") ? dataVolume : null;
            } else {
                ownerUser = getOwnerUser(ownerName);
                pathUnderTopVolume = topVolumePath + path;
                replaceTopDirName = path.equals("") ? userVolume : null;
                pathForShowing = userVolume + "/" + path;
            }
            if (ilevel != 2) {
                root = fileTree(ownerUser, topVolume, pathUnderTopVolume, ilevel, options, pathForShowing,
                        replaceTopDirName,
                        pathForLogMessage, isTopVolumeADataVolume);
            } else {
                root = getDirectoryListing(ownerUser, topVolume, pathUnderTopVolume, options, pathForShowing,
                        pathForLogMessage,
                        isTopVolumeADataVolume);
            }

            ObjectMapper om = new ObjectMapper();
            JsonNode n = om.valueToTree(root);

            return new ResponseEntity<>(n, HttpStatus.OK);
        } catch (Exception e) {
            return logException("DirectoryList", rootVolume, dataVolume, ownerName, userVolume, path, user, request, e,
                    HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
    }

    /**
     * Endpoint for uploading file.
     */
    @Operation(summary = "Uploads a file.",
               description = "Uploads a file to a particular destination directory path under a topVolume.",
               parameters = {
                   @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", description = "User's auth token.",
                              required = true, schema = @Schema(type = "string"))
               }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "401",
                     description = "User's auth token is missing or invalid."),
        @ApiResponse(responseCode = "500",
                     description = "Internal error when uploading file or permissions issue.")
    })
    @PutMapping("/api/file/{topVolume}/**")
    public ResponseEntity<JsonNode> uploadFile(
        @Parameter(description = "Name of the top volume, which can be a rootVolume or a dataVolume.",
                    required = true) @PathVariable String topVolume,
        @Parameter(description = "If file already exists at destination path, "
                                + "an exeception will be thrown when quiet=false.",
                    example = "false", schema = @Schema(defaultValue = "false"), required = false)
                    @RequestParam(defaultValue = "false", required = false) Boolean quiet,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws UnauthenticatedException, SciServerClientException {

        String ownerName = null;
        String userVolume = null;
        String path = null;
        String rootVolume = null;
        String dataVolume = null;
        User ownerUser = null;
        AuthenticatedUser user = Auth.get();

        try {

            quiet = quiet == null ? false : quiet;

            ArrayList<String> permissions = new ArrayList<>(Collections.singletonList("write"));
            String baseUri = "/api/file/";
            Quintet<Boolean, String, String, String, String> info = generalCheck(user, topVolume, permissions, baseUri,
                    request);
            Boolean isTopVolumeADataVolume = info.x1;
            rootVolume = !isTopVolumeADataVolume ? topVolume : null;
            dataVolume = isTopVolumeADataVolume ? topVolume : null;
            String topVolumePath = info.x2;// this is the relative path, in case of user volume, and base path in case
                                           // of
                                           // data volume
            ownerName = info.x3;
            userVolume = info.x4;
            path = info.x5;
            String pathForLogMessage = getPathForLogMessage(topVolume, ownerName, userVolume, path,
                    isTopVolumeADataVolume,
                    dataVolume);

            String pathUnderTopVolume = null;
            if (isTopVolumeADataVolume) {
                pathUnderTopVolume = path;
            } else {
                ownerUser = getOwnerUser(ownerName);
                pathUnderTopVolume = topVolumePath + path;
            }
            File newfile = createFile(ownerUser, topVolume, pathUnderTopVolume, request.getInputStream(), quiet,
                    pathForLogMessage, isTopVolumeADataVolume);
            response.setHeader("Etag", "W/" + newfile.lastModified());

            ObjectNode sentence = mapper.createObjectNode();
            sentence.put("subject", user.getUserName());
            sentence.put("verb", "uploaded");
            sentence.put("predicate", "file '" + pathForLogMessage + "'");
            String message = sentence.get("subject").asText() + " " + sentence.get("verb").asText() + " "
                    + sentence.get("predicate").asText();
            logMessage(message, "UploadFile", rootVolume, ownerName, userVolume, path, user, request, sentence, null,
                    null);

            return null;
        } catch (Exception e) {
            return logException("UploadFile", rootVolume, dataVolume, ownerName, userVolume, path, user, request, e,
                    HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
    }

    /**
     * Endpoint for download file.
     */
    @Operation(summary = "Downloads a file.",
               description = "Downloads a file located at a particular directory path under a topVolume.",
               parameters = {
                   @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", description = "User's auth token.",
                              required = true, schema = @Schema(type = "string"))
               }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "401",
                     description = "User's auth token is missing or invalid."),
        @ApiResponse(responseCode = "500",
                     description = "Internal error when downloading file or permissions issue.")
    })
    @RequestMapping(value = "/api/file/{topVolume}/**", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ResponseEntity<JsonNode> downloadFile(
        @Parameter(description = "Name of the top volume, which can be a rootVolume or a dataVolume.",
                   required = true) @PathVariable String topVolume,
        @Parameter(description = "Sets the response content disposition to inline when "
                               + "this parameter is true, or attachment if false.",
                               schema = @Schema(defaultValue = "false", type = "Boolean"),
                               required = false)
                               @RequestParam(defaultValue = "false", required = false) Boolean inline,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws UnauthenticatedException, SciServerClientException {

        String ownerName = null;
        String userVolume = null;
        String path = null;
        String rootVolume = null;
        String dataVolume = null;
        User ownerUser = null;
        AuthenticatedUser user = Auth.get();
        try {

            OutputStream output = response.getOutputStream();

            response.setContentType("application/octet-stream");

            ArrayList<String> permissions = new ArrayList<>(Collections.singletonList("read"));
            String baseUri = "/api/file/";
            Quintet<Boolean, String, String, String, String> info = generalCheck(user, topVolume, permissions, baseUri,
                    request);
            Boolean isTopVolumeADataVolume = info.x1;
            String topVolumePath = info.x2;// this is the relative path, in case of user volume, and base path in case
                                           // of
                                           // data volume
            ownerName = info.x3;
            userVolume = info.x4;
            path = info.x5;
            rootVolume = !isTopVolumeADataVolume ? topVolume : null;
            dataVolume = isTopVolumeADataVolume ? topVolume : null;
            String pathForLogMessage = getPathForLogMessage(topVolume, ownerName, userVolume, path,
                    isTopVolumeADataVolume,
                    dataVolume);

            String pathUnderTopVolume = null;
            if (isTopVolumeADataVolume) {
                pathUnderTopVolume = path;
            } else {
                ownerUser = getOwnerUser(ownerName);
                pathUnderTopVolume = topVolumePath + path;
            }
            getFile(ownerUser, topVolume, pathUnderTopVolume, output, inline, pathForLogMessage, response,
                    isTopVolumeADataVolume);

            ObjectNode sentence = mapper.createObjectNode();
            sentence.put("subject", user.getUserName());
            sentence.put("verb", "downloaded");
            sentence.put("predicate", "file '" + pathForLogMessage + "'");
            String message = sentence.get("subject").asText() + " " + sentence.get("verb").asText() + " "
                    + sentence.get("predicate").asText();
            logMessage(message, "DownloadFile", rootVolume, ownerName, userVolume, path, user, request, sentence, null,
                    null);

            return null;
        } catch (Exception e) {
            return logException("DownloadFile", rootVolume, dataVolume, ownerName, userVolume, path, user, request, e,
                    HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
    }

    /**
     * Request for downloading multiple files.
     */
    @Operation(summary = "Downloads multiple files.",
               description = "Downloads multiple files in one http request.",
               parameters = {
                   @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", description = "User's auth token.",
                              required = true, schema = @Schema(type = "string"))
               }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "400", description = "Bad request: exceeded max amount of files per request."),
        @ApiResponse(responseCode = "401", description = "User's auth token is missing or invalid.",
                     content = @Content(mediaType = "application/json",
                     schema = @Schema(implementation = JsonNode.class))),
    })
    @PostMapping("/api/multiple-file/")
    public ResponseEntity<DownloadMultipleFilesResponse> downloadFiles(
        @RequestBody(required = true, description = "Request body containing a list of files to be downloaded, "
                                                  + "as well as information about them.")
        @org.springframework.web.bind.annotation.RequestBody
                    DownloadMultipleFilesRequest[] filesReq,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws UnauthenticatedException, SciServerClientException, Exception {

        AuthenticatedUser user = Auth.get();

        if (filesReq.length > MAX_FILES) {
            return new ResponseEntity<DownloadMultipleFilesResponse>(
                    new DownloadMultipleFilesResponse("exceeded max amount of files per request."),
                    HttpStatus.BAD_REQUEST);
        }

        String topVolume = null;
        String ownerName = null;
        String userVolume = null;
        String path = null;
        User ownerUser = null;
        ArrayList<FileDataResponse> files = new ArrayList<>();

        for (DownloadMultipleFilesRequest requestedFile : filesReq) {
            String filePath = requestedFile.filePath;

            try {

                Quartet<String, String, String, String> quartet = getInfoFromUriTemplateMultiple(filePath);
                topVolume = quartet.x1;
                ownerName = quartet.x2;
                userVolume = quartet.x3;
                path = quartet.x4;

                ArrayList<String> permissions = new ArrayList<>(Collections.singletonList("read"));
                Quintet<Boolean, String, String, String, String> info = generalCheck(user, topVolume, permissions,
                        userVolume,
                        ownerName, path);
                Boolean isTopVolumeADataVolume = info.x1;

                // this is the relative path, in case of user volume, and base path in case of
                // data volume
                String topVolumePath = info.x2;
                path = info.x5;
                String pathUnderTopVolume = null;
                if (isTopVolumeADataVolume) {
                    pathUnderTopVolume = path;
                } else {
                    ownerUser = getOwnerUser(ownerName);
                    pathUnderTopVolume = topVolumePath + path;
                }

                File file = new File(getFullPath(topVolume, pathUnderTopVolume, ownerUser, isTopVolumeADataVolume));

                if (!file.isFile()) {
                    files.add(new FileDataResponse(filePath, HttpStatus.NOT_FOUND, "File not found."));
                } else if (file.length() > MAX_FILE_SIZE) {
                    files.add(new FileDataResponse(filePath, HttpStatus.PAYLOAD_TOO_LARGE, "File exceeded max size."));
                } else {
                    byte[] byteData = com.google.common.io.Files.toByteArray(file);
                    String base64String = Base64.getEncoder().encodeToString(byteData);

                    files.add(new FileDataResponse(filePath, base64String));
                }

            } catch (Exception e) {
                files.add(new FileDataResponse(filePath, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
            }
        }

        return new ResponseEntity<DownloadMultipleFilesResponse>(new DownloadMultipleFilesResponse(files),
                HttpStatus.OK);

    }

    /**
     * Creates folder.
     *
     * @throws SciServerClientException
     * @throws UnauthenticatedException
     */
    @Operation(summary = "Creates a folder.",
               description = "Creates a folder under a particular directory path under a topVolume.",
               parameters = {
                   @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", description = "User's auth token.",
                              required = true, schema = @Schema(type = "string"))
               }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "401", description = "User's auth token is missing or invalid."),
        @ApiResponse(responseCode = "500", description = "Internal error when creating folder or permissions issue.")
    })
    @PutMapping("/api/folder/{topVolume}/**")
    public ResponseEntity<JsonNode> createFolder(
        @Parameter(description = "Name of the top volume, which can be a rootVolume or a dataVolume.",
                    required = true) @PathVariable String topVolume,
        @Parameter(description = "If folder already exists, an error will be thrown when quiet=false.",
                   example = "false", schema = @Schema(defaultValue = "false"), required = false)
                   @RequestParam(required = false, defaultValue = "false") Boolean quiet,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws UnauthenticatedException, SciServerClientException {

        String ownerName = null;
        String userVolume = null;
        String path = null;
        String rootVolume = null;
        String dataVolume = null;
        User ownerUser = null;
        AuthenticatedUser user = Auth.get();
        try {
            quiet = quiet == null ? false : quiet;

            ArrayList<String> permissions = new ArrayList<>(Collections.singletonList("write"));
            String baseUri = "/api/folder/";
            Quintet<Boolean, String, String, String, String> info = generalCheck(user, topVolume, permissions, baseUri,
                    request);
            Boolean isTopVolumeADataVolume = info.x1;
            String topVolumePath = info.x2;// this is the relative path, in case of user volume, and base path in case
                                           // of
                                           // data volume
            ownerName = info.x3;
            userVolume = info.x4;
            path = info.x5;
            rootVolume = !isTopVolumeADataVolume ? topVolume : null;
            dataVolume = isTopVolumeADataVolume ? topVolume : null;
            String pathForLogMessage = getPathForLogMessage(topVolume, ownerName, userVolume, path,
                    isTopVolumeADataVolume,
                    dataVolume);

            String pathUnderTopVolume = null;
            if (isTopVolumeADataVolume) {
                pathUnderTopVolume = path;
            } else {
                ownerUser = getOwnerUser(ownerName);
                pathUnderTopVolume = topVolumePath + path;
            }

            createDir(ownerUser, topVolume, pathUnderTopVolume, quiet, pathForLogMessage, isTopVolumeADataVolume);

            ObjectNode sentence = mapper.createObjectNode();
            sentence.put("subject", user.getUserName());
            sentence.put("verb", "created");
            sentence.put("predicate", "folder '" + pathForLogMessage + "'");
            String message = sentence.get("subject").asText() + " " + sentence.get("verb").asText() + " "
                    + sentence.get("predicate").asText();
            logMessage(message, "CreateFolder", rootVolume, ownerName, userVolume, path, user, request, sentence, null,
                    null);
            return null;
        } catch (Exception e) {
            return logException("CreateFolder", rootVolume, dataVolume, ownerName, userVolume, path, user, request, e,
                    HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
    }

    /**
     * Endpoint for deleting data.
     */
    @Operation(summary = "Deletes a file or folder.",
               description = "Deletes a file or folder under a particular directory path under a topVolume.",
               parameters = {
                   @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", description = "User's auth token.",
                              required = true, schema = @Schema(type = "string"))
               }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "401", description = "User's auth token is missing or invalid."),
        @ApiResponse(responseCode = "500", description = "Internal error when deleting folder or permissions issue.")
    })
    @DeleteMapping("/api/data/{topVolume}/**")
    public ResponseEntity<JsonNode> deleteData(
        @Parameter(description = "Name of the top volume, which can be a rootVolume or a dataVolume.",
                   required = true) @PathVariable String topVolume,
        @Parameter(description = "If file (or folder) does not exist, an error will be thrown when quiet=false.",
                   example = "false", schema = @Schema(defaultValue = "false"), required = false)
                   @RequestParam(defaultValue = "false", required = false) Boolean quiet,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws UnauthenticatedException, SciServerClientException {

        String ownerName = null;
        String userVolume = null;
        String path = null;
        String rootVolume = null;
        String dataVolume = null;
        User ownerUser = null;
        AuthenticatedUser user = Auth.get();
        try {
            quiet = quiet == null ? false : quiet;

            ArrayList<String> permissions = new ArrayList<>(Collections.singletonList("write"));
            String baseUri = "/api/data/";
            Quintet<Boolean, String, String, String, String> info = generalCheck(user, topVolume, permissions, baseUri,
                    request);
            Boolean isTopVolumeADataVolume = info.x1;
            String topVolumePath = info.x2;// this is the relative path, in case of user volume, and base path in case
                                           // of
                                           // data volume
            ownerName = info.x3;
            userVolume = info.x4;
            path = info.x5;
            rootVolume = !isTopVolumeADataVolume ? topVolume : null;
            dataVolume = isTopVolumeADataVolume ? topVolume : null;

            if (ownerName != null && !ownerName.equals(user.getUserName())) {
                ownerUser = getOwnerUser(ownerName);
            } else {
                ownerUser = user;
            }

            String pathForLogMessage = getPathForLogMessage(topVolume, ownerName, userVolume, path,
                    isTopVolumeADataVolume,
                    dataVolume);

            String pathUnderTopVolume = null;
            if (isTopVolumeADataVolume) {
                pathUnderTopVolume = path;
            } else {
                pathUnderTopVolume = topVolumePath + path;

            }

            if (path != null && !path.matches("^\\s*$")) {
                deleteFile(ownerUser, topVolume, pathUnderTopVolume, quiet, pathForLogMessage, isTopVolumeADataVolume);
            }

            ObjectNode sentence = mapper.createObjectNode();
            sentence.put("subject", user.getUserName());
            sentence.put("verb", "deleted");
            sentence.put("predicate", "file '" + pathForLogMessage + "'");
            String message = sentence.get("subject").asText() + " " + sentence.get("verb").asText() + " "
                    + sentence.get("predicate").asText();
            logMessage(message, "DeleteData", rootVolume, ownerName, userVolume, path, user, request, sentence, null,
                    null);

            return null;
        } catch (Exception e) {
            return logException("DeleteData", rootVolume, dataVolume, ownerName, userVolume, path, user, request, e,
                    HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
    }

    /**
     * Endpoint for moving data.
     */
    @Operation(summary = "Moves a file or folder.",
               description = "Moves a file or folder to a particular destination directory path under a topVolume.",
               parameters = {
                   @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token",
                              description = "User's auth token.", required = true, schema = @Schema(type = "string"))
               }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "401", description = "User's auth token is missing or invalid."),
        @ApiResponse(responseCode = "500", description = "Internal error when moving data or permissions issue.")
    })
    @PutMapping("/api/data/{topVolume}/**")
    public ResponseEntity<JsonNode> moveData(
        @Parameter(description = "Name of the top volume, which can be a rootVolume or a dataVolume.",
                   required = true) @PathVariable String topVolume,
        @Parameter(description = "If the file or folder already exists in the destination directory path, "
                               + "this boolean variable decides whether to allow replacing them.",
                   required = true, example = "false")
                   @RequestParam(value = "replaceExisting", required = false) Boolean replaceExisting,
        @Parameter(description = "Boolean variable for deciding whether to copy (true) "
                               + "or move (false) the file or folder.",
                   required = true, example = "true") @RequestParam(value = "doCopy", required = false) Boolean doCopy,

        @RequestBody(required = true, description = "Contains the information needed for moving data within the "
                                                  + "file system, from one path to a destination path.")
        @org.springframework.web.bind.annotation.RequestBody
                    MoveDataRequestBody body,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws UnauthenticatedException, SciServerClientException {

        String ownerName = null;
        String userVolume = null;
        String path = null;
        String rootVolume = null;
        String dataVolume = null;
        User ownerUser = null;
        User destinationOwnerUser = null;
        AuthenticatedUser user = Auth.get();
        try {
            replaceExisting = replaceExisting == null ? false : replaceExisting;
            doCopy = doCopy == null ? false : doCopy;

            String destinationFileService = body.getDestinationFileService() == null ? config.getFileService().getName()
                    : body.getDestinationFileService();
            String destinationRootVolume = body.getDestinationRootVolume();// == null ? rootVolume :
                                                                           // body.getDestinationRootVolume();
            String destinationDataVolume = body.getDestinationDataVolume();
            String destinationUserVolume = body.getDestinationUserVolume();// == null ? userVolume :
                                                                           // body.getDestinationUserVolume();
            String destinationOwnerName = body.getDestinationOwnerName();// == null ? user.getUserName() :
                                                                         // body.getDestinationOwnerName();
            String destinationPath = body.getDestinationPath();
            if (!destinationFileService.equals(config.getFileService().getName())) {
                throw new Exception("Copying or moving data to another FileService is not currently supported");
            }

            if (body.getDestinationPath() == null) {
                throw new Exception("Request body parameter 'destinationPath' is not defined");
            }

            if (body.getDestinationRootVolume() == null && body.getDestinationDataVolume() == null) {
                throw new Exception(
                        "Request body parameter(s) 'destinationRootVolume' or 'destinationDataVolume' not defined");
            }

            if (body.getDestinationRootVolume() != null && body.getDestinationDataVolume() != null) {
                throw new Exception("Cannot specify Request body parameter(s) 'destinationRootVolume' "
                                  + "and 'destinationDataVolume' at the same time");
            }

            if (body.getDestinationRootVolume() != null
                    && (body.getDestinationOwnerName() == null || body.getDestinationUserVolume() == null)) {
                throw new Exception("Request body parameter(s) 'destinationUserVolume' "
                                  + "or 'destinationOwnerName' not defined");
            }

            // checking permissions and create folders if needed. Throws an exception if
            // checks are not met.
            ArrayList<String> permissions = null;
            if (doCopy) {
                permissions = new ArrayList<>(Collections.singletonList("read"));
            } else {
                permissions = new ArrayList<>(Collections.singletonList("write"));
            }

            String baseUri = "/api/data/";
            Quintet<Boolean, String, String, String, String> info = generalCheck(user, topVolume, permissions, baseUri,
                    request);
            Boolean isTopVolumeADataVolume = info.x1;
            String topVolumePath = info.x2;// this is the relative path, in case of user volume, and base path in case
                                           // of
                                           // data volume
            ownerName = info.x3;
            userVolume = info.x4;
            path = info.x5;
            rootVolume = !isTopVolumeADataVolume ? topVolume : null;
            dataVolume = isTopVolumeADataVolume ? topVolume : null;

            // now check the destination:

            permissions = new ArrayList<>(Collections.singletonList("write"));

            Boolean isDestinationTopVolumeADataVolume = destinationDataVolume != null ? true : false;
            String destinationTopVolume = destinationDataVolume != null ? destinationDataVolume : destinationRootVolume;
            info = generalCheck(user, destinationTopVolume, permissions, destinationUserVolume, destinationOwnerName,
                    destinationPath);
            String destinationTopVolumePath = info.x2;

            if (ownerName != null && !ownerName.equals(user.getUserName())) {
                ownerUser = getOwnerUser(ownerName);
            } else {
                ownerUser = user;
            }
            if (destinationOwnerName != null) {
                destinationOwnerUser = getOwnerUser(destinationOwnerName);
            }

            String pathForLogMessage = getPathForLogMessage(topVolume, ownerName, userVolume, path,
                    isTopVolumeADataVolume,
                    dataVolume);
            String newPathForLogMessage = getPathForLogMessage(destinationTopVolume, destinationOwnerName,
                    destinationUserVolume, destinationPath, isDestinationTopVolumeADataVolume, destinationDataVolume);
            String pathUnderDestinationTopVolume = null;
            String pathUnderTopVolume = null;

            if (isTopVolumeADataVolume) {
                pathUnderTopVolume = path;
            } else {
                // pathUnderTopVolume = userVolume + "/" + path;
                pathUnderTopVolume = topVolumePath + path;
            }

            if (isDestinationTopVolumeADataVolume) {
                pathUnderDestinationTopVolume = destinationPath;
            } else {
                // pathUnderDestinationRootVolume = destinationUserVolume + "/" +
                // destinationPath;
                pathUnderDestinationTopVolume = destinationTopVolumePath + destinationPath;
            }

            if (path != null && !path.matches("^\\s*$") && destinationPath != null
                    && !destinationPath.matches("^\\s*$")) {
                moveFile(ownerUser, destinationOwnerUser, topVolume, pathUnderTopVolume, destinationTopVolume,
                        pathUnderDestinationTopVolume, doCopy, replaceExisting, pathForLogMessage, newPathForLogMessage,
                        isTopVolumeADataVolume, isDestinationTopVolumeADataVolume);

            } else {
                throw new Exception("Invalid 'path' or 'destinationPath' parameters");
            }

            ObjectNode sentence = mapper.createObjectNode();
            String verb = doCopy == true ? "copied" : "moved";
            sentence.put("subject", user.getUserName());
            sentence.put("verb", verb);
            if (destinationFileService.equals(config.getFileService().getName())) {
                sentence.put("predicate", "'" + pathForLogMessage + "' to '" + newPathForLogMessage + "'");
            } else {
                sentence.put("predicate",
                        "'" + pathForLogMessage + "' in fileService '" + config.getFileService().getName()
                                + "' to '" + newPathForLogMessage + "' in fileService '" + destinationFileService
                                + "'");
            }

            String message = sentence.get("subject").asText() + " " + sentence.get("verb").asText() + " "
                    + sentence.get("predicate").asText();
            logMessage(message, "MoveData", rootVolume, ownerName, userVolume, path, user, request, sentence, null,
                    body);

            return null;
        } catch (Exception e) {
            return logException("MoveData", rootVolume, dataVolume, ownerName, userVolume, path, user, request, e,
                    HttpStatus.INTERNAL_SERVER_ERROR, body);
        }
    }

    /**
     *
     * Return the root folders that the user has access to, e.g., persistent/,
     * scratch/, owned volumes/, shared volumes/.<br/>
     * If the user volumes inside don't exist yet, either in racm or the file
     * system, create them.
     *
     * @param request
     * @param response
     * @return
     * @throws SciServerClientException
     * @throws UnauthenticatedException
     * @throws Exception
     **/
    @Operation(summary = "Returns all dataVolumes, rootVolumes, and userVolumes accessible to the user.",
               description = "Returns all userVolumes and dataVolumes accessible to the user, also including metadata "
                           + "such as allowed actions and information on shared volumes.",
               parameters = {
                   @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", description = "User's auth token.",
                              required = true, schema = @Schema(type = "string"))
               }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "401", description = "User's auth token is missing or invalid."),
        @ApiResponse(responseCode = "500", description = "Internal error when retrieving volume information.")
    })
    @GetMapping("/api/volumes")
    public FileServiceModel getVolumes(HttpServletRequest request) throws Exception {
        AuthenticatedUser user = Auth.get();
        try {
            return getVolumes(user);
        } catch (Exception e) {
            logException("GetVolumes", null, null, null, null, null, user, request, e, HttpStatus.INTERNAL_SERVER_ERROR,
                    null);
            throw (e);
        }
    }

    /**
     * Get file usage and quota info if known.
     *
     * @throws SciServerClientException
     * @throws UnauthenticatedException
     */
    @Operation(summary = "Returns information on volumes with size quotas.",
               description = "Returns information about the quota size and usage on each volume that has a quota."
    )
    @GetMapping("/api/usage")
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "401", description = "User's auth token is missing or invalid."),
        @ApiResponse(responseCode = "500", description = "Internal error when retrieving usage information.")
    })
    public ResponseEntity<?> getUsage(HttpServletRequest request)
            throws UnauthenticatedException, SciServerClientException {
        AuthenticatedUser user = Auth.get();
        try {
            ServiceLogTimer timer = Log.get().startTimer("getUsage");
            Collection<QuotaFromManager> quotas = usageInfoProvider.getUsage();
            timer.stop();
            return ResponseEntity.ok(volumesManager
                    .getVolumesAndLazilyCreate(user)
                    .getRootVolumes()
                    .stream()
                    .flatMap(rv -> rv.getUserVolumes()
                            .stream()
                            .flatMap(uv -> Stream.concat(quotas.stream()
                                    .filter(q -> q.getRootVolumeName().equals(rv.getName())
                                                 && q.getRelativePath().equals(uv.getRelativePath()))
                                    .map(q -> new QuotaPerVolume(q.getNumberOfFilesUsed(),
                                            q.getNumberOfFilesQuota(), q.getNumberOfBytesUsed(),
                                            q.getNumberOfBytesQuota(), uv.getId(), rv.getId())),
                                    quotas.stream()
                                            .filter(q -> q.getRootVolumeName().equals(rv.getName())
                                                         && uv.getOwner().equals(user.getUserName())
                                                         && uv.getRelativePath().startsWith(q.getRelativePath()))
                                            .map(q -> new QuotaPerUser(q.getNumberOfFilesUsed(),
                                                    q.getNumberOfFilesQuota(), q.getNumberOfBytesUsed(),
                                                    q.getNumberOfBytesQuota(), uv.getOwner(), rv.getId())))))
                    .distinct()
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            return logException("GetUsage", null, null, null, null, null, user, request, e,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    null);
        }
    }

    /**
     * Create a new user volume, i.e. a folder under the user.volumes.<br/>
     *
     * @return
     * @throws SciServerClientException
     * @throws UnauthenticatedException
     * @throws Exception
     */
    @Operation(summary = "Creates a userVolume.",
               description = "Creates a userVolume under a particular rootVolume.",
               parameters = {
                   @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", description = "User's auth token.",
                              required = true, schema = @Schema(type = "string"))
               }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "401", description = "User's auth token is missing or invalid."),
        @ApiResponse(responseCode = "500", description = "Internal error when creating user volume.")
    })
    @PutMapping("/api/volume/{rootVolume}/{ownerName}/{userVolume:.+}")
    public ResponseEntity<JsonNode> createUserVolume(
        @Parameter(description = "Name of the rootVolume.",
                   required = true, example = "Storage") @PathVariable String rootVolume,
        @Parameter(description = "Name of the user that owns this userVolume.",
                   required = true) @PathVariable String ownerName,
        @Parameter(description = "Name of the userVolume.",
                   required = true) @PathVariable String userVolume,
        @Parameter(description = "If userVolume already exists, an exeception will be thrown when quiet=false.",
                   example = "false", schema = @Schema(defaultValue = "false"), required = false)
                   @RequestParam(defaultValue = "false", required = false) Boolean quiet,
        @RequestHeader(value = "X-Service-Auth-ID", required = false) String xServiceID,
        @RequestBody(required = false, description = "Description of the new user volume.")
        @org.springframework.web.bind.annotation.RequestBody(required = false)
                    CreateUserVolumeRequestBody body,
        HttpServletRequest request
    ) throws UnauthenticatedException, SciServerClientException {
        AuthenticatedUser user = Auth.get();
        try {
            // do not allow to create user volume on behalf of another user:
            if (!user.getUserName().equals(ownerName)) {
                throw new UnauthenticatedException("Not allowed to create user volume on behalf of another user.");
            }

            quiet = quiet == null ? false : quiet;
            createDefaultUserVolumes(user);

            // check with RACM if this user volume has already been created in the past.
            if (racmClient.queryUserVolumeActions(
                    user.getToken(), fileServiceIdentifier, rootVolume, ownerName, userVolume).exists()) {
                if (quiet) {
                    return new ResponseEntity<>(HttpStatus.OK);
                } else {
                    throw new Exception("User volume '" + userVolume + "' already exists in root volume '" + rootVolume
                            + "' within fileService '" + config.getFileService().getName() + "'");
                }
            }

            String basePathInFileSystem = getRootVolumePathOnFileSystem(rootVolume);
            String description = body == null ? "UserVolume created by " + user.getUserName() : body.getDescription();
            String userVolumeNameInFileSystem = Utility.getRandomUUID();
            String pathForLogMessage = getPathForLogMessage(rootVolume, ownerName, userVolume, "");
            String relativePath = user.getUserId() + "/" + userVolumeNameInFileSystem + "/";

            // create file system path for user volume
            createUserVolume(user.getUserId(), basePathInFileSystem, rootVolume, userVolumeNameInFileSystem, quiet,
                    pathForLogMessage);

            // send to RACM the information about the newly created user volume.
            try {
                racmClient.newUserVolume(
                        user.getToken(), fileServiceIdentifier, rootVolume,
                        new RegisterNewUserVolumeModel(userVolume, description, relativePath, ownerName));
            } catch (Exception ex) {
                // clean up volume on disk
                try {
                    User ownerUser = user;
                    String path = userVolumeNameInFileSystem;

                    if (quotaManagerService != null) {
                        quotaManagerService.deleteVolume(
                                new ManagerVolumeDTO(rootVolume, ownerUser.getUserId() + "/" + path)).execute();
                    } else {
                        deleteUserVolume(ownerUser, rootVolume, path, true, pathForLogMessage);
                    }
                } catch (Exception e) {
                }
                throw (ex);
            }

            ObjectNode sentence = mapper.createObjectNode();
            sentence.put("subject", user.getUserName());
            sentence.put("verb", "created");
            sentence.put("predicate", "user volume '/" + pathForLogMessage + "'");
            String message = sentence.get("subject").asText() + " " + sentence.get("verb").asText() + " "
                    + sentence.get("predicate").asText();
            logMessage(message, "CreateUserVolume", rootVolume, ownerName, userVolume, null, user, request, sentence,
                    null,
                    body);
            return new ResponseEntity<>(HttpStatus.OK);

        } catch (Exception ex) {
            return logException("CreateUserVolume", rootVolume, null, ownerName, userVolume, null, user, request, ex,
                    HttpStatus.INTERNAL_SERVER_ERROR, body);
        }
    }

    /**
     * Create a new user volume, i.e. a folder under the user.volumes.<br/>
     *
     * @return
     * @throws SciServerClientException
     * @throws UnauthenticatedException
     * @throws Exception
     */
    @Operation(summary = "Creates a serviceVolume.",
               description = "Creates a serviceVolume, which is intended to be used/operated exclusively by a Service.",
               parameters = {
                   @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", description = "User's auth token.",
                              required = true, schema = @Schema(type = "string")),
               }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "401", description = "User's auth token is missing or invalid.",
                     content = @Content(mediaType = "application/json",
                     schema = @Schema(implementation = JsonNode.class))),
        @ApiResponse(responseCode = "500", description = "Internal error when creating service volume.",
                     content = @Content(mediaType = "application/json",
                     schema = @Schema(implementation = JsonNode.class))),
    })
    @PutMapping(value = "/api/service/{rootVolume}/{ownerName}/{serviceVolume:.+}")
    public ResponseEntity<RegisteredServiceVolumeModel> createServiceVolume(
        @Parameter(description = "Name of the rootVolume.", required = true) @PathVariable String rootVolume,
        @Parameter(description = "Name of the user that owns this serviceVolume.",
                    required = true) @PathVariable String ownerName,
        @Parameter(description = "Name of the serviceVolume.", required = true) @PathVariable String serviceVolume,
        @Parameter(in = ParameterIn.HEADER, name = "X-Service-Auth-Token", description = "Service's auth token.",
                   required = true, schema = @Schema(type = "string"))
                   @RequestHeader(value = "X-Service-Auth-ID", required = true) String serviceToken,
        @RequestBody(required = false, description = "Description of the new service volume.")
        @org.springframework.web.bind.annotation.RequestBody(required = false)
                     CreateServiceVolumeRequestBody body,
        HttpServletRequest request
    ) throws UnauthenticatedException, SciServerClientException, Exception {

        boolean quiet = false;
        AuthenticatedUser user = Auth.get();
        try {
            // do not allow to create courseware volume on behalf of another user:
            if (!user.getUserName().equals(ownerName)) {
                throw new Exception("Not allowed to create service volume on behalf of another user.");
            }

            createDefaultUserVolumes(user);

            // check with RACM if this user volume has already been created in the past.
            if (racmClient.queryServiceVolumeActions(
                    user.getToken(), serviceToken, fileServiceIdentifier, rootVolume, ownerName, serviceVolume)
                    .exists()) {
                if (quiet) {
                    return new ResponseEntity<>(HttpStatus.OK);
                } else {
                    throw new Exception(
                            "Service volume '" + serviceVolume + "' already exists in root volume '"
                                    + rootVolume + "' within fileService '" + this.config.getFileService().getName()
                                    + "'");
                }
            }

            String basePathInFileSystem = getRootVolumePathOnFileSystem(rootVolume);
            String description = body == null ? "Service Volume created by " + user.getUserName()
                    : body.getDescription();
            String serviceVolumeNameInFileSystem = Utility.getRandomUUID();
            String pathForLogMessage = getPathForLogMessage(rootVolume, ownerName, serviceVolume, "");
            String relativePath = user.getUserId() + "/" + serviceVolumeNameInFileSystem + "/";

            RegisterNewServiceVolumeModel model = new RegisterNewServiceVolumeModel(
                    serviceVolume, description, relativePath, body.getShares(), serviceToken,
                    body.getOwningResourceUUID(),
                    body.getUsage());

            // create file system path for user volume
            createUserVolume(user.getUserId(), basePathInFileSystem, rootVolume, serviceVolumeNameInFileSystem,
                    quiet, pathForLogMessage);

            // send to RACM the information about the newly created courseware volume.
            RegisteredServiceVolumeModel serviceVolumeInfo = null;
            try {
                serviceVolumeInfo = racmClient.newServiceVolume(
                        user.getToken(), fileServiceIdentifier, rootVolume, model);
            } catch (Exception ex) {
                // clean up volume on disk
                try {
                    User ownerUser = user;
                    String path = serviceVolumeNameInFileSystem;

                    if (quotaManagerService != null) {
                        quotaManagerService
                                .deleteVolume(new ManagerVolumeDTO(rootVolume, ownerUser.getUserId() + "/" + path))
                                .execute();
                    } else {
                        deleteUserVolume(ownerUser, rootVolume, path, true, pathForLogMessage);
                    }
                } catch (Exception e) {
                }
                throw new Exception("Cannot register service volume " + serviceVolume + ". " + ex.getMessage());
            }

            ObjectNode sentence = mapper.createObjectNode();
            sentence.put("subject", user.getUserName());
            sentence.put("verb", "created");
            sentence.put("predicate", "service volume '/" + pathForLogMessage + "'");
            String message = sentence.get("subject").asText() + " " + sentence.get("verb").asText() + " "
                    + sentence.get("predicate").asText();
            logMessage(message, "CreateServiceVolume", rootVolume, ownerName, serviceVolume, null, user, request,
                    sentence, null, body);

            return new ResponseEntity<>(serviceVolumeInfo, HttpStatus.OK);

        } catch (Exception ex) {
            logException("CreateServiceVolume", rootVolume, null, ownerName, serviceVolume, null, user, request,
                    ex, HttpStatus.INTERNAL_SERVER_ERROR, body);
            throw (ex);
        }
    }

    /**
     * Share a folder given by a path with a.
     *
     * NOTE currently only case allowed:
     * {folder}="volumes"
     * **= a single name, corresponding to a subfolder(volume) in the user's own
     * volumes/
     * or a name of pattern "~<username>/<name>" for a folder shared with the
     * current user by <username>, and the current user MUST have grant privilege on
     * that subfolder.
     *
     * @throws SciServerClientException
     * @throws UnauthenticatedException
     *
     *
     */
    @Operation(summary = "Shares a userVolume.",
               description = "Shares a userVolume with other SciServer users, "
                           + "including specific allowed actions such as read/write or read-only.",
               parameters = {
                   @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", description = "User's auth token.",
                              required = true, schema = @Schema(type = "string"))
               }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "401", description = "User's auth token is missing or invalid."),
        @ApiResponse(responseCode = "500", description = "Internal error when sharing service volume.")
    })
    @PatchMapping("/api/share/{rootVolume}/{ownerName}/{userVolume:.+}")
    public ResponseEntity<JsonNode> shareUserVolume(
        @Parameter(description = "Name of the userVolume.", required = true,
                   example = "Storage") @PathVariable String rootVolume,
        @Parameter(description = "Name of the user that owns this userVolume.",
                   required = true) @PathVariable String ownerName,
        @Parameter(description = "Name of the userVolume.", required = true,
                   example = "persistent") @PathVariable String userVolume,
        @RequestBody(required = true, description = "Contains information needed for sharing a user volume.")
        @org.springframework.web.bind.annotation.RequestBody
                     List<UpdateSharedWithEntry> sharedWithEntities,
        HttpServletRequest request
    ) throws UnauthenticatedException, SciServerClientException {

        AuthenticatedUser user = Auth.get();
        try {
            racmClient.shareUserVolume(user.getToken(), fileServiceIdentifier, rootVolume, ownerName, userVolume,
                    sharedWithEntities);
            return new ResponseEntity<>(HttpStatus.OK);
            // activity logged within racm
        } catch (Exception e) {
            return logException("ShareUserVolume", rootVolume, null, ownerName, userVolume, null, user, request, e,
                    HttpStatus.INTERNAL_SERVER_ERROR, sharedWithEntities);
        }
    }

    /**
     * Endpoint for sharing data volume.
     */
    @Operation(summary = "Shares a dataVolume.",
               description = "Shares a dataVolume with other SciServer users, "
                           + "including specific allowed actions such as read/write or read-only.",
               parameters = {
                   @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", description = "User's auth token.",
                              required = true, schema = @Schema(type = "string"))
               }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "401", description = "User's auth token is missing or invalid."),
        @ApiResponse(responseCode = "500", description = "Internal error when sharing data volume.")
    })
    @PatchMapping("/api/share/{dataVolume:.+}")
    public ResponseEntity<JsonNode> shareDataVolume(
        @Parameter(description = "Name of the dataVolume.", required = true) @PathVariable String dataVolume,
        @RequestBody(required = true, description = "Contains information needed for sharing a data volume.")
        @org.springframework.web.bind.annotation.RequestBody
                     List<UpdateSharedWithEntry> sharedWithEntities,
        HttpServletRequest request
    ) throws UnauthenticatedException, SciServerClientException {

        AuthenticatedUser user = Auth.get();
        try {
            racmClient.shareDataVolume(user.getToken(), fileServiceIdentifier, dataVolume, sharedWithEntities);
            return new ResponseEntity<>(HttpStatus.OK);
            // activity logged within racm
        } catch (Exception e) {
            return logException("ShareDataVolume", null, dataVolume, null, null, null, user, request, e,
                    HttpStatus.INTERNAL_SERVER_ERROR, sharedWithEntities);
        }
    }

    /**
     * Endpoint for deleting user volume.
     */
    @Operation(summary = "Deletes a userVolume.",
               description = "Deletes a userVolume. This action is only allowed to the owner of this userVolume.",
               parameters = {
                   @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", description = "User's auth token.",
                              required = true, schema = @Schema(type = "string"))
               }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "401", description = "User's auth token is missing or invalid."),
        @ApiResponse(responseCode = "500", description = "Internal error when deleting user volume "
                                                       + "or permissions issue.")
    })
    @DeleteMapping("/api/volume/{rootVolume}/{ownerName}/{userVolume:.+}")
    public ResponseEntity<JsonNode> deleteUserVolume(
        @Parameter(description = "Name of the rootVolume.", required = true,
                    example = "Storage") @PathVariable String rootVolume,
        @Parameter(description = "Name of the user that owns this userVolume.",
                    required = true) @PathVariable String ownerName,
        @Parameter(description = "Name of the userVolume.", required = true) @PathVariable String userVolume,
        @Parameter(description = "If the user volume does not exist, an error will be thrown when quiet=false.",
                   example = "false", schema = @Schema(defaultValue = "false"), required = false)
                   @RequestParam(defaultValue = "false", required = false) Boolean quiet,
        HttpServletRequest request
    ) throws UnauthenticatedException, SciServerClientException {

        String path = null;
        AuthenticatedUser user = Auth.get();
        try {
            // do not allow to create user volume on behalf of another user:
            if (!user.getUserName().equals(ownerName)) {
                throw new Exception("Not allowed to delete user volume on behalf of another user.");
            }

            quiet = quiet == null ? false : quiet;
            createDefaultUserVolumes(user);

            String userVolumeRelativePath = racmClient.queryUserVolumeActions(
                    user.getToken(), fileServiceIdentifier, rootVolume, ownerName, userVolume)
                    .requireActions("delete")
                    .getRelativePath();

            User ownerUser = getOwnerUser(ownerName);
            String pathForLogMessage = getPathForLogMessage(rootVolume, ownerName, userVolume, "");
            path = userVolumeRelativePath.endsWith("/") ? userVolumeRelativePath : userVolumeRelativePath + "/";
            path = path.split("/")[1];

            if (path != null && !path.matches("^\\s*$")) {
                if (quotaManagerService != null) {
                    Response<Void> response = quotaManagerService.deleteVolume(
                            new ManagerVolumeDTO(rootVolume, ownerUser.getUserId() + "/" + path)).execute();
                    if (!response.isSuccessful()) {
                        throw new Exception("Failure deleting user volume: " + response.message());
                    }
                } else {
                    deleteUserVolume(ownerUser, rootVolume, path, true, pathForLogMessage);
                }
            } else {
                throw new Exception("Invalid 'path' parameter");
            }

            racmClient.unregisterUserVolume(
                    user.getToken(), fileServiceIdentifier, rootVolume, ownerName, userVolume);

            ObjectNode sentence = mapper.createObjectNode();
            sentence.put("subject", user.getUserName());
            sentence.put("verb", "deleted");
            sentence.put("predicate", "user volume of path '" + pathForLogMessage + "'");
            String message = sentence.get("subject").asText() + " " + sentence.get("verb").asText() + " "
                    + sentence.get("predicate").asText();
            logMessage(message, "DeleteUserVolume", rootVolume, ownerName, userVolume, null, user, request, sentence,
                    null,
                    null);

            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return logException("DeleteUserVolume", rootVolume, null, ownerName, userVolume, null, user, request, e,
                    HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
    }

    /**
     * Endpoint for deleting service volume.
     */
    @Operation(summary = "Deletes a serviceVolume.",
               description = "Deletes a serviceVolume. This action is only allowed to the owner of this serviceVolume.",
               parameters = {
                   @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", description = "User's auth token.",
                              required = true, schema = @Schema(type = "string"))
               }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "401", description = "User's auth token is missing or invalid."),
        @ApiResponse(responseCode = "500", description = "Internal error when deleting service volume "
                                                       + "or permissions issue.")
    })
    @DeleteMapping("/api/service/{rootVolume}/{ownerName}/{serviceVolume:.+}")
    public ResponseEntity<JsonNode> deleteServiceVolume(
        @Parameter(description = "Name of the rootVolume.", required = true,
                   example = "Storage") @PathVariable String rootVolume,
        @Parameter(description = "Name of the user that owns this serviceVolume.",
                   required = true) @PathVariable String ownerName,
        @Parameter(description = "Name of the serviceVolume.", required = true,
                   example = "Storage") @PathVariable String serviceVolume,
        @Parameter(description = "If the service volume does not exist, an error will be thrown when quiet=false.",
                   example = "false", schema = @Schema(defaultValue = "false"), required = false)
                   @RequestParam(defaultValue = "false", required = false) Boolean quiet,
        @Parameter(in = ParameterIn.HEADER, name = "X-Service-Auth-Token", description = "Service's auth token.",
                   required = true, schema = @Schema(type = "string"))
                   @RequestHeader(value = "X-Service-Auth-ID", required = true) String serviceToken,
        HttpServletRequest request
    ) throws UnauthenticatedException, SciServerClientException {

        // @RequestHeader(value="X-Service-Auth-ID", required=true) String serviceToken
        // contains courseware's service token
        AuthenticatedUser user = Auth.get();
        String path = null;
        try {
            // do not allow to create user volume on behalf of another user:
            if (!user.getUserName().equals(ownerName)) {
                throw new Exception("Not allowed to delete service volume on behalf of another user.");
            }

            quiet = quiet == null ? false : quiet;
            createDefaultUserVolumes(user);

            String serviceVolumeRelativePath = racmClient.queryServiceVolumeActions(
                    user.getToken(), serviceToken, fileServiceIdentifier, rootVolume, ownerName, serviceVolume)
                    .requireActions("delete")
                    .getRelativePath();

            User ownerUser = getOwnerUser(ownerName);

            // Unregister service volume in RACM. Only if unregistering succeeds deleting it
            // from the file service.
            racmClient.unregisterServiceVolume(
                    user.getToken(), serviceToken, fileServiceIdentifier, rootVolume, ownerName, serviceVolume);

            String pathForLogMessage = getPathForLogMessage(rootVolume, ownerName, serviceVolume, "");
            path = serviceVolumeRelativePath.endsWith("/") ? serviceVolumeRelativePath
                    : serviceVolumeRelativePath + "/";
            path = path.split("/")[1];

            if (path != null && !path.matches("^\\s*$")) {
                if (quotaManagerService != null) {
                    Response<Void> response = quotaManagerService.deleteVolume(
                            new ManagerVolumeDTO(rootVolume, ownerUser.getUserId() + "/" + path)).execute();
                    if (!response.isSuccessful()) {
                        throw new Exception("Failure deleting service volume: " + response.message()
                                + ": response code = " + response.code());
                    }
                } else {
                    deleteUserVolume(ownerUser, rootVolume, path, true, pathForLogMessage);
                }
            } else {
                throw new Exception("Cannot delete service volume. Invalid 'path' parameter");
            }

            ObjectNode sentence = mapper.createObjectNode();
            sentence.put("subject", user.getUserName());
            sentence.put("verb", "deleted");
            sentence.put("predicate", "service volume of path '" + pathForLogMessage + "'");
            String message = sentence.get("subject").asText() + " " + sentence.get("verb").asText() + " "
                    + sentence.get("predicate").asText();
            logMessage(message, "DeleteServiceVolume", rootVolume, ownerName, serviceVolume, null, user, request,
                    sentence,
                    null, null);

            JsonNode json = new ObjectMapper().valueToTree("OK");
            return new ResponseEntity<JsonNode>(json, HttpStatus.OK);

        } catch (Exception e) {
            return logException("DeleteServiceVolume", rootVolume, null, ownerName, serviceVolume, null, user, request,
                    e,
                    HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
    }

    /**
     * Endpoint for patching user volume.
     */
    @Operation(summary = "Updates userVolume metadata.",
               description = "Updates userVolume metadata, such as name and description.",
               parameters = {
                   @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", description = "User's auth token.",
                             required = true, schema = @Schema(type = "string"))
               }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "401", description = "User's auth token is missing or invalid."),
        @ApiResponse(responseCode = "500", description = "Internal error when updating userVolume metadata.")
    })
    @PatchMapping("/api/volume/{rootVolume}/{ownerName}/{userVolume}")
    public ResponseEntity<JsonNode> patchUserVolume(
        @Parameter(description = "Name of the rootVolume.", required = true,
                   example = "Storage") @PathVariable String rootVolume,
        @Parameter(description = "Name of the user that owns this userVolume.",
                   required = true) @PathVariable String ownerName,
        @Parameter(description = "Name of the userVolume.", required = true) @PathVariable String userVolume,
        @RequestBody(required = true, description = "Contains information needed for updating user volume info.")
        @org.springframework.web.bind.annotation.RequestBody
                     UpdatedUserVolumeInfo volumeInfo,
        HttpServletRequest request
    ) throws UnauthenticatedException, SciServerClientException {

        String newUserVolumeDescription = null;
        String newUserVolumeName = null;
        AuthenticatedUser user = Auth.get();
        try {

            if (!ownerName.equals(user.getUserName())) {
                throw new Exception(
                        "User '" + user.getUserName() + "' cannot change name or description of user volume '"
                                + userVolume + "'");
            }

            newUserVolumeDescription = volumeInfo.getDescription().orElse(null);
            newUserVolumeName = volumeInfo.getName().orElse(null);

            racmClient.editUserVolume(user.getToken(), fileServiceIdentifier, rootVolume, ownerName, userVolume,
                    volumeInfo);

            ObjectNode sentence = mapper.createObjectNode();
            sentence.put("subject", user.getUserName());
            sentence.put("verb", "patched");
            String predicate = "user volume of path '/" + rootVolume + "/" + ownerName + "/" + userVolume + "'";

            if (newUserVolumeName != null) {
                predicate = predicate + " with new path name '/" + rootVolume + "/" + ownerName + "/"
                        + newUserVolumeName + "'";
            }
            if (newUserVolumeDescription != null) {
                if (newUserVolumeName != null) {
                    predicate = predicate + " and new description '" + newUserVolumeDescription + "'";
                } else {
                    predicate = predicate + " with new description '" + newUserVolumeDescription + "'";
                }
            }
            sentence.put("predicate", predicate);
            String message = sentence.get("subject").asText() + " " + sentence.get("verb").asText() + " "
                    + sentence.get("predicate").asText();
            logMessage(message, "PatchUserVolume", rootVolume, ownerName, userVolume, null, user, request, sentence,
                    null,
                    volumeInfo);

            return new ResponseEntity<>(HttpStatus.OK);

        } catch (Exception e) {
            return logException("PatchUserVolume", rootVolume, null, ownerName, userVolume, null, user, request, e,
                    HttpStatus.INTERNAL_SERVER_ERROR, volumeInfo);
        }
    }

    /*
     * -----------------------------------------------------------------------------
     * --------------------------------------------
     */

    /**
     * Get user owner of volume.
     */
    public User getOwnerUser(String ownerName) throws Exception {
        if (!config.getUsers().containsKey(ownerName)) {
            config.getUsers().put(ownerName, Auth.getClient().getUserByName(ownerName));
        }
        return config.getUsers().get(ownerName);
    }

    /**
     * Get file path from request URI.
     */
    public String getPathFromUriTemplate(UriTemplate template, HttpServletRequest request, String rootVolume,
            String ownerName, String userVolume) throws UnsupportedEncodingException {
        String path = null;
        String uri = UriUtils.decode(request.getRequestURI(), "UTF-8");
        ;
        Map<String, String> map = template.match(uri);
        path = map.get("path");
        return path == null ? "" : path;
    }

    /**
     * Get file path.
     */
    public Triplet<String, String, String> getInfoFromUriTemplate(String baseUri, String topVolume,
            Boolean isTopVolumeADataVolume, HttpServletRequest request) throws Exception {
        String ownerName = null;
        String userVolume = null;
        String path = "";
        String uri = UriUtils.decode(request.getRequestURI(), "UTF-8");
        // doing individual replaces protects against double slashes here and there
        // last replace removes (possibly multiple) leading slashes
        String rest = uri.replaceFirst("^.*?" + Pattern.quote(baseUri), "").replaceFirst(Pattern.quote(topVolume), "")
                .replaceAll("^/+", "");
        String[] parts = rest.split("[/]+"); // will not have parts after the last slash(es)
        int offset = 0;
        if (!isTopVolumeADataVolume) {
            if (parts.length < 1) {
                throw new Exception("Path variable 'ownerName' and 'userVolume' were not provided");
            }
            if (parts.length < 2) {
                throw new Exception("Path variable 'userVolume' was not provided");
            }
            ownerName = parts[0];
            userVolume = parts[1];
            offset = 2;
        }
        if (parts.length > offset) {
            StringBuffer sb = new StringBuffer();
            for (int i = offset; i < parts.length; i++) {
                if (i > offset) {
                    sb.append("/");
                }
                sb.append(parts[i]); // should not be necessary, as trailing slashes will not give rise to empty element
            }
            path = sb.toString();
        }
        return new Triplet<String, String, String>(ownerName, userVolume, path);
    }

    /**
     * Get file path for multiple file request.
     */
    public Quartet<String, String, String, String> getInfoFromUriTemplateMultiple(String filePath)
            throws Exception {

        String[] parts = filePath.split("[/]+");
        int offset = 1;
        String topVolume = parts[0];
        Boolean isTopVolumeADataVolume = !checkIfTopVolumeIsRootVolume(topVolume);
        String ownerName = null;
        String userVolume = null;
        String path = null;

        if (!isTopVolumeADataVolume) {
            if (parts.length < 1) {
                throw new Exception("Path variable 'ownerName' and 'userVolume' were not provided");
            }
            if (parts.length < 2) {
                throw new Exception("Path variable 'userVolume' was not provided");
            }
            ownerName = parts[1];
            userVolume = parts[2];
            offset = 3;
        }

        if (parts.length > offset) {
            parts = Arrays.copyOfRange(parts, offset, parts.length);
            path = String.join("/", parts);
        }
        return new Quartet<String, String, String, String>(topVolume, ownerName, userVolume, path);
    }

    /**
     * Get volumes by user.
     */
    private FileServiceModel getVolumes(AuthenticatedUser user) throws Exception {
        createDefaultUserVolumes(user);
        return racmClient.getDetailsOfFileService(user.getToken(), fileServiceIdentifier);
    }

    /**
     * Get Root volume path on file system.
     */
    public String getRootVolumePathOnFileSystem(String rootVolume) throws Exception {
        Set<RegisteredRootVolumeModel> rootVolumes = fileService.getRootVolumes();
        for (RegisteredRootVolumeModel rootVol : rootVolumes) {
            if (rootVol.getName().equals(rootVolume)) {
                return rootVol.getPathOnFileSystem();
            }
        }
        throw new Exception("Volume " + rootVolume + " could not be found");
    }

    /**
     * Create default user volumes.
     */
    private void createDefaultUserVolumes(AuthenticatedUser requestorUser) throws Exception {
        /*
         * Verifies if topVolume is a data volume or not. Lazily creates local paths of
         * default user volumes if they do not exist, and registers recently-created
         * default user volumes in racm
         */
        // creating default volumes in file system,in case they don't exists
        ArrayNode defaultVolumes = config.getDefaultUserVolumes();
        for (RegisteredRootVolumeModel rootVol : fileService.getRootVolumes()) {
            for (int i = 0; i < defaultVolumes.size(); i++) {
                JsonNode defaultVolume = defaultVolumes.get(i);
                String defaultRootVolumeName = defaultVolume.get("rootVolume").asText();
                String rootVolumeName = rootVol.getName();
                if (rootVolumeName.equals(defaultRootVolumeName)) {
                    String dir = rootVol.getPathOnFileSystem().endsWith("/") ? rootVol.getPathOnFileSystem()
                            : rootVol.getPathOnFileSystem() + "/";
                    dir = dir + requestorUser.getUserId();
                    File baseDir = new File(dir);
                    if (!baseDir.exists()) {
                        createVolumePathInFileSystem(baseDir); // creates volume path for the user in file system, with
                                                               // appropriate
                                                               // permissions
                    }
                    String defaultUserVolumeName = defaultVolume.get("userVolume") != null
                            ? defaultVolume.get("userVolume").asText()
                            : "default";
                    String defaultUserVolumeDescription = defaultVolume.get("description") != null
                            ? defaultVolume.get("description").asText()
                            : "Default user volume";
                    String basePathInFileSystem = rootVol.getPathOnFileSystem().endsWith("/")
                            ? rootVol.getPathOnFileSystem()
                            : rootVol.getPathOnFileSystem() + "/";
                    String userVolumeNameInFileSystem = defaultUserVolumeName;
                    String relativePath = requestorUser.getUserId() + "/" + userVolumeNameInFileSystem + "/";
                    String pathForLogMessage = getPathForLogMessage(rootVolumeName, requestorUser.getUserName(),
                            defaultUserVolumeName, "");
                    File userVolumeFile = new File(basePathInFileSystem + relativePath);
                    if (!userVolumeFile.exists()) {
                        createUserVolume(requestorUser.getUserId(), basePathInFileSystem, rootVolumeName,
                                userVolumeNameInFileSystem, true, pathForLogMessage);
                        racmClient.newUserVolume(requestorUser.getToken(), fileServiceIdentifier, rootVolumeName,
                                new RegisterNewUserVolumeModel(defaultUserVolumeName,
                                        defaultUserVolumeDescription,
                                        relativePath,
                                        requestorUser.getUserName()));
                    }
                }
            }
        }
    }

    /**
     * Delete file.
     */
    private void deleteFile(User ownerUser, String topVolume, String path, Boolean quiet, String pathForLogMessage,
            Boolean isTopVolumeADataVolume) throws Exception {
        try {
            String fullPath = getFullPath(topVolume, path, ownerUser, isTopVolumeADataVolume);

            Path p = FileSystems.getDefault().getPath(fullPath);
            if (!Files.exists(p) && !quiet) {
                throw new Exception("It does not exist.");
            }
            FileSystemUtils.deleteRecursively(p.toFile());
        } catch (Exception e) {
            throw new Exception(
                    "Error when deleting '" + pathForLogMessage + "'. "
                            + (e.getMessage() != null ? e.getMessage() : ""),
                    e);
        }
    }

    /**
     * Delete user volume.
     */
    private void deleteUserVolume(User user, String folder, String path, Boolean quiet, String pathForLogMessage)
            throws Exception {
        try {
            String fullPath = getFullPath(folder, path, user);

            Path p = FileSystems.getDefault().getPath(fullPath);
            if (!Files.exists(p) && !quiet) {
                throw new Exception("It does not exist.");
            }
            FileSystemUtils.deleteRecursively(p.toFile());
        } catch (Exception e) {
            throw new Exception(
                    "Error when deleting '" + pathForLogMessage + "'. "
                            + (e.getMessage() != null ? e.getMessage() : ""),
                    e);
        }
    }

    /**
     * Move file.
     */
    private void moveFile(User ownerUser, User destinationOwnerUser, String topVolume, String sourcePath,
            String destinationTopVolume, String destinationPath, Boolean doCopy, Boolean replaceExistingFile,
            String pathForLogMessage, String newPathForLogMessage, Boolean isTopVolumeADataVolume,
            Boolean isDestinationTopVolumeADataVolume) throws Exception {
        try {
            String sourcePathString = getFullPath(topVolume, sourcePath, ownerUser, isTopVolumeADataVolume);
            String destinationPathString = getFullPath(destinationTopVolume, destinationPath, destinationOwnerUser,
                    isDestinationTopVolumeADataVolume);

            Path sourceFilePath = Paths.get(sourcePathString);
            Path destinationFilePath = Paths.get(destinationPathString);
            File sourceFile = new File(sourcePathString);
            File destinationFile = new File(destinationPathString);

            if (!sourceFile.exists()) {
                throw new Exception("Source file or directory does not exist.");
            }

            mkdirsFor(destinationFile, config.getDefaultDirPerms());

            if (sourceFile.isDirectory()) {
                if (destinationFile.exists()) {
                    throw new Exception("Destination directory already exists.");
                }
                if (doCopy) {
                    FileUtils.copyDirectory(sourceFile, destinationFile);// if the new directory exists already, it does
                                                                         // a merge.
                } else {
                    FileUtils.moveDirectory(sourceFile, destinationFile);// if the new directory exists already, it
                                                                         // throws an
                                                                         // exception.
                }
                setPermissionsRecursively(
                        destinationFile, config.getDefaultFilePerms(), config.getDefaultDirPerms());
            } else {
                if (doCopy) {
                    if (replaceExistingFile) {
                        Files.copy(sourceFilePath, destinationFilePath, StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        if (destinationFile.exists()) {
                            throw new Exception("Destination file already exists");
                        } else {
                            Files.copy(sourceFilePath, destinationFilePath);
                        }
                    }
                } else {
                    if (replaceExistingFile) {
                        Files.move(sourceFilePath, destinationFilePath, StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        if (destinationFile.exists()) {
                            throw new Exception("Destination file already exists");
                        } else {
                            Files.move(sourceFilePath, destinationFilePath);
                        }
                    }

                }
                setPermissions(destinationFile, config.getDefaultFilePerms());
            }
        } catch (Exception e) {
            throw new Exception("Error when moving '" + pathForLogMessage + "' to '" + newPathForLogMessage + "'. "
                    + (e.getMessage() != null ? e.getMessage() : ""), e);
        }
    }

    /**
     * Get file for download and send it back with http response.
     */
    private void getFile(
            User ownerUser, String topVolume, String path, OutputStream output, Boolean inline,
            String pathForLogMessage,
            HttpServletResponse response, Boolean isTopVolumeADataVolume) throws Exception {
        try {
            File file = new File(getFullPath(topVolume, path, ownerUser, isTopVolumeADataVolume));
            if (file.exists() && file.isFile()) {
                long fileSize = file.length();
                String fileName = file.getName();
                String contentType = null;
                if (!inline) {
                    response.setHeader("Content-disposition", "attachment; filename=\"" + fileName + "\"");
                }
                contentType = tika.detect(file);
                response.setContentType(contentType == null ? "application/octet-stream" : contentType);
                response.setHeader("Content-Length", "" + fileSize);
                response.setHeader("Etag", "W/" + file.lastModified());
                InputStream input = new FileInputStream(file);
                IOUtils.copy(input, output);
                input.close();
                output.flush();
            }
        } catch (Exception e) {
            throw new Exception(
                    "Error when getting file '" + pathForLogMessage + "'. "
                            + (e.getMessage() != null ? e.getMessage() : ""),
                    e);
        }
    }

    /**
     * Create file.
     */
    private File createFile(User ownerUser, String topVolume, String path, InputStream input, Boolean quiet,
            String pathForLogMessage, Boolean isTopVolumeADataVolume) throws Exception {

        // TODO if the file is to be written to a shared folder, we must check whether
        // user is allowed to write there.

        try {
            String fullPath = getFullPath(topVolume, path, ownerUser, isTopVolumeADataVolume);

            File f = new File(fullPath);
            if (f.exists() && !quiet) {
                throw new Exception("File already exists.");
            }

            mkdirsFor(f, config.getDefaultDirPerms());

            try {
                OutputStream output = new FileOutputStream(f);
                IOUtils.copy(input, output);
                input.close();
                output.close();
            } catch (IOException ex) { // clean up possibly corrupted file
                if (f.exists()) {
                    FileSystemUtils.deleteRecursively(f);
                }
                throw ex;
            }

            // set permissions
            setPermissions(f, config.getDefaultFilePerms());
            return f;
        } catch (Exception e) {
            throw new Exception(
                    "Error when creating '" + pathForLogMessage + "'.  "
                            + (e.getMessage() != null ? e.getMessage() : ""),
                    e);
        }
    }

    /**
     * Set permissions on file.<br/>
     * Take into account differences between windows and posix file systems.
     *
     * @param f
     * @param permissions
     * @throws IOException
     */
    private void setPermissions(File f, Set<PosixFilePermission> permissions) throws IOException {
        if (config.isWindows()) {
            for (PosixFilePermission p : permissions) {
                switch (p) {
                    case OWNER_READ:
                        f.setReadable(true);
                        break;
                    case OWNER_WRITE:
                        f.setWritable(true);
                        break;
                    case OWNER_EXECUTE:
                        f.setExecutable(true);
                        break;
                    default: // ignore
                        break;
                }
            }
        } else {
            Path p = f.toPath();
            Files.setPosixFilePermissions(p, permissions);
        }
    }

    /**
     * Set permissions.
     */
    private void setPermissionsRecursively(File f, Set<PosixFilePermission> filePermissions,
            Set<PosixFilePermission> dirPermissions) throws IOException {
        if (f.isFile()) {
            setPermissions(f, filePermissions);
        } else {
            setPermissions(f, dirPermissions);
            File[] files = f.listFiles();
            for (File file : files) {
                setPermissionsRecursively(file, filePermissions, dirPermissions);
            }
        }
    }

    /**
     * Create directories up to f, and set their permissions.<br/>
     *
     * @param f
     */
    private void mkdirsFor(File f, Set<PosixFilePermission> perms) throws IOException {
        File dir = f.getParentFile();
        if (!dir.exists()) {
            mkdirsFor(dir, perms);
            dir.mkdir();
            setPermissions(dir, perms);
        }
    }

    /**
     * Create user volume.
     */
    private void createUserVolume(String userId, String basePathInFileSystem, String rootVolume,
            String userVolumeNameInFileSystem, Boolean quiet, String pathForLogMessage) throws Exception {
        if (quotaManagerService != null) {
            Response<Void> response = quotaManagerService
                    .createVolume(new ManagerVolumeDTO(rootVolume, userId + "/" + userVolumeNameInFileSystem))
                    .execute();
            if (!response.isSuccessful()) {
                throw new Exception("Error when creating directory '" + pathForLogMessage + "'. " + response.message());
            }
        } else {
            createDir(basePathInFileSystem, userId, rootVolume, userVolumeNameInFileSystem, quiet, pathForLogMessage);
        }
    }

    /**
     * Create directory.
     */
    private void createDir(String basePathInFileSystem, String ownerUserId, String rootVolume, String path,
            Boolean quiet,
            String pathForLogMessage) throws Exception {
        try {
            File d = new File(getFullPathInFileSystem(basePathInFileSystem, path, ownerUserId));
            if (d.exists() && !quiet) {
                throw new Exception("Directory already exists.");
            }

            Set<PosixFilePermission> perms = config.getDefaultDirPerms();
            mkdirsFor(d, perms);
            d.mkdir();
            setPermissions(d, perms);
        } catch (Exception e) {
            throw new Exception("Error when creating directory '" + pathForLogMessage + "'. "
                    + (e.getMessage() != null ? e.getMessage() : ""), e);
        }
    }

    /**
     * Create directorty.
     */
    private void createDir(User ownerUser, String topVolume, String path, Boolean quiet, String pathForLogMessage,
            Boolean isTopVolumeADataVolume) throws Exception {
        try {
            String fullPath = getFullPath(topVolume, path, ownerUser, isTopVolumeADataVolume);
            File d = new File(fullPath);
            if (d.exists() && !quiet) {
                throw new Exception("Directory already exists.");
            }

            Set<PosixFilePermission> perms = config.getDefaultDirPerms();
            mkdirsFor(d, perms);
            d.mkdir();
            setPermissions(d, perms);
        } catch (Exception e) {
            throw new Exception("Error when creating directory '" + pathForLogMessage + "'. "
                    + (e.getMessage() != null ? e.getMessage() : ""), e);
        }
    }

    /**
     * Get file tree for Volume.
     */
    private FileTree fileTree(User ownerUser, String topVolume, String path, short level, String options,
            String pathForShowing, String replaceTopDirName, String pathForLogMessage, Boolean isTopVolumeADataVolume)
            throws Exception {

        String queryPath = "";
        try {
            String _path = getFullPath(topVolume, path, ownerUser, isTopVolumeADataVolume);
            File file = new File(_path);
            if (!file.exists()) {
                throw new Exception("Path does not exist.");
            }

            if (isTopVolumeADataVolume) {
                queryPath = topVolume + "/" + ((path != null) ? path : "");
            } else {
                queryPath = topVolume + "/" + ownerUser.getUserName()
                        + (pathForShowing != null ? "/" + pathForShowing : "");
            }

            FileTree ft = new FileTree(queryPath, level, options, replaceTopDirName);
            Set<FileVisitOption> op = Collections.<FileVisitOption>emptySet();
            Files.walkFileTree(Paths.get(_path), op, level, ft);
            return ft;
        } catch (Exception e) {
            throw new Exception("Error when listing contents of '" + pathForLogMessage + "'. "
                    + (e.getMessage() != null ? e.getMessage() : ""), e);
        }
    }

    /**
     * Get directory listing.
     */
    private DirectoryListing getDirectoryListing(User ownerUser, String topVolume, String path, String pattern,
            String pathForShowing,
            String pathForLogMessage, Boolean isTopVolumeADataVolume) throws Exception {

        try {
            String _path = getFullPath(topVolume, path, ownerUser, isTopVolumeADataVolume);
            File file = new File(_path);
            if (!file.exists()) {
                throw new Exception("Path does not exist.");
            }

            String queryPath;
            if (isTopVolumeADataVolume) {
                queryPath = topVolume + "/" + ((path != null) ? path : "");
            } else {
                queryPath = topVolume + "/" + ownerUser.getUserName()
                        + (pathForShowing != null ? "/" + pathForShowing : "");
            }

            PathMatcher matcher = null;
            if (!StringUtils.isEmpty(pattern)) {
                matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            }

            BasicFileAttributes attr;
            ArrayList<FileProperties> files = new ArrayList<FileProperties>();
            ArrayList<FolderProperties> folders = new ArrayList<FolderProperties>();

            for (File f : file.listFiles()) {
                if (matcher == null || (matcher != null && matcher.matches(f.toPath()))) {
                    attr = Files.readAttributes(f.toPath(), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                    if (f.isDirectory()) {
                        folders.add(
                                new FolderProperties(f.getName(), attr.lastModifiedTime().toString(),
                                        attr.creationTime().toString()));
                    } else {
                        files.add(new FileProperties(f, attr));
                    }
                }
            }
            attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            DirectoryProperties dirDetails = new DirectoryProperties(file.getName(), attr.lastModifiedTime().toString(),
                    attr.creationTime().toString(), folders, files);
            DirectoryListing dirListing = new DirectoryListing(dirDetails, queryPath);
            return dirListing;
        } catch (Exception e) {
            throw new Exception("Error when listing contents of '" + pathForLogMessage + "'. "
                    + (e.getMessage() != null ? e.getMessage() : ""), e);
        }
    }

    /**
     * Determine base path for root folder and user. Create if it does not yet
     * exist.<br/>
     *
     * @param folder
     * @param user
     * @return
     * @throws Exception
     */
    private String getBasePath(String folder, User user) throws Exception {
        String basepath = null;
        RegisteredFileServiceModel fileService = config.getFileService();

        if (fileService.getRootVolumes().size() > 0) {
            for (RegisteredRootVolumeModel rootVolume : fileService.getRootVolumes()) {
                if (rootVolume.getName().equals(folder)) {
                    basepath = rootVolume.getPathOnFileSystem();
                }
            }
        } else {
            throw new Exception("FileService does not contain any volumes");
        }
        if (basepath == null) {
            throw new Exception("Fileservice does not contain root folder '" + folder + "'");
        }

        basepath = basepath + user.getUserId();

        File bp = new File(basepath);
        if (!bp.exists()) {
            bp.mkdirs();
        }
        return basepath;
    }

    /**
     * Get base path for volume.
     */
    private String getBasePath(String topVolume, User ownerUser, Boolean isTopVolumeADataVolume) throws Exception {

        String basepath = null;
        if (isTopVolumeADataVolume) {
            for (RegisteredDataVolumeModel dataVol : fileService.getDataVolumes()) {
                if (dataVol.getName().equals(topVolume)) {
                    basepath = dataVol.getPathOnFileSystem().endsWith("/") ? dataVol.getPathOnFileSystem()
                            : dataVol.getPathOnFileSystem() + "/";
                }
            }
        } else {
            for (RegisteredRootVolumeModel rootVol : fileService.getRootVolumes()) {
                if (rootVol.getName().equals(topVolume)) {
                    basepath = rootVol.getPathOnFileSystem().endsWith("/") ? rootVol.getPathOnFileSystem()
                            : rootVol.getPathOnFileSystem() + "/";
                }
            }
        }

        if (basepath == null) {
            throw new Exception("Fileservice does not contain top volume '" + topVolume + "'");
        }

        // does not need to create dirs, since the top volumes should already be in the
        // file system when they are registered

        /*
         * File bp = new File(basepath);
         * if(!bp.exists()){
         * bp.mkdirs();
         * }
         */
        return basepath;
    }

    /**
     * Get relative path in file system.
     */
    private String getRelativePathInFileSystem(String path, String userId) {
        return userId + "/" + ((path != null) ? path : "");
    }

    /**
     * Get full path in file system.
     */
    private String getFullPathInFileSystem(String basePath, String relativePath, String userId) {
        String relativePathInFileSystem = getRelativePathInFileSystem(relativePath, userId);

        String[] pathParts = relativePathInFileSystem.split("/");
        String basePathToCheck = basePath + pathParts[0] + "/" + pathParts[1];
        assertPathIsInBasePath(basePath, relativePathInFileSystem, basePathToCheck);
        return basePath + "/" + relativePathInFileSystem;
    }

    /**
     * Get full path.
     */
    private String getFullPath(String rootVolume, String path, User user) throws Exception {
        path = (path != null) ? path : "";
        String basePath = getBasePath(rootVolume, user);
        String[] pathParts = path.split("/");
        String basePathToCheck = basePath + "/" + pathParts[0];
        assertPathIsInBasePath(basePath, path, basePathToCheck);
        return basePath + "/" + path;
    }

    /**
     * Get full path.
     */
    private String getFullPath(String topVolume, String path, User ownerUser, boolean isTopVolumeADataVolume)
            throws Exception {
        path = (path != null) ? path : "";
        String basePath = getBasePath(topVolume, ownerUser, isTopVolumeADataVolume);

        String basePathToCheck = basePath;
        if (isTopVolumeADataVolume == false) {
            String[] pathParts = path.split("/");
            basePathToCheck = basePathToCheck + pathParts[0] + "/" + pathParts[1];
        }

        assertPathIsInBasePath(basePath, path, basePathToCheck);
        return basePath + path;
    }

    /**
     * Is path base path.
     */
    private void assertPathIsInBasePath(String base, String path, String basePathToCheck) {
        Path basePath = Paths.get(basePathToCheck);
        Path fullPath;
        try {
            fullPath = Paths.get(base, path).toFile().getCanonicalFile().toPath();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (!fullPath.startsWith(basePath)) {
            throw new IllegalStateException("Attempted to access a path outside of the requested volume");
        }
    }

    /**
     * General check for permissions over a file.
     */
    private Quintet<Boolean, String, String, String, String> generalCheck(AuthenticatedUser requestorUser,
            String topVolume, ArrayList<String> permissions, String baseUri, HttpServletRequest request)
            throws Exception {
        Boolean isTopVolumeARootVolume = checkIfTopVolumeIsRootVolume(topVolume);
        Boolean isTopVolumeADataVolume = !isTopVolumeARootVolume;
        Triplet<String, String, String> triplet = getInfoFromUriTemplate(baseUri, topVolume, isTopVolumeADataVolume,
                request);
        String ownerName = triplet.x1;
        String userVolume = triplet.x2;
        String path = triplet.x3;

        return generalCheck(requestorUser, topVolume, permissions, userVolume, ownerName, path);
    }

    /**
     * General check for permissions over a file.
     */
    private Quintet<Boolean, String, String, String, String> generalCheck(AuthenticatedUser requestorUser,
            String topVolume, ArrayList<String> permissions, String userVolume, String ownerName, String path)
            throws Exception {
        Boolean isTopVolumeARootVolume = checkIfTopVolumeIsRootVolume(topVolume);
        Boolean isTopVolumeADataVolume = !isTopVolumeARootVolume;
        String rootVolume = !isTopVolumeADataVolume ? topVolume : null;
        String dataVolume = isTopVolumeADataVolume ? topVolume : null;
        // create default user volumes, if needed
        createDefaultUserVolumes(requestorUser);

        String topVolumeBasePath = null;
        if (!isTopVolumeADataVolume) {

            // check that the user volume already exists in the rootVolume as RACM knows it
            // (case of root volume that can contain shared user volumes
            if (ownerName == null || ownerName.equals("")) {
                throw new Exception("owner name is not present");
            } else if (userVolume == null || userVolume.equals("")) {
                throw new Exception("user volume is not present");
            } else {
                topVolumeBasePath = racmClient.queryUserVolumeActions(
                        requestorUser.getToken(), fileServiceIdentifier, rootVolume, ownerName, userVolume)
                        .requireActions(permissions).getRelativePath();
            }
        } else {
            topVolumeBasePath = racmClient.queryDataVolumeActions(
                    requestorUser.getToken(), fileServiceIdentifier, dataVolume)
                    .requireActions(permissions).getRelativePath();
        }
        return new Quintet(isTopVolumeADataVolume, topVolumeBasePath, ownerName, userVolume, path);
    }

    /**
     * Get path for log message.
     */
    String getPathForLogMessage(String rootVolume, String ownerName, String userVolume, String path,
            Boolean isTopVolumeADataVolume, String dataVolume) {
        if (isTopVolumeADataVolume) {
            return dataVolume + "/" + path;
        } else {
            return rootVolume + "/" + ownerName.toString() + "/" + userVolume.toString() + "/" + path;
        }
    }

    /**
     * Get path for log message.
     */
    String getPathForLogMessage(String rootVolume, String ownerName, String userVolume, String path) {
        return rootVolume + "/" + ownerName + "/" + userVolume + "/" + path;
    }

    /**
     * Create volume path in file system.
     */
    private void createVolumePathInFileSystem(File dir) throws Exception {
        mkdirsFor(dir, config.getDefaultDirPerms());
        dir.mkdir();
    }

    /***
     * Checks if a new user volume can be created inside the root Folder and throws
     * an exception if not.
     *
     * @param rootFolder
     * @param token
     * @throws Exception
     */
    public void checkIfCanCreateUserVolumeInsideRootFolder(JsonNode rootFolders, User user, String rootFolder,
            String token) throws Exception {

        boolean containsRootFolder = false;
        boolean isShareable = false;
        Boolean canCreate = false;
        if (rootFolders.get("rootVolumes") != null) {
            ArrayNode rootVolumes = (ArrayNode) rootFolders.get("rootVolumes");

            Iterator<JsonNode> iterator = rootVolumes.elements();
            while (iterator.hasNext()) {
                JsonNode rootVolume = iterator.next();
                String name = rootVolume.get("name").asText();
                if (name.equals(rootFolder)) {
                    containsRootFolder = true;
                    isShareable = rootVolume.get("containsSharedVolumes").asBoolean();
                    ArrayNode allowedActions = (ArrayNode) rootVolume.get("allowedActions");
                    for (int i = 0; i < allowedActions.size(); i++) {
                        if (allowedActions.get(i).asText().equals("create")) {
                            canCreate = true;
                            String dir = rootVolume.get("pathOnFileSystem").asText() + user.getUserId();
                            File baseDir = new File(dir);
                            if (!baseDir.exists()) {
                                createVolumePathInFileSystem(baseDir);
                            }
                        }
                    }
                }
            }
        } else {
            throw new Exception(
                    "FileService '" + config.getFileService().getName() + "' does not contain root volumes");
        }
        if (!containsRootFolder) {
            throw new Exception(
                    "FileService '" + config.getFileService().getName() + "' does not contain root volume '"
                            + rootFolder + "'");
        }
        if (!isShareable || !canCreate) {
            throw new Exception(
                    user.getUserName() + " is not allowed to create a user volume inside root volume '" + rootFolder
                            + "'");
        }
    }

    /**
     * Log some info.
     *
     * @TODO why 3 separate messages?
     * @param requestURI
     */
    private void logInfo(User user, String requestURI, String message) {
        if (LOG.isInfoEnabled() && user != null) {
            String loggedMessage = String.format("User name: %s", user.getUserName()) + " " +
                    String.format("User ID: %s", user.getUserId()) + " " +
                    String.format("Request URI: %s", requestURI) + " " +
                    String.format("Message: %s", message);
            LOG.info(loggedMessage);
        }
    }

    /**
     * Send a simple message to the file service logger.
     *
     * @param message
     */
    private void logSimpleMessage(String message) {
        Message m = Log.getLogger().createFileServiceMessage(message, false);
        try {
            Log.getLogger().SendMessage(m);
        } catch (Exception e) {
        }
    }

    /**
     * Log message.
     */
    private void logMessage(String messageString, String action, String rootVolume, String ownerName, String userVolume,
            String path, AuthenticatedUser user, HttpServletRequest request, ObjectNode sentence,
            Boolean doShowInUserHistory,
            Object requestBody) {
        try {

            logInfo(user, request.getRequestURI(), messageString);

            ObjectNode oNode = mapper.createObjectNode();
            oNode.put("action", messageString);
            oNode.put("sentence", sentence);
            oNode.put("rootVolume", rootVolume);
            oNode.put("ownerName", ownerName);
            oNode.put("userVolume", userVolume);
            oNode.put("path", path);
            oNode.put("requestBody", requestBody == null ? null : mapper.valueToTree(requestBody));

            if (doShowInUserHistory == null) {
                doShowInUserHistory = request.getParameter("doShowInUserHistory") == null ? true
                        : (request.getParameter("doShowInUserHistory").toLowerCase().equals("true") ? true : false);
            }

            Message message = Log.getLogger().createFileServiceMessage(oNode.toString(), doShowInUserHistory);
            Utility.fillLoggingMessage(message, user, request, "FileService." + action);
            Log.getLogger().SendMessage(message);
        } catch (Exception ignored) {
        }
    }

    /**
     * Log an error message for the specified exception and throw the
     * exception.<br/>
     *
     * @param action
     * @param path
     * @param user
     * @param request
     * @param error
     * @return
     * @throws Exception
     */
    private ResponseEntity<JsonNode> logException(String action, String rootVolume, String dataVolume, String ownerName,
            String userVolume, String path, AuthenticatedUser user, HttpServletRequest request, Exception error,
            HttpStatus http, Object requestBody) {
        return logException(action, rootVolume, dataVolume, ownerName, userVolume, path, user, request, error, http,
                requestBody, true);
    }

    /**
     * Log exception.
     */
    private ResponseEntity<JsonNode> logException(String action, String rootVolume, String dataVolume, String ownerName,
            String userVolume, String path, AuthenticatedUser user, HttpServletRequest request, Exception error,
            HttpStatus http, Object requestBody, Boolean doReturnMessage) {
        String msg = "";
        try {
            String exceptionStackTrace = "";
            String exceptionMessage = "";
            if (error != null) {
                StringWriter sw = new StringWriter();
                error.printStackTrace(new PrintWriter(sw));
                exceptionStackTrace = sw.toString();
                exceptionMessage = error.getMessage();
            }
            if (exceptionStackTrace != null && !exceptionStackTrace.isEmpty()) {
                msg = msg + exceptionStackTrace;
            } else {
                if (exceptionMessage != null && !exceptionMessage.isEmpty()) {
                    msg = msg + exceptionMessage + "\n";
                }
            }
        } catch (Exception ignored) {
        }
        logInfo(user, request.getRequestURI(), msg);

        ObjectNode oNode = mapper.createObjectNode();
        oNode.put("action", action);
        oNode.put("dataVolume", dataVolume);
        oNode.put("rootVolume", rootVolume);
        oNode.put("ownerName", ownerName);
        oNode.put("userVolume", userVolume);
        oNode.put("path", path);
        oNode.put("requestUrl", request.getRequestURL().toString());
        oNode.put("requestBody", requestBody == null ? null : mapper.valueToTree(requestBody));

        Message message = Log.getLogger().createErrorMessage(error, oNode.toString());
        Utility.fillLoggingMessage(message, user, request, "FileService." + action);

        try {
            Log.getLogger().SendMessage(message);
        } catch (Exception loggingException) {
            LOG.error("While attempting to log {}, failed sending message to SciServer Logging", action,
                    loggingException);
        }
        if (doReturnMessage) {
            return jsonExceptionEntity(error, http);
        } else {
            return new ResponseEntity<>(http);
        }
    }

    /**
     * Json exception.
     */
    protected JsonNode jsonException(Exception e) {
        return jsonException(e.getMessage());
    }

    /**
     * Json exception.
     */
    private JsonNode jsonException(String errorMessage) {
        ObjectMapper om = new ObjectMapper();
        ObjectNode on = om.createObjectNode();
        on.put("status", "error");
        on.put("error", errorMessage);
        return on;
    }

    /**
     * Json exception entity.
     */
    protected ResponseEntity<JsonNode> jsonExceptionEntity(Exception e, HttpStatus http) {
        JsonNode json = jsonException(e);
        return new ResponseEntity<>(json, http);
    }

}
