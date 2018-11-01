package tools.mysql;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.stream.Collectors;

public final class MysqlService {
    private static final String SELECT_WHERE = "SELECT %s FROM %s WHERE %s";
    private static final String SELECT = "SELECT %s FROM %s";
    private static final String INSERT = "INSERT INTO %s SET %s";
    private static final String ARG = "`%s`='%s'";
    private static final String UPDATE = "UPDATE %s SET %s WHERE %s";
    private static final String DELETE = "DELETE FROM %s WHERE %s";
    private static volatile MysqlService mInstance;
    private JDBCConnectionPool pool;

    private MysqlService(MysqlConfig config) {
        pool = new JDBCConnectionPool(config);
    }

    public static MysqlService getInstance(MysqlConfig config) {
        if (mInstance == null) {
            synchronized (MysqlService.class) {
                if (mInstance == null) {
                    mInstance = new MysqlService(config);
                }
            }
        }
        return mInstance;
    }

    public ResultSet read(String select, String tableName, String where) throws NullPointerException {
        ResultSet result = null;
        if (where == null || where.isEmpty()) {
            result = executeQuery(String.format(SELECT, select, tableName));
        } else {
            result = executeQuery(String.format(SELECT_WHERE, select, tableName, where));
        }
        if(result != null){
            return result;
        }
        throw new NullPointerException();
    }

    public int countAll(String tableName, String where) {
        ResultSet read = this.read("COUNT(*)", tableName, where);
        try {
            read.next();
            return read.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void insert(String tableName, Map<String, Object> entity) {
        String params = toArgs(entity);
        execute(String.format(INSERT, tableName, params));
    }

    public void update(String tableName, Map<String, Object> entity, String where) {
        String params = toArgs(entity);
        execute(String.format(UPDATE, tableName, params, where));
    }

    public void delete(String tableName, String where) {
        execute(String.format(DELETE, tableName, where));
    }

    private String toArgs(Map<String, Object> entity) {
        return entity.entrySet().stream()
                .map(obj -> String.format(ARG, obj.getKey(), obj.getValue()))
                .collect(Collectors.joining(", "));
    }

    public synchronized void execute(String query) {
        Connection connection = pool.checkOut();
        try {
            Statement stmt = connection.createStatement();
            stmt.execute(query);
        } catch (SQLException e) {
            System.out.println(String.format("[SKIP] - %s", query));
        } finally {
            pool.pop(connection);
        }
    }

    public ResultSet executeQuery(String query) {
        Connection connection = pool.checkOut();
        try {
            Statement stmt = connection.createStatement();
            stmt.executeQuery(query);
            return stmt.getResultSet();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            pool.pop(connection);
        }

    }
}
