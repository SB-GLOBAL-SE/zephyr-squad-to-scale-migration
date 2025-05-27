package com.atlassian.migration.app.zephyr.scale.model;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record ScaleMigrationTestCaseStatusPayload(
        int projectId,
        String name

) {

    public static final Set<String> MIGRATION_TESTCASE_STATUSES = Stream.of("Draft", "Deprecated", "Approved")
            .collect(Collectors.toCollection(HashSet::new));

}

