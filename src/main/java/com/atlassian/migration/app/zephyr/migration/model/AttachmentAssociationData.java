package com.atlassian.migration.app.zephyr.migration.model;

import java.time.LocalDateTime;

public class AttachmentAssociationData {
    String attachmentName;
    String fileName;
    String mimeType;
    String size;
    String authorKey;
    String projectId;
    DestinationType destinationEntityType;
    Boolean temporary;
    String createdOn;
    String testCaseId;
    String stepId;
    String testResultId;
    String testScriptResultId;
    SquadOriginEntity squadOriginEntity;

    private AttachmentAssociationData(String attachmentName, String fileName, String mimeType, String size, String authorKey, String projectId, DestinationType destinationEntityType, Boolean temporary, String createdOn, String testCaseId, String stepId, String testResultI, String testScriptResultId, SquadOriginEntity squadOriginEntity) {
        this.attachmentName = attachmentName;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.size = size;
        this.authorKey = authorKey;
        this.projectId = projectId;
        this.destinationEntityType = destinationEntityType;
        this.temporary = temporary;
        this.createdOn = createdOn;
        this.testCaseId = testCaseId;
        this.stepId = stepId;
        this.testResultId = testResultI;
        this.testScriptResultId = testScriptResultId;
        this.squadOriginEntity = squadOriginEntity;
    }

    public static AttachmentAssociationData createAttachmentAssociationDataFromTestCase(
            String attachmentName,
            String fileName,
            String mimeType,
            String size,
            String authorKey,
            String projectId,
            String destinationEntityId,
            SquadOriginEntity squadOriginEntity
    ) {
        return new AttachmentAssociationData(
                attachmentName,
                fileName,
                mimeType,
                size,
                authorKey,
                projectId,
                DestinationType.TEST_CASE,
                false,
                LocalDateTime.now().toString(),
                destinationEntityId,
                null,
                null,
                null,
                squadOriginEntity
        );
    }

    public static AttachmentAssociationData createAttachmentAssociationDataFromTestStep(
            String attachmentName,
            String fileName,
            String mimeType,
            String size,
            String authorKey,
            String projectId,
            String destinationEntityId,
            SquadOriginEntity squadOriginEntity
    ) {
        return new AttachmentAssociationData(
                attachmentName,
                fileName,
                mimeType,
                size,
                authorKey,
                projectId,
                DestinationType.TEST_STEP,
                false,
                LocalDateTime.now().toString(),
                null,
                destinationEntityId,
                null,
                null,
                squadOriginEntity
        );
    }

    public static AttachmentAssociationData createAttachmentAssociationDataFromTestExecution(
            String attachmentName,
            String fileName,
            String mimeType,
            String size,
            String authorKey,
            String projectId,
            String destinationEntityId,
            SquadOriginEntity squadOriginEntity
    ) {
        return new AttachmentAssociationData(
                attachmentName,
                fileName,
                mimeType,
                size,
                authorKey,
                projectId,
                DestinationType.TEST_EXECUTION,
                false,
                LocalDateTime.now().toString(),
                null,
                null,
                destinationEntityId,
                null,
                squadOriginEntity
        );
    }

    public static AttachmentAssociationData createAttachmentAssociationDataFromExecutionStep(
            String attachmentName,
            String fileName,
            String mimeType,
            String size,
            String authorKey,
            String projectId,
            String destinationEntityId,
            SquadOriginEntity squadOriginEntity
    ) {
        return new AttachmentAssociationData(
                attachmentName,
                fileName,
                mimeType,
                size,
                authorKey,
                projectId,
                DestinationType.TEST_EXECUTION_STEP,
                false,
                LocalDateTime.now().toString(),
                null,
                null,
                null,
                destinationEntityId,
                squadOriginEntity
        );
    }

    public SquadOriginEntity getSquadOriginEntity() {
        return squadOriginEntity;
    }

    public String getAttachmentName() {
        return attachmentName;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getSize() {
        return size;
    }

    public String getAuthorKey() {
        return authorKey;
    }

    public String getProjectId() {
        return projectId;
    }

    public DestinationType getDestinationEntityType() {
        return destinationEntityType;
    }

    public Boolean getTemporary() {
        return temporary;
    }

    public String getCreatedOn() {
        return createdOn;
    }

    public String getTestCaseId() {
        return testCaseId;
    }

    public String getStepId() {
        return stepId;
    }

    public String getTestResultId() {
        return testResultId;
    }

    public String getTestScriptResultId() {
        return testScriptResultId;
    }

    public enum DestinationType {
        TEST_CASE,
        TEST_STEP,
        TEST_EXECUTION,
        TEST_EXECUTION_STEP
    }
}
