package org.sciserver.springapp.sciquery.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.sciserver.tableingestor.stil.WriteMode;

/**
 * Utility class for handling temporary uploaded files.
 */
public class UploadedFile {

    private static final int BUFFER_SIZE = 1024 * 1024;

    /**
     * Enumeration for write modes when handling uploaded files.
     */
    public static enum WriteModeEnum { APPEND, CREATE, DROP_CREATE }

    public UploadedFile() {
        // ignored
    }

    /**
     * Creates a temporary file from the provided InputStream. The file is created at the specified path.
     *
     * @param inputStream The InputStream from which the file data is read.
     * @param pathToFile The path where the temporary file will be created.
     * @param maxFileBytes The maximum allowed file size in bytes. If negative, any file size is allowed.
     * @return The created temporary file.
     * @throws IllegalArgumentException If the file size exceeds the maximum allowed size or if no data is supplied.
     * @throws IOException If an I/O error occurs.
     */
    public static File writeUploadedFile(InputStream inputStream, String pathToFile, long maxFileBytes) 
                                     throws IllegalArgumentException, IOException {
        File file = new File(pathToFile);
        byte[] bytes = new byte[BUFFER_SIZE];

        int readBytes = 0;
        long totalBytes = 0;

        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            while (true) {
                readBytes = inputStream.read(bytes);
                if (readBytes <= 0) {
                    break;
                }
                if (maxFileBytes > 0 && (totalBytes + readBytes) > maxFileBytes) {
                    throw new IllegalArgumentException("Uploaded file size can not be bigger than "
                            + maxFileBytes + " bytes.");
                }
                fileOutputStream.write(bytes, 0, readBytes);
                totalBytes += readBytes;
            }
            if (totalBytes == 0) {
                throw new IllegalArgumentException("File upload failed. No data in file.");
            }
        }
        return file;
    }

    /**
     * Converts the provided WriteModeEnum to the corresponding WriteMode of the file data 
     * when written into the database table.
     *
     * @param mode The WriteModeEnum to be converted.
     * @return The corresponding WriteMode.
     * @throws IllegalArgumentException If the provided WriteModeEnum is not supported.
     */ 
    public static WriteMode getDatabaseTableWriteMode(WriteModeEnum mode) throws IllegalArgumentException {
        
        switch (mode) {
            case APPEND:
                return WriteMode.APPEND;
            case CREATE:
                return WriteMode.CREATE;
            case DROP_CREATE:
                return WriteMode.DROP_CREATE;
            default:
                break;
        }
        throw new IllegalArgumentException("Unsupported write mode: " + mode.name());
    }
}
