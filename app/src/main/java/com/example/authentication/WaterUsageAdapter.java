package com.example.authentication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class WaterUsageAdapter extends RecyclerView.Adapter<WaterUsageAdapter.ViewHolder> {

    ArrayList<WaterUsage> list;

    OnSelectClick selectListener;
    OnDeleteClick deleteListener;

    public interface OnSelectClick {
        void onSelect(WaterUsage usage);
    }

    public interface OnDeleteClick {
        void onDelete(WaterUsage usage);
    }

    public WaterUsageAdapter(
            ArrayList<WaterUsage> list,
            OnSelectClick selectListener,
            OnDeleteClick deleteListener
    ) {
        this.list = list;
        this.selectListener = selectListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_water_usage, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        WaterUsage usage = list.get(position);

        holder.txtInfo.setText(
                usage.consumption + " Liters | "
                        + usage.users + " Users | "
                        + usage.hours + " Hours"
        );

        holder.txtInfo.setOnClickListener(v ->
                selectListener.onSelect(usage));

        holder.btnDelete.setOnClickListener(v ->
                deleteListener.onDelete(usage));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtInfo;
        Button btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtInfo = itemView.findViewById(R.id.txtInfo);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}