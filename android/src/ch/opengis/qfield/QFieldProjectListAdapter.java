package ch.opengis.qfield;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class QFieldProjectListAdapter extends ArrayAdapter<QFieldProjectListItem> {
 
        private final Context context;
        private final List<QFieldProjectListItem> values;
 
        public QFieldProjectListAdapter(Context context, List<QFieldProjectListItem> values) {
 
            super(context, android.R.layout.simple_list_item_2, android.R.id.text1, values);
 
            this.context = context;
            this.values = values;
        }
 
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
 
            LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
 
            QFieldProjectListItem item = values.get(position);
            
            View rowView = inflater.inflate(R.layout.list_project_item, parent, false);
 
            ImageView imgView = (ImageView)rowView.findViewById(R.id.item_icon);
            TextView titleView = (TextView)rowView.findViewById(R.id.item_title);

            imgView.setImageResource(item.getImageId());
            imgView.setImageAlpha(172);
            titleView.setText(item.getText());

            if (item.getType() == QFieldProjectListItem.TYPE_SEPARATOR){
                rowView = inflater.inflate(R.layout.list_separator, null);
                TextView separatorView = (TextView)rowView.findViewById(R.id.separator);
                separatorView.setText(item.getText());
            }
            
            return rowView;
        }
}
