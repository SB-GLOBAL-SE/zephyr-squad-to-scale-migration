package com.atlassian.migration.app.zephyr.scale.model;

import java.util.Map;

public record ScaleTestScriptResults(
        int id,
        int index,
        int sourceScriptId
) {
}
