package com.atlassian.migration.app.zephyr.scale.database;

import com.atlassian.migration.app.zephyr.common.DatabaseType;
import com.atlassian.migration.app.zephyr.common.DatabaseUtils;
import com.atlassian.migration.app.zephyr.scale.model.TestCaseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public class ScaleTestCaseRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String TABLE_NAME = "AO_4D28DD_TEST_CASE";
    private static final String FETCH_BY_KEY = "SELECT \"ID\", \"KEY\" FROM \"" + TABLE_NAME + "\" WHERE \"KEY\" = '%s'";
    private static final String FETCH_BY_KEY_MSSQL = "SELECT \"ID\", \"KEY\" FROM %s" + TABLE_NAME + " WHERE \"KEY\" = '%s'";
    private static final String FETCH_BY_KEY_MYSQL = "SELECT `ID`, `KEY` FROM `" + TABLE_NAME + "` WHERE `KEY` = '%s'";

    private final Map<DatabaseType, String> fetchByKeyQueries = Map.of(
            DatabaseType.POSTGRESQL, FETCH_BY_KEY,
            DatabaseType.SQLSERVER, FETCH_BY_KEY_MSSQL,
            DatabaseType.ORACLE, FETCH_BY_KEY,
            DatabaseType.MYSQL, FETCH_BY_KEY_MYSQL
    );

    public ScaleTestCaseRepository(DriverManagerDataSource datasource) {
        jdbcTemplate = new JdbcTemplate(datasource);
    }

    public Optional<TestCaseEntity> getByKey(String key) {

        String sql_stmt = buildGetByKeyQuery(key);

        List<TestCaseEntity> result = jdbcTemplate.query(sql_stmt, new TestCaseMapper());
        if(result != null && result.size() > 0)
            return result.stream().findFirst();
        else
            return null;
    }

    private String buildGetByKeyQuery(String key) {
        DriverManagerDataSource datasource = (DriverManagerDataSource) jdbcTemplate.getDataSource();

        var databaseType = DatabaseUtils.defineDatabaseType(datasource);

        String sql_stmt = fetchByKeyQueries.getOrDefault(databaseType, FETCH_BY_KEY);

        switch (databaseType) {
            case SQLSERVER, MSSQL: {
                if (datasource.getSchema() == null
                        || datasource.getSchema().isBlank()) {
                    return String.format(sql_stmt, "", key);
                }
                return String.format(sql_stmt,
                        datasource.getSchema() + ".", key);
            }
            default:
                return String.format(sql_stmt, key);
        }
    }

    private static class TestCaseMapper implements RowMapper<TestCaseEntity> {

        @Override
        public TestCaseEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new TestCaseEntity(
                    rs.getLong("ID"),
                    rs.getString("KEY")
            );
        }
    }

}
