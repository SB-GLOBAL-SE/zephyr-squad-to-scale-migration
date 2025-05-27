package com.atlassian.migration.app.zephyr.migration;

import com.atlassian.migration.app.zephyr.common.DataSourceFactory;
import com.atlassian.migration.app.zephyr.common.ProgressBarUtil;
import com.atlassian.migration.app.zephyr.jira.api.JiraApi;
import com.atlassian.migration.app.zephyr.jira.model.JIRAVersionResponse;
import com.atlassian.migration.app.zephyr.jira.model.JiraIssuesResponse;
import com.atlassian.migration.app.zephyr.migration.database.DatabasePostRepository;
import com.atlassian.migration.app.zephyr.migration.execution.TestExecutionPostMigrator;
import com.atlassian.migration.app.zephyr.migration.model.*;
import com.atlassian.migration.app.zephyr.migration.service.Resettable;
import com.atlassian.migration.app.zephyr.migration.service.ScaleCycleService;
import com.atlassian.migration.app.zephyr.migration.service.ScaleTestCasePayloadFacade;
import com.atlassian.migration.app.zephyr.migration.service.ScaleTestExecutionPayloadFacade;
import com.atlassian.migration.app.zephyr.migration.testcase.TestCasePostMigrator;
import com.atlassian.migration.app.zephyr.scale.api.ScaleApi;
import com.atlassian.migration.app.zephyr.scale.model.*;
import com.atlassian.migration.app.zephyr.squad.api.SquadApi;
import com.atlassian.migration.app.zephyr.squad.model.FetchSquadStatusResponse;
import com.atlassian.migration.app.zephyr.squad.model.SquadExecutionStatusResponse;
import com.atlassian.migration.app.zephyr.squad.model.SquadTestStepResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.atlassian.migration.app.zephyr.scale.model.ScaleMigrationTestCaseCustomFieldPayload.MIGRATION_JIRA_SCALE_CUSTOM_FIELD_TYPES;

public class SquadToScaleMigrator {

    private static final Logger logger = LoggerFactory.getLogger(SquadToScaleMigrator.class);
    private static final List<String> IGNORABLE_SQUAD_STATUSES = List.of("PASS", "FAIL", "WIP", "BLOCKED", "UNEXECUTED");

    private static final int SCALE_DEFECT_DEFAULT_TYPE = 3;
    private final MigrationConfiguration config;
    private final JiraApi jiraApi;
    private final ScaleApi scaleApi;
    private final SquadApi squadApi;

    private final ScaleCycleService scaleCycleService;
    private final ScaleTestExecutionPayloadFacade scaleTestExecutionPayloadFacade;

    private final ScaleTestCasePayloadFacade scaleTestCaseFacade;

    private final AttachmentsMigrator attachmentsMigrator;
    private final TestCasePostMigrator testCasePostMigrator;
    private final TestExecutionPostMigrator testExecutionPostMigrator;
    private final List<Resettable> resettables = new ArrayList<>();

    public SquadToScaleMigrator(JiraApi jiraApi, SquadApi squadApi, ScaleApi scaleApi, AttachmentsMigrator attachmentsMigrator,
                                TestCasePostMigrator testCasePostMigrator,
                                TestExecutionPostMigrator testExecutionPostMigrator,
                                MigrationConfiguration migConfig) {
        this.jiraApi = jiraApi;
        this.scaleApi = scaleApi;
        this.squadApi = squadApi;
        this.config = migConfig;

        this.scaleTestExecutionPayloadFacade = new ScaleTestExecutionPayloadFacade(jiraApi);
        this.scaleCycleService = new ScaleCycleService(scaleApi, config.cycleNamePlaceHolder());

        resettables.addAll(List.of(scaleCycleService, scaleTestExecutionPayloadFacade));

        this.scaleTestCaseFacade = new ScaleTestCasePayloadFacade(jiraApi, scaleApi);
        this.attachmentsMigrator = attachmentsMigrator;
        this.testCasePostMigrator = testCasePostMigrator;
        this.testExecutionPostMigrator = testExecutionPostMigrator;
    }

