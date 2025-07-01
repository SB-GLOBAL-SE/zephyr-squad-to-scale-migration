package com.atlassian.migration.app.zephyr.scale.model;

import java.util.List;

public record FetchScaleResultsStatusesResponse (
        List<ScaleResultsStatus> data
){}