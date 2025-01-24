package com.atlassian.migration.app.zephyr.migration.model;

import java.util.HashMap;

public class SquadToScaleTestExecutionMap extends HashMap<SquadToScaleTestExecutionMap.TestExecutionMapKey, String> {

    public record TestExecutionMapKey(
            String testExecutionId,
            Object createdBy,
            String createdOn,
            String modifiedBy,
            String modifiedOn
    ){}
}
