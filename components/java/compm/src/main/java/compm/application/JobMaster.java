package compm.application;


import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat; 
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sciserver.racm.jobm.model.COMPMDockerJobModel;
import org.sciserver.racm.jobm.model.COMPMJobModel;
import org.sciserver.racm.jobm.model.JobMessageModel;
import org.sciserver.racm.jobm.model.JobStatus;
import org.sciserver.racm.jobm.model.RDBJobModel;
import org.sciserver.racm.jobm.model.RequiredUserVolumeModel;



/**
 * The `JobMaster` class is responsible for managing job workers, handling job queues, and
 * interacting with external services to process and monitor jobs.
 */
public class JobMaster<T extends COMPMJobModel> {
    
    private static sciserver.logging.Logger logger = null;
    private static final Logger LOG = LogManager.getLogger(JobMaster.class);
    private FileService fileService = new FileService();

    HttpRequest request;
    LinkedHashMap<Integer, JobWorker> jobWorkers;
    ArrayList<JobMessageModel> jobMessages;
    LinkedList jobsQueue;
    ArrayList canceledJobs;
    ObjectMapper objectMapper;
    Integer numWorkers;
    Integer maxNumJobs;
    Integer minNumJobs;
    Long idleTimeBetweenJobs;
    Long idleTimeBetweenJobmRequests;
    Class workerClass;

    final Class<T> type;

    JobMaster(Class<T>  type) {
        this.type = type;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setSerializationInclusion(Include.NON_NULL);
        this.request = new HttpRequest(Config.getInstance().getCompmId());
        this.jobWorkers = new LinkedHashMap<Integer, JobWorker>();
        this.jobMessages = new ArrayList<JobMessageModel>();
        this.jobsQueue = new LinkedList();
        this.canceledJobs = new ArrayList();

        numWorkers = Config.getInstance().getNumWorkers();
        maxNumJobs = Config.getInstance().getMaxNumberOfJobs();
        minNumJobs = Config.getInstance().getMinNumberOfJobs();
            
        idleTimeBetweenJobs = (Config.getInstance().getIdleTimeBetweenJobs()); // jobTimeOut in seconds
        idleTimeBetweenJobmRequests = (Config.getInstance().getIdleTimeBetweenJobmRequests()); // jobTimeOut in seconds

    }

    /**
     * The main function initializes and runs different types of jobs based on configuration settings.
     */
    public static void main(String[] args) {
        
        try {
            LOG.info("Starting COMPM...");
            
            Config.load();
            logger = new sciserver.logging.Logger(Config.getInstance().getLogApplicationHost(), 
                                                  Config.getInstance().getLogApplicationName(), 
                                                  Config.getInstance().getLogMessagingHost(), 
                                                  Config.getInstance().getLogDatabaseQueueName(), 
                                                  Config.getInstance().getLogExchangeName(), 
                                                  Config.getInstance().getLogEnabled());
        
            String jobType = Config.getInstance().getJobType();
            LOG.info("Loaded config file for {} COMPM named '{}'", jobType, 
                     Config.getInstance().getLogApplicationName());
            if (jobType.equals("docker")) {
                JobMaster<COMPMDockerJobModel> jobMaster = 
                    new JobMaster<COMPMDockerJobModel>(COMPMDockerJobModel.class);
                jobMaster.run("compm.application.DockerJobWorker");
            } else if (jobType.equals("rdb")) {
                JobMaster<RDBJobModel> jobMaster = new JobMaster<RDBJobModel>(RDBJobModel.class);
                jobMaster.run("compm.application.rdb.RDBJobWorker");
            } else {
                throw new Exception("Job type defined in config file cannot be recognized.");
            }
            
            LOG.info("COMPM started successfully.");
            
        } catch (Exception e) {
            LogMessage("Error in JobMaster.", e);
        }
    }


