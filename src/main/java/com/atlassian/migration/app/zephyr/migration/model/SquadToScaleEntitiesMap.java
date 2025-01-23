package com.atlassian.migration.app.zephyr.migration.model;

public record SquadToScaleEntitiesMap (
        SquadToScaleTestCaseMap testCaseMap,

        SquadToScaleTestStepMap testStepMap,

        SquadToScaleTestExecutionMap testExecutionMap,

        SquadToScaleExecutionStepMap executionStepMap
){}