    public void getProjectListAndRunMigration() {
        try {
            GetAllProjectsResponse getAllProjectsResponse = squadApi.getAllProjects();
            List<Option> projects = getAllProjectsResponse.options();
            int projectIndex = 0;
            long startTimeMillis = System.currentTimeMillis();

            for (Option option : projects) {
                logger.info("Project progress: " + ProgressBarUtil.getProgressBar(projectIndex++, projects.size(), startTimeMillis));
                String projectKey = jiraApi.getProjectById(option.value()).key();
                runMigration(projectKey);
                reset();
            }

            logger.info("Project progress: " + ProgressBarUtil.getProgressBar(projects.size(), projects.size(), startTimeMillis));
        } catch (Exception exception) {
            logger.error("Failed to get project List " + exception.getMessage(), exception);
            throw new RuntimeException(exception);
        }
    }

    public void runMigration(String projectKey) {
        try {
            logger.info("Fetching total issues by project key...");
            var total = jiraApi.fetchTotalIssuesByProjectName(projectKey);

            var projectResponse = jiraApi.getProjectByKey(projectKey);
            if (total == 0) {
                logger.info("Project doesn't have Squad Objects, skipping it");
                return;
            }

            logger.info("Total issues: " + total);

            logger.info("Enabling project in Scale...");
            scaleApi.enableProject(new EnableProjectPayload(projectKey, true));

            logger.info("Creating migration Custom Fields...");
            createMigrationCustomFields(projectKey, projectResponse.id());

            logger.info("Creating migration Scale statuses Fields...");
            createMigrationTestResultsStatuses(projectKey);

            logger.info("updating priorities & Statuses.");
            scaleApi.updateTestCasePriorities(projectResponse.id());
            scaleApi.updateTestCaseStatuses(projectResponse.id());

            var jiraversions = jiraApi.fetchJiraVersionsByProject(projectKey);
            JIRAVersionMap jiraVersionMap = new JIRAVersionMap();
            jiraVersionMap.putAll(jiraversions.stream().collect(Collectors.toMap(JIRAVersionResponse::name, JIRAVersionResponse::id)));

            var startAt = 0;
            long startTimeMillis = System.currentTimeMillis();

            SquadToScaleTestCaseMap testCaseMap = new SquadToScaleTestCaseMap();
            SquadToScaleTestStepMap testStepMap = new SquadToScaleTestStepMap();
            SquadToScaleTestExecutionMap testExecutionMap = new SquadToScaleTestExecutionMap();
            SquadToScaleExecutionStepMap executionStepMap = new SquadToScaleExecutionStepMap();
            SquadToScaleEntitiesMap squadToScaleEntitiesMap = new SquadToScaleEntitiesMap(testCaseMap, testStepMap, testExecutionMap, executionStepMap);

            while (startAt < total) {
                logger.info("Issue progress: "
                        + ProgressBarUtil.getProgressBar(startAt, total, startTimeMillis));

                processPage(startAt, projectKey, projectResponse.id(), squadToScaleEntitiesMap, jiraVersionMap);

                startAt += config.pageSteps();
            }

            scaleCycleService.updateIssueLinksforCycle(projectKey);

            logger.info("Post migrion steps started, attachments copy and export of mappings.");
            attachmentsMigrator.export(squadToScaleEntitiesMap, projectKey);
            testCasePostMigrator.export(squadToScaleEntitiesMap, projectKey);
            testExecutionPostMigrator.export(squadToScaleEntitiesMap, projectKey);

            if(config.updateDatabaseFieldsPostMigration()) {
                var dataSourceFactory = new DataSourceFactory();
                var dataSource = dataSourceFactory.createDataSourceFromDatabaseName(config.databaseType());
                DatabasePostRepository databasePostRepository = new DatabasePostRepository(dataSource, config.testCaseCSVFile(), config.testExecutionCSVFile());
                databasePostRepository.updateTestCaseFields();
                databasePostRepository.updateTestResultsFields();
                databasePostRepository.updateAttachmentRecords(config.attachmentsMappedCsvFile());
            }
            logger.info("Issue progress: "
                    + ProgressBarUtil.getProgressBar(total, total, startTimeMillis));
        } catch (Exception exception) {
            logger.error("Failed to run migration " + exception.getMessage(), exception);
            throw new RuntimeException(exception);
        }
    }

