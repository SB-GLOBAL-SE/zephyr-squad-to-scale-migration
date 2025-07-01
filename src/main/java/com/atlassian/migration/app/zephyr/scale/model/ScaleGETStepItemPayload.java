package com.atlassian.migration.app.zephyr.scale.model;

import com.atlassian.migration.app.zephyr.common.TimeUtils;
import com.atlassian.migration.app.zephyr.squad.model.SquadCustomFieldValueResponse;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record ScaleGETStepItemPayload(
        String description,
        String testData,
        String expectedResult,
        String id,
        Integer index,
        List<ScaleTestStepCustomFieldValue> customFieldValues
) {

    //To Create a test step on Scale we don't need ID and Index, but we must set it to null, otherwise Scale
    //API will try to process these fields and throw an ERROR 500
    public static ScaleGETStepItemPayload createScaleGETStepItemPayloadForCreation(
            String description,
            String testData,
            String expectedResult,
            Map<String, SquadCustomFieldValueResponse> stringSquadCustomFieldValueResponseMap,
            Optional<TestCaseEntity> testCaseEntity, Map<String, ScaleCustomFieldResponse> projectTestStepCustomFields, Integer index) {
        List<ScaleTestStepCustomFieldValue> customFieldValues = new LinkedList<>();

        if(stringSquadCustomFieldValueResponseMap != null) {
            for (Map.Entry<String, SquadCustomFieldValueResponse> cfValueEntry : stringSquadCustomFieldValueResponseMap.entrySet()) {
                SquadCustomFieldValueResponse squadCustomFieldValueResponse = cfValueEntry.getValue();
                if (!projectTestStepCustomFields.containsKey(squadCustomFieldValueResponse.customFieldName())) {
                    continue;
                }

                String fieldValue = squadCustomFieldValueResponse.value();
                ScaleCustomFieldResponse customField = projectTestStepCustomFields.get(squadCustomFieldValueResponse.customFieldName());
                if (squadCustomFieldValueResponse.customFieldType().equals("DATE") ||
                        squadCustomFieldValueResponse.customFieldType().equals("DATE_TIME")) {
                    // Just adding .000Z to field value for passing API for the format.
                    String utcDateforEphocTime = TimeUtils.getUTCDateforEphocTime(fieldValue) + ".000Z";
                    customFieldValues.add(new ScaleTestStepCustomFieldValue(null, null, utcDateforEphocTime, customField));
                } else if (squadCustomFieldValueResponse.customFieldType().equals("SINGLE_SELECT") ||
                        squadCustomFieldValueResponse.customFieldType().equals("RADIO_BUTTON")) {
                    Map<String, ScaleCustomFieldOptionResponse> optionsByName = customField.getOptionsByName();
                    if (optionsByName.containsKey(fieldValue)) {
                        int id = optionsByName.get(fieldValue).id();
                        customFieldValues.add(new ScaleTestStepCustomFieldValue(null, id, null, customField));
                    }
                } else if (squadCustomFieldValueResponse.customFieldType().equals("MULTI_SELECT") ||
                        squadCustomFieldValueResponse.customFieldType().equals("CHECKBOX")) {
                    Map<String, ScaleCustomFieldOptionResponse> optionsByName = customField.getOptionsByName();
                    String optionsId = "";
                    for (String option : fieldValue.split(",")) {
                        if (optionsByName.containsKey(option.trim())) {
                            int id = optionsByName.get(option.trim()).id();
                            optionsId = optionsId + "-" + id;
                        }
                    }
                    if (!optionsId.isEmpty()) {
                        optionsId = optionsId + "-";
                        customFieldValues.add(new ScaleTestStepCustomFieldValue(optionsId, null, null, customField));
                    } else {
                        // do nothing
                    }
                } else if (squadCustomFieldValueResponse.customFieldType().equals("NUMBER")) {
                    customFieldValues.add(new ScaleTestStepCustomFieldValue(null, Integer.parseInt(fieldValue), null, customField));
                } else if (squadCustomFieldValueResponse.customFieldType().equals("LARGE_TEXT")) {
                    if (fieldValue.contains("\n")) {
                        fieldValue = fieldValue.replace("\n", "<br>");
                    }
                    customFieldValues.add(new ScaleTestStepCustomFieldValue(fieldValue, null, null, customField));
                } else {
                    customFieldValues.add(new ScaleTestStepCustomFieldValue(fieldValue, null, null, customField));
                }
            }
        }
        return new ScaleGETStepItemPayload(
                description,
                testData,
                expectedResult,
                null,
                index,
                customFieldValues
        );
    }

}