package com.atlassian.migration.app.zephyr.squad.model;

import java.util.List;
import java.util.Map;

public record SquadTestStepResponse(
        String id,
        String orderId,
        String htmlStep,
        String htmlData,
        String htmlResult,
        List<SquadAttachmentItemResponse> attachmentsMap,
        Map<String, SquadCustomFieldValueResponse> customFields

) { }