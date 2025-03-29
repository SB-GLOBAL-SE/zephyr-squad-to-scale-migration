package com.atlassian.migration.app.zephyr.scale.api;

import com.atlassian.migration.app.zephyr.common.ApiConfiguration;
import com.atlassian.migration.app.zephyr.common.ApiException;
import com.atlassian.migration.app.zephyr.common.BaseApi;
import com.atlassian.migration.app.zephyr.common.ZephyrApiException;
import com.atlassian.migration.app.zephyr.scale.model.*;
import com.atlassian.migration.app.zephyr.squad.model.FetchSquadStatusResponse;
import com.atlassian.migration.app.zephyr.squad.model.SquadExecutionStatusResponse;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.*;

public class ScaleApi extends BaseApi {

    private static final Logger logger = LoggerFactory.getLogger(ScaleApi.class);
    public static final String ENABLE_PROJECT_ENDPOINT = "/rest/atm/1.0/project";
    public static final String CREATE_CUSTOM_FIELD_ENDPOINT = "/rest/atm/1.0/customfield";
    public static final String ADD_OPTION_TO_CUSTOM_FIELD_ENDPOINT = "/rest/atm/1.0/customfield/%s/option";
    public static final String CREATE_SCALE_TEST_CASE_ENDPOINT = "/rest/atm/1.0/testcase";
    public static final String SCALE_TEST_STEP_ENDPOINT = "/rest/atm/1.0/testcase/%s";
    public static final String CREATE_SCALE_MIGRATION_TEST_CYCLE_ENDPOINT = "/rest/atm/1.0/testrun";
    public static final String CREATE_SCALE_TEST_RESULTS_ENDPOINT = "/rest/atm/1.0/testrun/%s/testresults";
    public static final String FETCH_SCALE_TEST_RESULTS_ENDPOINT = "/rest/tests/1.0/testresult/%s?fields=id,key,testScriptResults";
    public static final String FETCH_SCALE_RESULTS_STATUSES = "/rest/tests/1.0/testresultstatus?projectId=%s";
    public static final String FETCH_SCALE_TESTCASE_STATUSES = "/rest/tests/1.0/testcasestatus?projectId=%s";
    public static final String CREATE_SCALE_TESTCASE_STATUSES = "/rest/tests/1.0/testcasestatus";
    public static final String FETCH_SCALE_TESTCASE_PRIORITY = "/rest/tests/1.0/testcasepriority?projectId=%s";
    public static final String CREATE_SCALE_TESTCASE_PRIORITY = "/rest/tests/1.0/testcasepriority";
    public static final String CREATE_SCALE_TEST_RESULTS_STATUS_ENDPOINT = "/rest/tests/1.0/testresultstatus";
    public static final String CREATE_SCALE_TEST_SCRIPT_RESULT_DEFECT_ENDPOINT = "/rest/tests/1.0/tracelink/testresult/bulk/create";
    public static final String CUSTOM_FIELD_DUPLICATED_EXPECTED_MESSAGE = "Custom field name is duplicated";

    public ScaleApi(ApiConfiguration config) {
        super(config);
    }

    public String createTestCases(ScaleTestCaseCreationPayload testCaseCreationPayload) throws ZephyrApiException {
        String response = "";
        Map<String, Object> result = new HashMap<>();
        try {
            response = sendHttpPost(CREATE_SCALE_TEST_CASE_ENDPOINT, testCaseCreationPayload);

            result = gson.fromJson(response, Map.class);
        } catch (ApiException e) {
            ScaleApiErrorLogger.logAndThrow(ScaleApiErrorLogger.ERROR_CREATE_TEST_CASE, e);
        } catch (JsonSyntaxException e) {
            ScaleApiErrorLogger.logAndThrow(
                    String.format(ScaleApiErrorLogger.ERROR_CREATE_TEST_CASE_PAYLOAD_PARSE, response),
                    new ApiException(e));

        }

        return (String) result.get("key");
    }

    public void updateTestStep(String key, SquadUpdateStepPayload step) throws ZephyrApiException {
        try {
            sendHttpPut(String.format(SCALE_TEST_STEP_ENDPOINT, key), step);
        } catch (ApiException e) {
            ScaleApiErrorLogger.logAndThrow(String.format(ScaleApiErrorLogger.ERROR_CREATE_TEST_STEP, key), e);
        }
    }