    /**
    * The `run` function dynamically creates and starts worker threads based on a specified worker class,
    * manages job queues, refills jobs if needed, and continuously checks for canceled jobs within 
    * a loop before sleeping for a set interval.
    *
    * @param workerClassName The name of the class that represents the worker responsible for processing jobs. 
    */
    public void run(String workerClassName) throws Exception {

        this.workerClass = Class.forName(workerClassName);
        
        //get initial managed jobs, if any
        getAlreadyManagedJobs();
        
        // fully fill job queue if needed.
        //getMoreManagedJobs();
        
        //create, store and start workers:
        for (Integer i = 1; i <= numWorkers; i++) {
            JobWorker jobWorker = null;
            Thread workerThread = null;
            try {
                LOG.info("Starting worker {}", i);
                
                jobWorker = (JobWorker) workerClass.getDeclaredConstructor(
                    new Class[] {JobMaster.class, int.class, long.class}).newInstance(this, i, idleTimeBetweenJobs);
                jobWorkers.put(i,  jobWorker);
                workerThread = new Thread(jobWorker, Integer.toString(i));
                workerThread.start();
                
                LOG.info(".........worker {} started successfully.", i);
                                
            } catch (Exception e) {
                if (jobWorker != null) {
                    jobWorker.stop();
                }
                //LogMessage("Error when creating or starting worker thread " + i + ".\n", e);
                throw(e);
            }
        }
        
        //loop for managing jobs
        while (true) {
                
            //refilling jobs queue if number of available jobs in there is low.
            if (isNumManagedJobsLow()) {
                getMoreManagedJobs();
            }
                

            //get canceled messages from JOBM and initiate process of canceling jobs in respective workers
            manageCanceledJobs();

            Thread.sleep((long) (this.idleTimeBetweenJobmRequests * 1000.0)); //sleep time in milliseconds.
        }
    }

    /**
     * Processes canceled jobs by updating their status, sending a
     * request to update the job status, and removing the job from the queue if the request is
     * successful.
     */
    public void manageCanceledJobs() {
        
        getCanceledJobs();

        //if the canceled job is in the local queue, lock the queue and delete the canceled jobs from it.
        if (this.canceledJobs.size() > 0) {
            synchronized (this.jobsQueue) {
                
                for (int i = 0; i < this.jobsQueue.size(); i++) {
        
                    Long jobId = ((T) this.jobsQueue.get(i)).getId();
                    if (this.canceledJobs.contains(jobId)) {
                        while (true) {
                            try {
                                
                                T job = ((T) this.jobsQueue.get(i));
                                job.setStatus(JobStatus.STATUS_CANCELED);
                                job.setMessages(putNewMessageInMessagesList(job, JobMessageModel.CANCEL, 
                                    "Job was cancelled by the user", false));
                                
                                Hashtable<String, String> httpHeaderFields = new Hashtable<String, String>();
                                httpHeaderFields.put("Content-Type", "application/json; charset=UTF-8");
                                
                                String jobJsonString = convertObjectToJsonString(job);
                                HttpResponseResult result = request.executePost(
                                    Config.getInstance().getJobmUrl_updateJobStatus().replace("{jobId}", 
                                    ((T) job).getId().toString()), httpHeaderFields, jobJsonString);
                                
                                if (result != null && result.responseCode >= 200 && result.responseCode <= 299) {
                                    this.jobsQueue.remove(i);
                                    return;
                                } else {
                                    Exception e = new Exception(
                                        "HttpResponse code:" + result.responseCode + ".\n Message: \n" + result.message
                                    );
                                    throw(e);
                                }
                            } catch (Exception e) {
                                LogMessage("Could not cancel or remove job", e);
                                waitBetweenFailedRequests();
                                //manageCanceledJobs();
                            }
                        }
                    }
                }
            }
            
            
            //if the cancelled job is already being run by a worker:
            for (JobWorker jobWorker : jobWorkers.values()) {
                if (jobWorker.job != null) {
                    Long jobId = jobWorker.job.getId();
                    if (this.canceledJobs.contains(jobId)) {
                        jobWorker.cancelJob();
                        //return;
                    }
                }
            }
        }
    }


    /**
     * This function adds a new message to a job.
     *
     * @param job The job object.
     * @param label the message label.
     * @param content the message content.
     * @param withRepeatedLabels Boolean flag that determines whether messages with repeated labels should be allowed.
     * @return ArrayList of `JobMessageModel` objects.
     */
    public List<JobMessageModel> putNewMessageInMessagesList(
        T job, String label, String content, Boolean withRepeatedLabels
    ) {
        
        List<JobMessageModel> messagesList = job.getMessages();
        if (messagesList == null) {
            messagesList = new ArrayList<JobMessageModel>();
        }
        // if there is already a message of type "label", then it wont add the message.
        Boolean addMessage = true;
        if (withRepeatedLabels) {
            if (getLabelIndexes(messagesList, label).size() > 0) {
                addMessage = false;
            }
        }
        if (addMessage) {
            JobMessageModel jmm = new JobMessageModel();
            jmm.setLabel(label);
            jmm.setContent(content);
            messagesList.add(jmm);
        }
        return messagesList;
    }

