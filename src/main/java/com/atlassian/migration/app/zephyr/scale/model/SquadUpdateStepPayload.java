package com.atlassian.migration.app.zephyr.scale.model;

public record SquadUpdateStepPayload(
        int id,
        ScaleUpdateTestScript testScript
) { }

