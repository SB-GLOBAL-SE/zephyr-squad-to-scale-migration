package com.atlassian.migration.app.zephyr.migration.database;

import com.atlassian.migration.app.zephyr.common.DatabaseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvBeanReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


public class DatabasePostRepository {

    private static final Logger logger = LoggerFactory.getLogger(DatabasePostRepository.class);
    private final JdbcTemplate jdbcTemplate;

    private static final String TEST_CASE_TABLE_NAME = "AO_4D28DD_TEST_CASE";
    private static final String TEST_EXECUTION_TABLE_NAME = "AO_4D28DD_TEST_RESULT";
    private static final String ATTACHMENT_TABLE_NAME = "AO_4D28DD_ATTACHMENT";
    private static final String TEST_SCRIPT_RESULT_TABLE_NAME = "AO_4D28DD_TEST_SCRIPT_RESULT";
    private final String testcaseCsvFile;
    private final String testResultCsvFile;
    private final String databaseType;

    public DatabasePostRepository(DriverManagerDataSource datasource,
                                  String testcaseCsvFile,
                                  String testResultCsvFile, 
                                  String databaseType) {
        this.jdbcTemplate = new JdbcTemplate(datasource);
        this.testcaseCsvFile = testcaseCsvFile;
        this.testResultCsvFile = testResultCsvFile;
        this.databaseType = databaseType;
    }

    public void updateTestCaseFields() {
        try {
            Path destinationPath = getCurrentPath().resolve(testcaseCsvFile);
            try (ICsvBeanReader beanReader = new CsvBeanReader(new FileReader(destinationPath.toFile()), CsvPreference.STANDARD_PREFERENCE)) {
                String[] header = beanReader.getHeader(true);
                CellProcessor[] processors = getProcessors(); // Processors for each column
                TestCaseMapper testCaseMapper;
                while ((testCaseMapper = beanReader.read(TestCaseMapper.class, header, processors)) != null) {
                    try {
                        updateDatabaseforTestCase(testCaseMapper);
                        logger.info("Updated fields for test case: "+testCaseMapper.getSCALE_TESTCASE_ID());
                    }catch (Exception e){
                        logger.error("Updating failed for test case: "+testCaseMapper.getSCALE_TESTCASE_ID(), e);
                    }
                }

            } catch (IOException e) {
                logger.error("error occurred while updating test case fields."+e.getMessage(), e);
            }
        }catch (Exception e){
            logger.error("error while reading test case mapped fields. "+e.getMessage(), e);
        }
    }

    private void updateDatabaseforTestCase(TestCaseMapper testCaseMapper) {
        String sql_stmt = buildUpdateTestcaseByKeyQuery(testCaseMapper);
        int result = jdbcTemplate.update(sql_stmt);
    }

    private String buildUpdateTestcaseByKeyQuery(TestCaseMapper testCaseMapper) {
        DriverManagerDataSource datasource = (DriverManagerDataSource) jdbcTemplate.getDataSource();

        var databaseType = DatabaseUtils.defineDatabaseType(datasource);
        String setfields = " \"CREATED_BY\" = '"+testCaseMapper.getCREATED_BY()+"', \"CREATED_ON\" = '"+testCaseMapper.getCREATED_ON()+"'";
        String schema = datasource.getSchema();
        String sql_stmt = "UPDATE "+schema+".\""+TEST_CASE_TABLE_NAME+ "\" set "+setfields+" WHERE \"KEY\" = '"+testCaseMapper.getSCALE_TESTCASE_ID()+"'";
        if(schema == null){
            sql_stmt = "UPDATE \""+TEST_CASE_TABLE_NAME+ "\" set "+setfields+" WHERE \"KEY\" = '"+testCaseMapper.getSCALE_TESTCASE_ID()+"'";
        }
        return sql_stmt;
    }

    public void updateTestResultsFields() {
        try {
            Path destinationPath = getCurrentPath().resolve(testResultCsvFile);
            try (ICsvBeanReader beanReader = new CsvBeanReader(new FileReader(destinationPath.toFile()), CsvPreference.STANDARD_PREFERENCE)) {
                String[] header = beanReader.getHeader(true);
                CellProcessor[] processors = getTestResultsProcessors(); // Processors for each column
                TestExecutionMapper testExecutionMapper;
                while ((testExecutionMapper = beanReader.read(TestExecutionMapper.class, header, processors)) != null) {
                    try {
                        updateDatabaseforTestResults(testExecutionMapper);
                        updateDatabaseforTestScriptResults(testExecutionMapper);
                        logger.info("Updated fields for test results/execution: "+testExecutionMapper.getSCALE_EXECUTION_ID());
                    }catch (Exception e){
                        logger.error("Updating failed for test results: "+testExecutionMapper.getSCALE_EXECUTION_ID(), e.getMessage(), e);
                    }
                }

            } catch (IOException e) {
                logger.error("error occurred while updating test results fields."+e.getMessage(), e);
            }
        }catch (Exception e){
            logger.error("error occurred while reading test execution mapped fields."+e.getMessage(), e);
        }
    }