    public void updateTestStepdefects(List<ScaleExecutionStepDefectsPayload> step) throws ZephyrApiException {
        try {
            sendHttpPost(String.format(CREATE_SCALE_TEST_SCRIPT_RESULT_DEFECT_ENDPOINT), step);
        } catch (ApiException e) {
            ScaleApiErrorLogger.logAndThrow(String.format(ScaleApiErrorLogger.ERROR_CREATE_TEST_STEP_DEFECT), e);
        }
    }

    public ScaleGETStepsPayload fetchTestStepsFromTestCaseKey(String key) throws ZephyrApiException {

        try {
            var response = sendHttpGet(getUri(urlPath(SCALE_TEST_STEP_ENDPOINT, key)));

            return gson.fromJson(response, ScaleGETStepsPayload.class);
        } catch (ApiException e) {
            ScaleApiErrorLogger.logAndThrow(String.format(ScaleApiErrorLogger.ERROR_FETCHING_TEST_STEP, key), e);
        }
        return new ScaleGETStepsPayload("", "", new SquadGETStepItemPayload());
    }

    public FetchScaleResultsStatusesResponse fetchTestResultsStatuses(String projectId) throws ZephyrApiException {
        try {
            var response = sendHttpGet(getUri(urlPath(FETCH_SCALE_RESULTS_STATUSES, projectId)));
            List<ScaleResultsStatus> listofScaleResultsStatus = gson.fromJson(response, new TypeToken<List<ScaleResultsStatus>>(){}.getType());
            return new FetchScaleResultsStatusesResponse(listofScaleResultsStatus);
        } catch (ApiException e) {
            ScaleApiErrorLogger.logAndThrow(String.format(ScaleApiErrorLogger.ERROR_FETCHING_TESTRESULTS_STATUS, projectId), e);
        }
        return new FetchScaleResultsStatusesResponse(new ArrayList<>());
    }

    public FetchScaleTestResults fetchTestResultsbyId(String testResultId) throws ZephyrApiException {
        try {
            var response = sendHttpGet(getUri(urlPath(FETCH_SCALE_TEST_RESULTS_ENDPOINT, testResultId)));
            FetchScaleTestResults scaleTestResults = gson.fromJson(response, FetchScaleTestResults.class);
            return scaleTestResults;
        } catch (ApiException e) {
            ScaleApiErrorLogger.logAndThrow(String.format(ScaleApiErrorLogger.ERROR_FETCHING_TESTRESULTS_STATUS, testResultId), e);
        }
        return null;
    }

    public String createScaleTestResultsStatus(String projectId, String statusName, String statusDescription, String statusColor) throws ZephyrApiException{
        Map<String, Object> params = new HashMap<>();
        params.put("projectId", Integer.parseInt(projectId));
        params.put("name", statusName);
        params.put("description", statusDescription);
        params.put("color", statusColor);

        String response = "";
        try {
            response = sendHttpPost(CREATE_SCALE_TEST_RESULTS_STATUS_ENDPOINT, params);
        } catch (ApiException e) {
            ScaleApiErrorLogger.logAndThrow(ScaleApiErrorLogger.ERROR_CREATE_TEST_RESULTS_STATUS, e);
        }

        Map<String, Object> result = gson.fromJson(response, Map.class);
        return result.get("id").toString();
    }

    public void updateTestCaseStatuses(String projectId) throws ZephyrApiException {
        try {
            var response = sendHttpGet(getUri(urlPath(FETCH_SCALE_TESTCASE_STATUSES, projectId)));
            List<ScaleMigrationTestCaseStatusPayload> listOfScaleTestcaseStatuses = gson.fromJson(response, new TypeToken<List<ScaleMigrationTestCaseStatusPayload>>(){}.getType());
            listOfScaleTestcaseStatuses.forEach(testCaseStatusPayload -> {
                ScaleMigrationTestCaseStatusPayload.MIGRATION_TESTCASE_STATUSES.add(testCaseStatusPayload.name());
            });
        } catch (ApiException e) {
            ScaleApiErrorLogger.logAndThrow(String.format(ScaleApiErrorLogger.ERROR_FETCHING_TESTCASE_STATUS, projectId), e);
        }

    }

