package tools.mysql;

public class Config implements MysqlConfig {


    private static volatile Config mInstance;
    private String database;
    private String user;
    private String password;
    private String server;

    private Config(String server, String database) {
        this(database, "root", "", server);
    }

    private Config(String database, String user, String password, String server) {
        this.database = database;
        this.user = user;
        this.password = password;
        this.server = server;
    }

    public static void createInstance(String server, String database) {
        if (mInstance == null) {
            synchronized (Config.class) {
                if (mInstance == null) {
                    mInstance = new Config(server, database);
                }
            }
        }
    }

    public static Config getInstance() {
        if (mInstance == null) {
            throw new Error("Config не инициализирован! перед использованием вызовите createInstance()");
        }
        return mInstance;
    }

    @Override
    public String getServer() {
        return server;
    }

    @Override
    public String getDataBase() {
        return database;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public String getPassword() {
        return password;
    }
}
