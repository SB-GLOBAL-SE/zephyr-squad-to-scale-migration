package com.atlassian.migration.app.zephyr.common;


import com.atlassian.migration.app.zephyr.scale.api.ScaleApi;
import com.atlassian.migration.app.zephyr.scale.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@ExtendWith(MockitoExtension.class)
public class ScaleApiTest {

    @Mock
    private ApiConfiguration apiConfMock;

    private ScaleApi scaleApiSpy;

    @BeforeEach
    void setup() {
        when(apiConfMock.httpVersion()).thenReturn("2");
        scaleApiSpy = spy(new ScaleApi(apiConfMock));
    }


    @Nested
    class whenCreating {

        @Test
        void shouldThrowExceptionWhenHttpVersionIsInvalid() {
            when(apiConfMock.httpVersion()).thenReturn("3");
            assertThrows(IllegalArgumentException.class, () -> new ScaleApi(apiConfMock));
        }

        @Test
        void shouldSetTheHttpVersionTo2() {
            when(apiConfMock.httpVersion()).thenReturn("2");
            var sutScaleApi = new ScaleApi(apiConfMock);
            assertEquals(sutScaleApi.client.version(), HttpClient.Version.HTTP_2);

            when(apiConfMock.httpVersion()).thenReturn("2.0");
            sutScaleApi = new ScaleApi(apiConfMock);
            assertEquals(sutScaleApi.client.version(), HttpClient.Version.HTTP_2);
        }

        @Test
        void shouldSetTheHttpVersionTo1() {
            when(apiConfMock.httpVersion()).thenReturn("1");
            var sutScaleApi = new ScaleApi(apiConfMock);
            assertEquals(sutScaleApi.client.version(), HttpClient.Version.HTTP_1_1);

            when(apiConfMock.httpVersion()).thenReturn("1.1");
            sutScaleApi = new ScaleApi(apiConfMock);
            assertEquals(sutScaleApi.client.version(), HttpClient.Version.HTTP_1_1);
        }

    }

    @Nested
    class WhileInteractingWithTestCases {

        private final String endpoint = ScaleApi.CREATE_SCALE_TEST_CASE_ENDPOINT;

        private final ScaleTestCaseCreationPayload scaleTestCaseCreationPayload =
                new ScaleTestCaseCreationPayload("KEY-1", "name", "objective",
                        Collections.emptyList(), "owner", Collections.emptyList(), "priority", "status", null);


        @Test
        void shouldThrowZephyrApiExceptionIfApiExceptionWhenCreateTestCases() throws ApiException {
            doThrow(ApiException.class).when(scaleApiSpy).sendHttpPost(endpoint, scaleTestCaseCreationPayload);

            assertThrows(ZephyrApiException.class, () -> scaleApiSpy.createTestCases(scaleTestCaseCreationPayload));
        }


        @Test
        void shouldCreateTestCasesAndReceiveKeyAsResponse() throws ApiException, ZephyrApiException {
            var expectedKey = "KEY-1";

            var responseMock = String.format("{key:%s}", expectedKey);

            doReturn(responseMock).when(scaleApiSpy).sendHttpPost(endpoint, scaleTestCaseCreationPayload);

            var testCaseCreated = scaleApiSpy.createTestCases(scaleTestCaseCreationPayload);

            assertEquals(expectedKey, testCaseCreated);
        }

        @Test
        void shouldThrowZephyrApiExceptionIfApiExceptionWHenCreateTestCasesWrongResponsePayload() throws ApiException {
            var responseMock = "wrong_payload";

            doReturn(responseMock).when(scaleApiSpy).sendHttpPost(endpoint, scaleTestCaseCreationPayload);

            assertThrows(ZephyrApiException.class, () -> scaleApiSpy.createTestCases(this.scaleTestCaseCreationPayload));
        }

    }

    @Nested
    class WhileInteractingWithTestSteps {

        private final String endpoint = String.format(ScaleApi.SCALE_TEST_STEP_ENDPOINT, "TEST-1");
        private final SquadUpdateStepPayload squadUpdateStepPayload = new SquadUpdateStepPayload(
                new SquadGETStepItemPayload());

