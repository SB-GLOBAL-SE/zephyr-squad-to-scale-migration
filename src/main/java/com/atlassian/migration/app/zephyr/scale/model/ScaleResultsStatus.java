package com.atlassian.migration.app.zephyr.scale.model;

public record ScaleResultsStatus(
        boolean isDefault,
        String color,
        String i18nKey,
        String name,
        int count,
        int index,
        int id,
        int projectId
) {
}
