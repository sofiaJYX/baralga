package org.remast.baralga.repository.file;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;

import org.apache.commons.lang3.StringUtils;
import org.h2.tools.RunScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.remast.baralga.repository.file.BaralgaFileRepository.LATEST_DATABASE_VERSION;

public class BaralgaDatabaseManager {

    /** The logger. */
    private static final Logger log = LoggerFactory.getLogger(BaralgaFileRepository.class);

    private Connection connection;

    public BaralgaDatabaseManager(Connection connection) {
        this.connection = connection;
    }

    void updateDatabase() throws SQLException {
        boolean databaseExists = checkDatabaseExists();

        if (!databaseExists) {
            log.info("Creating Baralga DB.");
            createDatabase();
            log.info("Baralga DB successfully created.");
        }

        int databaseVersion = getDatabaseVersion();
        updateDatabaseToLatestVersion(databaseVersion);

        log.info("Using Baralga DB Version: {}, description: {}.", databaseVersion, getDatabaseDescription());
    }

    /** Function to create database. */
    void createDatabase() throws SQLException {
        executeScript("setup_database.sql");
        connection.commit();
    }

    /** Function to check the current version number of the database. */
    int getDatabaseVersion() throws SQLException {
        int failure = -1;
        try (final Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery("select version from db_version order by version desc limit 1")) {
                if (resultSet.next()) {
                    return resultSet.getInt("version");
                }
            }
        }
        return failure;
    }

    /** Function to update the current version of database. */
    void updateDatabaseToLatestVersion(int currentVersion) throws SQLException {
        int versionDifference = LATEST_DATABASE_VERSION - currentVersion;
        for (int i = 1; i <= versionDifference; i++) {
            final int versionUpdate = currentVersion + i;
            log.info("Updating database to version {}.", versionUpdate);
            final String updateScript = "db_version_" + StringUtils.leftPad(String.valueOf(versionUpdate), 3, "0") + ".sql";
            executeScript(updateScript);
        }
        connection.commit();
    }

    String getDatabaseDescription() throws SQLException {
        try (final Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery("select description from db_version order by version desc limit 1")) {
                if (resultSet.next()) {
                    return resultSet.getString("description");
                }
            }
        }
        return "-";
    }

    boolean checkDatabaseExists() throws SQLException {
        try (final Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery("SHOW TABLES")) {
                while (resultSet.next()) {
                    if ("db_version".equalsIgnoreCase(resultSet.getString("TABLE_NAME"))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    void executeScript(final String scriptName) throws SQLException {
        log.info("Executing sql script {}.", scriptName);

        if (StringUtils.isBlank(scriptName)) {
            return;
        }

        InputStream is = BaralgaFileRepository.class.getResourceAsStream("sql/h2/" + scriptName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        RunScript.execute(connection, reader);
    }

    PreparedStatement prepare(final String sql) throws SQLException {
        return connection.prepareStatement(sql);
    }


}