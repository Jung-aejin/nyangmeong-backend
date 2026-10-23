package com.nyangmeong.seed.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.nyangmeong.seed.SeedException;

/**
 * [메모리의 시드 행]
 * - 테이블마다 PK(테이블명_SN)를 1부터 매기고 행을 모았다가 flush에서 한 번에 넣는다
 * - 순환 참조는 나중에 행을 고쳐서 채운다. 넣을 때는 FK 검사를 끄고, 검사는 SeedValidator가 한다
 */
public final class RowStore {

    private static final int BATCH_SIZE = 500;
    private static final String SEED_ACTOR = "SEED";

    private final Map<String, List<Row>> tables = new LinkedHashMap<>();

    /**
     * 행 추가
     * - PK를 매기고 fill로 컬럼을 채운다
     */
    public Row add(String table, Consumer<Row> fill) {
        List<Row> rows = tables.computeIfAbsent(table, k -> new ArrayList<>());
        Row row = new Row(table, rows.size() + 1L);
        fill.accept(row);
        rows.add(row);
        return row;
    }

    public List<Row> rows(String table) {
        return tables.getOrDefault(table, List.of());
    }

    public Row row(String table, long sn) {
        return rows(table).get((int) sn - 1);
    }

    public Map<String, Integer> counts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        tables.forEach((table, rows) -> counts.put(table, rows.size()));
        return counts;
    }

    public long totalRows() {
        return tables.values().stream().mapToLong(List::size).sum();
    }

    /**
     * 넣기 전 컬럼 점검
     * - DB에 없는 컬럼을 넣거나, 값이 꼭 필요한 컬럼을 빠뜨리면 테이블·컬럼을 짚어 멈춘다
     */
    public void checkColumns(Schema schema) {
        List<String> problems = new ArrayList<>();
        tables.forEach((table, rows) -> {
            if (!schema.hasTable(table)) {
                problems.add(table + " 테이블이 DB에 없음");
                return;
            }
            Map<String, Schema.Column> columns = schema.columns(table);
            Set<String> given = new LinkedHashSet<>();
            rows.forEach(row -> given.addAll(row.values().keySet()));
            for (String name : given) {
                Schema.Column column = columns.get(name);
                if (column == null) {
                    problems.add(table + "." + name + " 컬럼이 DB에 없음");
                } else if (column.generated()) {
                    problems.add(table + "." + name + " 생성 컬럼에 값을 넣음");
                }
            }
            for (Schema.Column column : columns.values()) {
                if (column.requiresValue() && rows.stream().anyMatch(row -> !row.values().containsKey(column.name()))) {
                    problems.add(table + "." + column.name() + " NOT NULL인데 값이 없음");
                }
            }
        });
        if (!problems.isEmpty()) {
            throw new SeedException("DB 구조와 생성기가 맞지 않습니다. src/seed의 해당 Seeder를 고쳐 주세요\n  - "
                    + String.join("\n  - ", problems));
        }
    }

    /**
     * DB에 넣기
     * - 호출한 쪽의 트랜잭션 안에서 실행. 컬럼 구성이 같은 행끼리 묶어 배치로 넣는다
     */
    public void flush(Connection conn, Schema schema) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS = 0");
        }
        try {
            for (Map.Entry<String, List<Row>> entry : tables.entrySet()) {
                String table = entry.getKey();
                boolean audited = schema.columns(table).containsKey("RGTR_ID");
                boolean modified = schema.columns(table).containsKey("MDFCN_DT");
                Map<List<String>, List<Row>> groups = new LinkedHashMap<>();
                for (Row row : entry.getValue()) {
                    if (audited) {
                        row.setIfAbsent("RGTR_ID", SEED_ACTOR);
                        row.setIfAbsent("MDFR_ID", SEED_ACTOR);
                    }
                    if (modified && row.values().containsKey("REG_DT")) {
                        row.setIfAbsent("MDFCN_DT", row.get("REG_DT"));
                    }
                    groups.computeIfAbsent(List.copyOf(row.values().keySet()), k -> new ArrayList<>()).add(row);
                }
                for (Map.Entry<List<String>, List<Row>> group : groups.entrySet()) {
                    insert(conn, table, group.getKey(), group.getValue());
                }
            }
        } finally {
            try (Statement st = conn.createStatement()) {
                st.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
        }
    }

    private void insert(Connection conn, String table, List<String> columns, List<Row> rows) throws SQLException {
        String sql = "INSERT INTO " + table + " (" + String.join(", ", columns) + ") VALUES ("
                + String.join(", ", columns.stream().map(c -> "?").toList()) + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int pending = 0;
            for (Row row : rows) {
                for (int i = 0; i < columns.size(); i++) {
                    ps.setObject(i + 1, row.get(columns.get(i)));
                }
                ps.addBatch();
                if (++pending == BATCH_SIZE) {
                    ps.executeBatch();
                    pending = 0;
                }
            }
            if (pending > 0) {
                ps.executeBatch();
            }
        } catch (SQLException e) {
            throw new SQLException(table + " 넣기 실패: " + e.getMessage(), e.getSQLState(), e.getErrorCode(), e);
        }
    }

    /**
     * 행이 있는 테이블
     */
    public static Map<String, Long> nonEmpty(Connection conn, Schema schema, List<String> tables) throws SQLException {
        Map<String, Long> filled = new LinkedHashMap<>();
        try (Statement st = conn.createStatement()) {
            for (String table : tables) {
                schema.columns(table);
                try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    rs.next();
                    if (rs.getLong(1) > 0) {
                        filled.put(table, rs.getLong(1));
                    }
                }
            }
        }
        return filled;
    }

    /**
     * 행 전부 삭제(seedClean)
     * - 호출한 쪽의 트랜잭션 안에서 DELETE. TRUNCATE와 달리 실패하면 롤백된다
     * @return 테이블 → 지운 행 수(0행 제외)
     */
    @SafeVarargs
    public static Map<String, Long> deleteAll(Connection conn, Schema schema, List<String>... tableLists) throws SQLException {
        Map<String, Long> deleted = new LinkedHashMap<>();
        try (Statement st = conn.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS = 0");
            try {
                for (List<String> tables : tableLists) {
                    for (String table : tables) {
                        if (schema.hasTable(table)) {
                            long n = st.executeLargeUpdate("DELETE FROM " + table);
                            if (n > 0) {
                                deleted.put(table, n);
                            }
                        }
                    }
                }
            } finally {
                st.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
        }
        return deleted;
    }

    /**
     * 테이블 비우기(로컬 -Preset 전용)
     */
    @SafeVarargs
    public static void truncate(Connection conn, Schema schema, List<String>... tableLists) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS = 0");
            try {
                for (List<String> tables : tableLists) {
                    for (String table : tables) {
                        if (schema.hasTable(table)) {
                            st.execute("TRUNCATE TABLE " + table);
                        }
                    }
                }
            } finally {
                st.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
        }
    }
}
