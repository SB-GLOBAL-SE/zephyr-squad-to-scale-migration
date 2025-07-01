package com.atlassian.migration.app.zephyr.jira.model;

public record CustomField(String id, String name, String description, String type, String searcherKey) {
}