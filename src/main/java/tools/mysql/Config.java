package tools.mysql;

public abstract class Config implements MysqlConfig {

    @Override
    public String getServer() {
        return "localhost:3306";
    }

    @Override
    public String getUser() {
        return "root";
    }

    @Override
    public String getPassword() {
        return /*"Пароль"*/"";
    }
}
