package com.atlassian.migration.app.zephyr.squad.model;

import java.util.List;

public record SquadExecutionStepResponse(
        int id,
        int orderId,
        int status,
        String comment,

        int stepResultAttachmentCount,
        List<SquadExecutionDefectResponse> defects

) { }