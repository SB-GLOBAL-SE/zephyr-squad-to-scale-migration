package com.atlassian.migration.app.zephyr.scale.model;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.atlassian.migration.app.zephyr.scale.model.ScaleCustomFieldPayload.*;

public record ScaleMigrationTestCasePriorityPayload(
        int projectId,
        String name

) {

    public static final Set<String> MIGRATION_TESTCASE_PRIORITIES = Stream.of("High", "Low", "Normal")
            .collect(Collectors.toCollection(HashSet::new));

}