    /**
     * Gets a list of indexes where a specified label appears in a list of
     * JobMessageModel objects.
     *
     * @param list A list of JobMessageModel objects.
     * @param label String that represents the label that is searched for.
     * @return ArrayList of Integer values, which represent the indexes of JobMessageModel objects in the input list 
     *      that have a specific label matching the provided label parameter.
     */
    public ArrayList<Integer> getLabelIndexes(List<JobMessageModel> list, String label) {
        ArrayList<Integer> labelIndex = new ArrayList<Integer>();
        for (Integer j = 0; j < list.size(); j++) {
            if (list.get(j).getLabel().equals(label)) {
                labelIndex.add(j);
            }
        }
        return labelIndex;
    }


    /**
     * Retrieves from JOBM a list of IDs of all canceled jobs that are managed by this COMPM, and sets them locally.
     */
    public void getCanceledJobs() {
        // should get them from JOBM
        
        ArrayList currentCanceledJobs = new ArrayList();
        while (true) {
            try {
                HttpResponseResult result = request.executeGet(Config.getInstance().getJobmUrl_getCanceledJobs());
                if (result != null && result.responseCode >= 200 && result.responseCode <= 299) {
                        
                    String newJobsAsJson = result.message;

                    JsonNode rootNode = this.objectMapper.readTree(newJobsAsJson);
                    ArrayNode arrayNode = (ArrayNode) rootNode;
                    Iterator<JsonNode> iterator = arrayNode.elements();
                    while (iterator.hasNext()) {
                        JsonNode node = iterator.next();
                        long jobId = node.longValue();
                        currentCanceledJobs.add(jobId);
                    }
                    this.canceledJobs = currentCanceledJobs;
                    return;
                } else {
                    Exception e = new Exception(
                        "HttpResponse code:" + result.responseCode + ".\n Message: \n" + result.message
                    );
                    throw(e);
                }
                    
            } catch (Exception e) {
                LogMessage("Could not get canceled jobs list from jobm.", e);
                waitBetweenFailedRequests();
                //getCanceledJobs();
            }
        }
    }

