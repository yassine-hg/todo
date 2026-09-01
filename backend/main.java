import java.sql.*;

public class main {
    public static void main(String[] args) throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/todoapp", "root", "yassine123!@");
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM tasks");
        while(rs.next()){
            System.out.println(rs.getInt("id") + "--" + rs.getString("title"));
        }
        conn.close();

    } 
}