    private void processPage(int startAt, String projectKey, String projectId, SquadToScaleEntitiesMap squadToScaleEntitiesMap, JIRAVersionMap jiraVersionMap) {
        try {

            logger.info("Fetching issues starting at " + startAt + "...");

            var issues = jiraApi.fetchIssuesOrderedByCreatedDate(
                    projectKey,
                    startAt,
                    config.pageSteps());

            logger.info("Fetched " + issues.size() + " issues.");

            var testCaseMap = createScaleTestCases(issues, projectKey, projectId);
            updateStepsAndPostExecution(testCaseMap, projectKey, squadToScaleEntitiesMap, jiraVersionMap);
            squadToScaleEntitiesMap.testCaseMap().putAll(testCaseMap);
        } catch (IOException exception) {
            logger.error("Failed to process page with start at: " + startAt + " " + exception.getMessage(), exception);
            throw new RuntimeException(exception);
        }
    }

    private void createMigrationTestResultsStatuses(String projectKey) {
        try {
            GetProjectResponse projectResponse = jiraApi.getProject(projectKey);
            FetchSquadStatusResponse squadExecutionStatuses = squadApi.fetchLatestTestExecutionStatuses();
            FetchSquadStatusResponse squadStepResultsStatuses = squadApi.fetchLatestTestStepExecutionStatuses();

            var tobeCreatedStatuses = new LinkedHashMap<String, SquadExecutionStatusResponse>();
            var scaleTestResultsStatuses = new LinkedHashMap<String, ScaleResultsStatus>();
            var scaleTestResultsIgnoreCaseStatuses = new LinkedHashMap<String, String>();

            FetchScaleResultsStatusesResponse fetchscaleTestResultsStatuses = scaleApi.fetchTestResultsStatuses(projectResponse.id());
            if(fetchscaleTestResultsStatuses != null && fetchscaleTestResultsStatuses.data() != null){
                fetchscaleTestResultsStatuses.data().forEach( scaleResultsStatus -> {
                    scaleTestResultsStatuses.put(scaleResultsStatus.name(), scaleResultsStatus);
                    scaleTestResultsIgnoreCaseStatuses.put(scaleResultsStatus.name().toLowerCase(), scaleResultsStatus.name());
                });
            }

            // checking for SQUAD Execution statuses which are not part of scale test results statuses
            if(squadExecutionStatuses != null && squadExecutionStatuses.data() != null){
                squadApi.updateExecutionStatusTypes(squadExecutionStatuses.data());
                squadExecutionStatuses.data().forEach( executionStatus -> {
                    String statusName = executionStatus.name();
                    if(config.ignoreTestResultsStatusCase()){
                        if(!IGNORABLE_SQUAD_STATUSES.contains(statusName)){
                            if(!scaleTestResultsIgnoreCaseStatuses.containsKey(statusName.toLowerCase())) {
                                tobeCreatedStatuses.put(statusName, executionStatus);
                            }else{
                                scaleTestExecutionPayloadFacade.statusTranslation.put(statusName, scaleTestResultsIgnoreCaseStatuses.get(statusName.toLowerCase()));
                            }
                        }
                    }else {
                        if (!IGNORABLE_SQUAD_STATUSES.contains(statusName) && !scaleTestResultsStatuses.containsKey(statusName)) {
                            tobeCreatedStatuses.put(statusName, executionStatus);
                        }
                    }
                });
            }

            // checking for SQAUD Step Execution statuses which are not part of scale test results statuses
            if(squadStepResultsStatuses != null && squadStepResultsStatuses.data() != null){
                squadApi.updateExecutionStepStatusTypes(squadStepResultsStatuses.data());
                squadStepResultsStatuses.data().forEach( executionStatus -> {
                    String statusName = executionStatus.name();
                    if(config.ignoreTestResultsStatusCase()){
                        if(!IGNORABLE_SQUAD_STATUSES.contains(statusName)){
                            if(!scaleTestResultsIgnoreCaseStatuses.containsKey(statusName.toLowerCase())) {
                                tobeCreatedStatuses.put(statusName, executionStatus);
                            }else{
                                scaleTestExecutionPayloadFacade.statusTranslation.put(statusName, scaleTestResultsIgnoreCaseStatuses.get(statusName.toLowerCase()));
                            }
                        }
                    }else {
                        if(!IGNORABLE_SQUAD_STATUSES.contains(statusName) && !scaleTestResultsStatuses.containsKey(statusName)) {
                            tobeCreatedStatuses.put(statusName, executionStatus);
                        }
                    }
                });
            }

            for (var squadStatustoCreate : tobeCreatedStatuses.entrySet()) {
                scaleApi.createScaleTestResultsStatus(projectResponse.id(),
                        squadStatustoCreate.getValue().name(),
                        squadStatustoCreate.getValue().description(),
                        squadStatustoCreate.getValue().color()
                        );
                logger.info(String.format("Test results status with name '%s' has been created", squadStatustoCreate.getKey()));
            }
        } catch (IOException exception) {
            logger.error("Failed to create migration test results statuses " + exception.getMessage(),
                    exception);
            throw new RuntimeException(exception);
        }
    }

