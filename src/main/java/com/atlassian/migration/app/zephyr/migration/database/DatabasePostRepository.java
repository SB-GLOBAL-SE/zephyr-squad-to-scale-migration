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
import org.supercsv.io.ICsvBeanReader;
import org.supercsv.prefs.CsvPreference;

import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;


public class DatabasePostRepository {

    private static final Logger logger = LoggerFactory.getLogger(DatabasePostRepository.class);
    private final JdbcTemplate jdbcTemplate;

    private static final String TEST_CASE_TABLE_NAME = "AO_4D28DD_TEST_CASE";
    private static final String TEST_EXECUTION_TABLE_NAME = "AO_4D28DD_TEST_RESULT";
    private final String testcaseCsvFile;
    private final String testResultCsvFile;

    public DatabasePostRepository(DriverManagerDataSource datasource,
                                  String testcaseCsvFile,
                                  String testResultCsvFile) {
        jdbcTemplate = new JdbcTemplate(datasource);
        this.testcaseCsvFile = testcaseCsvFile;
        this.testResultCsvFile = testResultCsvFile;
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
                        logger.error("Updating failed for test case: "+testCaseMapper.getSCALE_TESTCASE_ID(), e.getMessage());
                    }
                }

            } catch (IOException e) {
                logger.error("error occurred while updating test case fields."+e.getMessage());
            }
        }catch (Exception e){
            logger.error("error while reading test case mapped fields. "+e.getMessage());
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
        String sql_stmt = "UPDATE "+datasource.getSchema()+".\""+TEST_CASE_TABLE_NAME+ "\" set "+setfields+" WHERE \"KEY\" = '"+testCaseMapper.getSCALE_TESTCASE_ID()+"'";
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
                        logger.info("Updated fields for test results/execution: "+testExecutionMapper.getSCALE_EXECUTION_ID());
                    }catch (Exception e){
                        logger.error("Updating failed for test results: "+testExecutionMapper.getSCALE_EXECUTION_ID(), e.getMessage());
                    }
                }

            } catch (IOException e) {
                logger.error("error occurred while updating test results fields."+e.getMessage());
            }
        }catch (Exception e){
            logger.error("error occurred while reading test execution mapped fields."+e.getMessage());
        }
    }

    private void updateDatabaseforTestResults(TestExecutionMapper testExecutionMapper) {
        String sql_stmt = buildUpdateTestResultsByKeyQuery(testExecutionMapper);
        int result = jdbcTemplate.update(sql_stmt);
    }

    private String buildUpdateTestResultsByKeyQuery(TestExecutionMapper testExecutionMapper) {
        DriverManagerDataSource datasource = (DriverManagerDataSource) jdbcTemplate.getDataSource();
        var databaseType = DatabaseUtils.defineDatabaseType(datasource);
        String setfields = " \"CREATED_BY\" = '"+testExecutionMapper.getCREATED_BY()+"', \"CREATED_ON\" = '"+testExecutionMapper.getCREATED_ON()+"'";
        String sql_stmt = "UPDATE "+datasource.getSchema()+".\""+TEST_EXECUTION_TABLE_NAME+ "\" set "+setfields+" WHERE \"ID\" = "+testExecutionMapper.getSCALE_EXECUTION_ID();
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
                new Optional()
        };
    }

    private Path getCurrentPath() throws URISyntaxException {
        return Paths.get(DatabasePostRepository.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
    }

}
