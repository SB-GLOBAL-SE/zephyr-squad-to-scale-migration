package com.atlassian.migration.app.zephyr.migration;

import com.atlassian.migration.app.zephyr.common.ApiException;
import com.atlassian.migration.app.zephyr.common.DataSourceFactory;
import com.atlassian.migration.app.zephyr.common.ProgressBarUtil;
import com.atlassian.migration.app.zephyr.common.ZephyrApiException;
import com.atlassian.migration.app.zephyr.jira.api.JiraApi;
import com.atlassian.migration.app.zephyr.jira.model.GetJiraIssueFieldResponse;
import com.atlassian.migration.app.zephyr.jira.model.IssueType;
import com.atlassian.migration.app.zephyr.jira.model.JiraCustomFieldOptionValue;
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
import com.atlassian.migration.app.zephyr.scale.database.ScaleTestCaseRepository;
import com.atlassian.migration.app.zephyr.scale.model.*;
import com.atlassian.migration.app.zephyr.squad.api.SquadApi;
import com.atlassian.migration.app.zephyr.squad.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SquadToScaleMigrator {

    private static final Logger logger = LoggerFactory.getLogger(SquadToScaleMigrator.class);
    private static final List<String> IGNORABLE_SQUAD_STATUSES = List.of("PASS", "FAIL", "WIP", "BLOCKED", "UNEXECUTED");
    private static final List<String> SQUAD_CUSTOM_FIELDS_HAS_OPTIONS = List.of("CHECKBOX", "RADIO_BUTTON", "MULTI_SELECT", "SINGLE_SELECT");
    private static final List<String> JIRA_CUSTOM_FIELDS_HAS_OPTIONS = List.of(
            "com.atlassian.jira.plugin.system.customfieldtypes:multicheckboxes",
            "com.atlassian.jira.plugin.system.customfieldtypes:radiobuttons",
            "com.atlassian.jira.plugin.system.customfieldtypes:select",
            "com.atlassian.jira.plugin.system.customfieldtypes:multiselect"
    );
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

    private List<String> projectExecutionCustomFieldNames = new ArrayList<>();
    private Map<String, ScaleCustomFieldResponse> projectTestcaseCustomFieldNames = new HashMap();
    private Map<String, ScaleCustomFieldResponse> projectTestStepCustomFields = new HashMap<>();

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

        this.scaleTestCaseFacade = new ScaleTestCasePayloadFacade(jiraApi);
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
                runMigration(jiraApi.getProjectById(option.value()).key());
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

            //resetting the project custom fields.
            projectExecutionCustomFieldNames = new ArrayList<String>();
            projectTestStepCustomFields = new HashMap<>();

            var projectResponse = jiraApi.getProjectByKey(projectKey);
            if (total == 0) {
                logger.info("Project doesn't have Squad Objects, skipping it");
                return;
            }

            var testIssueTypeId = getTestIssueTypeIdFrom(projectResponse.issueTypes());
            logger.info("Issue type id: " + testIssueTypeId);
            logger.info("Total issues: " + total);

            logger.info("Enabling project in Scale...");
            scaleApi.enableProject(new EnableProjectPayload(projectKey, true));

            logger.info("updating priorities & Statuses.");
            scaleApi.updateTestCasePriorities(projectResponse.id());
            scaleApi.updateTestCaseStatuses(projectResponse.id());

            logger.info("Creating migration Custom Fields...");
            createMigrationCustomFields(projectKey, projectResponse.id(), testIssueTypeId);

            logger.info("Creating migration Scale statuses Fields...");
            createMigrationTestResultsStatuses(projectKey);

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

                processPage(startAt, projectKey, projectResponse.id(), squadToScaleEntitiesMap);

                startAt += config.pageSteps();
            }

            logger.info("Post migrion steps started, attachments copy and export of mappings.");
            attachmentsMigrator.export(squadToScaleEntitiesMap, projectKey);
            testCasePostMigrator.export(squadToScaleEntitiesMap, projectKey);
            testExecutionPostMigrator.export(squadToScaleEntitiesMap, projectKey);

            if(config.updateDatabaseFieldsPostMigration()) {
                var dataSourceFactory = new DataSourceFactory();
                var dataSource = dataSourceFactory.createDataSourceFromDatabaseName(config.databaseType());
                DatabasePostRepository databasePostRepository = new DatabasePostRepository(dataSource,
                        config.testCaseCSVFile(), config.testExecutionCSVFile(), config.databaseType());
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

    private String getTestIssueTypeIdFrom(List<IssueType> issueTypes) {
        if(issueTypes == null || issueTypes.size() <= 0){
            return null;
        }
        for(IssueType issueType:issueTypes){
            if(issueType.name().equals("Test")){
                return issueType.id();
            }
        }
        return null;
    }

    private void processPage(int startAt, String projectKey, String projectId, SquadToScaleEntitiesMap squadToScaleEntitiesMap) {
        try {

            logger.info("Fetching issues starting at " + startAt + "...");

            var issues = jiraApi.fetchIssuesOrderedByCreatedDate(
                    projectKey,
                    startAt,
                    config.pageSteps());

            logger.info("Fetched " + issues.size() + " issues.");

            var testCaseMap = createScaleTestCases(issues, projectKey, projectId);
            updateStepsAndPostExecution(testCaseMap, projectKey, squadToScaleEntitiesMap);
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

            FetchScaleResultsStatusesResponse fetchscaleTestResultsStatuses = scaleApi.fetchTestResultsStatuses(projectResponse.id());
            if(fetchscaleTestResultsStatuses != null && fetchscaleTestResultsStatuses.data() != null){
                fetchscaleTestResultsStatuses.data().forEach( scaleResultsStatus -> {
                    scaleTestResultsStatuses.put(scaleResultsStatus.name(), scaleResultsStatus);
                });
            }
            // checking for SQUAD Execution statuses which are not part of scale test results statuses
            if(squadExecutionStatuses != null && squadExecutionStatuses.data() != null){
                squadApi.updateExecutionStatusTypes(squadExecutionStatuses.data());
                squadExecutionStatuses.data().forEach( executionStatus -> {
                    String statusName = executionStatus.name();
                    if(!IGNORABLE_SQUAD_STATUSES.contains(statusName) && !scaleTestResultsStatuses.containsKey(statusName)) {
                        tobeCreatedStatuses.put(statusName, executionStatus);
                    }
                });
            }

            // checking for SQAUD Step Execution statuses which are not part of scale test results statuses
            if(squadStepResultsStatuses != null && squadStepResultsStatuses.data() != null){
                squadApi.updateExecutionStepStatusTypes(squadStepResultsStatuses.data());
                squadStepResultsStatuses.data().forEach( executionStatus -> {
                    String statusName = executionStatus.name();
                    if(!IGNORABLE_SQUAD_STATUSES.contains(statusName) && !scaleTestResultsStatuses.containsKey(statusName)) {
                        tobeCreatedStatuses.put(statusName, executionStatus);
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

   /* private void createProjectCustomFields(String projectKey){
        try{
            var projectCustomFields = ScaleProjectTestCaseCustomFieldPayload.CUSTOM_FIELD_ID_TO_NAMES;

            for(var customField : projectCustomFields.values()){
                logger.info("Creating Project Custom Field " + customField.name() + " ...");
                var customFieldId = scaleApi.createCustomField(
                        new ScaleCustomFieldPayload(
                                customField.name(),
                                "TEST_CASE",
                                projectKey,
                                customField.type()
                        ));

                if (customFieldId != null && !customFieldId.isBlank()
                        && customField.type().equals(ScaleCustomFieldPayload.SINGLE_CHOICE_SELECT_LIST)){
                    for(var option : customField.options()){
                        scaleApi.addOptionToCustomField(customFieldId, option);
                    }
                }
                logger.info("Project Custom Field " + customField.name() + " created successfully.");
            }
        } catch (IOException exception) {
            logger.error("Failed to create project custom fields " + exception.getMessage(),
                    exception);
            throw new RuntimeException(exception);
        }
    }*/

    private void createMigrationCustomFields(String projectKey, String projectId, String testIssueTypeId) {
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

                    scaleApi.createCustomField(
                            new ScaleCustomFieldPayload(
                                    customFieldName,
                                    customFieldToCreate.getKey(),
                                    projectKey,
                                    customFieldToType.get(customFieldName)
                            )
                    );
                    logger.info("Migration Custom Field " + customFieldName + " created successfully.");
                }
            }
            //create Migration CustomFields for Testcase
            migrateTestCaseCustomFields(projectKey, projectId, testIssueTypeId);

            // create Migration CustomFields for Execution.
            migrateTestExecutionCustomFields(projectKey, projectId);

            // create Migration CustomFields for TestSteo.
            migrateTeststepCustomFields(projectKey, projectId);
        } catch (IOException exception) {
            logger.error("Failed to create migration custom fields " + exception.getMessage(),
                    exception);
            throw new RuntimeException(exception);
        }
    }

    private void migrateTestCaseCustomFields(String projectKey, String projectId, String testIssueTypeId) throws IOException {
        var issueFieldsResponse = jiraApi.getIssueFieldsByIssuetype(projectId, testIssueTypeId);
        List<GetJiraIssueFieldResponse> mandatoryCustomFields = new LinkedList<>();
        Map<String, String> tobeMigrateFieldsNameandId = new LinkedHashMap<>();
        if(issueFieldsResponse != null && issueFieldsResponse.values() != null && issueFieldsResponse.values().size() > 0){
            mandatoryCustomFields.addAll(issueFieldsResponse.values().stream()
                    .filter(f -> f.required())
                    .filter(f -> f.fieldId().startsWith("customfield_"))
                    .collect(Collectors.toList()));
        }
        if(mandatoryCustomFields.size() > 0){
            for(GetJiraIssueFieldResponse jiraissueField:mandatoryCustomFields){
                if(!ScaleCustomFieldPayload.JIRA_SCALE_CUSTOM_FIELD_TYPE.containsKey(jiraissueField.schema().custom())){
                    continue;
                }
                List<ScaleTestCaseCustomFieldOption> options = null;
                if(JIRA_CUSTOM_FIELDS_HAS_OPTIONS.contains(jiraissueField.schema().custom())){
                    options = new LinkedList<>();
                    if(jiraissueField.allowedValues() != null &&
                            jiraissueField.allowedValues().size() > 0){
                        int index = 1;
                        for(JiraCustomFieldOptionValue customFieldOptionValue:jiraissueField.allowedValues()){
                            options.add(new ScaleTestCaseCustomFieldOption(customFieldOptionValue.value(), index, false));
                            index++;
                        }
                    }
                }
                String customFieldName = jiraissueField.name();
                var customFieldId = scaleApi.createCustomField(
                        new ScaleCustomFieldPayload(
                                customFieldName,
                                ScaleMigrationTestCaseCustomFieldPayload.ENTITY_TYPE,
                                projectKey,
                                ScaleCustomFieldPayload.JIRA_SCALE_CUSTOM_FIELD_TYPE.get(jiraissueField.schema().custom())
                        )
                );
                if(customFieldId != null && !customFieldId.isEmpty() && options != null && options.size() > 0) {
                    for (var option : options) {
                        scaleApi.addOptionToCustomField(customFieldId, option);
                    }
                }
                if(!tobeMigrateFieldsNameandId.containsKey(customFieldName)) {
                    tobeMigrateFieldsNameandId.put(customFieldName, jiraissueField.fieldId());
                }
                Map<String, ScaleCustomFieldResponse> tempCustomFieldsMap = new HashMap<>();
                List<ScaleCustomFieldResponse> scaleTestcaseCustomFields = scaleApi.fetchScaleCustomFields("testcase", projectId);
                if(scaleTestcaseCustomFields != null && scaleTestcaseCustomFields.size() >0) {
                    tempCustomFieldsMap.putAll(scaleTestcaseCustomFields.stream().filter(e -> tobeMigrateFieldsNameandId.containsKey(e.name())).collect(Collectors.toMap(ScaleCustomFieldResponse::name, Function.identity())));
                }
                for(String fieldName:tobeMigrateFieldsNameandId.keySet()){
                    if(tempCustomFieldsMap.containsKey(fieldName)){
                        projectTestcaseCustomFieldNames.put(tobeMigrateFieldsNameandId.get(fieldName), tempCustomFieldsMap.get(fieldName));
                    }
                }
                logger.info("Migration Custom Field " + customFieldName + " created successfully.");
            }
        }
    }

    private void migrateTestExecutionCustomFields(String projectKey, String projectId) throws ApiException, ZephyrApiException {
        FetchSquadCustomFieldResponse fetchSquadCustomFieldResponse = squadApi.fetchSquadCustomFieldResponse("EXECUTION", projectId);
        if(fetchSquadCustomFieldResponse != null && fetchSquadCustomFieldResponse.data().size() > 0){
            for(SquadCustomFieldResponse squadCustomFieldResponse:fetchSquadCustomFieldResponse.data()){
                if(squadCustomFieldResponse.isActive()){
                    List<ScaleTestCaseCustomFieldOption> options = null;
                    if(SQUAD_CUSTOM_FIELDS_HAS_OPTIONS.contains(squadCustomFieldResponse.fieldType())){
                        options = new LinkedList<>();
                        if(squadCustomFieldResponse.customFieldOptionValues() != null &&
                            squadCustomFieldResponse.customFieldOptionValues().size() > 0){
                            int index = 1;
                            for(Map.Entry<String, String> customFieldEntry:squadCustomFieldResponse.customFieldOptionValues().entrySet()){
                                options.add(new ScaleTestCaseCustomFieldOption(customFieldEntry.getValue(), index, false));
                                index++;
                            }
                        }
                    }
                    String customFieldName = squadCustomFieldResponse.name();
                    var customFieldId = scaleApi.createCustomField(
                            new ScaleCustomFieldPayload(
                                    customFieldName,
                                    ScaleMigrationExecutionCustomFieldPayload.ENTITY_TYPE,
                                    projectKey,
                                    ScaleCustomFieldPayload.SQUAD_SCALE_CUSTOM_FIELD_TYPE.get(squadCustomFieldResponse.fieldType())
                            )
                    );
                    if(customFieldId != null && !customFieldId.isEmpty() && options != null && options.size() > 0) {
                        for (var option : options) {
                            scaleApi.addOptionToCustomField(customFieldId, option);
                        }
                    }
                    if(!projectExecutionCustomFieldNames.contains(customFieldName)) {
                        projectExecutionCustomFieldNames.add(customFieldName);
                    }
                    logger.info("Migration Custom Field " + customFieldName + " created successfully.");
                }
            }
        }
    }

    private void migrateTeststepCustomFields(String projectKey, String projectId) throws ApiException, ZephyrApiException {
        FetchSquadCustomFieldResponse fetchSquadStepCustomFieldResponse = squadApi.fetchSquadCustomFieldResponse("TESTSTEP", projectId);
        List<String> tobeMigratedStepFields = new ArrayList<>();
        if(fetchSquadStepCustomFieldResponse != null && fetchSquadStepCustomFieldResponse.data().size() > 0){
            for(SquadCustomFieldResponse squadCustomFieldResponse:fetchSquadStepCustomFieldResponse.data()){
                if(squadCustomFieldResponse.isActive()){
                    List<ScaleTestCaseCustomFieldOption> options = null;
                    if(SQUAD_CUSTOM_FIELDS_HAS_OPTIONS.contains(squadCustomFieldResponse.fieldType())){
                        options = new LinkedList<>();
                        if(squadCustomFieldResponse.customFieldOptionValues() != null &&
                                squadCustomFieldResponse.customFieldOptionValues().size() > 0){
                            int index = 1;
                            for(Map.Entry<String, String> customFieldEntry:squadCustomFieldResponse.customFieldOptionValues().entrySet()){
                                options.add(new ScaleTestCaseCustomFieldOption(customFieldEntry.getValue(), index, false));
                                index++;
                            }
                        }
                    }
                    String customFieldName = squadCustomFieldResponse.name();
                    var customFieldId = scaleApi.createCustomField(
                            new ScaleCustomFieldPayload(
                                    customFieldName,
                                    ScaleMigrationTestStepCustomFieldPayload.ENTITY_TYPE,
                                    projectKey,
                                    ScaleCustomFieldPayload.SQUAD_SCALE_CUSTOM_FIELD_TYPE.get(squadCustomFieldResponse.fieldType())
                            )
                    );
                    if(customFieldId != null && !customFieldId.isEmpty() && options != null && options.size() > 0) {
                        for (var option : options) {
                            scaleApi.addOptionToCustomField(customFieldId, option);
                        }
                    }
                    tobeMigratedStepFields.add(customFieldName);
                    logger.info("Migration Custom Field " + customFieldName + " created successfully.");
                }
            }
        }
        List<ScaleCustomFieldResponse> scaleTestStepCustomFields = scaleApi.fetchScaleCustomFields("teststep", projectId);
        if(scaleTestStepCustomFields != null && scaleTestStepCustomFields.size() > 0) {
            projectTestStepCustomFields.putAll(scaleTestStepCustomFields.stream().filter(e -> tobeMigratedStepFields.contains(e.name())).collect(Collectors.toMap(ScaleCustomFieldResponse::name, Function.identity())));
        }
    }

    private SquadToScaleTestCaseMap createScaleTestCases(List<JiraIssuesResponse> issues, String
            projectKey, String projectId) throws IOException {
        try {
            var map = new SquadToScaleTestCaseMap();

            for (var issue : issues) {
                var scaleTestCaseKey = createTestCaseForIssue(issue, projectKey, projectId);
//                map.put(new SquadToScaleTestCaseMap.TestCaseMapKey(issue.id(), issue.key()), scaleTestCaseKey);
                String creatorKey = (issue.fields().creator != null && issue.fields().creator.key() != null)
                        ? issue.fields().creator.key()
                        : null;
                map.put(new SquadToScaleTestCaseMap.TestCaseMapKey(issue.id(), issue.key(), creatorKey, issue.fields().created, null, issue.fields().updated), scaleTestCaseKey);
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

            ScaleTestCaseCreationPayload testCasePayload = this.scaleTestCaseFacade.createTestCasePayload(issue, projectKey, projectTestcaseCustomFieldNames);
            var scaleTestCaseKey = scaleApi.createTestCases(testCasePayload);

            logger.info("Created Scale test Case from Squad test case " + issue.id() + ".");

            return scaleTestCaseKey;
        } catch (IOException exception) {
            logger.error("Failed to create Scale Test Case from Squad test case with id: " + issue.id() + " " + exception.getMessage(), exception);
            throw new RuntimeException(exception);
        }
    }

    private void updateStepsAndPostExecution(SquadToScaleTestCaseMap
                                                                        testCaseMap, String projectKey, SquadToScaleEntitiesMap squadToScaleEntitiesMap) throws IOException {
        try {
            var orderedIssueList = testCaseMap.getListOfAllEntriesOrdered();


            logger.info("Updating steps and posting execution for " + orderedIssueList.size() + " issues...");

            var testStepMap = new SquadToScaleTestStepMap();
            var testExecutionMap = new SquadToScaleTestExecutionMap();
            var squadToScaleExecutionStepMap = new SquadToScaleExecutionStepMap();
            for (var testCaseItem : orderedIssueList) {
                testStepMap.putAll(updateStepsForTestCase(testCaseItem));
                testExecutionMap.putAll(createTestExecutionForTestCase(testCaseItem, projectKey, squadToScaleExecutionStepMap));
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
            if(attachmentsMigrator.getDataSource() != null) {
                var scaleRepo = new ScaleTestCaseRepository(attachmentsMigrator.getDataSource());
                var testCaseEntity = scaleRepo.getByKey(testCaseItem.getValue());
                if (testCaseEntity != null || testCaseEntity.isPresent()) {
                    var steps = new SquadUpdateStepPayload(testCaseEntity.get().id(), new ScaleStepByStepScript(new SquadGETStepItemPayload()));

                    List<ScaleGETStepItemPayload> scaleSteps = new LinkedList<>();
                    int index = 0;
                    for(var squadStep: squadTestSteps.stepBeanCollection()){
                        scaleSteps.add(ScaleGETStepItemPayload.createScaleGETStepItemPayloadForCreation(
                                squadStep.htmlStep(),
                                squadStep.htmlData(),
                                squadStep.htmlResult(),
                                squadStep.customFields(),
                                testCaseEntity,
                                projectTestStepCustomFields,
                                index++
                        ));
                    }
                    steps.testScript().stepByStepScript().steps = scaleSteps;
                    logger.info("Updating steps for scale test case...");
                    scaleApi.updateTestStep(String.valueOf(testCaseEntity.get().id()), steps);

                    //only mapping if updateTestStep was successful
                    testStepMap.put(testCaseItem.getValue(),
                            squadTestSteps.stepBeanCollection().stream().collect(Collectors
                                    .toMap(testStepResponse -> new SquadToScaleTestStepMap.TestStepMapKey(
                                            testStepResponse.id(), testStepResponse.orderId()
                                    ), SquadTestStepResponse::attachmentsMap)));
                    return testStepMap;

                }
            }
            var steps = new SquadUpdateStepPayloadKey(new SquadGETStepItemPayloadKey());

            steps.testScript().steps = squadTestSteps.stepBeanCollection().stream()
                    .map(e -> ScaleGETStepItemPayload.createScaleGETStepItemPayloadForCreation(
                            e.htmlStep(),
                            e.htmlData(),
                            e.htmlResult(),
                            null,
                            null,
                            null, null)).toList();
            scaleApi.updateTestStepByKey(testCaseItem.getValue(), steps);

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
                    String> item, String projectKey, SquadToScaleExecutionStepMap squadToScaleExecutionStepMap) throws IOException {
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
                var testExecutionCfValueResponse = squadApi.fetchSquadExecutionCustomFieldValueResponse(execution.id());
                var testExecutionPayload = scaleTestExecutionPayloadFacade
                        .buildPayload(execution, item.getValue(), projectKey, testExectuionStepResponse,
                                testExecutionCfValueResponse, projectExecutionCustomFieldNames);

                var scaleTestExecutionCreatedPayload = scaleApi.createTestExecution(scaleCycleKey,
                        testExecutionPayload);
//                testExecutionMap.put(new SquadToScaleTestExecutionMap.TestExecutionMapKey(execution.id()),
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
                        List<ScaleTestScriptResults> scaleTestScriptResultsMapOld = fetchScaleTestResults.testScriptResults();
                        List<ScaleTestScriptResults> scaleTestScriptResultsMap = scaleTestScriptResultsMapOld.stream()
                                .sorted(Comparator.comparingInt(ScaleTestScriptResults::index))
                                .toList();
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