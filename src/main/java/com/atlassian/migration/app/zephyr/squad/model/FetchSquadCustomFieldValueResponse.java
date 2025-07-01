package com.atlassian.migration.app.zephyr.squad.model;

import java.util.Map;

public record FetchSquadCustomFieldValueResponse(
        Map<String, SquadCustomFieldValueResponse> valueMap
){}
