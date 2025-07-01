package com.atlassian.migration.app.zephyr.squad.model;

public record SquadCustomFieldValueResponse(
    Integer customFieldId,
    String customFieldName,
    String customFieldType,
    String value
)
{}
