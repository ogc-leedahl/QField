package ch.opengis.qfield;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QFieldDCSActivityModel extends ViewModel {
    private MutableLiveData<List<QFieldProjectListItem>> liveValues;
    private MutableLiveData<Boolean> fetchEnabled;
    private String errorMessage;
    private boolean succeeded;

    public String getErrorMessage() { return errorMessage; }
    private void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public boolean didLoadSucceed() { return succeeded; }
    private void setSucceeded(boolean succeeded) { this.succeeded = succeeded; }

    public LiveData<Boolean> getFetchEnabled() {
        if(fetchEnabled == null) {
            fetchEnabled = new MutableLiveData<>();
            fetchEnabled.setValue(false);
        }

        return  fetchEnabled;
    }

    public void setFetchEnabled(boolean enabled) {
        this.fetchEnabled.setValue(enabled);
    }

    public LiveData<List<QFieldProjectListItem>> getValues() {
        if(liveValues == null) {
            liveValues = new MutableLiveData<>();

            WebClient webClient = new WebClient();
            Thread webClientThread = new Thread(webClient);
            webClientThread.start();
            try {
                webClientThread.join();

            } catch(Exception ex) {
                // Thread interrupted; Nothing to do in this situation; execution continues.
            }

            // Initialize list
            List<QFieldProjectListItem> values = new ArrayList<>();
            try {
                JSONObject json = webClient.getJson();
                JSONArray layers = json.getJSONArray("layers");
                for (int i = 0; i < layers.length(); i++) {
                    String layerId = layers.getString(i);
                    values.add(new QFieldProjectListItem(new File(layerId), layerId,
                            android.R.drawable.checkbox_off_background, QFieldProjectListItem.TYPE_ITEM));
                }
                setSucceeded(true);

            } catch(Exception ex) {
                // Leave the list empty if we can't parse the JSON body.
                setErrorMessage(String.format("Couldn't parse JSON response: %s.", ex.getMessage()));
                setSucceeded(false);
            }
            liveValues.setValue(values);

        } else setSucceeded(true);

        return liveValues;
    }

    public static class WebClient implements Runnable {
        private boolean suceeded = false;
        private String errorMessage;
        private JSONObject json;

        public boolean IsSucceeded() { return this.suceeded; }
        public String getErrorMessage() { return this.errorMessage; }
        public JSONObject getJson() { return json; }

        @Override
        public void run() {
            try {
                InetAddress serverAddr = InetAddress.getLoopbackAddress();
                BufferedReader reader;
                Socket socket = new Socket(serverAddr, 8080);
                PrintWriter output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
                output.print("GET /layers HTTP/1.1\r\n" +
                        "User-Agent: Mozilla/4.0 (compatible; MSIE5.01; Windows NT)\r\n" +
                        "Host: localhost:8080\r\n" +
                        "Accept-Language: en-us\r\n" +
                        "\r\n");
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
                            json = (JSONObject)new JSONTokener(new String(body)).nextValue();
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
}
