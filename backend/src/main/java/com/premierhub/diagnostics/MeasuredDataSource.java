package com.premierhub.diagnostics;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

final class MeasuredDataSource extends DelegatingDataSource {
    private final Map<Connection, Boolean> seen = Collections.synchronizedMap(new WeakHashMap<>());
    MeasuredDataSource(DataSource target) { super(target); }

    @Override public Connection getConnection() throws SQLException { return acquire(null, null, false); }
    @Override public Connection getConnection(String username, String password) throws SQLException {
        return acquire(username, password, true);
    }
    private Connection acquire(String username, String password, boolean credentials) throws SQLException {
        ReadTrace trace = ReadTrace.current();
        if (trace == null) return credentials ? super.getConnection(username, password) : super.getConnection();
        long start = System.nanoTime();
        Connection connection;
        try { connection = credentials ? super.getConnection(username, password) : super.getConnection(); }
        catch (SQLException failure) { trace.connection(System.nanoTime() - start, false); throw failure; }
        long elapsed = System.nanoTime() - start;
        Connection physical;
        try { physical = connection.unwrap(Connection.class); }
        catch (SQLException unsupported) { physical = connection; }
        boolean reused = seen.put(physical, Boolean.TRUE) != null;
        trace.connection(elapsed, reused);
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    boolean control = method.getName().matches("commit|rollback|close|setAutoCommit|setReadOnly|setTransactionIsolation");
                    long controlStart = System.nanoTime();
                    try {
                        Object value = invoke(connection, method, args);
                        if (value instanceof Statement statement) {
                            String sql = args != null && args.length > 0 && args[0] instanceof String text ? text : null;
                            return statement(statement, sql, trace);
                        }
                        return value;
                    } finally { if (control) trace.control(System.nanoTime() - controlStart); }
                });
    }
    private Object statement(Statement target, String preparedSql, ReadTrace trace) {
        Class<?> type = target instanceof CallableStatement ? CallableStatement.class
                : target instanceof PreparedStatement ? PreparedStatement.class : Statement.class;
        int[] batchItems = {0};
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            boolean execute = method.getName().startsWith("execute");
            boolean batch = method.getName().equals("executeBatch") || method.getName().equals("executeLargeBatch");
            String sql = preparedSql;
            if (args != null && args.length > 0 && args[0] instanceof String text) sql = text;
            long start = System.nanoTime();
            Object value;
            try {
                value = invoke(target, method, args);
                if (method.getName().equals("addBatch")) batchItems[0]++;
                if (method.getName().equals("clearBatch")) batchItems[0] = 0;
            } finally {
                if (execute) trace.execution(sql, System.nanoTime() - start, batch ? batchItems[0] : 1);
                if (batch) batchItems[0] = 0;
            }
            if (value instanceof ResultSet result) return resultSet(result, trace);
            return value;
        });
    }
    private Object resultSet(ResultSet target, ReadTrace trace) {
        return Proxy.newProxyInstance(ResultSet.class.getClassLoader(), new Class<?>[]{ResultSet.class},
                (proxy, method, args) -> {
                    boolean fetch = method.getName().equals("next");
                    long start = System.nanoTime();
                    try { return invoke(target, method, args); }
                    finally { if (fetch) trace.fetch(System.nanoTime() - start); }
                });
    }
    private static Object invoke(Object target, Method method, Object[] args) throws Throwable {
        try { return method.invoke(target, args); }
        catch (InvocationTargetException failure) { throw failure.getCause(); }
    }
}