    /**
     * Checks if a job with a given ID is present in the local list of canceled jobs.
     *
     * @param jobId The unique identifier of a job.
     * @return A boolean value indicating whether the job with the given jobId has been canceled.
     */
    public boolean isJobCanceled(Long jobId) {
        if (this.canceledJobs.contains(jobId)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Retrieves from JOBM the list of jobs already managed by thi COMPM (as known by JOBM), and adds
     * them to the local jobs queue.
     */
    public void getAlreadyManagedJobs() {
        while (true) {
            try {
                HttpResponseResult result = request.executeGet(Config.getInstance().getJobmUrl_registerCompm());
                if (result != null && result.responseCode >= 200 && result.responseCode <= 299) {
                        
                    String newJobsAsJson = result.message;

                    JsonNode rootNode = this.objectMapper.readTree(newJobsAsJson);
                    ArrayNode arrayNode = (ArrayNode) rootNode;
                    Iterator<JsonNode> iterator = arrayNode.elements();
                    while (iterator.hasNext()) {
                        JsonNode node = iterator.next();
                        T job = convertJsonNodeToJob(node);
                        this.jobsQueue.add(job);
                    }
                    return;
                } else {
                    Exception e = new Exception(
                        "HttpResponse code:" + result.responseCode + ".\n Message: \n" + result.message
                    );
                    throw(e);
                }
                    
            } catch (Exception e) {
                LogMessage("Could not get already managed jobs from jobm.", e);
                waitBetweenFailedRequests();
                //getAlreadyManagedJobs();
            }
        }
    }


    /**
     * Checks if there are available job workers not currently running a job.
     *
     * @return Returns a boolean value `true` if there is at least one available worker that 
     *     is not currently running a job), and `false` if not.
     */
    public boolean areThereAvailableWorkers() {
        for (Integer i = 1; i <= numWorkers; i++) {
            JobWorker jobWorker = jobWorkers.get(i); 
            if (jobWorker != null && !jobWorker.isRunningJob) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fetches new jobs from JOBM in the case there are available workers to run jobs, and adds them to the
     * local jobs queue, if any.
     */
    public void getMoreManagedJobs() {
        while (true) {
            
            int numNewJobs = this.maxNumJobs - this.jobsQueue.size();
            
            if (numNewJobs > 0 && areThereAvailableWorkers()) {

                try {
                    HttpResponseResult result = request.executeGet(
                        Config.getInstance().getJobmUrl_getNextJob() + "?N=" + Integer.toString(numNewJobs)
                    );
                    
                    if (result != null && result.responseCode >= 200 && result.responseCode <= 299) {
                        
                        String newJobsAsJson = result.message;

                        JsonNode rootNode = this.objectMapper.readTree(newJobsAsJson);
                        ArrayNode arrayNode = (ArrayNode) rootNode;
                        Iterator<JsonNode> iterator = arrayNode.elements();
                        while (iterator.hasNext()) {
                            JsonNode node = iterator.next();
                            T job = convertJsonNodeToJob(node);
                            this.jobsQueue.add(job);
                        }
                        return;
                    } else {
                        Exception e = new Exception(
                            "HttpResponse code:" + result.responseCode + ".\n Message: \n" + result.message
                        );
                        throw(e);
                    }
                    
                } catch (Exception e) {
                    LogMessage("Could not get new jobs from Jobm.", e);
                    waitBetweenFailedRequests();
                    //getMoreManagedJobs();
                }
            } else {
                return;
            }
        }
    }

    /**
     * Retrieves the next job from the jobs queue, to be assigned to a job worker.
     *
     * @param workerId The ID o fthe job worker.
     * @return the next jobfrom the jobsQueue.
    */
    public synchronized T getNewJobForWorker(int workerId) {
        
        T job = null;
        try {
            job = (T) this.jobsQueue.peek();
        } catch (Exception e) { }

        if (job == null) {
            return null;
        } else {
            
            if (job.getStatus() < JobStatus.STATUS_STARTED) { // that is, status=QUEUED
                this.jobsQueue.poll();
                return job;
            } else {
                this.jobsQueue.poll();
                return job;
            }
        }
    }


    /**
     * The function `setAndCreateResultsFolderForJob` sets and creates a results folder for a job based
     * on certain conditions and parameters.
     *
     * @param job The `setAndCreateResultsFolderForJob` method is responsible for setting and creating
     *     a results folder for a given job. It takes a job object `T` as a parameter. The method performs
     *     the following steps:
     */
    public void setAndCreateResultsFolderForJob(T job) throws Exception {
        
        //DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String date = dateFormat.format(job.getSubmissionTime());
        dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String datetime = dateFormat.format(job.getSubmissionTime());

        String fullPathJobDirectory = null;
        
        if (job.getResultsFolderURI() == null || job.getResultsFolderURI().equals("")) {

            String jobFolderName = Config.getInstance().getFileServiceJobsFolderRelativePath().replaceAll("^/+", "");
            fullPathJobDirectory = Config.getInstance().getFileServiceBasePath() 
                                 + Config.getInstance().getFileServiceRootVolume() + "/" 
                                 + job.getUsername() + "/" 
                                 + Config.getInstance().getFileServiceUserVolume() + "/" + jobFolderName; 
        } else {
            fullPathJobDirectory = "/" + job.getResultsFolderURI().replaceAll("^/+", "").replaceAll("/+$", "");
        }
        
        fullPathJobDirectory = "/" + fullPathJobDirectory.replaceAll("^/+", "")
                                                         .replaceAll("/+$", "") 
                                                         + "/" + date + "/" + datetime +  "-" 
                                                         + job.getId().toString(); // append 2 subfolders
        
        job.setResultsFolderURI(fullPathJobDirectory);
        
        String relativeJobDirUrl = fullPathJobDirectory.replace(Config.getInstance().getFileServiceBasePath(), "");
        if (!fullPathJobDirectory.startsWith(Config.getInstance().getFileServiceBasePath())) {
            throw new Exception(
                "Invalid directory '" + fullPathJobDirectory + "' is not located under '" 
                + Config.getInstance().getFileServiceBasePath() + "'"
            );
        }

        //create user volume that contains job results folder:
        String [] urlPieces = relativeJobDirUrl.split("/");
        if (urlPieces.length <= 3) { // relativeJobDirUrl = rootVolume/ownerName/userVolume/path...
            throw new Exception("Invalid job directory '" + fullPathJobDirectory + "'");
        }

        String userVolumeRelativeUrl = urlPieces[0] + "/" + urlPieces[1] + "/" + urlPieces[2] + "/";
        String rootVolumeName = urlPieces[0];
        String ownerName = urlPieces[1];
        String userVolumeName = urlPieces[2];

        fileService.createUserVolume(userVolumeRelativeUrl, job.getSubmitterToken(), null);
        addJobOutputUserVolumeToJob(job, rootVolumeName, userVolumeName, ownerName);
    }

    /**
     * Adds the user volume that contains the jobs output directory to the list of user volumes in the job object, 
     * in case it is not included yet.
     *
     * @param job The job object.
     * @param rootVolumeName The name of the root volume that containes the user volume.
     * @param userVolumeName The name of the user volume.
     * @param ownerName The nameo fo th eowner of the user volume.
     */
    public void addJobOutputUserVolumeToJob(
        T job, String rootVolumeName, String userVolumeName, String ownerName
    ) throws Exception {

        Long id = null;
        JsonNode rootFolders = fileService.getVolumes(job.getSubmitterToken());

        if (rootFolders.get("rootVolumes") != null) {
            ArrayNode rootVolumes = (ArrayNode) rootFolders.get("rootVolumes");
            for (int i = 0; i < rootVolumes.size(); i++) {
                JsonNode rootVolumeJson = rootVolumes.get(i);
                String rootVolume = rootVolumeJson.get("name").asText(); 
                if (rootVolume.equals(rootVolumeName)) {
                    ArrayNode userVolumesList = (ArrayNode) rootVolumeJson.get("userVolumes");
                    for (int j = 0; j < userVolumesList.size(); j++) {
                        JsonNode volumeJson = userVolumesList.get(j);
                        if (volumeJson.get("name").asText().equals(userVolumeName) 
                            && volumeJson.get("owner").asText().equals(ownerName)) {
                            id = volumeJson.get("id").asLong();
                        }
                    }
                }
            }
            
            if (id != null) {
                List<RequiredUserVolumeModel> userVolumes = job.getUserVolumes();
                if (userVolumes != null) {
                
                    Boolean isAlreadyIncluded = false;
                    for (RequiredUserVolumeModel userVol : userVolumes) {
                        if (userVol.getId() == id) {
                            isAlreadyIncluded = true;
                        }
                    }
                    if (!isAlreadyIncluded) {
                        RequiredUserVolumeModel userVolume = new RequiredUserVolumeModel();

                        userVolume.setUserVolumeId(id);
                        userVolume.setNeedsWriteAccess(true);
                        userVolumes.add(userVolume);

                        job.setUserVolumes(userVolumes);
                        job = updateJob(job);
                    }
                }
            } else {
                throw new Exception("Could not add default job output user volume '" + userVolumeName + "'");
            }
        } else {
            throw new Exception("User not allowed to access any root volume.");
        }
    }


    /**
     * Checks if the length of the local jobs que has reached a minimum value.
     *
     * @return Returns the boolean value `true` if the size of the local `jobsQueue` object is 
     *      less than or equal to the `minNumJobs`, and `false` if not.
     */
    public boolean isNumManagedJobsLow() {
        if (this.jobsQueue.size() <= this.minNumJobs || this.jobsQueue.size() == 0) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * Persists current/modified job object into JOBM.
     *
     * @param job The job obbject.
     * @return The newly persisted job object back from JOBM.
     */
    public T updateJob(Object job) {
        while (true) {
            try {
                Hashtable<String, String> httpHeaderFields = new Hashtable<String, String>();
                httpHeaderFields.put("Content-Type", "application/json; charset=UTF-8");
                
                String jobJsonString = convertObjectToJsonString(job);
                HttpResponseResult result = request.executePost(
                    Config.getInstance().getJobmUrl_updateJobStatus().replace("{jobId}", 
                    ((T) job).getId().toString()), httpHeaderFields, jobJsonString
                );
                if (result != null && result.responseCode >= 200 && result.responseCode <= 299) {
                    return (T) convertJsonStringToObject(result.message);
                } else {
                    Exception e = new Exception(
                        "HttpResponse code:" + result.responseCode + ".\n Message: \n" + result.message
                    );
                    throw(e);
                }
            } catch (Exception e) {
                LogMessage("Could not update job to jobm.", e);
                waitBetweenFailedRequests();
                //return updateJob(job);
            }
        }
    }

    public boolean shouldJobWorkerSleep(int workerId) {
        return false;
    }


    // utilities:

    /**
     * Converts a JSON string to a job object.
     *
     * @param jsonString A JSON string that represents an object.
     * @return Returning job object.
     */
    public T convertJsonStringToObject(String jsonString) throws Exception {
        return convertJsonNodeToJob(convertJsonStringToJsonNode(jsonString));
    }

    /**
     * Converts a JSON string to a JsonNode object.
     *
     * @param jsonString The json string.
     * @return JsonNode object.
     */
    public JsonNode convertJsonStringToJsonNode(String jsonString) throws Exception {
        JsonNode jsonNode = null;
        if (jsonString != null && !jsonString.isEmpty()) {
            jsonNode = this.objectMapper.readValue(jsonString, JsonNode.class);
        }
        return jsonNode;
    }

    /**
     * Converts a JsonNode object into a job object of type T.
     *
     * @param jsonNode JsonNode object.
     * @return object of type `T`.
     */
    public T convertJsonNodeToJob(JsonNode jsonNode) throws Exception { 
        
        T object = null;
        if (jsonNode != null) {
            try {
                object = this.objectMapper.convertValue(jsonNode, type);
            } catch (Exception e) {
                throw(e);
                //LogSciserverLoggingMessage("compm.application.COMPM.convertJsonNodeToCOMPMDockerJobModel",
                //"Could not convert JsonNode to COMPMDockerJobModel.", e);
            }
        }
        return object;
    }


    /**
     * Converts a Java object to a JSON string using an ObjectMapper.
     *
     * @param object The `object`to be converted.
     * @return The JSON string representation of the input object.
     */
    public String convertObjectToJsonString(Object object) {
        String jsonString = null;
        if (object != null) {
            JsonNode jsonNode = this.objectMapper.valueToTree(object);
            jsonString = jsonNode.toString();
        }
        return jsonString;
    }

    /**
     * Converts an object to a JsonNode using an ObjectMapper.
     *
     * @param object Object to be converted to a `JsonNode`
     *     @return The `JsonNode` representation of the input object.
     */
    public JsonNode objectToJsonNode(Object object) {
        if (object != null) {
            return this.objectMapper.valueToTree(object);
        } else {
            return null;
        }
    }

    /**
     * Logs a message.
     *
     * @param message Message to be logged.
     * @return Boolean value - `true` if the logging operation is
     *     successful, and `false` if an exception occurs during the logging process.
     */
    public static synchronized boolean LogMessage(String message) {
        return LogMessage(message, null);
    }

    /**
     * Logs a message and exception details to a local log4j file and a
     * database, handling exceptions and returning a boolean indicating success.
     *
     * @param message Message to be logged.
     * @param e Exception object to be logged along with the message, if any.
     * @return Boolean value - `true` if the logging operation is
     *     successful, and `false` if an exception occurs during the logging process.
     */
    public static synchronized boolean LogMessage(String message, Exception e) {
        try {
            String msg = "";
            String exceptionStackTrace = "";
            String exceptionMessage = "";
            if (e != null) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                exceptionStackTrace = sw.toString();
                exceptionMessage = e.getMessage();
            }
            
            if (message != null && !message.isEmpty()) {
                msg = message + "\n";
            }
                
            if (exceptionStackTrace != null && !exceptionStackTrace.isEmpty()) {
                msg = msg + exceptionStackTrace;
            } else {
                if (exceptionMessage != null && !exceptionMessage.isEmpty()) {
                    msg = msg + exceptionMessage + "\n";
                }
            }

            // writes to local log4j file:
            LOG.error(msg, e);
            // writes in sciserverlog database:
            if (Config.getInstance().getLogEnabled()) {
                
                StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
                logger.setMethod(stackTraceElements[2].getClassName() + "." + stackTraceElements[2].getMethodName());

                sciserver.logging.Message loggingMessage = logger.createCOMPMMessage(msg);
                loggingMessage.MessageBody.Properties.put("compmId", Config.getInstance().getCompmId());
                logger.SendMessage(loggingMessage);
            }
            return true;
        } catch (Exception ex) {
            try {
                String errorMessage = "Could not log error message.\n" + ex.getMessage();
                LOG.error(errorMessage, ex);
            } catch (Exception ex1) { };
            return false;
        }
    }

    /**
     * This function sleeps for a specified duration of time between failed
     * requests.
     */
    public void waitBetweenFailedRequests() {
        try {
            Thread.sleep(
                Config.getInstance().getIdleTimeBetweenFailedRequests().longValue() * 1000
            ); // sleep time in milliseconds.
        } catch (Exception e) {
            LogMessage("Error in waiting between failed requets.", e);
        }
    }

}
