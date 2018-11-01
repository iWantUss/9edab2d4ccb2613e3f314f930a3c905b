package tools.mysql;

import com.mysql.jdbc.Driver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class JDBCConnectionPool extends ExpiredObjectPool<Connection> {
    private MysqlConfig config;

    public JDBCConnectionPool(MysqlConfig config) {
        this.config = config;
        try {
            Driver.class.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
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
            throw new Error("Нет соединения с БД.");
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
