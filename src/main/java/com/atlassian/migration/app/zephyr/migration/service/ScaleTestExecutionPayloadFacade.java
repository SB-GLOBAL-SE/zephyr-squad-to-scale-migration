package com.atlassian.migration.app.zephyr.migration.service;

import com.atlassian.migration.app.zephyr.common.ApiException;
import com.atlassian.migration.app.zephyr.common.TimeUtils;
import com.atlassian.migration.app.zephyr.jira.api.JiraApi;
import com.atlassian.migration.app.zephyr.jira.model.AssignableUserResponse;
import com.atlassian.migration.app.zephyr.scale.model.ScaleExecutionCreationPayload;
import com.atlassian.migration.app.zephyr.scale.model.ScaleExecutionStepPayload;
import com.atlassian.migration.app.zephyr.scale.model.ScaleMigrationExecutionCustomFieldPayload;
import com.atlassian.migration.app.zephyr.squad.model.FetchSquadCustomFieldValueResponse;
import com.atlassian.migration.app.zephyr.squad.model.FetchSquadExecutionStepParsedResponse;
import com.atlassian.migration.app.zephyr.squad.model.SquadCustomFieldValueResponse;
import com.atlassian.migration.app.zephyr.squad.model.SquadExecutionItemParsedResponse;

import java.io.IOException;
import java.util.*;

public class ScaleTestExecutionPayloadFacade implements Resettable {

    private static final String DEFAULT_NONE_USER = "None";

    private static final String SQUAD_STATUS_WIP = "wip";
    private static final String SQUAD_STATUS_UNEXECUTED = "unexecuted";
    private static final String SQUAD_VERSION_UNSCHEDULED = "unscheduled";

    private static final String SCALE_EXEC_STATUS_IN_PROGRESS = "In Progress";
    private static final String SCALE_EXEC_STATUS_NOT_EXECUTED = "Not Executed";

    private final Map<String, String> statusTranslation = Map.of(
            SQUAD_STATUS_WIP, SCALE_EXEC_STATUS_IN_PROGRESS,
            SQUAD_STATUS_UNEXECUTED, SCALE_EXEC_STATUS_NOT_EXECUTED
    );

    private final Set<String> assignableUsers = new HashSet<>();
    private final Set<String> unassignableUsers = new HashSet<>(Set.of(DEFAULT_NONE_USER));
    private final JiraApi jiraApi;

    public ScaleTestExecutionPayloadFacade(JiraApi jiraApi) {
        this.jiraApi = jiraApi;
    }

    @Override
    public void reset() {
        assignableUsers.clear();

        unassignableUsers.clear();
        unassignableUsers.add(DEFAULT_NONE_USER);
    }

    public ScaleExecutionCreationPayload buildPayload(
            SquadExecutionItemParsedResponse executionData, String scaleTestCaseKey, String projectKey,
            FetchSquadExecutionStepParsedResponse testExectuionStepResponse,
            FetchSquadCustomFieldValueResponse testExecutionCfValueResponse, List<String> projectCustomFieldNames) throws IOException {

        var executedByValidation = validateAssignedUser(executionData.executedBy() == null ? null:executionData.executedBy().toString(), projectKey);
        var assignedToValidation = validateAssignedUser(executionData.assignedToOrStr().toString(), projectKey);

        var executedUserKey = executedByValidation ? getAssignableUserKey(executionData.executedBy().toString(), projectKey): null;
        var assigneeUserKey = getAssignableUserKey(executionData.assignedToOrStr().toString(), projectKey);

        List<String> defects = new ArrayList<String>();
        if(executionData.defects() != null && executionData.defects().size() > 0){
            executionData.defects().forEach( defect -> {
                if(defect.key() != null && !defect.key().isEmpty()) {
                    defects.add(defect.key());
                }
            });
        }
        List<ScaleExecutionStepPayload> scriptResults = new ArrayList<>();
        if(testExectuionStepResponse != null &&
                testExectuionStepResponse.executionSteps() != null &&
                testExectuionStepResponse.executionSteps().size() > 0){
            for(var executionStepResponse:testExectuionStepResponse.executionSteps()){
                scriptResults.add( new ScaleExecutionStepPayload(executionStepResponse.index(),
                                        translateSquadToScaleExecStatus(executionStepResponse.status().name()),
                                        executionStepResponse.comment()
                                ));
            }
        }
        Map<String, String> cfValueMap = new LinkedHashMap<>();
        cfValueMap.put(ScaleMigrationExecutionCustomFieldPayload.EXECUTED_ON, executionData.executedOnOrStr() == null? null:executionData.executedOnOrStr().toString());
        cfValueMap.put(ScaleMigrationExecutionCustomFieldPayload.ASSIGNED_TO, assignedToValidation ? executionData.assignedTo().toString() : DEFAULT_NONE_USER);
        cfValueMap.put(ScaleMigrationExecutionCustomFieldPayload.SQUAD_VERSION, translateSquadToScaleVersion(executionData.versionName()));
        cfValueMap.put(ScaleMigrationExecutionCustomFieldPayload.SQUAD_CYCLE_NAME, executionData.cycleName());
        cfValueMap.put(ScaleMigrationExecutionCustomFieldPayload.FOLDER_NAME, executionData.folderNameOrStr());

        if(testExecutionCfValueResponse != null &&
                testExecutionCfValueResponse.valueMap() != null &&
                testExecutionCfValueResponse.valueMap().size() > 0){
            for(Map.Entry<String, SquadCustomFieldValueResponse> cfValueEntry:testExecutionCfValueResponse.valueMap().entrySet()){
                SquadCustomFieldValueResponse squadCustomFieldValueResponse = cfValueEntry.getValue();
                if(!projectCustomFieldNames.contains(squadCustomFieldValueResponse.customFieldName())){
                    continue;
                }
                if(squadCustomFieldValueResponse.customFieldType().equals("DATE") ||
                    squadCustomFieldValueResponse.customFieldType().equals("DATE_TIME")){
                    cfValueMap.put(squadCustomFieldValueResponse.customFieldName(), TimeUtils.getUTCDateforEphocTime(squadCustomFieldValueResponse.value()));
                } else if(squadCustomFieldValueResponse.customFieldType().equals("LARGE_TEXT")) {
                    String fieldValue = squadCustomFieldValueResponse.value();
                    if(fieldValue.contains("\n")){
                        fieldValue = fieldValue.replace("\n", "<br>");
                    }
                    cfValueMap.put(squadCustomFieldValueResponse.customFieldName(), fieldValue);
                } else {
                    cfValueMap.put(squadCustomFieldValueResponse.customFieldName(), squadCustomFieldValueResponse.value());
                }
            }
        }

        return new ScaleExecutionCreationPayload(
                translateSquadToScaleExecStatus(executionData.status().name()),
                scaleTestCaseKey,
                executedByValidation ? executedUserKey : null,
                executionData.executedOn() != null ? TimeUtils.getUTCTimestampforSquadDate(executionData.executedOn().toString()) : null,
                assignedToValidation ? assigneeUserKey : null,
                executionData.htmlComment(),
                translateSquadToScaleVersion(executionData.versionName()),
                defects,
                scriptResults,
                cfValueMap
//                new ScaleMigrationExecutionCustomFieldPayload(
//                        executionData.executedOnOrStr(),
//                        assignedToValidation ? executionData.assignedTo() : DEFAULT_NONE_USER,
//                        translateSquadToScaleVersion(executionData.versionName()),
//                        executionData.cycleName(),
//                        executionData.folderNameOrStr(),
//                        cfValueMap)
        );
    }

