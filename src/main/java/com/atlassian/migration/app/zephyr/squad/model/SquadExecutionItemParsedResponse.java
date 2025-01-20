package com.atlassian.migration.app.zephyr.squad.model;

public record SquadExecutionItemParsedResponse(
        String id,
        SquadExecutionTypeResponse status,
        String createdOn,
        Object createdBy,
        String createdByUserName,
        String versionName,
        Object htmlComment,
        String cycleName,
        String folderName,
        Object executedOn,
        Object assignedTo,
        String assignedToDisplay,
        String assignedToUserName,
        Object executedOnOrStr,
        Object assignedToOrStr,
        String folderNameOrStr
) {

    public SquadExecutionItemParsedResponse(
            String id,
            SquadExecutionTypeResponse status,
            String createdOn,
            Object createdBy,
            String createdByUserName,
            String versionName,
            Object htmlComment,
            String executedOn,
            String assignedTo,
            String assignedToDisplay,
            String assignedToUserName,
            String cycleName,
            String folderName) {
        this(id,
                status,
                createdOn,
                createdBy,
                createdByUserName,
                versionName,
                htmlComment,
                cycleName,
                folderName,
                executedOn,
                assignedTo,
                assignedToDisplay,
                assignedToUserName,
                executedOn == null ? "None" : executedOn,
                assignedToUserName == null || assignedToDisplay.toLowerCase().contains("inactive")
                        ? "None" : assignedToUserName,
                folderName == null ? "None" : folderName);
    }
}
