import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.nio.file.*;

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
				sendRedirect(output, "/grades");
				return;
            } else if (method.equals("POST") && path.equals("/grades/delete")) {
    			char[] bodyChars = new char[contentLength];
    			reader.read(bodyChars, 0, contentLength);
    			String postBody = new String(bodyChars);
    			// parse the id out of "id=5"
    			String idStr = postBody.replace("id=", "").trim();
    			Database.deleteGrade(Integer.parseInt(idStr));
    			sendRedirect(output, "/grades");
    			return;
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

	private static String buildGradesPage() throws IOException {
		String template = new String(Files.readAllBytes(Paths.get("grades.html")));
		double mean = Database.calculateMean();
		String meanStr = mean >= 0 ? String.format("%.1f", mean) : "No data yet";
		String meanClass = mean >= 0 ? "" : "no-data"; // dimmed style when empty
		return template
			.replace("GRADES_TABLE_ROWS", Database.getGradesAsHtmlRows())
			.replace("##MEAN##", meanStr)
			.replace("GRADES_MEAN_CLASS", meanClass);
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

	private static void sendRedirect(OutputStream output, String location) throws IOException {
    String response = "HTTP/1.1 303 See Other\r\n" +
                      "Location: " + location + "\r\n" +
                      "\r\n";
    output.write(response.getBytes());
    output.flush();
	}

}