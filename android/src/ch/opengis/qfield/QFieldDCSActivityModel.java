package ch.opengis.qfield;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
                    JSONObject layer = layers.getJSONObject(i);
                    String layerId = layer.getString("name");
                    String title = layer.getString("title");
                    values.add(new QFieldProjectListItem(new File(layerId), title,
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
}
