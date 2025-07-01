package com.atlassian.migration.app.zephyr.migration;

import com.atlassian.migration.app.zephyr.migration.model.AttachmentAssociationData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.nio.file.Paths.get;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;


public class AttachmentsCopier {

    private static final String TEST_STEP_ENTITY = "teststep";
    private static final String TEST_EXECUTION_ENTITY = "schedule";
    private static final String TEST_EXECUTION_STEP_ENTITY = "teststepresult";
    private static final String FILES_FULL_PERMISSION = "rwxrwxrwx";
    private final String DESTINATION_DIR_PATH;
    private final String baseDir;
    private final Map<String, ProjectHistoricalKeys> projectHistoricalKeysMap = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(AttachmentsCopier.class);

    public AttachmentsCopier(String baseDir) {
        this.baseDir = baseDir;

        DESTINATION_DIR_PATH = baseDir + "kanoahTests/";
    }


    public void copyAttachments(List<AttachmentAssociationData> attachmentsMapped, String projectKey,
                                List<String> historicalProjectKeys) throws java.io.IOException {

        var projectHistoricalKeys = projectHistoricalKeysMap.computeIfAbsent(projectKey, key ->
                getOriginalProjectKey(key, historicalProjectKeys));

        if (projectHistoricalKeys == null) {
            logger.info("This project has historical keys so attachments are not copied.");
            return;
        }

        String originFilePath = "";
        for (AttachmentAssociationData attachment : attachmentsMapped) {
            String entityId = attachment.getSquadOriginEntity().id();

            switch (attachment.getDestinationEntityType()) {
                case TEST_STEP -> originFilePath = getSquadEntityAttachmentPath(projectHistoricalKeys.keysHoldingData,
                        attachment.getFileName(),
                        entityId,
                        TEST_STEP_ENTITY);

                case TEST_EXECUTION ->
                        originFilePath = getSquadEntityAttachmentPath(projectHistoricalKeys.keysHoldingData,
                                attachment.getFileName(),
                                entityId,
                                TEST_EXECUTION_ENTITY);
                case TEST_EXECUTION_STEP ->
                        originFilePath = getSquadEntityAttachmentPath(projectHistoricalKeys.keysHoldingData,
                                attachment.getFileName(),
                                entityId,
                                TEST_EXECUTION_STEP_ENTITY);

                case TEST_CASE -> {
                    String issueKeyAndNum = attachment.getSquadOriginEntity().key();
                    originFilePath = getJiraEntityAttachmentPath(issueKeyAndNum, projectHistoricalKeys.originalKey,
                            attachment.getFileName(), attachment.getSquadOriginEntity().id());
                }
            }

            if (originFilePath.isBlank() || originFilePath.isEmpty()) {
                continue;
            }

            copyFile(originFilePath, attachment.getFileName());
        }
    }

    void copyFile(String originFilePath, String fileName) throws IOException {

        var destinationDir = setupDestinationDir(DESTINATION_DIR_PATH);

        Path destinationFilePath = get(new StringBuilder(destinationDir.toString())
                .append("/")
                .append(fileName).toString());
        logger.info("copied the file to: "+destinationFilePath);
        try {
            Files.copy(Paths.get(originFilePath), destinationFilePath, REPLACE_EXISTING);
            Files.setPosixFilePermissions(destinationFilePath, PosixFilePermissions.fromString(FILES_FULL_PERMISSION));
        } catch (IOException e) {
            logger.error("Error copying file: " + originFilePath);
        }
    }

    String calculateBucket(int issueNum) {
        return String.valueOf(((issueNum - 1) / 10000 + 1) * 10000);
    }

    String buildOriginPath(String attachmentsBaseFolder, String entityDir, String entityType, String entityId,
                           String fileName) {
        return new StringBuilder(attachmentsBaseFolder)
                .append(entityDir)
                .append("/")
                .append(entityType)
                .append("/")
                .append(entityId)
                .append("/")
                .append(fileName).toString();

    }

