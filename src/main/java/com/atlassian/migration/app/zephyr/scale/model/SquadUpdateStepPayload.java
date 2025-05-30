package com.atlassian.migration.app.zephyr.scale.model;

public record SquadUpdateStepPayload(

        Long id,
        ScaleStepByStepScript testScript
) {
}