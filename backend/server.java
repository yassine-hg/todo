import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.*;
import java.util.Properties;


public class server {
    
    public static void main(String[] args) throws IOException {
    Properties props = new Properties();
    props.load(new FileInputStream("config.properties"));
    String url = props.getProperty("db.url");
    String user = props.getProperty("db.user");
    String password = props.getProperty("db.password");

    HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/tasks", exchange -> { // this runs when we hit the request /api/taks
            try{
                String methode = exchange.getRequestMethod();
                Connection conn = DriverManager.getConnection(url , user, password);
                if(methode.equals("GET")){
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT * FROM tasks");
                    StringBuilder sb = new StringBuilder(); // create a space so we can add strings to it 
                    sb.append("[");   
                    while (rs.next()){
                        sb.append("{\"id\":" + rs.getInt("id") + ",\"title\":\"" + rs.getString("title") + "\",\"deadline\":\"" + rs.getDate("deadline") + "\",\"done\":" + rs.getBoolean("done") + "},");                
                    }
                    if (sb.length() > 1) sb.deleteCharAt(sb.length() - 1);
                    sb.append("]");
                    String response  =  sb.toString();
                    byte[] bytes = response.getBytes("UTF-8") ;     //this  convert the txt into bytes cz http transmet bytes
                    exchange.sendResponseHeaders(200, bytes.length); //send  the https request and header
                    OutputStream os = exchange.getResponseBody();
                    os.write(bytes);
                    os.close();
                // inserting an element 
                }else if(methode.equals("POST")){
                    InputStream is = exchange.getRequestBody();
                    String body = new String(is.readAllBytes(), "UTF-8");
                    String title = "";
                    String deadline = "";
                    for(String pair : body.split("&")){
                        String[] kv = pair.split("=" , 2);
                        if (kv[0].equals("title")) title = URLDecoder.decode(kv[1], "UTF-8"); // mean if  the  first  part that we  split  using  the "=" equals to the title  we take  the  second  one  that  contait  the title 
                        if (kv[0].equals("deadline")) deadline = kv[1]; // same thing as the  first one

                    }
                    PreparedStatement pstmt = conn.prepareStatement("INSERT INTO tasks (title, deadline) VALUES (? , ?)");
                    pstmt.setString(1, title);
                    pstmt.setString(2, deadline.isEmpty() ? null : deadline);
                    pstmt.executeUpdate();
                    byte[] bytes = "{\"status\":\"ok\"}".getBytes("UTF-8");
                    exchange.sendResponseHeaders(201, bytes.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(bytes);
                    os.close();
                }
                //deleting items
                else if(methode.equals("DELETE")){
                    String path = exchange.getRequestURI().getPath();
                    String idStr = path.substring(path.lastIndexOf("/") + 1); // to take after the / with  is the id
                    int id = Integer.parseInt(idStr);

                    PreparedStatement pstmt = conn.prepareStatement("DELETE FROM tasks where id = ?");
                    pstmt.setInt(1, id);
                    pstmt.executeUpdate();
                    exchange.sendResponseHeaders(200, 0);
                    OutputStream os = exchange.getResponseBody();
                    os.close();
                }
            }catch(SQLException e){
                e.printStackTrace();
            }   
            });  
            server.setExecutor(null);
            server.start();   
        }
    }

















/*
200 OK — GET succeeded
201 Created — POST succeeded
204 No Content — DELETE succeeded, nothing to send back
400 Bad Request — the client sent something invalid
404 Not Found — path or resource doesn't exist
500 Internal Server Error — your code broke
*/