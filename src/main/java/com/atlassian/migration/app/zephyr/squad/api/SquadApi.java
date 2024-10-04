package com.atlassian.migration.app.zephyr.squad.api;

import com.atlassian.migration.app.zephyr.common.ApiConfiguration;
import com.atlassian.migration.app.zephyr.common.ApiException;
import com.atlassian.migration.app.zephyr.common.BaseApi;
import com.atlassian.migration.app.zephyr.scale.model.GetAllProjectsResponse;
import com.atlassian.migration.app.zephyr.squad.model.*;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SquadApi extends BaseApi {

    public static final String FETCH_SQUAD_TEST_STEP_ENDPOINT = "/rest/zapi/latest/teststep/%s";
    public static final String FETCH_SQUAD_EXECUTION_ENDPOINT = "/rest/zapi/latest/execution?issueId=%s";
    public static final String FETCH_ATTACHMENT_ENDPOINT = "/rest/zapi/latest/attachment/attachmentsByEntity?entityId=%s&entityType=%s";
    public static final String GET_ALL_PROJECTS_ENDPOINT = "/rest/zapi/latest/util/project-list";
    public static final String ENTITY_TYPE_TEST_EXECUTION = "execution";
    public static final String ENTITY_TYPE_TEST_STEP = "teststep";

    public static final Map<Integer, SquadExecutionTypeResponse> EXECUTION_TYPES = Stream.of(
            new SquadExecutionTypeResponse(-1, "Unexecuted"),
            new SquadExecutionTypeResponse(1, "Pass"),
            new SquadExecutionTypeResponse(2, "Fail"),
            new SquadExecutionTypeResponse(3, "WIP"),
            new SquadExecutionTypeResponse(4, "Blocked"),
            new SquadExecutionTypeResponse(5, "Descoped"),
            new SquadExecutionTypeResponse(6, "Retested"),
            new SquadExecutionTypeResponse(7, "On Hold")
    ).collect(Collectors.toMap(SquadExecutionTypeResponse::id, e -> e));

    public SquadApi(ApiConfiguration config) {
        super(config);
    }

    public GetAllProjectsResponse getAllProjects() throws java.io.IOException {
        var response = sendHttpGet(getUri(urlPath(GET_ALL_PROJECTS_ENDPOINT)));
        return gson.fromJson(response, GetAllProjectsResponse.class);

    }

    public FetchSquadTestStepResponse fetchLatestTestStepByTestCaseId(String testCaseId) throws ApiException {

        var response = sendHttpGet(getUri(urlPath(FETCH_SQUAD_TEST_STEP_ENDPOINT, testCaseId)));
        return gson.fromJson(response, FetchSquadTestStepResponse.class);
    }

    public FetchSquadExecutionParsedResponse fetchLatestExecutionByIssueId(String issueId) throws ApiException {

        var response = sendHttpGet(getUri(urlPath(FETCH_SQUAD_EXECUTION_ENDPOINT, issueId)));
        var data = gson.fromJson(response, FetchSquadExecutionResponse.class);

        var executions = data.executions().stream()
                .map(e -> new SquadExecutionItemParsedResponse(
                        e.id(),
                        EXECUTION_TYPES.get(e.executionStatus()),
                        e.createdBy(),
                        e.createdByUserName(),
                        e.versionName(),
                        e.htmlComment(),
                        e.executedOn(),
                        e.assignedTo(),
                        e.assignedToDisplay(),
                        e.assignedToUserName(),
                        e.cycleName(),
                        e.folderName())).toList();

        return new FetchSquadExecutionParsedResponse(
                data.status(),
                data.issueId(),
                data.recordsCount(),
                data.executionsToBeLogged(),
                data.isExecutionWorkflowEnabledForProject(),
                data.isTimeTrackingEnabled(),
                executions);
    }

    public FetchSquadAttachmentResponse fetchTestExecutionAttachmentById(String testExecutionId) throws ApiException {
        return fetchAttachmentByEntityType(testExecutionId, ENTITY_TYPE_TEST_EXECUTION);
    }

    //It seems the API doesn't deliver Test Steps attachments through this endpoint (but it should)
    public FetchSquadAttachmentResponse fetchTestStepAttachmentById(String testStepId) throws ApiException {
        return fetchAttachmentByEntityType(testStepId, ENTITY_TYPE_TEST_STEP);
    }


    private FetchSquadAttachmentResponse fetchAttachmentByEntityType(String entityId, String entityType) throws ApiException {
        var response = sendHttpGet(getUri(urlPath(FETCH_ATTACHMENT_ENDPOINT, entityId, entityType)));

        return gson.fromJson(response, FetchSquadAttachmentResponse.class);
    }


}
