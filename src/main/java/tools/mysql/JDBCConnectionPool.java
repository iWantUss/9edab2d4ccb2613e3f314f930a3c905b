package tools.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class JDBCConnectionPool extends ExpiredObjectPool<Connection> {
    private MysqlConfig config;

    public JDBCConnectionPool(MysqlConfig config) {
        this.config = config;
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    protected Connection create() {
        String url = String.format("jdbc:mysql://%s/%s", config.getServer(), config.getDataBase());
        Properties properties = new Properties();
        properties.setProperty("user", config.getUser());
        properties.setProperty("password", config.getPassword());
        properties.setProperty("useUnicode", "true");
        properties.setProperty("characterEncoding", "UTF-8");
        try {
            return (DriverManager.getConnection(url, properties));
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void expire(Connection o) {
        try {
            o.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean validate(Connection o) {
        try {
            return (!o.isClosed());
        } catch (SQLException e) {
            e.printStackTrace();
            return (false);
        }
    }
}
