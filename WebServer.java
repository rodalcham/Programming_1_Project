import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import Database.java

public class WebServer {
    private static final int PORT = 8080;
    private static final int THREAD_POOL_SIZE = 50;

    public static void main(String[] args) {
        Database.initialize();
        ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (Socket socket = clientSocket;
             InputStream input = socket.getInputStream();
             OutputStream output = socket.getOutputStream()) {

            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            // parse request line
            String requestLine = reader.readLine();
            if (requestLine == null) return;
            System.out.println("Request: " + requestLine);

            String[] parts = requestLine.split(" ");
            String method = parts[0];
            String path = parts[1];

            // read headers
            int contentLength = 0;
            String line;
            while (!(line = reader.readLine()).isEmpty()) {
                if (line.startsWith("Content-Length:")) {
                    contentLength = Integer.parseInt(line.split(":")[1].trim());
                }
            }

            // route requests
            String body;
            if (method.equals("GET") && path.equals("/grades")) {
                body = buildGradesPage();
            } else if (method.equals("POST") && path.equals("/grades")) {
                // read post body
                char[] bodyChars = new char[contentLength];
                reader.read(bodyChars, 0, contentLength);
                String postBody = new String(bodyChars);
                handlePostGrade(postBody);
                body = buildGradesPage();
            } else {
                body = buildNotFoundPage();
            }

            sendResponse(output, body);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handlePostGrade(String postBody) {
        String student = null;
        String grade = null;
        for (String param : postBody.split("&")) {
            String[] kv = param.split("=");
            if (kv.length == 2) {
                String key = URLDecoder.decode(kv[0]);
                String value = URLDecoder.decode(kv[1]);
                if (key.equals("student")) student = value;
                if (key.equals("grade")) grade = value;
            }
        }
        if (student != null && grade != null) {
            Database.insertGrade(student, grade);
        }
    }

    private static String buildGradesPage() {
        return "<!DOCTYPE html><html><body>" +
               "<h1>Student Grades</h1>" +
               "<form method='POST' action='/grades'>" +
               "  <label>Student: <input type='text' name='student'/></label><br/>" +
               "  <label>Grade: <input type='text' name='grade'/></label><br/>" +
               "  <button type='submit'>Add Grade</button>" +
               "</form>" +
               "<h2>All Grades</h2>" +
               Database.getGradesAsHtmlTable() +
               "</body></html>";
    }

    private static String buildNotFoundPage() {
        return "<!DOCTYPE html><html><body><h1>404 Not Found</h1></body></html>";
    }

    private static void sendResponse(OutputStream output, String body) throws IOException {
        byte[] bodyBytes = body.getBytes();
        String headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + bodyBytes.length + "\r\n" +
                "\r\n";
        output.write(headers.getBytes());
        output.write(bodyBytes);
        output.flush();
    }
}