package com.atlassian.migration.app.zephyr.squad.model;

import java.util.List;

public record SquadExecutionStepParsedResponse(
        int id,
        int index,
        SquadExecutionTypeResponse status,
        String comment,
        int attachmentCount,
        List<String> defects

) { }