    private void createProjectCustomFields(String projectKey, String id){
        try{
            var projectCustomFields = ScaleProjectTestCaseCustomFieldPayload.CUSTOM_FIELD_ID_TO_NAMES;
            var testCustomFields = scaleApi.fetchTestCaseCustomFields(id);
            Map<String, ScaleTestCaseCustomField> fieldNameAndIds = new HashMap<>();
            if(testCustomFields != null && testCustomFields.size() > 0){
                fieldNameAndIds = testCustomFields.stream()
                        .filter(field -> !field.archived()) // Filter based on name
                        .collect(Collectors.toMap(ScaleTestCaseCustomField::name,
                                field -> field));
            }
            for(var customField : projectCustomFields.values()){
                String customFieldName = customField.name();
                if(fieldNameAndIds.containsKey(customFieldName)){
                    ScaleTestCaseCustomField scaleTestCaseCustomField = fieldNameAndIds.get(customFieldName);
                    ScaleProjectTestCaseCustomFieldPayload.SCALE_CUSTOMFIELD_NAME_ID.put(customFieldName, String.valueOf(scaleTestCaseCustomField.id()));
                    if(scaleTestCaseCustomField.options() != null && scaleTestCaseCustomField.options().size() > 0) {
                        ScaleProjectTestCaseCustomFieldPayload.SCALE_CUSTOMFIELD_NAME_OPTIONS.put(customFieldName,
                                scaleTestCaseCustomField.options().stream().map(ScaleTestCaseCustomFieldOption::name).collect(Collectors.toSet()));
                    }
                    continue;
                }
                logger.info("Creating Project Custom Field " + customFieldName + " ...");
                var customFieldId = scaleApi.createCustomField(
                        new ScaleCustomFieldPayload(
                                customFieldName,
                                "TEST_CASE",
                                projectKey,
                                customField.type()
                        ));
                ScaleProjectTestCaseCustomFieldPayload.SCALE_CUSTOMFIELD_NAME_ID.put(customFieldName, customFieldId);
                if (customFieldId != null && !customFieldId.isBlank()
                        && customField.type().equals(ScaleCustomFieldPayload.SINGLE_CHOICE_SELECT_LIST)){
                    Set<String> options = new HashSet<>();
                    for(var option : customField.options()){
                        scaleApi.addOptionToCustomField(customFieldId, option);
                        options.add(option.name());
                    }
                    ScaleProjectTestCaseCustomFieldPayload.SCALE_CUSTOMFIELD_NAME_OPTIONS.put(customFieldName,
                        options);
                }
                logger.info("Project Custom Field " + customFieldName + " created successfully.");
            }

        } catch (IOException exception) {
            logger.error("Failed to create project custom fields " + exception.getMessage(),
                    exception);
            throw new RuntimeException(exception);
        }
    }

