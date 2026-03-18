package com.example.citirecontoare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PerformanceAdapter extends RecyclerView.Adapter<PerformanceAdapter.ViewHolder> {

    private List<PerformanceLog> logs;

    public PerformanceAdapter(List<PerformanceLog> logs) {
        this.logs = logs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_performance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PerformanceLog log = logs.get(position);
        holder.dateText.setText(log.dataFinalizare); // Va fi formatul HH:mm cerut de tine
        holder.routeText.setText("Traseu: " + log.casaInceput + " -> " + log.casaSfarsit);
        holder.durationText.setText("Durată: " + log.durataMinute + " min");
    }

    @Override
    public int getItemCount() { return logs.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateText, routeText, durationText;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.textDate);
            routeText = itemView.findViewById(R.id.textRoute);
            durationText = itemView.findViewById(R.id.textDuration);
        }
    }
}