        @Test
        void shouldGetTestStepFromTestCaseKey() throws ApiException, ZephyrApiException {

            var responseMock = "{key:KEY-1," +
                    "projectKey:PROJECT," +
                    "testScript:{" +
                    "type:STEP_BY_STEP, " +
                    "steps:[" +
                    "{description:desc,testData:data,expectedResult:result,id:1,index:0}]}}";

            doReturn(responseMock).when(scaleApiSpy).sendHttpGet(any());

            var squadGETStepItemPayloadExpected = new SquadGETStepItemPayload();
            squadGETStepItemPayloadExpected.steps = List.of(
                    new ScaleGETStepItemPayload("desc", "data", "result", "1", "0"));

            var scaleGETStepsPayloadExpected = new ScaleGETStepsPayload("KEY-1",
                    "PROJECT", squadGETStepItemPayloadExpected);

            var scaleGETStepsPayloadFetched = scaleApiSpy.fetchTestStepsFromTestCaseKey("KEY-1");

            assertEquals(scaleGETStepsPayloadExpected, scaleGETStepsPayloadFetched);
        }

        @Test
        void shouldThrowZephyrApiExceptionIfApiExceptionWhenFetchTestSteps() throws ApiException {
            doThrow(ApiException.class).when(scaleApiSpy).sendHttpGet(any());

            assertThrows(ZephyrApiException.class, () -> scaleApiSpy.fetchTestStepsFromTestCaseKey("KEY-1"));
        }

        @Test
        void shouldCallUpdateTestStepEndpointOnlyOnce() throws ApiException, ZephyrApiException {

            doNothing().when(scaleApiSpy).sendHttpPut(endpoint, squadUpdateStepPayload);

            scaleApiSpy.updateTestStep("TEST-1", squadUpdateStepPayload);

            verify(scaleApiSpy, times(1)).sendHttpPut(endpoint, squadUpdateStepPayload);
        }

        @Test
        void shouldThrowZephyrApiExceptionIfApiExceptionWhenUpdateTestStep() throws ApiException {

            doThrow(ApiException.class).when(scaleApiSpy).sendHttpPut(endpoint, squadUpdateStepPayload);

            assertThrows(ZephyrApiException.class, () -> scaleApiSpy.updateTestStep("TEST-1", squadUpdateStepPayload));

        }
    }

    @Nested
    class WhileInteractingWithTestCycle {

        private final String urlToCreate = ScaleApi.CREATE_SCALE_MIGRATION_TEST_CYCLE_ENDPOINT;

        private final String projectKey = "PROJECT";

        private final String cycleName = "cycleName";

        private final String cycleVersion = "0.1";

        private final Map<String, Object> paramsMock = new HashMap<>(Map.of(
                "name", cycleName,
                "projectKey", projectKey,
                "version", cycleVersion
        ));

        @Test
        void shouldCreateMigrationTestCycleAndReceiveKeyAsResponse() throws ApiException, ZephyrApiException {
            var responseMock = "{key:CYCLE-1}";

            doReturn(responseMock).when(scaleApiSpy).sendHttpPost(urlToCreate, paramsMock);

            var migrationTestCycleKeyReceived = scaleApiSpy.createMigrationTestCycle(projectKey, cycleName, cycleVersion);

            assertEquals("CYCLE-1", migrationTestCycleKeyReceived);
        }

        @Test
        void shouldThrowZephyrApiExceptionIfApiExceptionWhenCreatingMigrationTestCycle() throws ApiException {
            doThrow(ApiException.class).when(scaleApiSpy).sendHttpPost(urlToCreate, paramsMock);

            assertThrows(ZephyrApiException.class, () -> scaleApiSpy.createMigrationTestCycle(projectKey, cycleName, cycleVersion));
        }
    }

    @Nested
    class WhileInteractingWithTestResults {

        private final String cycleKey = "CYCLE-1";
        private final String endpoint = String.format(ScaleApi.CREATE_SCALE_TEST_RESULTS_ENDPOINT, cycleKey);


        private final ScaleExecutionCreationPayload scaleExecToCreate = new ScaleExecutionCreationPayload(
                "status", "TEST-1", "user", "executionDate","assignee", "comment", "1.0", List.of("issuekey"),
                List.of(new ScaleExecutionStepPayload(0, "status", "comment")), null);

        private final List<ScaleExecutionCreationPayload> testResultsToCreate = List.of(scaleExecToCreate);


