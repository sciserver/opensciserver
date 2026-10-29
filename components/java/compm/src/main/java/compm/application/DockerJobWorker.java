package compm.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import org.sciserver.racm.jobm.model.COMPMDockerJobModel;
import org.sciserver.racm.jobm.model.COMPMJobModel;
import org.sciserver.racm.jobm.model.JobMessageModel;
import org.sciserver.racm.jobm.model.JobStatus;
import org.sciserver.racm.jobm.model.VolumeContainerModel;


/**
 * The `DockerJobWorker` class extends `JobWorker` and contains methods for managing the
 * lifecycle of Docker jobs being run in Docker Containers.
 */
public class DockerJobWorker extends JobWorker {

    HttpRequest request;
    FileService fileService; 


    /**
     * Constructor for `DockerJobWorker` class. 
     *
     * @param master The JobMaster instance.
     * @param workerId The worker id. 
     * @param idleTimeBetweenJobs idle time 
     */
    public DockerJobWorker(JobMaster master, int workerId, long idleTimeBetweenJobs) {
        super(master, workerId, idleTimeBetweenJobs);
        request = new HttpRequest(Config.getInstance().getCompmId());
        fileService = new FileService();
    }


    /**
     * Runs the Docker job, including the submission of the job to the SciServer-Compute REST API, 
     * and management of the job's life cycle.
     */
    @Override
    public void runJob(COMPMJobModel job) throws Exception {
        
        COMPMDockerJobModel dockerJob = (COMPMDockerJobModel) job;
        try {
            
            deleteUnmanagedContainers(getUserToken(dockerJob));
            
            if (dockerJob.getStatus() < JobStatus.STATUS_STARTED) {
                submitJobToCompute(dockerJob);
            }
                
            
            while (true && dockerJob.getStatus() <= JobStatus.STATUS_FINISHED) {
                    
                if (isCanceled && dockerJob.getStatus() < JobStatus.STATUS_FINISHED) {
                        
                    dockerJob.setMessages(master.putNewMessageInMessagesList(dockerJob, JobMessageModel.CANCEL, 
                                    "Job was cancelled by the user", false));
                    dockerJob.setStatus(JobStatus.STATUS_FINISHED);
                    dockerJob = (COMPMDockerJobModel) updateJob(dockerJob);
                        
                    removeJob(dockerJob, JobStatus.STATUS_CANCELED);
                    break;
                        
                } else if (dockerJob.getStartTime() != null  && dockerJob.getStatus() < JobStatus.STATUS_FINISHED) {
                        
                    Date now = new Date();
                    Date start = dockerJob.getStartTime();
                    boolean hasTimedOut = start.getTime() + job.getTimeout() * 1000 < now.getTime();
                    if (hasTimedOut) {
                            
                        dockerJob.setMessages(master.putNewMessageInMessagesList(dockerJob, JobMessageModel.TIMEOUT, 
                                              "Job timed out", false));
                        dockerJob.setStatus(JobStatus.STATUS_FINISHED);
                        dockerJob = (COMPMDockerJobModel) updateJob(dockerJob);
                            
                        removeJob(dockerJob, JobStatus.STATUS_CANCELED);
                        break;
                    }
                }
                    

                Integer jobStatus = dockerJob.getStatus();
                
                if (jobStatus < JobStatus.STATUS_FINISHED) {
                
                    Integer computeStatus = getJobStatusFromCompute(dockerJob);
                    
                    if (computeStatus == JobStatus.STATUS_STARTED) {
                            
                        Thread.sleep(Config.getInstance().getIdleTimeBetweenJobmRequests() * 1000);
                        // sleep time in milliseconds.
                        continue; // job has not finished yet.
                            
                    } else if (computeStatus == JobStatus.STATUS_SUCCESS || computeStatus == JobStatus.STATUS_ERROR) {
                            
                        getJobResultsFromCompute(dockerJob, computeStatus.toString());

                        dockerJob.setStatus(JobStatus.STATUS_FINISHED);
                        dockerJob = (COMPMDockerJobModel) updateJob(dockerJob);
                        
                        removeJob(dockerJob, computeStatus);
                        break;
                    }
                        
                } else if (jobStatus == JobStatus.STATUS_FINISHED) {


                    if (isDockerContainerOpen(dockerJob)) {
                        deleteJobContainer(getUserToken(dockerJob), dockerJob.getExecutorDID(), 
                                                        dockerJob.getId().toString());
                    }

                    ArrayList<Integer> labelIndexes = master.getLabelIndexes(dockerJob.getMessages(),
                                                                             JobMessageModel.STATUS); 
                    if (labelIndexes.size() > 0) {
                        Integer index = (Integer) labelIndexes.get(0);
                        Integer status = Integer.parseInt(dockerJob.getMessages().get(index).getContent());
                        if (status == JobStatus.STATUS_ERROR) {
                            dockerJob.setStatus(status);
                        } else {
                            dockerJob.setStatus(JobStatus.STATUS_SUCCESS);
                        }
                    } else {
                        dockerJob.setStatus(JobStatus.STATUS_SUCCESS);
                    }
                    updateJob(job);
                    break;
                    
                } else {
                    throw new Exception(
                        "Job has wrong status " + jobStatus + "\n" + master.convertObjectToJsonString(dockerJob)
                    );
                }
                
                Thread.sleep(Config.getInstance().getIdleTimeBetweenJobmRequests() * 1000); 
                // sleep time in milliseconds.
                    
            } //end while
            
        } catch (Exception e) {
            master.LogMessage(
                "Fatal error running DockerJob " + job.getId() + ".\n" + master.convertObjectToJsonString(job), e
            );
            throw(e);
        } finally {
        }
    }


