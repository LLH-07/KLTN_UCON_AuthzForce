/*
Đây là file chứa code kết nối đến database PostgreSQL, được sử dụng trong quá trình đánh giá chính sách của Authzforce.
- Có thể chỉnh sửa URL, USER và PASSWORD để phù hợp với cấu hình của máy.

Nó chứa:
- JDBC URL
- User
- Password
- Method getConnection()

Đây là file quan trọng vì:
- RequestContextRepository dùng nó để đọc dữ liệu liên quan đến request đọc sách (session_id) từ database, rồi gửi cho AuthzforceEvaluator để đánh giá chính sách.
- Custom policy provider cũng dùng JDBC URL từ config để đọc policy XML
*/

package com.demo.ebook;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Db {
    private static final String URL = "jdbc:postgresql://localhost:5432/ebook_demo?currentSchema=public";
    private static final String USER = "postgres";
    private static final String PASSWORD = "12345678";

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);

        /*Debug information about the database connection*/
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("select current_database(), current_schema(), current_user")) {
            if (rs.next()) {
                System.out.println("DB DEBUG => database=" + rs.getString(1)
                        + ", schema=" + rs.getString(2)
                        + ", user=" + rs.getString(3));
            }
        }

        return conn;
    }
}