    public void updateAttachmentRecords(String attachmentsFileName){
        try {
            try {
                Path destinationPath = getCurrentPath().resolve(attachmentsFileName);
                try (ICsvListReader listReader = new CsvListReader(new FileReader(destinationPath.toFile()), CsvPreference.STANDARD_PREFERENCE)) {
                    // Read the header (if present) and skip it
                    listReader.getHeader(true);

                    List<String> row;
                    while ((row = listReader.read()) != null) {
                        // Process each row as a List of Strings
                        createrow(row);
                    }
                } catch (IOException e) {
                    logger.error("error occurred while reading test execution mapped fields."+e.getMessage());
                }
            }catch (Exception e){
                logger.error("error occurred while reading test execution mapped fields."+e.getMessage());
            }
        }catch (Exception e){
            logger.error("error occurred while reading test execution mapped fields."+e.getMessage());
        }
    }

    private void createrow(List<String> row) {
        DriverManagerDataSource datasource = (DriverManagerDataSource) jdbcTemplate.getDataSource();
        String fileName = row.get(0) == null ? null : "'"+row.get(0)+"'";
        Integer fileSize = row.get(1) == null ? null : Integer.parseInt(row.get(1));
        String name = row.get(2) == null ? null : row.get(2);
        if(name != null){
            if(name.contains("'")) {
                name = "'"+name.replace("'", "''")+"'";
            }else{
                name = "'"+name+"'";
            }
        }
        Integer projectId = row.get(3) == null ? null : Integer.parseInt(row.get(3));
        String user = row.get(4) == null ? null : "'"+row.get(4)+"'";
        Integer temp = Boolean.parseBoolean(row.get(5)) ? 1 : 0;
        String createdOn = row.get(6) == null ? null : "'"+row.get(6)+"'";
        String mimetype = row.get(7) == null ? null : "'"+row.get(7)+"'";

        Integer testcaseId = row.get(8) == null ? null : Integer.parseInt(row.get(8));
        Integer stepId = row.get(9) == null ? null : Integer.parseInt(row.get(9));
        Integer testresultId = row.get(10) == null ? null : Integer.parseInt(row.get(10));
        Integer scriptResultsId = row.get(11) == null ? null : Integer.parseInt(row.get(11));
        String tableName = datasource.getSchema()+".\""+ATTACHMENT_TABLE_NAME+"\"";
        if(datasource.getSchema() == null){
            tableName = "\""+ATTACHMENT_TABLE_NAME+"\"";
        }
        String insertQuery = "INSERT INTO "+tableName+" (FILE_NAME, FILE_SIZE, NAME, PROJECT_ID, USER_KEY, TEMPORARY, CREATED_ON, MIME_TYPE, TEST_CASE_ID, STEP_ID, TEST_RESULT_ID, TEST_SCRIPT_RESULT_ID) "
                + "VALUES ("+
                fileName +", " +
                fileSize+ ", " +
                name+", " +
                projectId+", " +
                user+", " +
                temp+", " +
                createdOn+", " +
                mimetype+", " +
                testcaseId +", " +
                stepId+", " +
                testresultId+", " +
                scriptResultsId+")";
        if(databaseType.equals("postgresql")){
            String tempstr = Boolean.parseBoolean(row.get(5)) ? "true" : "false";
            insertQuery = "INSERT INTO "+tableName+" (\"FILE_NAME\", \"FILE_SIZE\", \"NAME\", \"PROJECT_ID\", \"USER_KEY\", \"TEMPORARY\", \"CREATED_ON\", \"MIME_TYPE\", \"TEST_CASE_ID\", \"STEP_ID\", \"TEST_RESULT_ID\", \"TEST_SCRIPT_RESULT_ID\") "
                    + "VALUES ("+
                    fileName +", " +
                    fileSize+ ", " +
                    name+", " +
                    projectId+", " +
                    user+", " +
                    tempstr+", " +
                    createdOn+", " +
                    mimetype+", " +
                    testcaseId +", " +
                    stepId+", " +
                    testresultId+", " +
                    scriptResultsId+")";
        }
        jdbcTemplate.execute(insertQuery);
    }

    private void updateDatabaseforTestResults(TestExecutionMapper testExecutionMapper) {
        try {
            String sql_stmt = buildUpdateTestResultsByKeyQuery(testExecutionMapper);
            int result = jdbcTemplate.update(sql_stmt);
        }catch (Exception e){
            logger.error(String.format("Unable to update created fields for test result %s", testExecutionMapper.getSCALE_EXECUTION_ID()));
        }
        try {
            String sql_stmt = buildUpdateTestResultsActualStartDateByKeyQuery(testExecutionMapper);
            int result = jdbcTemplate.update(sql_stmt);
        }catch (Exception e){
            logger.error(String.format("Unable to update Actual start date fields for test result %s", testExecutionMapper.getSCALE_EXECUTION_ID()));
        }

    }

