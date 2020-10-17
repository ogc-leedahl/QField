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

import java.util.List;

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
    }

    public void onRestart(){
        Log.d(TAG, "onRestart ");

        drawView();
        lifecycleRegistry.markState(Lifecycle.State.RESUMED);
        super.onRestart();
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
