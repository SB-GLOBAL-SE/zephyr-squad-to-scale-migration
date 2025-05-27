package com.atlassian.migration.app.zephyr.scale.model;

import java.util.List;

public record GetAllCyclesResponse(
        int maxResults,
        List<GetCycleResponse> results) { }

