package org;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;

public class DatabaseManager {
    private static HikariConfig CONFIG;
    private static HikariDataSource DATA_SOURCE;

    static {
        DatabaseManager.CONFIG = new HikariConfig();

        DatabaseManager.CONFIG.setJdbcUrl(MessageFormat.format("{0}:{1}/{2}", Main.DOTENV.get("DATABASE_URL"),
                Main.DOTENV.get("DATABASE_PORT"), Main.DOTENV.get("DATABASE_NAME")));
        DatabaseManager.CONFIG.setUsername(Main.DOTENV.get("DATABASE_NAME"));
        DatabaseManager.CONFIG.setPassword(Main.DOTENV.get("DATABASE_PASSWORD"));
        DatabaseManager.CONFIG.addDataSourceProperty("cachePrepStmts", "true");
        DatabaseManager.CONFIG.addDataSourceProperty("prepStmtCacheSize", "250");
        DatabaseManager.CONFIG.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        DatabaseManager.DATA_SOURCE = new HikariDataSource(DatabaseManager.CONFIG);
    }

    private DatabaseManager() {
    }

    public static Connection getConnection() throws SQLException {
        return DatabaseManager.DATA_SOURCE.getConnection();
    }
}