        @Test
        void shouldIdWhenCreateTestResult() throws ApiException, ZephyrApiException {

            var testResultExpected = new ScaleTestResultCreatedPayload("1");

            String responseMock = "[{id:1}]";

            doReturn(responseMock).when(scaleApiSpy).sendHttpPost(
                    endpoint, testResultsToCreate);

            var testResultCreated = scaleApiSpy.createTestExecution(cycleKey, scaleExecToCreate);

            assertEquals(testResultExpected, testResultCreated);
        }

        @Test
        void shouldThrowZephyrApiExceptionIfApiExceptionWhenCreateTestResults() throws ApiException {
            doThrow(ApiException.class).when(scaleApiSpy).sendHttpPost(endpoint, testResultsToCreate);

            assertThrows(ZephyrApiException.class, () -> scaleApiSpy.createTestExecution(cycleKey, scaleExecToCreate));
        }
    }

    @Nested
    class WhileInteractingWithProjects {

        private final EnableProjectPayload projectToEnable = new EnableProjectPayload("PROJECT-1", true);

        @Test
        void shouldCallEnableProjectEndpointOnlyOnce() throws ApiException, ZephyrApiException {

            doReturn("").when(scaleApiSpy).sendHttpPost(ScaleApi.ENABLE_PROJECT_ENDPOINT, projectToEnable);

            scaleApiSpy.enableProject(projectToEnable);

            verify(scaleApiSpy, times(1)).sendHttpPost(ScaleApi.ENABLE_PROJECT_ENDPOINT, projectToEnable);
        }

        @Test
        void shouldThrowZephyrApiExceptionIfApiExceptionWhenEnableProject() throws ApiException {

            doThrow(ApiException.class).when(scaleApiSpy).sendHttpPost(ScaleApi.ENABLE_PROJECT_ENDPOINT, projectToEnable);

            assertThrows(ZephyrApiException.class, () -> scaleApiSpy.enableProject(projectToEnable));
        }
    }

    @Nested
    class WhileInteractingWithCustomField {

        private final ScaleCustomFieldPayload scaleCustomFieldPayload = new ScaleCustomFieldPayload("name",
                "cat", "PROJECT-1", "TYPE");

        @Test
        void shouldCallCreateCustomFieldEndpointOnlyOnce() throws ApiException, ZephyrApiException {

            doReturn("{id:1}").when(scaleApiSpy).sendHttpPost(ScaleApi.CREATE_CUSTOM_FIELD_ENDPOINT,
                    scaleCustomFieldPayload);

            scaleApiSpy.createCustomField(scaleCustomFieldPayload);

            verify(scaleApiSpy, times(1)).sendHttpPost(ScaleApi.CREATE_CUSTOM_FIELD_ENDPOINT,
                    scaleCustomFieldPayload);
        }

        @Test
        void shouldThrowZephyrApiExceptionIfApiExceptionWhileCreateCustomField() throws ApiException {

            doThrow(ApiException.class).when(scaleApiSpy).sendHttpPost(ScaleApi.CREATE_CUSTOM_FIELD_ENDPOINT,
                    scaleCustomFieldPayload);

            assertThrows(ZephyrApiException.class, () -> scaleApiSpy.createCustomField(scaleCustomFieldPayload));

        }

        @Test
        void shouldReturnGracefullyIfApiExceptionHasCode400AndCorrectMessageWhileCallingCustomField() throws ApiException {
            doThrow(new ApiException(400, ScaleApi.CUSTOM_FIELD_DUPLICATED_EXPECTED_MESSAGE)).when(scaleApiSpy)
                    .sendHttpPost(ScaleApi.CREATE_CUSTOM_FIELD_ENDPOINT, scaleCustomFieldPayload);

            assertDoesNotThrow(() -> scaleApiSpy.createCustomField(scaleCustomFieldPayload));
        }

        @Test
        void shouldThrowZephyrApiExceptionIfApiExceptionHasCode400AndWrongMessageWhileCallingCustomField() throws ApiException {
            doThrow(new ApiException(400, "Wrong Message")).when(scaleApiSpy)
                    .sendHttpPost(ScaleApi.CREATE_CUSTOM_FIELD_ENDPOINT, scaleCustomFieldPayload);

            assertThrows(ZephyrApiException.class, () -> scaleApiSpy.createCustomField(scaleCustomFieldPayload));
        }
    }
}
