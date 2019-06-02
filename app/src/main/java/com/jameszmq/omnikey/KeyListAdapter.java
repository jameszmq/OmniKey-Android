package com.jameszmq.omnikey;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class KeyListAdapter extends RecyclerView.Adapter<KeyListAdapter.MyViewHolder> {

    Context context;
    int numKey;
    SharedPreferences sp;

    public KeyListAdapter(Context context, int numKey) {
        this.context = context;
        this.numKey = numKey;
        sp = context.getSharedPreferences("Key",Context.MODE_PRIVATE);
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        TextView key, key_des;
        Button key_set, key_remove;

        public MyViewHolder(ConstraintLayout constraintLayout) {
            super(constraintLayout);
            key = constraintLayout.findViewById(R.id.key);
            key_des = constraintLayout.findViewById(R.id.key_des);
            key_set = constraintLayout.findViewById(R.id.key_set);
            key_remove = constraintLayout.findViewById(R.id.key_remove);
        }

        public TextView getKey() {
            return key;
        }

        public TextView getKeyDes() {
            return key_des;
        }

        public Button getKeySet() {
            return key_set;
        }

        public TextView getKeyRemove() {
            return key_remove;
        }
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        ConstraintLayout v = (ConstraintLayout) LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.list_item, viewGroup, false);
        MyViewHolder vh = new MyViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(MyViewHolder viewHolder, final int i) {
        final int number = i + 1;
        viewHolder.getKey().setText(getName(number));
        viewHolder.getKey().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Rename");

                final EditText input = new EditText(context);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setHint("Please enter new name");
                builder.setView(input);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String m_Text = input.getText().toString();
                        setName(m_Text, number);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
            }
        });
        viewHolder.getKeyDes().setText("Lat: " + getLat(number) + "\nLng: " + getLng(number));
        viewHolder.getKeySet().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity)context).addFence(number);
            }
        });
        viewHolder.getKeyRemove().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sp.edit().remove("Name" + number).remove("Lat" + number).remove("Lng" + number).commit();
                ((MainActivity)context).removeFence(number);
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return numKey;
    }

    public String getName(int i) {
        return sp.getString("Name" + i, "Key" + i);
    }
    public String getLat(int i) {
        double lat = Double.longBitsToDouble(sp.getLong("Lat" + i, 0));
        if (lat == 0) {
            return "Not set";
        }
        else {
            return Double.toString(lat);
        }
    }
    public String getLng(int i) {
        double lng = Double.longBitsToDouble(sp.getLong("Lng" + i, 0));
        if (lng == 0) {
            return "Not set";
        }
        else {
            return Double.toString(lng);
        }    }
    public void setName(String name, int i) {
        sp.edit().putString("Name" + i, name).commit();
        notifyDataSetChanged();
    }
    public void setLat(double lat,int i) {
        sp.edit().putLong("Lat" + i, Double.doubleToRawLongBits(lat)).commit();
        notifyDataSetChanged();
    }
    public void setLng(double lng, int i) {
        sp.edit().putLong("Lng" + i, Double.doubleToRawLongBits(lng)).commit();
        notifyDataSetChanged();
    }
}
