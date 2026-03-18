package com.example.citirecontoare;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class PerformanceHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PerformanceAdapter adapter;
    private List<PerformanceLog> logList;
    private String selectedZone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_performance_history);

        selectedZone = getIntent().getStringExtra("zoneName");

        recyclerView = findViewById(R.id.recyclerPerformance);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        logList = new ArrayList<>();
        adapter = new PerformanceAdapter(logList);
        recyclerView.setAdapter(adapter);

        loadPerformanceLogs();
    }

    private void loadPerformanceLogs() {
        FirebaseFirestore.getInstance()
                .collection("zones").document(selectedZone)
                .collection("logsPerformanta")
                .orderBy("dataFinalizare", Query.Direction.DESCENDING) // Cele mai noi sus
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    logList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        PerformanceLog log = doc.toObject(PerformanceLog.class);
                        logList.add(log);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Eroare la încărcare date!", Toast.LENGTH_SHORT).show());
    }
}