    private void createMigrationCustomFields(String projectKey, String id) {
        try {
            Map<String, List<String>> mapCustomFieldsToCreate = Map.of(
                    ScaleMigrationTestCaseCustomFieldPayload.ENTITY_TYPE, ScaleMigrationTestCaseCustomFieldPayload.CUSTOM_FIELDS_NAMES,
                    ScaleMigrationExecutionCustomFieldPayload.ENTITY_TYPE, ScaleMigrationExecutionCustomFieldPayload.CUSTOM_FIELDS_NAMES
            );

            Map<String, String> customFieldToType = new HashMap<>();
            customFieldToType.putAll(ScaleMigrationTestCaseCustomFieldPayload.MIGRATION_CUSTOM_FIELDS);
            customFieldToType.putAll(ScaleMigrationExecutionCustomFieldPayload.CUSTOM_FIELD_TO_TYPE);

            for (var customFieldToCreate : mapCustomFieldsToCreate.entrySet()) {

                for (var customFieldName : customFieldToCreate.getValue()) {
                    logger.info("Creating Migration Custom Field " + customFieldName + " ...");

                    String type = customFieldToType.get(customFieldName);
                    String customFieldId = scaleApi.createCustomField(
                            new ScaleCustomFieldPayload(
                                    customFieldName,
                                    customFieldToCreate.getKey(),
                                    projectKey,
                                    type
                            )
                    );

                    logger.info("Migration Custom Field " + customFieldName + " created successfully.");
                }
            }

            createProjectCustomFields(projectKey, id);
//            var customFieldResponse = jiraApi.fetchCustomFieldsFromProjectByProjectId(id);
//            if(customFieldResponse != null && customFieldResponse.size() > 0){
//                for(var customField:customFieldResponse){
//                    logger.info("Creating Migration Custom Field " + customField.name() + " ...");
//
//                    scaleApi.createCustomField(
//                            new ScaleCustomFieldPayload(
//                                    customField.name(),
//                                    ScaleMigrationTestCaseCustomFieldPayload.ENTITY_TYPE,
//                                    projectKey,
//                                    MIGRATION_JIRA_SCALE_CUSTOM_FIELD_TYPES.get(customField.type())
//                            )
//                    );
//                    logger.info("Migration Custom Field " + customField.name() + " created successfully.");
//                }
//            }

        } catch (IOException exception) {
            logger.error("Failed to create migration custom fields " + exception.getMessage(),
                    exception);
            throw new RuntimeException(exception);
        }
    }

    private SquadToScaleTestCaseMap createScaleTestCases(List<JiraIssuesResponse> issues, String
            projectKey, String projectId) throws IOException {
        try {
            var map = new SquadToScaleTestCaseMap();
            for (var issue : issues) {
                var scaleTestCaseKey = createTestCaseForIssue(issue, projectKey, projectId);
                String creatorKey = (issue.fields().creator != null && issue.fields().creator.key() != null)
                        ? issue.fields().creator.key()
                        : null;
                var issueLinks = this.scaleTestCaseFacade.getIssueLinksIssueIds(issue);
                map.put(new SquadToScaleTestCaseMap.TestCaseMapKey(issue.id(), issue.key(), creatorKey, issue.fields().created, null, issue.fields().updated, issueLinks), scaleTestCaseKey);
            }
            return map;
        } catch (IOException exception) {
            logger.error("Failed to create Scale test cases " + exception.getMessage(), exception);
            throw new RuntimeException(exception);
        }
    }

