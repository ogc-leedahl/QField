package ch.opengis.qfield;

import android.app.Activity;
import android.arch.lifecycle.*;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class QFieldDCSProjectActivity extends Activity implements LifecycleOwner, ViewModelStoreOwner, ViewModelProvider.Factory {

    private static final String TAG = "QField DCS Activity";
    private static final ViewModelStore store = new ViewModelStore();
    private QFieldDCSActivityModel model;
    private LifecycleRegistry lifecycleRegistry;
    private ListView list;
    private Button fetch;

    @Override
    public void onCreate(Bundle bundle) {
        Log.d(TAG, "onCreate() ");
        super.onCreate(bundle);

        setContentView(R.layout.list_layers);
        getActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#80CC28")));
        lifecycleRegistry = new LifecycleRegistry(this);
        lifecycleRegistry.markState(Lifecycle.State.CREATED);
        drawView();
    }

    private void drawView() {
        list = (ListView)findViewById(R.id.list);
        fetch = (Button)findViewById(R.id.fetch);

        setTitle(getString(R.string.select_layers));

        model = new ViewModelProvider(this, this).get(QFieldDCSActivityModel.class);
        model.getValues().observe(this, new Observer<List<QFieldProjectListItem>>() {
            @Override
            public void onChanged(List<QFieldProjectListItem> qFieldProjectListItems) {
                if(model.didLoadSucceed()) {
                    QFieldProjectListAdapter adapter = new QFieldProjectListAdapter(QFieldDCSProjectActivity.this, qFieldProjectListItems);
                    list.setAdapter(adapter);

                } else Toast.makeText(QFieldDCSProjectActivity.this, model.getErrorMessage(), Toast.LENGTH_LONG).show();
            }
        });

        model.getFetchEnabled().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                fetch.setEnabled(aBoolean);
            }
        });

        // Put the data into the list
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                Log.d(TAG, "onItemClick ");
                QFieldDCSProjectActivity.this.onItemClick(position);
            }
        });

        // Fetch project
        OnFetch onFetch = new OnFetch(list);
        fetch.setOnClickListener(onFetch);
    }

    public void onRestart(){
        Log.d(TAG, "onRestart ");

        drawView();
        lifecycleRegistry.markState(Lifecycle.State.RESUMED);
        super.onRestart();
    }

    private static class OnFetch implements View.OnClickListener {
        private ListView list;

        OnFetch(ListView list) { this.list = list; }

        @Override
        public void onClick(View v) {
            Log.d(TAG, "OnFetch.onClick ");
            try {
                // Get the project file
                JSONObject json = new JSONObject();
                JSONArray layers = new JSONArray();
                SparseBooleanArray positions = list.getCheckedItemPositions();
                for (int i = 0; i < positions.size(); i++) {
                    int key = positions.keyAt(i);
                    boolean checked = positions.get(key);
                    if (checked) {
                        QFieldProjectListItem item = (QFieldProjectListItem)list.getAdapter().getItem(key);
                        String name = item.getFile().getName();
                        layers.put(name);
                    }
                }
                json.put("layers", layers);

                WebClient webClient = new WebClient(WebClient.Method.PROJECT_FILE, json.toString());
                Thread webClientThread = new Thread(webClient);
                webClientThread.start();
                try {
                    webClientThread.join();

                } catch(Exception ex) {
                    // Thread interrupted; Nothing to do in this situation; execution continues.
                }

                // Save Project File in a random directory name.
                String directory = UUID.randomUUID().toString();
                createFile(directory, "project.qgs", webClient.getText());
                
                // Get layer files
                Element root = webClient.getXml();
                NodeList layerNodes = root.getElementsByTagName("layer-tree-layer");
                for (int nodeIndex = 0; nodeIndex < layerNodes.getLength(); nodeIndex++) {
                    Element layer = (Element) layerNodes.item(nodeIndex);
                    String filename = new File(layer.getAttribute("source")).getName();
                    String id = layer.getAttribute("id");
                    JSONObject request = new JSONObject();
                    request.put("layer", id);

                    webClient = new WebClient(WebClient.Method.LAYER_FILE, request.toString());
                    webClientThread = new Thread(webClient);
                    webClientThread.start();
                    try {
                        webClientThread.join();

                    } catch(Exception ex) {
                        // Thread interrupted; Nothing to do in this situation; execution continues.
                    }

                    // Save the layer file
                    createFile(directory, filename, webClient.getText());
                }

            } catch (JSONException jsonException) {
                // Do nothing
            }
        }

        private void createFile(String subdirectory, String filename, String body) {
            String path = "/sdcard/gis";
            File directory = new File(path);
            if (!directory.exists()) directory.mkdir();
            path += "/" + subdirectory;
            directory = new File(path);
            directory.mkdir();
            directory.setReadable(true);

            path += "/" + filename;
            File file = new File(path);
            try {
                file.createNewFile();
                FileOutputStream stream = new FileOutputStream(file);
                stream.write(body.getBytes(StandardCharsets.UTF_8));
                stream.flush();
                stream.close();
                file.setReadable(true);
            }
            catch (IOException ex) {
                // Nothing to do
            }
            int x = 1;
            x++;
        } // 4d4d93c8-1500-493a-bb8f-86176ff05750
    }

    private void onItemClick(int position) {
        Log.d(TAG, "onListItemClick ");

        final QFieldProjectListItem item = (QFieldProjectListItem) list.getAdapter().getItem(position);

        if (item.getType() == QFieldProjectListItem.TYPE_SEPARATOR){
            return;
        }

        // Update image
        SparseBooleanArray positions = list.getCheckedItemPositions();
        item.setImageId(positions.valueAt(position)
                ? android.R.drawable.checkbox_on_background : android.R.drawable.checkbox_off_background);
        ((QFieldProjectListAdapter)list.getAdapter()).notifyDataSetChanged();

        // Update fetch button.
        boolean found = false;
        for(int index = 0; index < positions.size(); index++)
            if(positions.valueAt(index)) {
                found = true;
                break;
            }

        model.setFetchEnabled(found);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult ");
        Log.d(TAG, "resultCode: " + resultCode);

        // Close recursively the activity stack
        if (resultCode == Activity.RESULT_OK){
            if (getParent() == null) {
                setResult(Activity.RESULT_OK, data);
            } else {
                getParent().setResult(Activity.RESULT_OK, data);
            }
            finish();
        }
    }

    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }

    @Override
    protected void onStart() {
        super.onStart();
        lifecycleRegistry.markState(Lifecycle.State.STARTED);
    }

    @Override
    protected void onResume() {
        super.onResume();
        lifecycleRegistry.markState(Lifecycle.State.RESUMED);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        lifecycleRegistry.markState(Lifecycle.State.DESTROYED);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        store.clear();
    }

    @Override
    public ViewModelStore getViewModelStore() {
        return store;
    }

    @Override
    public <T extends ViewModel> T create(Class<T> aClass) {
        T instance;

        try {
            instance = aClass.newInstance();

        } catch (Exception ex) {
            throw new RuntimeException("Invalid class type.");
        }

        return instance;
    }
}