    /**
     * Deletes the Docker container running the job in SciServer-Compute, 
     * and then updates the job status in JOBM.
     *
     * @param job the job object.
     * @param jobStatus the `status` the job is updated to.
     */
    public void removeJob(COMPMDockerJobModel job, int jobStatus) {
        try {
            
            deleteJobContainer(getUserToken(job), job.getExecutorDID(), job.getId().toString());
            job.setStatus(jobStatus);
            updateJob(job);
            
        } catch (Exception e) {
            master.LogMessage("Could not remove job " + job.getId() + ".\n" + master.convertObjectToJsonString(job), e);
            throw(e);
        }
    }

    /**
     * Checks if a Docker container associated with a job is still running in SciServer-Compute.
     *
     * @param job the job object.
     * @return a boolean value determining if the job is running or not.
     */
    public boolean isDockerContainerOpen(COMPMDockerJobModel job) {
        ArrayList<String> openDockerContainers = getOpenDockerContainers(getUserToken(job));
        if (openDockerContainers.contains(job.getExecutorDID())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Retrieves job results (stdout and stderr) from the Docker container running the Job and
     * updates the job status and its internal messages accordingly.
     *
     * @param job the job object.
     * @param jobStatus the status the job is updtaed to.
     */
    public void getJobResultsFromCompute(COMPMDockerJobModel job, String jobStatus) {
        while (true) {
            try {
                String standardOutput = "";
                String standardError = "";
                
                Hashtable<String, String> extraHeaderFields = new Hashtable<String, String>();
                extraHeaderFields.put("X-Auth-Token", getUserToken(job));
                HttpResponseResult result = request.executeGet(
                    Config.getInstance().getComputeUrl_getJobOutput().replace("{jobId}", job.getExecutorDID()), 
                    extraHeaderFields
                );
                if (result != null && result.responseCode >= 200 && result.responseCode <= 299) {
                    // TODO:
                
                    JsonNode jsonNode = master.convertJsonStringToJsonNode(result.message);
                    standardOutput = jsonNode.get("StdOut").textValue();
                    standardError  = jsonNode.get("StdErr").textValue();
                    
                    List<JobMessageModel> messagesList = job.getMessages();
                    if (messagesList == null) {
                        messagesList = new ArrayList<JobMessageModel>();
                    }

                    //adding the Output result message
                    
                    if (!standardOutput.isEmpty() & standardOutput != null) {
                        master.putNewMessageInMessagesList(job, JobMessageModel.OUTPUT, standardOutput, false);
                    }

                    //adding the Output Error message
                    if (!standardError.isEmpty() & standardError != null) {
                        master.putNewMessageInMessagesList(job, JobMessageModel.ERROR, standardError, false);
                    }
                    
                    master.putNewMessageInMessagesList(job, JobMessageModel.STATUS, jobStatus, false);
                    return;
                    //Updating the JobResultModel of the job with the standardOutput and standardError
                    //job.setMessages(messagesList);
                    
                } else if (result.responseCode == 404) {
                    String errorMessage = "Job ended with an unexepected error. Could not retrieve job results.";
                    job.setMessages(
                        master.putNewMessageInMessagesList(job, JobMessageModel.ERROR, errorMessage, false)
                    );
                    return;
                } else {
                    Exception e = new Exception(
                        "HttpResponse code:" + result.responseCode + ".\n Message: \n" + result.message
                    );
                    throw(e);
                }
            } catch (Exception e) {
                String errorMessage = "Could not get from Docker container " + job.getExecutorDID() 
                                    + " the results of job " + job.getId() + ".";
                master.LogMessage(errorMessage, e);
                master.waitBetweenFailedRequests();
                //getJobResultsFromCompute(job, jobStatus);
            }
        }
    }

    /**
     * Deletes a Docker container where the job is running.
     *
     * @param token The SciServer authentication token of the user whose job Docker container needs to
     *     be deleted.
     * @param containerID unique identifier of the Docker container where the job is/was running. 
     * @param jobID unique identifier of the job.
     */
    public void deleteJobContainer(String token, String containerID, String jobID) {
        HttpResponseResult result = null;
        while (true) {
            try {
                Hashtable<String, String> extraHeaderFields = new Hashtable<String, String>();
                extraHeaderFields.put("X-Auth-Token", token);
                result = request.executeDelete(
                    Config.getInstance().getComputeUrl_deleteJob().replace("{jobId}", containerID), 
                    extraHeaderFields
                );
                if (result != null 
                    && ((result.responseCode >= 200 && result.responseCode <= 299) || result.responseCode == 404)
                ) {
                    return; // true;
                } else {
                    Exception e = new Exception(
                        "HttpResponse code:" + result.responseCode + ".\n Message: \n" + result.message
                    );
                    throw(e);
                }
            } catch (Exception e) {
                String errorMessage = "Could not delete Docker container " 
                                    + containerID + " running job " + jobID + ".";
                master.LogMessage(errorMessage, e);
                master.waitBetweenFailedRequests();
                //deleteJobContainer(token, containerID, jobID);
            }
        }
    }


    /**
     * Retrieves and processes the status of a job running in a Docker
     * container, seting the time duration and messages in the job object.
     *
     * @param job the job object.
     * @return the integer value representing the `JobStatus` of the job.
     */
    public int getJobStatusFromCompute(COMPMDockerJobModel job) {
        while (true) {
            try {
            
                String status = "UNKNOWN";
                Integer exitCode = null;
                String error = "UNDEFINED";
                String startedAt = "";
                String finishedAt = "";
                Boolean running = false;
                

                Hashtable<String, String> extraHeaderFields = new Hashtable<String, String>();
                extraHeaderFields.put("X-Auth-Token", getUserToken(job));
                HttpResponseResult result = request.executeGet(
                    Config.getInstance().getComputeUrl_checkJobStatus().replace("{jobId}", 
                    job.getExecutorDID()), 
                    extraHeaderFields
                );
                //TODO:
                
                if (result != null && result.responseCode >= 200 && result.responseCode <= 299) {
                    JsonNode jsonNode = master.convertJsonStringToJsonNode(result.message);
                    JsonNode params = jsonNode.get("State");
                    status = params.get("Status").textValue();
                    exitCode = params.get("ExitCode").asInt();
                    error = params.get("Error").textValue();
                    running = params.get("Running").asBoolean();
                    startedAt = params.get("StartedAt").textValue();
                    finishedAt = params.get("FinishedAt").textValue();
                    try {
                        
                        Instant startDate = Instant.parse(startedAt);
                        Instant finishedDate = Instant.parse(finishedAt);
                        double jobDuration = Duration.between(startDate, finishedDate)
                                                     .toMillis() / 1000.0; // measured in seconds
                        if (jobDuration >= 0) {
                            job.setDuration(jobDuration);
                        }
                            
                    } catch (Exception exc) { };

                    
                    //see github.com/fsouza/go-dockerclient/blob/master/container.go#L118
                    //and github.com/fsouza/go-dockerclient/blob/master/container.go#L166
                    
                    if (status.equals("exited") || !running) {
                        
                        if (exitCode == 0) {
                            return JobStatus.STATUS_SUCCESS;
                        } else {
                            String errorMessage = "Job ended with an error. Exit code: \"" + exitCode 
                                                + "\". Error message: \"" + error + "\".";
                            job.setMessages(
                                master.putNewMessageInMessagesList(job, JobMessageModel.ERROR, errorMessage, false)
                            );
                            return JobStatus.STATUS_ERROR;
                        }
                    } else {
                        if (!error.isEmpty() && error != null) {
                            String errorMessage = "Job ended with an error. Exit code = " 
                                                + exitCode + ". Error message: \"" + error + "\".";
                            job.setMessages(
                                master.putNewMessageInMessagesList(job, JobMessageModel.ERROR, errorMessage, false)
                            );
                            return JobStatus.STATUS_ERROR;
                        } else {
                            return JobStatus.STATUS_STARTED;
                        }
                    }
                } else if (result.responseCode == 404) {
                    String errorMessage = "Job ended with an unexepected error. Could not get job status.";
                    job.setMessages(
                        master.putNewMessageInMessagesList(job, JobMessageModel.ERROR, errorMessage, false)
                    );
                    return JobStatus.STATUS_ERROR;
                } else {
                    Exception e = new Exception("HttpResponse code:" + result.responseCode + ".\n" + result.message);
                    throw(e);
                }
            } catch (Exception e) {
                String errorMessage = "Could not get from Docker container " 
                                    + job.getExecutorDID() + " status of job " + job.getId() + ".";
                master.LogMessage(errorMessage, e);
                master.waitBetweenFailedRequests();
                //return getJobStatusFromCompute(job);
            }
        }
    }


    @Override
    public void cancelJob() {
        super.cancelJob();
        /*
        COMPMDockerJobModel rdbJob = (COMPMDockerJobModel)job;
        
        try {
            //sqlStatement.cancel();
            rdbJob.setStatus(JobStatus.STATUS_CANCELED);
        }
        catch (Exception e) {
            rdbJob.setStatus(JobStatus.STATUS_FINISHED);
            
            List<JobMessageModel> a = rdbJob.getMessages();
            JobMessageModel msg = new JobMessageModel();
            msg.setLabel(JobMessageModel.ERROR);
            msg.setContent("Could not cancel job. " + e.getMessage());
            a.add(msg);
        }
        finally {
            try { 
                //sqlStatement.getConnection().close(); 
            }catch (Exception e) { }
            
            updateJob(rdbJob);
        }
        */
    }


    /**
    * The `submitJobToCompute` function submits a Docker job to the SciServer-Compute REST API.
    *
    * @param job job object of class `COMPMDockerJobModel`.
    */
    public void submitJobToCompute(COMPMDockerJobModel job) {
        
        if (job.getStatus() > JobStatus.STATUS_STARTED) { 
            // means that the Job was already managed and submitted to compute 
            // by this compm, so there is no need to submit to compute again.
            return; // true;
        }

        String [] shellCommand = new String[3];
        String token = getUserToken(job);
        try {
            

            //set jobs directory, and creates it in the file system
            master.setAndCreateResultsFolderForJob(job);

            // setting paths for job directory:
            String fullPathJobDirectory = "/" + job.getResultsFolderURI().replaceAll("^/+", "")
                                                .replaceAll("/+$", "") + "/";
            String escapedPath = fullPathJobDirectory.replace("'", "'\\''");
            String relativeJobDirUrl = fullPathJobDirectory.replace(Config.getInstance().getFileServiceBasePath(), "");
            if (!fullPathJobDirectory.startsWith(Config.getInstance().getFileServiceBasePath())) {
                throw new Exception(
                    "Invalid directory '" + fullPathJobDirectory + "' is not located under '" 
                    + Config.getInstance().getFileServiceBasePath() + "'"
                );
            }

            String [] urlPieces = relativeJobDirUrl.split("/");
            if (urlPieces.length <= 3) { // relativeJobDirUrl = rootVolume/ownerName/userVolume/path...
                throw new Exception("Invalid job directory '" + fullPathJobDirectory + "'");
            }
                
            String destinationRootVolume = urlPieces[0]; 
            String destinationOwnerName  = urlPieces[1];
            String destinationUserVolume = urlPieces[2];
            String destinationRelativeUrl =  String.join("/", Arrays.copyOfRange(urlPieces, 3, 
                                                         urlPieces.length)) + "/"; 
            //  destinationRootVolume + "/" + destinationOwnerName + "/" + destinationUserVolume + "/"; 

            //getting the executables...
            String command = job.getCommand() == null ? "" : job.getCommand();
            String dockerImageName = job.getDockerImageName();
            String scriptURI = job.getScriptURI() == null ? "" : job.getScriptURI();
            
            //getting the volumes selected by the user
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode vols = mapper.createArrayNode();

            List<VolumeContainerModel> volumes = job.getVolumeContainers();
            if (volumes != null) {
                for (int i = 0; i < volumes.size(); i++) {
                    vols.add(volumes.get(i).getName());
                }
            }
            
            String commandPreamble = String.format(
                "echo %s > /home/idies/keystone.token;" 
                + "cd %s;" 
                + "pwd > /home/idies/jobs.path;" 
                + "PATH=/home/idies/miniconda3/bin:/home/idies/anaconda3/bin:"
                + "/home/idies/ds_apps0/matlab-r2013a/bin/:$PATH;"
                + "if [[ $(id -u) == 0 ]]; then RUNNER=\"sudo -E -u idies env PATH=$PATH \"; "
                + "else RUNNER=\"env PATH=$PATH \" ; fi;",
                token, escapedPath
            );

            String outputReadme = "echo -e '# Stdout\n```' >> README.md;" 
                                + "cat stdout.txt >> README.md;" 
                                + "echo -e '```\n\n# Stderr\n```' >> README.md;" 
                                + "cat stderr.txt >> README.md;" 
                                + "echo '```' >> README.md";

            if (command != null && !command.isEmpty() && (scriptURI == null || scriptURI.isEmpty())) { 
                // command line job

                //upload command into a file in JobFolder. This creates the JobFolder as well.
                String commandFile = "command.txt";
                String url = relativeJobDirUrl + commandFile;
                
                fileService.uploadContentToFile(url, token, command);

                
                //set docker command
                shellCommand[0] = "bash";
                shellCommand[1] = "-c";
                shellCommand[2] = commandPreamble + String.format(
                    "$RUNNER bash %s >> stdout.txt 2>> stderr.txt;" 
                    + "EXIT=$?;" 
                    + "if [[ -f README.md ]]; then exit $EXIT; fi;" 
                    + "echo > README.md; %s;" 
                    + "exit $EXIT",
                    commandFile, 
                    outputReadme
                );
                
            } else if (scriptURI != null && !scriptURI.isEmpty()) { // notebook job

                //make sure the script is under the base path
                if (!scriptURI.startsWith("/")) {
                    scriptURI = "/" + scriptURI;
                }
                    
                if (!scriptURI.startsWith(Config.getInstance().getFileServiceBasePath())) {
                    throw new Exception(
                        "Invalid file '" + scriptURI + "' is not located under '"
                        + Config.getInstance().getFileServiceBasePath() + "'"
                    );
                }
                    

                String relativeScriptUrl = scriptURI.replace(Config.getInstance().getFileServiceBasePath(), "");

                
                String scriptName = scriptURI.substring(scriptURI.lastIndexOf("/") + 1, scriptURI.length());
                String originalScriptName = "";
                if (scriptName.endsWith(".ipynb")) {
                    originalScriptName = scriptName.replace(".ipynb", ".original.ipynb");
                } else {
                    originalScriptName = scriptName + ".original.ipynb";
                }
                    
                String fullPathToScriptInResultsDir = fullPathJobDirectory + scriptName;
                fullPathToScriptInResultsDir  = "'" + fullPathToScriptInResultsDir.replace("'", "'\\''") + "'";

                urlPieces = relativeScriptUrl.split("/"); // relativeScriptUrl = rootVolume/ownerName/userVolume/path...
                if (urlPieces.length <= 1) {
                    throw new Exception("Invalid file '" + scriptURI + "'");
                }
                
                //String relativeScriptDestinationPath = 
                // scriptURI.replace(Config.getInstance().getFileServiceBasePath() 
                // + urlPieces[0] + "/" + urlPieces[1] + "/" + urlPieces[2] + "/",  "").replaceAll(scriptName, "");
                
                //relativeScriptDestinationPath = fullPathToScriptInResultsDir.replace(  , newChar)
                //upload parameters into a file in JobFolder. This creates the JobFolder as well.
                String paramFile = "parameters.txt";
                String url = relativeJobDirUrl + paramFile;
                fileService.uploadContentToFile(url, token, command);

                //upload original notebook into job directory:
                url = relativeScriptUrl;
                fileService.moveFile(url, token, destinationRootVolume, destinationOwnerName, 
                                     destinationUserVolume, destinationRelativeUrl + originalScriptName);
                fileService.moveFile(url, token, destinationRootVolume, destinationOwnerName, 
                                     destinationUserVolume, destinationRelativeUrl + scriptName);
                //set Docker command
                shellCommand[0] = "bash";
                shellCommand[1] = "-c";
                shellCommand[2] = commandPreamble + String.format(
                    "$RUNNER jupyter nbconvert --to notebook --ExecutePreprocessor.allow_errors=False " 
                    + "  --ExecutePreprocessor.timeout=%d --execute %s --output %s >> stdout.txt 2>> stderr.txt;" 
                    + "EXIT=$?;" 
                    + "if [[ -f README.md ]]; then exit $EXIT; fi;" 
                    + "$RUNNER jupyter nbconvert --to markdown --output README.md %s;" 
                    + "echo ___ >> README.md; %s;" 
                    + "exit $EXIT",
                    job.getTimeout(), fullPathToScriptInResultsDir, fullPathToScriptInResultsDir, 
                    fullPathToScriptInResultsDir, outputReadme
                );

            } else {
                
                //upload "Empty job" into a file in JobFolder. This creates the JobFolder as well.
                String url = relativeJobDirUrl + "emptyJob.txt";
                fileService.uploadContentToFile(url, token, "This jobs is empty.");
                
                shellCommand[0] = "bash";
                shellCommand[1] = "-c";
                shellCommand[2] = commandPreamble;
            }
            
        } catch (Exception e) {
            
            // updates the job with an error and ends the job execution.
            
            String errorMessage = "Could not set job directory environment.";
            master.LogMessage(errorMessage, e);
            job.setMessages(master.putNewMessageInMessagesList(
                job, JobMessageModel.ERROR, errorMessage + " " + e.getMessage(), false
            ));
            job.setStatus(JobStatus.STATUS_ERROR);
            updateJob(job);
            return;
        }

        Long loop = 1L;
        while (true) {
            try {
                
                job.setFullDockerCommand(shellCommand);
                String computeJobString = master.convertObjectToJsonString(job);
                
                
                //HttpResponseResult result = request.executePost(this.computeUrl_submitJob, computeJob, 
                // "application/json");
                Hashtable<String, String> extraHeaderFields = new Hashtable<String, String>();
                extraHeaderFields.put("Content-Type", "application/json");
                extraHeaderFields.put("X-Auth-Token", token);
                
                HttpResponseResult result = request.executePost(
                    Config.getInstance().getComputeUrl_submitJob(), extraHeaderFields, computeJobString
                );
                Date time = new Date();
                time.setTime(time.getTime());
                if (result != null && result.responseCode >= 200 && result.responseCode <= 299) { 
                    //successful submission.
                    
                    //String uJobId = getJobId(result);
                    String userJobId = result.message.replace("\r", "")
                                                  .replace("\n", "")
                                                  .replace(" ", "").trim();
                    if (userJobId.isEmpty()) { //can't get compute's jobId 
                        Exception e = new Exception(
                            "\nExecutorDID from SciServer-Compute response is unreadable. HttpResponse code:" 
                            + result.responseCode + ".\n" + result.message
                        );
                        throw(e);
                    }

                    job.setExecutorDID(userJobId);
                    job.setStartTime(time); // maybe not necessary since jobm sets the start time
                    job.setStatus(JobStatus.STATUS_STARTED);

                    job = (COMPMDockerJobModel) updateJob(job);

                    return; // true;

                } else { //cannot submit, so job should be updated with an error.
                    Exception e = new Exception(
                        "Unsuccessful response from SciServer-Compute API. HttpResponse code:" 
                        + result.responseCode + ".\n" + result.message
                    );
                    throw(e);
                }
            } catch (Exception e) {
                String errorMessage = "Could not sucessfully submit job " + job.getId() + " to SciServer-Compute.";
                master.LogMessage(errorMessage, e);
                if (loop == Config.getInstance().getMaxTries()) {
                    job.setMessages(master.putNewMessageInMessagesList(
                        job, JobMessageModel.ERROR, errorMessage, false
                    ));
                    job.setStatus(JobStatus.STATUS_ERROR);
                    job = (COMPMDockerJobModel) updateJob(job);
                    return;
                }
                loop++;
                master.waitBetweenFailedRequests();
                //submitJobToCompute(job);//return false;
            }
        } // end while loop
        
    }


    /**
     * Retrieves the user's SciServer auth token from the job object.
    *
    * @param job The job object.
    * @return the user auth token as a string.
    */
    public String getUserToken(COMPMDockerJobModel job) {
        try {
            String token = "";
            token = job.getSubmitterToken();
            return token;
        } catch (Exception e) {
            master.LogMessage("Could not get token from Job object.", e);
            return "";
        }
    }


    

    /**
     * Gets a list of IDs of all Docker containers running jobs, as known by JOBM and currently managed by COMPM.
     *
     * @return ArrayList of Docker container IDs.
     */
    public ArrayList<String> getLocalContainerIDs() {
        
        ArrayList<String> containerIDs = new ArrayList<String>();
        
        for (Object jobWorker : master.jobWorkers.values()) {
            
            COMPMDockerJobModel job = (COMPMDockerJobModel) ((JobWorker) jobWorker).job;
            String executorDID = job != null ? job.getExecutorDID() : null; 
            if (executorDID != null) {
                containerIDs.add(executorDID);
            }
        }
        return containerIDs;
    }


    /**
     * Gets a list of all Docker containers curently running jobs, as known by SciuServer-Compute. 
     *
     * @param token The SciServer auth token of the user.
     * @return ArrayList of Docker container IDs.
     */
    public ArrayList<String> getOpenDockerContainers(String token) {
        while (true) {
            ArrayList<String> containerIDs = new ArrayList<String>(); 
            
            try {
                Hashtable<String, String> extraHeaderFields = new Hashtable<String, String>();
                extraHeaderFields.put("X-Auth-Token", token);
                HttpResponseResult result = request.executeGet(
                    Config.getInstance().getComputeUrl_getContainers(), extraHeaderFields
                );
                if (result != null && result.responseCode >= 200 && result.responseCode <= 299) {
        
                    ArrayNode arrayNode = (ArrayNode) master.convertJsonStringToJsonNode(result.message);
                    Iterator<JsonNode> iterator = arrayNode.elements();
                    while (iterator.hasNext()) {
                        JsonNode node = iterator.next();
                        containerIDs.add(node.toString());
                    }
                } else {
                    Exception e = new Exception("HttpResponse code:" + result.responseCode + ".\n" + result.message);
                    throw(e);
                }
                return containerIDs;
            } catch (Exception e) {
        
                String errorMessage = "Could not get list of open Docker containers in SciServer-Compute.";
                master.LogMessage(errorMessage, e);
                master.waitBetweenFailedRequests();
                //return getOpenDockerContainers(token);
            }
        }
    }


    /**
     * Deletes containers currently running in SciServer-Compute, but not known by JOBM and not managed by COMPM.
     *
     * @param token The SciServer auth token of the user.
     */
    public void deleteUnmanagedContainers(String token) {

        try {

            ArrayList<String> openDockerContainers = getOpenDockerContainers(token);
            ArrayList<String> localContainers = getLocalContainerIDs();
            
            for (int i = 0; i < openDockerContainers.size(); i++) {
                String openDockerContainer = openDockerContainers.get(i);
                if (!localContainers.contains(openDockerContainer)) {
                    deleteJobContainer(token, openDockerContainer, "");
                }
            }
            
        } catch (Exception e) {
            String errorMessage = "Could not delete umanaged containers.";
            master.LogMessage(errorMessage, e);
        }
    }


}