    private String createTestCaseForIssue(JiraIssuesResponse issue, String projectKey, String projectId) throws
            IOException {

        try {
            String sanitizeStatus = scaleTestCaseFacade.sanitizeStatus(issue.fields().status);
            if(sanitizeStatus != null && !sanitizeStatus.isEmpty() && !ScaleMigrationTestCaseStatusPayload.MIGRATION_TESTCASE_STATUSES.contains(sanitizeStatus)){
                String id = scaleApi.CreateScaleTestcaseStatus(projectId, sanitizeStatus);
                ScaleMigrationTestCaseStatusPayload.MIGRATION_TESTCASE_STATUSES.add(sanitizeStatus);
            }

            String sanitizepriority = scaleTestCaseFacade.sanitizePriority(issue.fields().priority);
            if(sanitizepriority != null && !sanitizepriority.isEmpty() && !ScaleMigrationTestCasePriorityPayload.MIGRATION_TESTCASE_PRIORITIES.contains(sanitizepriority)){
                String id = scaleApi.CreateScaleTestcasePriority(projectId, sanitizepriority);
                ScaleMigrationTestCasePriorityPayload.MIGRATION_TESTCASE_PRIORITIES.add(sanitizepriority);
            }
            ScaleTestCaseCreationPayload testCasePayload = this.scaleTestCaseFacade.createTestCasePayload(issue, projectKey);

            var scaleTestCaseKey = scaleApi.createTestCases(testCasePayload);

            logger.info("Created Scale test Case from Squad test case " + issue.id() + ".");

            return scaleTestCaseKey;
        } catch (IOException exception) {
            logger.error("Failed to create Scale Test Case from Squad test case with id: " + issue.id() + " " + exception.getMessage(), exception);
            throw new RuntimeException(exception);
        }
    }

    private void updateStepsAndPostExecution(SquadToScaleTestCaseMap testCaseMap, String projectKey,
                                             SquadToScaleEntitiesMap squadToScaleEntitiesMap, JIRAVersionMap jiraVersionMap) throws IOException {
        try {
            var orderedIssueList = testCaseMap.getListOfAllEntriesOrdered();


            logger.info("Updating steps and posting execution for " + orderedIssueList.size() + " issues...");

            var testStepMap = new SquadToScaleTestStepMap();
            var testExecutionMap = new SquadToScaleTestExecutionMap();
            var squadToScaleExecutionStepMap = new SquadToScaleExecutionStepMap();
            for (var testCaseItem : orderedIssueList) {
                testStepMap.putAll(updateStepsForTestCase(testCaseItem));
                testExecutionMap.putAll(createTestExecutionForTestCase(testCaseItem, projectKey, squadToScaleExecutionStepMap, jiraVersionMap));
            }

            logger.info("Updated steps and created test executions for " + orderedIssueList.size() + " issues.");
            squadToScaleEntitiesMap.testStepMap().putAll(testStepMap);
            squadToScaleEntitiesMap.testExecutionMap().putAll(testExecutionMap);
            squadToScaleEntitiesMap.executionStepMap().putAll(squadToScaleExecutionStepMap);

        } catch (IOException exception) {
            logger.error("Failed to update steps and post execution " + exception.getMessage(), exception);
            throw new RuntimeException(exception);
        }
    }

