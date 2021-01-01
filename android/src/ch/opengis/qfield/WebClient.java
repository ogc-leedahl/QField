package ch.opengis.qfield;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebClient implements Runnable {
    public enum Method {
        LAYERS,
        PROJECT_FILE,
        LAYER_FILE
    }

    public WebClient() {
        method = Method.LAYERS;
        body = "";
    }

    public WebClient(Method method, String body) {
        this.method = method;
        this.body = body;
    }

    private Method method;
    private String body;
    private boolean suceeded = false;
    private String errorMessage;
    private JSONObject json;
    private Element xml;
    private String text;

    public boolean IsSucceeded() { return this.suceeded; }
    public String getErrorMessage() { return this.errorMessage; }
    public JSONObject getJson() { return json; }
    public Element getXml() { return xml; }
    public String getText() { return text; }

    @Override
    public void run() {
        try {
            InetAddress serverAddr = InetAddress.getLoopbackAddress();
            BufferedReader reader;
            Socket socket = new Socket(serverAddr, 8080);
            PrintWriter output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
            String path;
            String contentType;
            String contentLengh;
            String httpMethod;
            boolean createJson;
            switch(method) {
                case PROJECT_FILE:
                    path = "project";
                    httpMethod = "POST";
                    contentType = "Content-Type: application/json\r\n";
                    contentLengh = "Content-Length: " + Integer.toString(body.length()) + "\r\n";
                    createJson = false;
                    break;

                case LAYER_FILE:
                    path = "layer";
                    httpMethod = "POST";
                    contentType = "Content-Type: application/json\n";
                    contentLengh = "Content-Length: " + Integer.toString(body.length()) + "\r\n";
                    createJson = true;
                    break;

                default:
                    path = "layers";
                    httpMethod = "GET";
                    contentType = "";
                    contentLengh = "";
                    body = "";
                    createJson = true;
                    break;
            }
            output.print(String.format("%s /%s HTTP/1.1\r\n", httpMethod, path) +
                    "User-Agent: Mozilla/4.0 (compatible; MSIE5.01; Windows NT)\r\n" +
                    "Host: localhost:8080\r\n" +
                    "Accept-Language: en-us\r\n" +
                    contentType +
                    contentLengh +
                    "\r\n" +
                    body);
            output.flush();

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Map<String, String> headers = new HashMap<>();
            String statusLine = reader.readLine();
            if(statusLine.contains(" 200 ")) {
                String line = reader.readLine();
                while (line.trim().length() > 0) {
                    Pattern pattern = Pattern.compile("^([A-Za-z-]+)\\s*:\\s*([A-Za-z0-9,;: =-]+)$");
                    Matcher matcher = pattern.matcher(line.trim());
                    if (matcher.matches()) {
                        headers.put(matcher.group(1), matcher.group(2));
                    }
                    line = reader.readLine();
                }

                String contentLength = headers.get("Content-Length");
                int length = contentLength != null ? Integer.parseInt(contentLength) : 0;
                json = new JSONObject();
                if (length > 0) {
                    char[] body = new char[length];
                    if (reader.read(body, 0, length) == length) {
                        text = new String(body);
                        if (createJson) json = (JSONObject)new JSONTokener(text).nextValue();
                        else {
                            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                            DocumentBuilder builder = factory.newDocumentBuilder();
                            Document document = builder.parse(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
                            xml = document.getDocumentElement();
                        }
                    }
                }
                suceeded = true;
                errorMessage = statusLine;

            } else errorMessage = String.format("Failed to access offline cache: %s", statusLine);
        }
        catch (Exception ex) {
            errorMessage = String.format("Failed to access offline cache: %s", ex.getMessage());
        }
    }
}
