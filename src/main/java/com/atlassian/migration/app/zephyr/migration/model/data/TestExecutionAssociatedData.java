package com.atlassian.migration.app.zephyr.migration.model.data;

import com.atlassian.migration.app.zephyr.common.TimeUtils;

public class TestExecutionAssociatedData {

    String id;
    String mappedScaleId;
    String createdBy;
    String createdOn;
    String modifiedBy;
    String modifiedOn;
    String executedOn;

    private TestExecutionAssociatedData(String id, String mappedScaleId, String createdBy, String createdOn, String modifiedBy, String modifiedOn, String executedOn) {
        this.id = id;
        this.mappedScaleId = mappedScaleId;
        this.createdBy = createdBy;
        this.createdOn = createdOn;
        this.modifiedBy = modifiedBy;
        this.modifiedOn = modifiedOn;
        this.executedOn = executedOn;;
    }

    public static TestExecutionAssociatedData createExecutionAssociatedData(String id, String mappedScaleId, String createdBy, String createdOn, String modifiedBy, String modifiedOn, String executedOn){
        return new TestExecutionAssociatedData(id, mappedScaleId, createdBy,
                TimeUtils.getUTCTimestampforSquadDate(createdOn),
                modifiedBy,
                TimeUtils.getUTCTimestampforSquadDate(modifiedOn),
                TimeUtils.getUTCTimestampforSquadDate(executedOn));
    }

    public String getId() {
        return id;
    }

    public String getMappedScaleId() {
        return mappedScaleId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getCreatedOn() {
        return createdOn;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public String getModifiedOn() {
        return modifiedOn;
    }

    public String getExecutedOn() {
        return executedOn;
    }

}