    public String CreateScaleTestcaseStatus(String projectId, String name) throws ZephyrApiException{
        Map<String, Object> params = new HashMap<>();
        params.put("projectId", Integer.parseInt(projectId));
        params.put("name", name);

        String response = "";
        try {
            response = sendHttpPost(CREATE_SCALE_TESTCASE_STATUSES, params);
        } catch (ApiException e) {
            ScaleApiErrorLogger.logAndThrow(ScaleApiErrorLogger.ERROR_CREATING_TESTCASE_STATUS, e);
        }

        Map<String, Object> result = gson.fromJson(response, Map.class);
        return result.get("id").toString();
    }

    public void updateTestCasePriorities(String projectId) throws ZephyrApiException {
        try {
            var response = sendHttpGet(getUri(urlPath(FETCH_SCALE_TESTCASE_PRIORITY, projectId)));
            List<ScaleMigrationTestCasePriorityPayload> listofScaleResultsStatus = gson.fromJson(response, new TypeToken<List<ScaleMigrationTestCasePriorityPayload>>(){}.getType());
            listofScaleResultsStatus.forEach(priotiy -> {
                ScaleMigrationTestCasePriorityPayload.MIGRATION_TESTCASE_PRIORITIES.add(priotiy.name());
            });
        } catch (ApiException e) {
            ScaleApiErrorLogger.logAndThrow(String.format(ScaleApiErrorLogger.ERROR_FETCHING_TESTCASE_PRIORITY, projectId), e);
        }
    }

    public String CreateScaleTestcasePriority(String projectId, String name) throws ZephyrApiException{
        Map<String, Object> params = new HashMap<>();
        params.put("projectId", Integer.parseInt(projectId));
        params.put("name", name);

        String response = "";
        try {
            response = sendHttpPost(CREATE_SCALE_TESTCASE_PRIORITY, params);
        } catch (ApiException e) {
            ScaleApiErrorLogger.logAndThrow(ScaleApiErrorLogger.ERROR_CREATING_TESTCASE_PRIORITY, e);
        }

        Map<String, Object> result = gson.fromJson(response, Map.class);
        return result.get("id").toString();
    }

    public String createMigrationTestCycle(String projectKey, String cycleName, String cycleVersion) throws ZephyrApiException {

        Map<String, Object> params = new HashMap<>();
        params.put("version", cycleVersion);
        params.put("name", cycleName);
        params.put("projectKey", projectKey);

        String response = "";
        try {
            response = sendHttpPost(CREATE_SCALE_MIGRATION_TEST_CYCLE_ENDPOINT, params);
        } catch (ApiException e) {
            ScaleApiErrorLogger.logAndThrow(ScaleApiErrorLogger.ERROR_CREATE_TEST_CYCLE, e);

        }

        Map<String, Object> result = gson.fromJson(response, Map.class);
        return (String) result.get("key");
    }

    public ScalePOSTTestResultPayload createTestResults(String cycleKey, List<ScaleExecutionCreationPayload> datas) throws ZephyrApiException {
        try {
            var response = sendHttpPost(String.format(CREATE_SCALE_TEST_RESULTS_ENDPOINT, cycleKey), datas);
            Type testResultCreatedPayload = new TypeToken<List<ScaleTestResultCreatedPayload>>() {
            }.getType();
            return new ScalePOSTTestResultPayload(gson.fromJson(response, testResultCreatedPayload));

        } catch (ApiException e) {
            ScaleApiErrorLogger.logAndThrow(String.format(ScaleApiErrorLogger.ERROR_CREATE_TEST_RESULTS, cycleKey), e);
        }
        return new ScalePOSTTestResultPayload(Collections.emptyList());
    }

    public ScaleTestResultCreatedPayload createTestExecution(String cycleKey, ScaleExecutionCreationPayload data)
            throws ZephyrApiException {
        //Test Results creation endpoint only accepts a List of Test Results as payload
        return createTestResults(cycleKey, List.of(data)).testResultsCreated().get(0);
    }

    public void enableProject(EnableProjectPayload enableProjectPayload) throws ZephyrApiException {
        try {
            sendHttpPost(ENABLE_PROJECT_ENDPOINT, enableProjectPayload);
        } catch (ApiException e) {
            ScaleApiErrorLogger.logAndThrow(ScaleApiErrorLogger.ERROR_ENABLE_PROJECT, e);
        }
    }

