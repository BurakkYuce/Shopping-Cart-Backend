package com.datapulse.service;

import com.datapulse.exception.UnsafeSqlException;
import com.datapulse.logging.LogEventPublisher;
import com.datapulse.logging.LogEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class SqlExecutionService {

    private static final Pattern SELECT_START = Pattern.compile(
            "^\\s*(WITH\\b[\\s\\S]*?\\)\\s*)?SELECT\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern BLACKLIST = Pattern.compile(
            "\\b(INSERT|UPDATE|DELETE|DROP|TRUNCATE|ALTER|CREATE|GRANT|REVOKE|EXEC|EXECUTE|MERGE|CALL|VACUUM|COPY|LOCK)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern LINE_COMMENT = Pattern.compile("--[^\\n]*");
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*[\\s\\S]*?\\*/");
    private static final Pattern EXISTING_LIMIT = Pattern.compile("\\bLIMIT\\s+\\d+\\b", Pattern.CASE_INSENSITIVE);

    private final DataSource dataSource;
    private final LogEventPublisher logEventPublisher;

    @Value("${app.chatbot.sql.query-timeout-seconds:5}")
    private int queryTimeoutSeconds;

    @Value("${app.chatbot.sql.default-row-cap:10000}")
    private int defaultRowCap;

    public SqlExecutionResult execute(String rawSql, String userId, String userRole) {
        if (rawSql == null || rawSql.isBlank()) {
            throw new UnsafeSqlException("SQL query is empty");
        }

        String sanitized = stripComments(rawSql).trim();

        if (BLACKLIST.matcher(sanitized).find()) {
            logEventPublisher.publish(
                    LogEventType.CHATBOT_SQL_REJECTED,
                    userId,
                    userRole,
                    Map.of("reason", "blacklist", "sql", truncate(rawSql)));
            throw new UnsafeSqlException("Only SELECT statements are allowed");
        }

        if (!SELECT_START.matcher(sanitized).find()) {
            logEventPublisher.publish(
                    LogEventType.CHATBOT_SQL_REJECTED,
                    userId,
                    userRole,
                    Map.of("reason", "not_select", "sql", truncate(rawSql)));
            throw new UnsafeSqlException("Only SELECT statements are allowed");
        }

        String effectiveSql = injectLimitIfMissing(rawSql);
        long started = System.currentTimeMillis();

        try (Connection conn = dataSource.getConnection()) {
            conn.setReadOnly(true);
            conn.setAutoCommit(false);
            try (Statement preamble = conn.createStatement()) {
                preamble.execute("SET LOCAL default_transaction_read_only = on");
            }
            try (Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(queryTimeoutSeconds);
                try (ResultSet rs = stmt.executeQuery(effectiveSql)) {
                    List<Map<String, Object>> rows = readRows(rs);
                    long durationMs = System.currentTimeMillis() - started;

                    logEventPublisher.publish(
                            LogEventType.CHATBOT_SQL_EXEC,
                            userId,
                            userRole,
                            Map.of(
                                    "sql", truncate(effectiveSql),
                                    "row_count", rows.size(),
                                    "duration_ms", durationMs));

                    return new SqlExecutionResult(rows, rows.size(), durationMs, effectiveSql);
                }
            } finally {
                conn.rollback();
            }
        } catch (SQLException e) {
            long durationMs = System.currentTimeMillis() - started;
            logEventPublisher.publish(
                    LogEventType.CHATBOT_SQL_REJECTED,
                    userId,
                    userRole,
                    Map.of(
                            "reason", "execution_error",
                            "sql", truncate(effectiveSql),
                            "error", e.getMessage() != null ? e.getMessage() : "unknown",
                            "duration_ms", durationMs));
            if (isTimeoutError(e)) {
                throw new SqlQueryTimeoutException("Query exceeded " + queryTimeoutSeconds + "s timeout");
            }
            throw new UnsafeSqlException("SQL execution failed: " + e.getMessage());
        }
    }

    private String stripComments(String sql) {
        String noBlock = BLOCK_COMMENT.matcher(sql).replaceAll(" ");
        return LINE_COMMENT.matcher(noBlock).replaceAll(" ");
    }

    private String injectLimitIfMissing(String sql) {
        String stripped = stripComments(sql).trim();
        if (EXISTING_LIMIT.matcher(stripped).find()) {
            return sql;
        }
        String trimmed = sql.trim();
        while (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return trimmed + " LIMIT " + defaultRowCap;
    }

    private List<Map<String, Object>> readRows(ResultSet rs) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= cols; i++) {
                row.put(meta.getColumnLabel(i), rs.getObject(i));
            }
            rows.add(row);
        }
        return rows;
    }

    private boolean isTimeoutError(SQLException e) {
        String msg = e.getMessage();
        if (msg == null) return false;
        String lower = msg.toLowerCase();
        return lower.contains("timeout") || lower.contains("cancel");
    }

    private String truncate(String sql) {
        if (sql == null) return "";
        return sql.length() > 500 ? sql.substring(0, 500) + "..." : sql;
    }

    public record SqlExecutionResult(
            List<Map<String, Object>> rows,
            int rowCount,
            long durationMs,
            String executedSql) {
    }

    public static class SqlQueryTimeoutException extends RuntimeException {
        public SqlQueryTimeoutException(String message) {
            super(message);
        }
    }
}