    private String getAssignableUserKey(String assignedUsername, String projectKey){
        try{
            if (assignedUsername == null
                    || assignedUsername.isBlank()
                    || unassignableUsers.contains(assignedUsername)) {
                return null;
            }

            var fetchedAssignableUsers = jiraApi.fetchAssignableUserByUsernameAndProject(assignedUsername,
                    projectKey);

            if (fetchedAssignableUsers.isEmpty()) {
                unassignableUsers.add(assignedUsername);
                return null;
            }

            var assignableUser = fetchedAssignableUsers.stream().filter(user -> isSameUser(user, assignedUsername)).toList();

            if (assignableUser.size() > 1) {
                return null;
            }
            return assignableUser.get(0).key();


        }catch(Exception e){

        }
        return null;
    }
    private Boolean validateAssignedUser(String assignedUsername, String projectKey) throws IOException {
        if (assignedUsername == null
                || assignedUsername.isBlank()
                || unassignableUsers.contains(assignedUsername)) {
            return false;
        } else if (assignableUsers.contains(assignedUsername)) {
            return true;
        }

        var fetchedAssignableUsers = jiraApi.fetchAssignableUserByUsernameAndProject(assignedUsername,
                projectKey);

        if (fetchedAssignableUsers.isEmpty()) {
            unassignableUsers.add(assignedUsername);
            return false;
        }

        var assignableUser = fetchedAssignableUsers.stream().filter(user -> isSameUser(user, assignedUsername)).toList();

        if (assignableUser.size() > 1) {
            throw new ApiException(-1, "Multiple users found for the same username: " + assignedUsername);
        }

        assignableUsers.add(assignedUsername);
        
        return true;
    }
    
    /* 
    private Boolean validateAssignedUser(String assignedUsername, String projectKey) throws IOException {
        if (assignedUsername == null
                || assignedUsername.isBlank()
                || unassignableUsers.contains(assignedUsername)) {
            return false;
        } else if (assignableUsers.contains(assignedUsername)) {
            return true;
        }

        var assignableUser = jiraApi.fetchAssignableUserByUsernameAndProject(assignedUsername,
                projectKey);

        if (assignableUser.isEmpty()) {
            unassignableUsers.add(assignedUsername);
            return false;
        }

        if (assignableUser.size() > 1) {
            throw new ApiException(-1, "Multiple users found for the same username: " + assignedUsername);
        }

        if (isSameUser(assignableUser.get(0), assignedUsername)) {
            assignableUsers.add(assignedUsername);
            return true;
        }

        throw new ApiException(-1, "Error on Assigned User checking for user: " + assignedUsername);
    }
        */
    private boolean isSameUser(AssignableUserResponse fetchedUser, String assignedUser) {
        return fetchedUser.name().equals(assignedUser);
    }

    private String translateSquadToScaleExecStatus(String squadStatusName) {
        var squadStatusNameLower = squadStatusName.toLowerCase();
        return statusTranslation.getOrDefault(squadStatusNameLower, squadStatusName);
    }

    private String translateSquadToScaleVersion(String versionName) {
        if (versionName == null || versionName.equalsIgnoreCase(SQUAD_VERSION_UNSCHEDULED)) {
            return null;
        }
        return versionName;
    }

}
