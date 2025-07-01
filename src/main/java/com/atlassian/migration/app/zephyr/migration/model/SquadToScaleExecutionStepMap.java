package com.atlassian.migration.app.zephyr.migration.model;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SquadToScaleExecutionStepMap extends HashMap<SquadToScaleExecutionStepMap.SquadExecutionStepMapKey,
            SquadToScaleExecutionStepMap.ScaleExecutionStepMapValue> {

    public List<Map.Entry<SquadToScaleExecutionStepMap.SquadExecutionStepMapKey,
            SquadToScaleExecutionStepMap.ScaleExecutionStepMapValue>> getExecutionStepMapHasAttachments(){
        return this.entrySet().stream().filter( e -> e.getKey().attchmentCount > 0).toList();
    }

    public List<Map.Entry<SquadToScaleExecutionStepMap.SquadExecutionStepMapKey,
            SquadToScaleExecutionStepMap.ScaleExecutionStepMapValue>> getExecutionStepMapHasDefects(){
        return this.entrySet().stream().filter( e -> e.getKey().defects().size() > 0).toList();
    }

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
