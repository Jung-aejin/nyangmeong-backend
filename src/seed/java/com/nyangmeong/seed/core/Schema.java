package com.nyangmeong.seed.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * [실제 DB 구조]
 * - information_schema에서 컬럼·FK를 읽는다. migration으로 바뀐 구조도 그대로 반영된다
 */
public final class Schema {

    private static final Pattern CODE_GROUP = Pattern.compile("\\(([A-Z]{3}G\\d{2})\\)");

    /** 테이블 → 컬럼 이름 → 컬럼 */
    private final Map<String, Map<String, Column>> tables;
    private final List<ForeignKey> foreignKeys;

    private Schema(Map<String, Map<String, Column>> tables, List<ForeignKey> foreignKeys) {
        this.tables = tables;
        this.foreignKeys = foreignKeys;
    }

    /**
     * @param nullable    NULL 허용
     * @param hasDefault  기본값 있음(넣지 않아도 된다)
     * @param generated   생성 컬럼(넣으면 안 된다)
     * @param codeGroup   주석의 공통코드 그룹, 없으면 null
     */
    public record Column(String name, boolean nullable, boolean hasDefault, boolean generated, boolean autoIncrement,
            String codeGroup) {

        public boolean requiresValue() {
            return !nullable && !hasDefault && !generated && !autoIncrement;
        }
    }

    public record ForeignKey(String table, String column, String refTable, String refColumn) {
    }

    public static Schema load(Connection conn) throws SQLException {
        Map<String, Map<String, Column>> tables = new LinkedHashMap<>();
        String columnSql = """
                SELECT UPPER(TABLE_NAME), COLUMN_NAME, IS_NULLABLE, COLUMN_DEFAULT, EXTRA, COLUMN_COMMENT
                  FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE()
                 ORDER BY TABLE_NAME, ORDINAL_POSITION""";
        try (PreparedStatement ps = conn.prepareStatement(columnSql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String extra = rs.getString(5).toUpperCase();
                Matcher group = CODE_GROUP.matcher(rs.getString(6));
                Column column = new Column(rs.getString(2), "YES".equals(rs.getString(3)),
                        rs.getString(4) != null || extra.contains("DEFAULT_GENERATED"),
                        extra.contains("VIRTUAL GENERATED") || extra.contains("STORED GENERATED"),
                        extra.contains("AUTO_INCREMENT"), group.find() ? group.group(1) : null);
                tables.computeIfAbsent(rs.getString(1), k -> new LinkedHashMap<>()).put(column.name(), column);
            }
        }
        List<ForeignKey> foreignKeys = new ArrayList<>();
        String fkSql = """
                SELECT UPPER(TABLE_NAME), COLUMN_NAME, UPPER(REFERENCED_TABLE_NAME), REFERENCED_COLUMN_NAME
                  FROM information_schema.KEY_COLUMN_USAGE
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND REFERENCED_TABLE_NAME IS NOT NULL""";
        try (PreparedStatement ps = conn.prepareStatement(fkSql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                foreignKeys.add(new ForeignKey(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)));
            }
        }
        return new Schema(tables, foreignKeys);
    }

    public boolean hasTable(String table) {
        return tables.containsKey(table);
    }

    public Map<String, Column> columns(String table) {
        Map<String, Column> columns = tables.get(table);
        if (columns == null) {
            throw new IllegalStateException("DB에 " + table + " 테이블이 없습니다. 기준 SQL과 db/migration을 확인해 주세요");
        }
        return columns;
    }

    public List<ForeignKey> foreignKeys() {
        return foreignKeys;
    }
}
