package com.atlassian.migration.app.zephyr.migration.model;

import java.util.HashMap;
import java.util.List;

public class SquadToScaleExecutionStepMap extends HashMap<SquadToScaleExecutionStepMap.SquadExecutionStepMapKey,
            SquadToScaleExecutionStepMap.ScaleExecutionStepMapValue> {

    public record SquadExecutionStepMapKey(
            int executionStepId,
            String testExecutionId,
            int attchmentCount,
            List<String> defects

    ){}

    public record ScaleExecutionStepMapValue(
            int testScriptResultId,
            String testResultId
    ){}
}
