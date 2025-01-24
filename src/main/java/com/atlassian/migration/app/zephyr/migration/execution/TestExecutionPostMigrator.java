package com.atlassian.migration.app.zephyr.migration.execution;

import com.atlassian.migration.app.zephyr.common.ApiException;

import com.atlassian.migration.app.zephyr.jira.api.JiraApi;

import com.atlassian.migration.app.zephyr.migration.model.SquadToScaleEntitiesMap;
import com.atlassian.migration.app.zephyr.migration.model.SquadToScaleTestExecutionMap;
import com.atlassian.migration.app.zephyr.migration.model.data.TestExecutionAssociatedData;
import com.atlassian.migration.app.zephyr.scale.model.GetProjectResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.*;


public class TestExecutionPostMigrator {

    private static final Logger logger = LoggerFactory.getLogger(TestExecutionPostMigrator.class);
    private final JiraApi jiraApi;
    private final TestExecutionCsvExporter testExecutionCsvExporter;

    private static final String[] csvHeader = {"ID", "SCALE_EXECUTION_ID", "CREATED_BY", "CREATED_ON", "MODIFIED_BY", "MODIFIED_ON"};

    private static final String[] csvMapping = {"id", "mappedScaleId", "createdBy", "createdOn", "modifiedBy", "modifiedOn"};

    private final Map<String, GetProjectResponse> projectMetadata = new HashMap<>();

    public TestExecutionPostMigrator(JiraApi jiraApi,
                                     TestExecutionCsvExporter testExecutionCsvExporter) {
        this.jiraApi = jiraApi;
        this.testExecutionCsvExporter = testExecutionCsvExporter;
    }

    public void export(SquadToScaleEntitiesMap entitiesMap, String projectKey) throws IOException {

        var project = projectMetadata.computeIfAbsent(projectKey, key -> {
            try {
                return jiraApi.getProjectByKeyWithHistoricalKeys(key);
            } catch (ApiException e) {
                throw new RuntimeException(e);
            }
        });

        SquadToScaleTestExecutionMap testExecutionsMap = entitiesMap.testExecutionMap();

        List<TestExecutionAssociatedData> associatedData = processTestExecutionAssociatedData(testExecutionsMap);
        try {
            logger.info("Exporting mapped executions to csv");
            testExecutionCsvExporter.dump(associatedData, csvHeader, csvMapping);
            logger.info("Exporting mapped executions to csv finished");
        } catch (IOException | URISyntaxException e) {
            logger.error("Failed to export mapped executions csv " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private List<TestExecutionAssociatedData> processTestExecutionAssociatedData(SquadToScaleTestExecutionMap testExecutionsMap) {
        List<TestExecutionAssociatedData> executionsMapped = new ArrayList<>();

        for(var testExecution:testExecutionsMap.entrySet()){
            SquadToScaleTestExecutionMap.TestExecutionMapKey key = testExecution.getKey();
            executionsMapped.add(TestExecutionAssociatedData.createExecutionAssociatedData(key.testExecutionId(),
                    testExecution.getValue(),
                    key.createdBy().toString(),
                    key.createdOn(),
                    key.modifiedBy(),
                    key.modifiedOn()));
        }
        return executionsMapped;
    }
}
