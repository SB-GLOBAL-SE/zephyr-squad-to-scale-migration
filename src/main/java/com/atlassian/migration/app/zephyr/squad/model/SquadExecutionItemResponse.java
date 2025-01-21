package com.atlassian.migration.app.zephyr.squad.model;

import java.util.List;

public record SquadExecutionItemResponse(
        String id,
        int executionStatus,
        String createdOn,
        Object createdBy,
        String createdByUserName,
        String versionName,
        String htmlComment,
        String cycleName,
        String folderName,
        String executedOn,
        String assignedTo,
        String assignedToDisplay,
        String assignedToUserName,
        List<SquadExecutionDefectResponse> defects
) { }