    private void updateDatabaseforTestScriptResults(TestExecutionMapper testExecutionMapper) {
        String sql_stmt = buildUpdateTestScriptResultsByKeyQuery(testExecutionMapper);
        int result = jdbcTemplate.update(sql_stmt);
    }

    private String buildUpdateTestScriptResultsByKeyQuery(TestExecutionMapper testExecutionMapper) {
        DriverManagerDataSource datasource = (DriverManagerDataSource) jdbcTemplate.getDataSource();
        var databaseType = DatabaseUtils.defineDatabaseType(datasource);
        String executedOn = testExecutionMapper.getEXECUTED_ON() == null ? null : "'" + testExecutionMapper.getEXECUTED_ON() + "'" ;
        String setfields = " \"EXECUTION_DATE\" = "+ executedOn +"";
        String sql_stmt = "UPDATE "+datasource.getSchema()+".\""+TEST_SCRIPT_RESULT_TABLE_NAME+ "\" set "+setfields+" WHERE \"TEST_RESULT_ID\" = "+testExecutionMapper.getSCALE_EXECUTION_ID();
        if(datasource.getSchema() == null){
            sql_stmt = "UPDATE \""+TEST_SCRIPT_RESULT_TABLE_NAME+ "\" set "+setfields+" WHERE \"TEST_RESULT_ID\" = "+testExecutionMapper.getSCALE_EXECUTION_ID();
        }
        return sql_stmt;
    }


    private String buildUpdateTestResultsByKeyQuery(TestExecutionMapper testExecutionMapper) {
        DriverManagerDataSource datasource = (DriverManagerDataSource) jdbcTemplate.getDataSource();
        var databaseType = DatabaseUtils.defineDatabaseType(datasource);
        String setfields = " \"CREATED_BY\" = '"+testExecutionMapper.getCREATED_BY()+"', \"CREATED_ON\" = '"+testExecutionMapper.getCREATED_ON()+"'";
        String schema = datasource.getSchema();
        String sql_stmt = "UPDATE "+ schema +".\""+TEST_EXECUTION_TABLE_NAME+ "\" set "+setfields+" WHERE \"ID\" = "+testExecutionMapper.getSCALE_EXECUTION_ID();
        if(schema == null){
            sql_stmt = "UPDATE \""+TEST_EXECUTION_TABLE_NAME+ "\" set "+setfields+" WHERE \"ID\" = "+testExecutionMapper.getSCALE_EXECUTION_ID();
        }
        return sql_stmt;
    }

    private String buildUpdateTestResultsActualStartDateByKeyQuery(TestExecutionMapper testExecutionMapper) {
        DriverManagerDataSource datasource = (DriverManagerDataSource) jdbcTemplate.getDataSource();
        var databaseType = DatabaseUtils.defineDatabaseType(datasource);
        String executedOn = testExecutionMapper.getEXECUTED_ON() == null ? null : "'" + testExecutionMapper.getEXECUTED_ON() + "'" ;
        String setfields =  " \"ACTUAL_START_DATE\" = "+executedOn+"";
        if(executedOn == null){
            setfields =  " \"ACTUAL_START_DATE\" = "+executedOn+", "+" \"EXECUTION_DATE\" = "+executedOn+"";
        }
        String sql_stmt = "UPDATE "+datasource.getSchema()+".\""+TEST_EXECUTION_TABLE_NAME+ "\" set "+setfields+" WHERE \"ID\" = "+testExecutionMapper.getSCALE_EXECUTION_ID();
        if(datasource.getSchema() == null){
            sql_stmt = "UPDATE \""+TEST_EXECUTION_TABLE_NAME+ "\" set "+setfields+" WHERE \"ID\" = "+testExecutionMapper.getSCALE_EXECUTION_ID();
        }
        return sql_stmt;
    }

    private static CellProcessor[] getProcessors() {
        // Define processors for each column in the CSV
        return new CellProcessor[] {
                new ParseInt(),
                new org.supercsv.cellprocessor.Optional(),
                new org.supercsv.cellprocessor.Optional(),
                new org.supercsv.cellprocessor.Optional(),
                new org.supercsv.cellprocessor.Optional(),
                new Optional()
        };
    }

    private static CellProcessor[] getTestResultsProcessors() {
        // Define processors for each column in the CSV
        return new CellProcessor[] {
                new ParseInt(),
                new ParseInt(),
                new org.supercsv.cellprocessor.Optional(),
                new org.supercsv.cellprocessor.Optional(),
                new org.supercsv.cellprocessor.Optional(),
                new Optional(),
                new Optional()
        };
    }

    private Path getCurrentPath() throws URISyntaxException {
        return Paths.get(DatabasePostRepository.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
    }

}