    private SquadToScaleTestStepMap updateStepsForTestCase
            (Map.Entry<SquadToScaleTestCaseMap.TestCaseMapKey, String> testCaseItem)
            throws IOException {
        try {
            logger.info("Fetching latest Squad test step from " + testCaseItem.getKey().testCaseId() + "...");

            var testStepMap = new SquadToScaleTestStepMap();

            var squadTestSteps = squadApi.fetchLatestTestStepByTestCaseId(testCaseItem.getKey().testCaseId());

            if (squadTestSteps.stepBeanCollection().isEmpty()) {
                return testStepMap;
            }

            var steps = new SquadUpdateStepPayload(new SquadGETStepItemPayload());

            steps.testScript().steps = squadTestSteps.stepBeanCollection().stream()
                    .map(e -> ScaleGETStepItemPayload.createScaleGETStepItemPayloadForCreation(
                            e.htmlStep(),
                            e.htmlData(),
                            e.htmlResult())).toList();

            logger.info("Updating steps for scale test case...");
            scaleApi.updateTestStep(testCaseItem.getValue(), steps);

            //only mapping if updateTestStep was successful
            testStepMap.put(testCaseItem.getValue(),
                    squadTestSteps.stepBeanCollection().stream().collect(Collectors
                            .toMap(testStepResponse -> new SquadToScaleTestStepMap.TestStepMapKey(
                                    testStepResponse.id(), testStepResponse.orderId()
                            ), SquadTestStepResponse::attachmentsMap)));
            return testStepMap;
        } catch (IOException exception) {
            logger.error("Failed to update steps for Scale test case with test case id: " + testCaseItem.getKey().testCaseId() + " " + exception.getMessage(), exception);
            throw new RuntimeException(exception);
        }
    }

