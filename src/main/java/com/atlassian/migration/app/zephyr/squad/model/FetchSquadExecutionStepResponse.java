package com.atlassian.migration.app.zephyr.squad.model;

import java.util.List;

public record FetchSquadExecutionStepResponse(
        List<SquadExecutionStepResponse> executionsteps
) {
}
