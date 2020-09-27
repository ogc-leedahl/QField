package ch.opengis.qfield;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;

import android.os.Bundle;
import android.os.Environment;
import android.net.Uri;
import android.app.Activity;
import android.app.ListActivity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;

public class QFieldDCSProjectActivity extends Activity {

    private static final String TAG = "QField DCS Project Activity";
    private String path;
    private SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    private ListView list;

    @Override
    public void onCreate(Bundle bundle) {
        Log.d(TAG, "onCreate() ");
        super.onCreate(bundle);

        sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        setContentView(R.layout.list_projects);
        getActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#80CC28"))); 
        drawView();
    }

    private void drawView() {

        setTitle(getString(R.string.select_project));

    }

    public void onRestart(){
        Log.d(TAG, "onRestart ");

        // The first opened activity
        if (!getIntent().hasExtra("path")) {
            drawView();
        }
        super.onRestart();
    }
    private void onItemClick(int position) {
        Log.d(TAG, "onListItemClick ");

        final QFieldProjectListItem item = (QFieldProjectListItem) list.getAdapter().getItem(position);
        if (item.getType() == QFieldProjectListItem.TYPE_SEPARATOR){
            return;
        }
        // Show a warning if it's the first time the sd-card is used
        boolean showSdCardWarning = sharedPreferences.getBoolean("ShowSdCardWarning", true);
        if (item.getType() == QFieldProjectListItem.TYPE_SECONDARY_ROOT && showSdCardWarning){
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle(getString(R.string.alert_sd_card_title));
            alertDialog.setMessage(getString(R.string.alert_sd_card_message));
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.alert_sd_card_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        startItemClickActivity(item);
                    }
                });
            alertDialog.show();
            editor.putBoolean("ShowSdCardWarning", false);
            editor.commit();
        }else {
            startItemClickActivity(item);
        }
    }

    private void startItemClickActivity(QFieldProjectListItem item){
        File file = item.getFile();
        Log.d(TAG, "file: "+file.getPath());                
        if (file.isDirectory()) {
            Intent intent = new Intent(this, QFieldDCSProjectActivity.class);
            intent.putExtra("path", file.getPath());
            intent.putExtra("label", item.getText());
            startActivityForResult(intent, 123);
        } else {
            Intent data = new Intent();

            Uri uri = Uri.fromFile(file);
            data.setData(uri);
            
            Toast.makeText(this, getString(R.string.loading) + " " + file.getPath(), Toast.LENGTH_LONG).show();
            setResult(Activity.RESULT_OK, data);

            String lastUsedProjects = sharedPreferences.getString("LastUsedProjects", null);
            ArrayList<String> lastUsedProjectsArray = new ArrayList<String>();
            if (lastUsedProjects != null){
                lastUsedProjectsArray = new ArrayList<String>(Arrays.asList(lastUsedProjects.split("--;--")));
            }
            // If the element is already present, delete it. It will be added again in the last position
            lastUsedProjectsArray.remove(file.getPath());
            if (lastUsedProjectsArray.size() >= 5){
                lastUsedProjectsArray.remove(0);
            }
            // Add the project path to the array
            lastUsedProjectsArray.add(file.getPath());

            // Write the recent projects into the shared preferences
            editor.putString("LastUsedProjects", TextUtils.join("--;--", lastUsedProjectsArray));
            editor.commit();

            finish();
        }
    }

    private boolean onItemLongClick(int position) {

        QFieldProjectListItem item = (QFieldProjectListItem) list.getAdapter().getItem(position);
        if (item.getType() != QFieldProjectListItem.TYPE_ITEM) {
            return true;
        }
        File file = item.getFile();
        if (! file.isDirectory()){
            return true;
        }

        String favoriteDirs = sharedPreferences.getString("FavoriteDirs", null);
        ArrayList<String> favoriteDirsArray = new ArrayList<String>();
        if (favoriteDirs != null){
            favoriteDirsArray = new ArrayList<String>(Arrays.asList(favoriteDirs.split("--;--")));
        }

        // If the element is already present, delete it. It will be added again in the last position
        favoriteDirsArray.remove(file.getPath());

        // First activity
        if (! getIntent().hasExtra("path")) {
            // Remove the recent projects from shared preferences
            favoriteDirs = TextUtils.join("--;--", favoriteDirsArray);
            if (favoriteDirs == ""){
                favoriteDirs = null;
            }

            editor.putString("FavoriteDirs", favoriteDirs);
            editor.commit();
            drawView();

            Toast.makeText(this, file.getName() + " " + getString(R.string.removed_from_favorites), Toast.LENGTH_LONG).show();
        } else {
            // Write the recent projects into the shared preferences
            favoriteDirsArray.add(file.getPath());
            editor.putString("FavoriteDirs", TextUtils.join("--;--", favoriteDirsArray));
            editor.commit();

            Toast.makeText(this, file.getName() + " " + getString(R.string.added_to_favorites), Toast.LENGTH_LONG).show();
        }

        return true;
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
}