    private SquadToScaleTestExecutionMap createTestExecutionForTestCase
            (Map.Entry<SquadToScaleTestCaseMap.TestCaseMapKey,
                    String> item, String projectKey, SquadToScaleExecutionStepMap squadToScaleExecutionStepMap,
                    JIRAVersionMap jiraVersionMap) throws IOException {
        try {
            logger.info("Fetching latest Squad execution for test case " + item.getKey().testCaseId() + "...");
            var execData = squadApi.fetchLatestExecutionByIssueId(item.getKey().testCaseId());

            var executions = execData.executions();

            var testExecutionMap = new SquadToScaleTestExecutionMap();


            if (executions.isEmpty()) {
                logger.info("Test case " + item.getKey().testCaseId() + " doesn't have executions, skipping...");
                return testExecutionMap;
            }

            for (var execution : executions) {

                SquadToScaleExecutionStepMap executionStepMap = new SquadToScaleExecutionStepMap();
                var scaleCycleKey = scaleCycleService.getCycleKeyBySquadCycleName(execution.cycleName(),
                        projectKey, execution.versionName());

                logger.info("Creating test executions...");

                var testExectuionStepResponse = squadApi.fetchTestExecutionStepById(execution.id());
                var testExecutionPayload = scaleTestExecutionPayloadFacade
                        .buildPayload(execution, item.getValue(), projectKey, testExectuionStepResponse);

                var scaleTestExecutionCreatedPayload = scaleApi.createTestExecution(scaleCycleKey,
                        testExecutionPayload);
                if(item.getKey().issueLinks() != null && item.getKey().issueLinks().size() > 0) {
                    scaleCycleService.addIssueLinkstoCycleService(scaleCycleKey, new HashSet<>(item.getKey().issueLinks()));
                }
//                testExecutionMap.put(new SquadToScaleTestExecutionMap.TestExecutionMapKey(execution.id()),
                if(jiraVersionMap.containsKey(testExecutionPayload.version())){
                    String jiraVersionId = jiraVersionMap.get(testExecutionPayload.version());
                    ScaleExecutionVersionPayload scaleExecutionVersionPayload = new ScaleExecutionVersionPayload(Integer.parseInt(scaleTestExecutionCreatedPayload.id()), jiraVersionId);
                    scaleApi.updateTestResultVersion(scaleTestExecutionCreatedPayload.id(), scaleExecutionVersionPayload);
                }
                testExecutionMap.put(new SquadToScaleTestExecutionMap.TestExecutionMapKey(execution.id(), execution.createdBy(), execution.createdOn(), null, null, execution.executedOn() == null ? null : execution.executedOn().toString()),
                        scaleTestExecutionCreatedPayload.id());

                // fetching Step Results or Execution Step Mapping
                var fetchScaleTestResults = scaleApi.fetchTestResultsbyId(scaleTestExecutionCreatedPayload.id());
                if(testExectuionStepResponse != null &&
                        testExectuionStepResponse.executionSteps() !=null &&
                        testExectuionStepResponse.executionSteps().size() > 0){
                    if(fetchScaleTestResults != null &&
                        fetchScaleTestResults.testScriptResults() != null &&
                        fetchScaleTestResults.testScriptResults().size() > 0){
                        List<ScaleTestScriptResults> scaleTestScriptResultsMap = fetchScaleTestResults.testScriptResults();
                        int index = 0;
                        int length = scaleTestScriptResultsMap.size();
                        for(var executionStepRespone:testExectuionStepResponse.executionSteps()){
                           if(index >= length){
                               break;
                           }

                           var scaleTestScriptResults = scaleTestScriptResultsMap.get(index);
                           var squadExecutionStep = new SquadToScaleExecutionStepMap.SquadExecutionStepMapKey(executionStepRespone.id(), execution.id(), executionStepRespone.attachmentCount(), executionStepRespone.defects());
                           var scaleExecutionStep = new SquadToScaleExecutionStepMap.ScaleExecutionStepMapValue(scaleTestScriptResults.id(), scaleTestExecutionCreatedPayload.id());
                           executionStepMap.put(squadExecutionStep, scaleExecutionStep);
                           index++;
                        }
                    }
                }
                if(executionStepMap != null && executionStepMap.size() > 0){
                    var stepResultsHasDefects = executionStepMap.getExecutionStepMapHasDefects();
                    if(stepResultsHasDefects.size() > 0){
                        List<ScaleExecutionStepDefectsPayload> defectsPayloads = new LinkedList<>();
                        for(var scriptResultEntry:stepResultsHasDefects){
                            defectsPayloads.addAll(createScaleStepResultDefects(scriptResultEntry.getKey(), scriptResultEntry.getValue()));

                        }
                        scaleApi.updateTestStepdefects(defectsPayloads);
                    }
                }
                squadToScaleExecutionStepMap.putAll(executionStepMap);
            }

            return testExecutionMap;
        } catch (IOException exception) {
            logger.error("Failed to create test executions for Scale test case. " + exception.getMessage(), exception);
            throw new RuntimeException(exception);
        }
    }

    private List<ScaleExecutionStepDefectsPayload> createScaleStepResultDefects(SquadToScaleExecutionStepMap.SquadExecutionStepMapKey squadExecutionStepMapKey,
                                                                                SquadToScaleExecutionStepMap.ScaleExecutionStepMapValue scaleExecutionStepMapValue) throws IOException {
        List<ScaleExecutionStepDefectsPayload> defectsPayloads = new LinkedList<>();
        for(String defect:squadExecutionStepMapKey.defects()){
            try {
                var jiraIssueResponse = jiraApi.getIssueByIssueKey(defect);
                String issueId = jiraIssueResponse.id();
                int testScriptResultId = scaleExecutionStepMapValue.testScriptResultId();
                int testresultId = Integer.parseInt(scaleExecutionStepMapValue.testResultId());
                defectsPayloads.add(new ScaleExecutionStepDefectsPayload(testresultId, testScriptResultId, SCALE_DEFECT_DEFAULT_TYPE, issueId));
            }catch (Exception e){
                logger.error("Unable to create defect for step result with defect key: "+defect);
            }
        }
        return defectsPayloads;
    }

    //clearing per project caches to avoid heavy memory usage and conflicts
    private void reset() {
        resettables.forEach(Resettable::reset);
    }

}