package com.atlassian.migration.app.zephyr.squad.model;

public record SquadExecutionStatusResponse(
    String id,
    String name,
    String description,
    String color)
{}