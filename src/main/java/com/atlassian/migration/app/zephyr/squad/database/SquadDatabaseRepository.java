package com.atlassian.migration.app.zephyr.squad.database;

import com.atlassian.migration.app.zephyr.common.DatabaseType;
import com.atlassian.migration.app.zephyr.common.DatabaseUtils;
import com.atlassian.migration.app.zephyr.squad.model.SquadAttachmentEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public class SquadDatabaseRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String TABLE_NAME = "AO_7DEABF_ATTACHMENT";
    private static final String FETCH_BY_KEY = "SELECT \"ID\", \"DATE_CREATED\" FROM \"" + TABLE_NAME + "\" WHERE \"ID\" = %s";
    private static final String FETCH_BY_KEY_MSSQL = "SELECT \"ID\", \"DATE_CREATED\" FROM %s" + TABLE_NAME + " WHERE \"ID\" = %s";
    private static final String FETCH_BY_KEY_MYSQL = "SELECT `ID`, `DATE_CREATED` FROM `" + TABLE_NAME + "` WHERE `ID` = %s";

    private final Map<DatabaseType, String> fetchByKeyQueries = Map.of(
            DatabaseType.POSTGRESQL, FETCH_BY_KEY,
            DatabaseType.SQLSERVER, FETCH_BY_KEY_MSSQL,
            DatabaseType.ORACLE, FETCH_BY_KEY,
            DatabaseType.MYSQL, FETCH_BY_KEY_MYSQL
    );

    public SquadDatabaseRepository(DriverManagerDataSource datasource) {

        jdbcTemplate = new JdbcTemplate(datasource);
    }

    public Optional<SquadAttachmentEntity> getByID(String id) {

        String sql_stmt = buildGetByKeyQuery(id);

        List<SquadAttachmentEntity> result = jdbcTemplate.query(sql_stmt, new SquadAttachmentMapper());

        return result.stream().findFirst();
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

    private static class SquadAttachmentMapper implements RowMapper<SquadAttachmentEntity> {

        @Override
        public SquadAttachmentEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new SquadAttachmentEntity(
                    rs.getLong("ID"),
                    rs.getString("DATE_CREATED")
            );
        }
    }

}
