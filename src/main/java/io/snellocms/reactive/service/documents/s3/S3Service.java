package io.snellocms.reactive.service.documents.s3;


import io.minio.*;
import io.quarkus.runtime.StartupEvent;
import io.snellocms.reactive.management.AppConstants;
import io.snellocms.reactive.service.documents.DocumentsService;
import io.snellocms.reactive.util.MimeUtils;
import io.snellocms.reactive.util.ResourceFileUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static io.snellocms.reactive.management.AppConstants.*;


@Singleton
public class S3Service implements DocumentsService {

    private static final int BUFFER_SIZE = 1024;
    private static final long PART_SIZE = 50 * 1024 * 1024;

    Logger logger = Logger.getLogger(getClass());

    @Inject
    MinioClient minioClient;

    @ConfigProperty(name = S3_BUCKET_NAME)
    String s3_bucket_name;

    @ConfigProperty(name = S3_BUCKET_FOLDER)
    String s3_bucket_folder;


    public void onLoad(@ObservesAsync StartupEvent event) {
        logger.info("S3Service load");
        init();
    }

    public void init() {
        try {
            if (!verifyBucket()) {
                createBucket();
            }
            verifyFolder();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public boolean verifyBucket() throws Exception {
        return minioClient.bucketExists(BucketExistsArgs.builder().bucket(s3_bucket_name).build());
    }

    public void createBucket() throws Exception {
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(s3_bucket_name).build());
    }


    public String verifyFolder() {
        if (s3_bucket_folder != null && !s3_bucket_folder.trim().isEmpty()) {
            if (!s3_bucket_folder.endsWith("/")) {
                return s3_bucket_folder + "/";
            } else {
                return s3_bucket_folder;
            }
        } else {
            return "";
        }
    }


    @Override
    public String basePath(String folder) {
        if (s3_bucket_folder != null && !s3_bucket_folder.trim().isEmpty()) {
            return s3_bucket_folder + folder;
        }
        return folder;
    }

    @Override
    public Map<String, Object> upload(InputStream file,
                                      MediaType mediaType, String filename, String uuid, String table_name, String table_key) throws Exception {
        String extension = ResourceFileUtils.getExtension(filename);
        String name = basePath(table_name) + "/" + uuid + "." + extension;
        Map<String, Object> map = new HashMap<>();
        map.put(AppConstants.UUID, uuid);
        map.put(DOCUMENT_NAME, uuid + "." + extension);
        map.put(DOCUMENT_ORIGINAL_NAME, filename);
        map.put(DOCUMENT_PATH, name);
        map.put(DOCUMENT_MIME_TYPE, mediaType.getType());
//        map.put(SIZE, file.getSize());
        map.put(TABLE_NAME, table_name);
        map.put(TABLE_KEY, table_key);
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(s3_bucket_name)
                            .object(filename)
                            .contentType(mediaType.getType())
                            .stream(file, -1, PART_SIZE)
                            .build());
        } catch (Exception e) {
            throw new Exception("Failed uploading file [{0}]", e);
        }
//        minioClient.putObject(s3_bucket_name, name, file, file.getSize(), file.getContentType().toString());
        logger.info("document uploaded!");
        return map;
    }

    @Override
    public Map<String, Object> write(File file, String uuid, String table_name) throws Exception {
        Map<String, Object> map = new HashMap<>();
        String extension = ResourceFileUtils.getExtension(file.getName());
        String name = basePath(table_name) + "/" + uuid + "." + extension;
//        minioClient.putObject(s3_bucket_name, name, file.getAbsolutePath());
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(s3_bucket_name)
                            .object(name)
                            .stream(new FileInputStream(file), -1, PART_SIZE)
                            .build());
        } catch (Exception e) {
            throw new Exception("Failed uploading file [{0}]", e);
        }
        map.put(TABLE_NAME, table_name);
        map.put(DOCUMENT_PATH, name);
        return map;
    }

    @Override
    public Map<String, Object> write(byte[] bytes, String uuid, String table_name, String extension) throws Exception {
        Map<String, Object> map = new HashMap<>();
        String name = basePath(table_name) + "/" + uuid + "." + extension;
        InputStream stream = new ByteArrayInputStream(bytes);
        int size = bytes.length;
        String contentType = MimeUtils.getContentType(name);
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(s3_bucket_name)
                            .object(name)
                            .stream(new ByteArrayInputStream(bytes), -1, PART_SIZE)
                            .build());
        } catch (Exception e) {
            throw new Exception("Failed uploading file [{0}]", e);
        }
//        minioClient.putObject(s3_bucket_name, name, stream, size, contentType);
        map.put(TABLE_NAME, table_name);
        map.put(DOCUMENT_PATH, name);
        return map;
    }

    @Override
    public StreamingOutput streamingOutput(String path, String mediatype) throws Exception {
        try {
            InputStream input = minioClient.getObject(
                    GetObjectArgs.builder().bucket(s3_bucket_name).object(path).build());
            StreamingOutput fileStream = new StreamingOutput() {
                @Override
                public void write(OutputStream output) throws IOException, WebApplicationException {
                    try {
                        Files.copy(Paths.get(path), output);
                    } catch (Exception e) {
                        logger.error("An exception (NoSuchFile) occured. MESSAGE=" + e.getMessage());
                    }
                }
            };
            return fileStream;
        } catch (Exception e) {
            throw new Exception("Failed downloading object with object name [{0}]", e);
        }

    }


    @Override
    public boolean delete(String filename) throws Exception {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(s3_bucket_name).object(filename).build());
        } catch (Exception e) {
            throw new Exception("Failed removing object with object name [{0}]", e);
        }
        return true;
    }

    @Override
    public File getFile(String path) throws Exception {
        String ext = ResourceFileUtils.getExtension(path);
        File temp = File.createTempFile(java.util.UUID.randomUUID().toString(), ext);
        try {
            InputStream input = minioClient.getObject(
                    GetObjectArgs.builder().bucket(s3_bucket_name).object(path).build());

                byte[] buffer = new byte[BUFFER_SIZE]; // Adjust if you want
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                input.close();
        } catch (Exception e) {
            throw new Exception("Failed downloading object with object name [{0}]", e);
        }
//        InputStream inputStream = minioClient.getObject(s3_bucket_name, path);
//        Files.copy(inputStream, temp.toPath());
        return temp;
    }


}
