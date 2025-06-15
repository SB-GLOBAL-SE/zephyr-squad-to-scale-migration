package com.atlassian.migration.app.zephyr.migration.testcase;

import com.atlassian.migration.app.zephyr.common.ApiException;
import com.atlassian.migration.app.zephyr.jira.api.JiraApi;
import com.atlassian.migration.app.zephyr.migration.model.SquadToScaleEntitiesMap;
import com.atlassian.migration.app.zephyr.migration.model.SquadToScaleTestCaseMap;
import com.atlassian.migration.app.zephyr.migration.model.data.TestCaseAssociatedData;
import com.atlassian.migration.app.zephyr.scale.model.GetProjectResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TestCasePostMigrator {

    private static final Logger logger = LoggerFactory.getLogger(TestCasePostMigrator.class);
    private final JiraApi jiraApi;
    private final TestCaseCsvExporter testCaseCsvExporter;

    private static final String[] csvHeader = {"ID", "SCALE_TESTCASE_ID", "CREATED_BY", "CREATED_ON", "MODIFIED_BY", "MODIFIED_ON"};

    private static final String[] csvMapping = {"id", "mappedScaleId", "createdBy", "createdOn", "modifiedBy", "modifiedOn"};

    private final Map<String, GetProjectResponse> projectMetadata = new HashMap<>();

    public TestCasePostMigrator(JiraApi jiraApi,
                                TestCaseCsvExporter testCaseCsvExporter) {
        this.jiraApi = jiraApi;
        this.testCaseCsvExporter = testCaseCsvExporter;
    }

    public void export(SquadToScaleEntitiesMap entitiesMap, String projectKey) throws IOException {

        var project = projectMetadata.computeIfAbsent(projectKey, key -> {
            try {
                return jiraApi.getProjectByKeyWithHistoricalKeys(key);
            } catch (ApiException e) {
                throw new RuntimeException(e);
            }
        });

        SquadToScaleTestCaseMap testCasesMap = entitiesMap.testCaseMap();

        List<TestCaseAssociatedData> associatedData = processTestExecutionAssociatedData(testCasesMap);
        try {
            logger.info("Exporting mapped test cases to csv");
            testCaseCsvExporter.dump(associatedData, csvHeader, csvMapping);
            logger.info("Exporting mapped test cases to csv finished");
        } catch (IOException | URISyntaxException e) {
            logger.error("Failed to export mapped test cases csv " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private List<TestCaseAssociatedData> processTestExecutionAssociatedData(SquadToScaleTestCaseMap testCasesMap) {
        List<TestCaseAssociatedData> executionsMapped = new ArrayList<>();

        for(var testCase:testCasesMap.entrySet()){
            SquadToScaleTestCaseMap.TestCaseMapKey key = testCase.getKey();
            if(key.createdBy() != null) {
                executionsMapped.add(TestCaseAssociatedData.createExecutionAssociatedData(key.testCaseId(),
                        testCase.getValue(),
                        key.createdBy() == null ? null : key.createdBy().toString(),
                        key.createdOn(),
                        key.modifiedBy(),
                        key.modifiedOn()));
            }
        }
        return executionsMapped;
    }
}
