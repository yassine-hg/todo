import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.security.*;


public class server {
    
    public static void main(String[] args) throws IOException {
    Properties props = new Properties();
    props.load(new FileInputStream("config.properties"));
    String url = props.getProperty("db.url");
    String user = props.getProperty("db.user");
    String password = props.getProperty("db.password");
    String clientId = props.getProperty("google.client.id");
    String clientSecret = props.getProperty("google.client.secret");
    String redirectUri = props.getProperty("google.redirect.uri");
    Map<String, String> session = new ConcurrentHashMap<>();
    Map<String, String> stateMap = new ConcurrentHashMap<>();


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
            
            server.createContext("/login", exchange -> {
                try{
                    SecureRandom codeVerifier = new SecureRandom(); 
                    byte[] bytes = new byte[32];
                    codeVerifier.nextBytes(bytes);
                    String encoding =  Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    byte[] hashBytes = md.digest(encoding.getBytes("UTF-8"));
                    String codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes);
                    String state = UUID.randomUUID().toString();
                    stateMap.put(state, encoding);

                    String authUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                    + "?client_id=" + clientId
                    + "&redirect_uri=" + URLEncoder.encode(redirectUri, "UTF-8")
                    + "&response_type=code"
                    + "&scope=" + URLEncoder.encode("openid email profile", "UTF-8")
                    + "&code_challenge=" + codeChallenge
                    + "&code_challenge_method=S256"
                    + "&state=" + state;

                    exchange.getResponseHeaders().set("Location", authUrl);
                    exchange.sendResponseHeaders(302, -1);
                    exchange.close();
                    
                }catch(NoSuchAlgorithmException e){
                    System.err.println("Error SHA-256 not found");
                }
            
            });
            server.createContext("/callback", exchange -> {
                try {
                    String query = exchange.getRequestURI().getQuery();
                    String code = "";
                    String state = "";

                    for (String pair : query.split("&")) {
                        String[] kv = pair.split("=", 2);
                        if (kv[0].equals("code")) code = URLDecoder.decode(kv[1], "UTF-8");
                        if(kv[0].equals("state")) state = URLDecoder.decode(kv[1] , "UTF-8");
                    }
                    String codeVerifier = stateMap.get(state);
                    stateMap.remove(state);
                    String form = "code=" + URLEncoder.encode(code, "UTF-8")
                        + "&client_id=" + URLEncoder.encode(clientId, "UTF-8")
                        + "&client_secret=" + URLEncoder.encode(clientSecret, "UTF-8")
                        + "&redirect_uri=" + URLEncoder.encode(redirectUri, "UTF-8")
                        + "&grant_type=authorization_code"
                        + "&code_verifier=" + URLEncoder.encode(codeVerifier , "UTF-8");

                    HttpClient client = HttpClient.newHttpClient(); //creating  the  cliend  for sending  la requette 
                    HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("https://oauth2.googleapis.com/token"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form))
                        .build();

                    HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString()); //sending 
                    System.out.println("TOKEN RESPONSE: " + res.body());
                    String body = res.body();
                    int keyPos = body.indexOf("\"id_token\"");
                    if (keyPos == -1) {
                        System.out.println("NO ID TOKEN. RESPONSE: " + body);
                        return;
                    }
                    int start = body.indexOf("\"", body.indexOf(":", keyPos)) + 1;
                    int end = body.indexOf("\"", start);
                    String idToken = body.substring(start, end);


                    String[] parts = idToken.split("\\.");
                    String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), "UTF-8");

                    // fetching  and  printing the  email 
                    int emailKey = payloadJson.indexOf("\"email\"");
                    int emailStart = payloadJson.indexOf("\"", payloadJson.indexOf(":", emailKey)) + 1;
                    int emailEnd = payloadJson.indexOf("\"", emailStart);
                    String email = payloadJson.substring(emailStart, emailEnd);

                    System.out.println("EMAIL: " + email);

                    String  sessionId = UUID.randomUUID().toString();
                    session.put(sessionId, email);
                    exchange.getResponseHeaders().set("Set-Cookie", "session=" + sessionId + "; Path=/; HttpOnly");
                    
                    exchange.getResponseHeaders().set("Location", "/");
                    exchange.sendResponseHeaders(302, -1);
                    exchange.close();

                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(500, -1);
                    exchange.close();
                }
            });

            //read the session cookie return the email or a 401
            server.createContext("/api/me", exchange -> {
               try {
                String email = null;
                List<String> cookies = exchange.getRequestHeaders().get("Cookie");

                if (cookies != null) {
                    for (String header : cookies) {
                        for (String c : header.split(";")) {
                            String[] kv = c.trim().split("=", 2);
                            if (kv[0].equals("session")) email = session.get(kv[1]);
                        }
                    }
                }

                if (email == null) {
                    exchange.sendResponseHeaders(401, -1);
                    exchange.close();
                    return;
                }

                byte[] bytes = ("{\"email\":\"" + email + "\"}").getBytes("UTF-8");
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();

               } catch (Exception e) {
                   e.printStackTrace();
               }
            });

            //logout
            server.createContext("/logout", exchange -> {
                try {
                    List<String> cookies = exchange.getRequestHeaders().get("Cookie");
                    if (cookies != null) {
                        for (String header : cookies) {
                            for (String c : header.split(";")) {
                                String[] kv = c.trim().split("=", 2);
                                if (kv[0].equals("session")) session.remove(kv[1]);
                            }
                        }
                    }
                
                    exchange.getResponseHeaders().set("Set-Cookie", "session=; Path=/; Max-Age=0");
                    exchange.getResponseHeaders().set("Location", "/");
                    exchange.sendResponseHeaders(302, -1);
                    exchange.close();
                } catch (Exception e) {
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