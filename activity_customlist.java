package com.example.app_login;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class activity_customlist extends ArrayAdapter<String> {

    private final LayoutInflater inflater;

    public activity_customlist(@NonNull Context context, @NonNull List<String> dataList) {
        super(context, R.layout.activity_customlist, dataList);
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            view = inflater.inflate(R.layout.activity_customlist, parent, false);
        }

        String item = getItem(position);

        if (item != null) {
            String[] parts = item.split(", ");

            // Assuming the format is "Name: [name], Phone: [phone]"
            String name = parts[0].substring(parts[0].indexOf(":") + 2);
            String phone = parts[1].substring(parts[1].indexOf(":") + 2);

            TextView textName = view.findViewById(R.id.textName);
            TextView textPhone = view.findViewById(R.id.textPhone);

            textName.setText(name);
            textPhone.setText(phone);
        }

        return view;
    }
}
