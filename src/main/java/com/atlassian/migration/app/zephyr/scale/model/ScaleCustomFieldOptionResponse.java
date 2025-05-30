package com.atlassian.migration.app.zephyr.scale.model;

public record ScaleCustomFieldOptionResponse(
        String name,
        int index,
        boolean archived,
        int id
) {
}