    public String createCustomField(ScaleCustomFieldPayload scaleCustomFieldPayload) throws ZephyrApiException {

        try {
            var response = sendHttpPost(CREATE_CUSTOM_FIELD_ENDPOINT, scaleCustomFieldPayload);

            var createdCustomField = gson.fromJson(response, Map.class);
            Object customFieldId =  createdCustomField.get("id");

            if(customFieldId instanceof Double){
                return String.valueOf(((Double) customFieldId).longValue());
            }

            return String.valueOf(customFieldId);

        } catch (ApiException e) {
            //While creating a custom field that already exists, Scale API returns a 400 status with the
            //message "Custom field name is duplicated". Catching it here and ignoring this status
            if (e.code == 400 && e.getMessage().contains(CUSTOM_FIELD_DUPLICATED_EXPECTED_MESSAGE)) {
                return "";
            }

            ScaleApiErrorLogger.logAndThrow(
                    ScaleApiErrorLogger.ERROR_CREATE_CUSTOM_FIELD, e
            );

            return "";

        }
    }

    public void addOptionToCustomField(String customFieldId, ScaleTestCaseCustomFieldOption option) throws ZephyrApiException {
        try {
            sendHttpPost(String.format(ADD_OPTION_TO_CUSTOM_FIELD_ENDPOINT, customFieldId), option);
        } catch (ApiException e) {
            ScaleApiErrorLogger.logAndThrow(
                    "Error while adding option to custom field at " + String.format(ADD_OPTION_TO_CUSTOM_FIELD_ENDPOINT, customFieldId),
                    e
            );
        }
    }

    private static class ScaleApiErrorLogger {

        public static final String ERROR_CREATE_TEST_CASE = "Error while creating Test Case at " +
                CREATE_SCALE_TEST_CASE_ENDPOINT;

        public static final String ERROR_CREATE_TEST_CASE_PAYLOAD_PARSE = "Error while creating Test Case at " +
                CREATE_SCALE_TEST_CASE_ENDPOINT + " - Unexpected Payload received: %s \n";

        public static final String ERROR_CREATE_TEST_STEP = "Error while creating Test Steps at " +
                SCALE_TEST_STEP_ENDPOINT;

        public static final String ERROR_CREATE_TEST_STEP_DEFECT = "Error while creating step results defects.";

        public static final String ERROR_FETCHING_TEST_STEP = "Error while fetching Test Steps at " +
                SCALE_TEST_STEP_ENDPOINT;

        public static final String ERROR_FETCHING_TESTRESULTS_STATUS = "Error while fetching Test results status at " +
                FETCH_SCALE_RESULTS_STATUSES;

        public static final String ERROR_FETCHING_TESTCASE_PRIORITY = "Error while fetching Test Case prioirity at " +
                FETCH_SCALE_TESTCASE_PRIORITY;

        public static final String ERROR_CREATING_TESTCASE_PRIORITY = "Error while creating Test Case prioirity at " +
                CREATE_SCALE_TESTCASE_PRIORITY;

        public static final String ERROR_FETCHING_TESTCASE_STATUS = "Error while fetching Test Case status at " +
                FETCH_SCALE_TESTCASE_STATUSES;

        public static final String ERROR_CREATING_TESTCASE_STATUS = "Error while creating Test Case status at " +
                CREATE_SCALE_TESTCASE_STATUSES;

        public static final String ERROR_FETCHING_TESTRESULTS = "Error while fetching Test results at " +
                FETCH_SCALE_TEST_RESULTS_ENDPOINT;

        public static final String ERROR_CREATE_TEST_CYCLE = "Error while creating Test Cycle at "
                + CREATE_SCALE_MIGRATION_TEST_CYCLE_ENDPOINT;

        public static final String ERROR_CREATE_TEST_RESULTS_STATUS = "Error while creating Test results status at "
                + CREATE_SCALE_TEST_RESULTS_STATUS_ENDPOINT;

        public static final String ERROR_CREATE_TEST_RESULTS = "Error while creating Test Results at "
                + CREATE_SCALE_TEST_RESULTS_ENDPOINT;

        public static final String ERROR_CREATE_CUSTOM_FIELD = "Error while creating Custom Fields at "
                + CREATE_CUSTOM_FIELD_ENDPOINT;
        public static final String ERROR_ENABLE_PROJECT = "Error while enabling Project as Scale project at "
                + ENABLE_PROJECT_ENDPOINT;

        public static void logAndThrow(String message, ApiException e) throws ZephyrApiException {
            logger.error(message + " " + e.getMessage(), e);

            throw new ZephyrApiException(e);
        }

    }
}
