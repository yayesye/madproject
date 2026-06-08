package com.example.authentication;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class StationAdapter extends RecyclerView.Adapter<StationAdapter.ViewHolder> {

    private Context context;
    private ArrayList<StationModel> stationList;

    public StationAdapter(Context context, ArrayList<StationModel> stationList) {
        this.context = context;
        this.stationList = stationList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_station, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StationModel model = stationList.get(position);

        holder.tvName.setText(model.getName());
        holder.tvTurbidity.setText("Turbidity: " + model.getTurbidity() + " NTU");
        holder.tvPH.setText("pH Level: " + model.getPh() + " pH");

        if (model.getTurbidity() > model.getMaxTurbidityLimit() || model.getPh() < model.getMinPhLimit()) {
            holder.tvBadge.setText("⚠️ Alert");
            holder.tvBadge.setBackgroundColor(0xFFFFCDD2);
            holder.tvBadge.setTextColor(0xFFB71C1C);
        } else {
            holder.tvBadge.setText("✅ Healthy");
            holder.tvBadge.setBackgroundColor(0xFFC8E6C9);
            holder.tvBadge.setTextColor(0xFF2E7D32);
        }

        holder.btnDetails.setOnClickListener(v -> {
            Intent intent = new Intent(context, AdminDashboard2Activity.class);
            intent.putExtra("STATION_ID", model.getStationId());
            intent.putExtra("STATION_NAME", model.getName());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return stationList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvBadge, tvTurbidity, tvPH, btnDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.itemStationName);
            tvBadge = itemView.findViewById(R.id.itemStatusBadge);
            tvTurbidity = itemView.findViewById(R.id.itemTurbidity);
            tvPH = itemView.findViewById(R.id.itemPH);
            btnDetails = itemView.findViewById(R.id.btnSeeDetails);
        }
    }
}