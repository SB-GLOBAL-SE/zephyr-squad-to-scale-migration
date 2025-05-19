package com.atlassian.migration.app.zephyr.squad.model;

import java.util.Map;

public record SquadCustomFieldResponse(
        String name,
        String description,
        String fieldType,
        boolean isActive,
        int id,
        String entityType,
        Object fieldOptions,
        String comment,
        String fileId,
        Map<String, String> customFieldOptionValues
)
{}
