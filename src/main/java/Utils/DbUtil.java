package Utils;


import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DbUtil {
    private static DataSource ds = null;
    static final Properties prop = new Properties();

    static {
        //创建mysql的数据源
        try {
            //加载db.properties配置文件
            InputStream in = DbUtil.class.getClassLoader().getResourceAsStream("db.properties");
            prop.load(in);
            HikariConfig config = new HikariConfig();
            config.setDriverClassName(prop.getProperty("driverClassName"));
            config.setJdbcUrl(prop.getProperty("url"));
            config.setUsername(prop.getProperty("username"));
            config.setPassword(prop.getProperty("password"));

            ds = new HikariDataSource(config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    public static void main(String[] args) throws SQLException {
        try (Connection connection = getConnection()) {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("select * from user");
            while (resultSet.next()) {
                String user = resultSet.getString(1);
                String password = resultSet.getString(2);
                System.out.println(user + ";" + password);
            }
        }
    }
}