    private Path setupDestinationDir(String destinationDir) throws IOException {
        Path destinationDirPath = get(destinationDir);
        if (!Files.exists(destinationDirPath)) {
            Files.createDirectory(destinationDirPath);
        }

        Files.setPosixFilePermissions(destinationDirPath, PosixFilePermissions.fromString(FILES_FULL_PERMISSION));
        return destinationDirPath;
    }

    private String getJiraEntityAttachmentPath(String issueKeyAndNum, String originalKey, String attachmentName, String entityId) {
        var issueNum = Integer.parseInt(issueKeyAndNum.split("-")[1]);
        var bucket = calculateBucket(issueNum);

        var historicalIssueKeyAndNum = originalKey + "-" + issueNum;

        var originPath = buildOriginPath(baseDir, originalKey, bucket, historicalIssueKeyAndNum, attachmentName);

        if (isPathToAttachment(originPath)) {
            return originPath;
        }

        logger.warn("Couldn't find the attachment " + attachmentName
                + " from Test Case " + entityId
                + " in any of the project directories.");

        return "";

    }

    private String getSquadEntityAttachmentPath(List<String> keysHoldingData, String attachmentName, String entityId, String entityType) {

        for (var key : keysHoldingData) {
            var targetPath = buildOriginPath(baseDir, key, entityType, entityId, attachmentName);

            if (isPathToAttachment(targetPath)) {
                return targetPath;
            }
        }
        logger.warn("Couldn't find the attachment " + attachmentName
                + " from " + entityType + " with id " + entityId
                + " in any of the project directories.");
        return "";
    }

    boolean isPathToAttachment(String targetPath) {
        var path = get(targetPath);
        return Files.exists(path);
    }

    ProjectHistoricalKeys getOriginalProjectKey(String currKey, List<String> historicalKeys) {

        if (historicalKeys.size() == 1) {
            return new ProjectHistoricalKeys(currKey, currKey, historicalKeys);
        }

        logger.info("Project has changed keys. Looking for the original one...");

        var originalKey = "";
        List<String> keysHoldingData = new ArrayList<>();

        for (String key : historicalKeys) {
            Path originPath = getProjectAttachmentsDir(key);

            if ((originalKey.isEmpty() || originalKey.isBlank()) && holdsIssuesDir(originPath)) {
                originalKey = key;
                keysHoldingData.add(key);
            } else if (holdsSquadDirs(originPath)) {
                keysHoldingData.add(key);
            }

        }

        if (originalKey.isEmpty() || originalKey.isBlank()) {
            logger.warn("Couldn't define the original Project Key to execute the attachments copy operation. No attachments will be copied from Squad to Scale.");
            return null;

        }
        logger.info("Project original key found.");

        return new ProjectHistoricalKeys(currKey, originalKey, keysHoldingData);

    }

    Path getProjectAttachmentsDir(String projectKey) {
        return get(this.baseDir).resolve(projectKey);
    }

    boolean holdsIssuesDir(Path dirPath) {

        Pattern bucketDirPattern = Pattern.compile("\\d+");

        try (Stream<Path> stream = Files.walk(dirPath, 1)) {
            return stream
                    .filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .anyMatch(fileName -> bucketDirPattern.matcher(fileName).matches());
        } catch (IOException e) {
            return false;
        }
    }

    boolean holdsSquadDirs(Path dirPath) {

        try (Stream<Path> stream = Files.walk(dirPath, 1)) {
            return stream
                    .filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .anyMatch(fileName -> fileName.equals(TEST_STEP_ENTITY) || fileName.equals(TEST_EXECUTION_ENTITY) || fileName.equals(TEST_EXECUTION_STEP_ENTITY));
        } catch (IOException e) {
            return false;
        }
    }

    record ProjectHistoricalKeys(String currKey, String originalKey, List<String> keysHoldingData) {
    }

}
