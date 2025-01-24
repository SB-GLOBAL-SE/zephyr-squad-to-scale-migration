package com.atlassian.migration.app.zephyr.migration.model.data;

import com.atlassian.migration.app.zephyr.common.TimeUtils;

public class TestCaseAssociatedData {

    String id;
    String mappedScaleId;
    String createdBy;
    String createdOn;
    String modifiedBy;
    String modifiedOn;

    private TestCaseAssociatedData(String id, String mappedScaleId, String createdBy, String createdOn, String modifiedBy, String modifiedOn) {
        this.id = id;
        this.mappedScaleId = mappedScaleId;
        this.createdBy = createdBy;
        this.createdOn = createdOn;
        this.modifiedBy = modifiedBy;
        this.modifiedOn = modifiedOn;
    }

    public static TestCaseAssociatedData createExecutionAssociatedData(String id, String mappedScaleId, String createdBy, String createdOn, String modifiedBy, String modifiedOn){
        return new TestCaseAssociatedData(id,
                mappedScaleId,
                createdBy,
                TimeUtils.getUTCTimestampforJiraDate(createdOn),
                modifiedBy,
                TimeUtils.getUTCTimestampforJiraDate(modifiedOn));
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

}
