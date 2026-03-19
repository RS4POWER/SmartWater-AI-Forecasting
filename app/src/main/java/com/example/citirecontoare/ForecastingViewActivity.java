package com.example.citirecontoare;

import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import java.util.ArrayList;
import java.util.List;

public class ForecastingViewActivity extends AppCompatActivity {

    private LineChart lineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecasting_view);

        lineChart = findViewById(R.id.reportingChart);
        findViewById(R.id.btnCloseChart).setOnClickListener(v -> finish());

        // Preluăm datele istorice trimise din MeterReadingActivity
        ArrayList<Double> history = (ArrayList<Double>) getIntent().getSerializableExtra("HISTORY_DATA");

        if (history != null && !history.isEmpty()) {
            setupChart(history);
        }
    }

    private void setupChart(List<Double> history) {
        List<Entry> entriesReal = new ArrayList<>();
        List<Entry> entriesRegression = new ArrayList<>();

        // 1. Punctele Reale (din Firebase)
        for (int i = 0; i < history.size(); i++) {
            entriesReal.add(new Entry(i + 1, history.get(i).floatValue()));
        }

        // 2. Calculul liniei de trend (Regresia Liniară)
        int n = history.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            double x = i + 1;
            double y = history.get(i);
            sumX += x; sumY += y; sumXY += x * y; sumX2 += x * x;
        }
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        // Desenăm trendul până la luna viitoare (n + 1)
        for (int i = 0; i <= n; i++) {
            float x = i + 1;
            float y = (float) (slope * x + intercept);
            entriesRegression.add(new Entry(x, y));
        }

        // 3. Setări vizuale pentru Punctele Reale (Albastru)
        LineDataSet dataSetReal = new LineDataSet(entriesReal, "Consum Real (m³)");
        dataSetReal.setColor(Color.BLUE);
        dataSetReal.setCircleColor(Color.BLUE);
        dataSetReal.setLineWidth(0f); // Nu unim punctele reale, lăsăm doar punctele
        dataSetReal.setCircleRadius(6f);
        dataSetReal.setDrawCircleHole(false);

        // 4. Setări vizuale pentru Trendul AI (Roșu)
        LineDataSet dataSetReg = new LineDataSet(entriesRegression, "Trend Predicitiv AI");
        dataSetReg.setColor(Color.RED);
        dataSetReg.setLineWidth(3f);
        dataSetReg.setDrawCircles(false);
        dataSetReg.enableDashedLine(10f, 10f, 0f); // Linie întreruptă

        // 5. Aplicare date pe grafic
        LineData lineData = new LineData(dataSetReal, dataSetReg);
        lineChart.setData(lineData);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getDescription().setText("Evoluție Consum per Lună");
        lineChart.animateX(1000); // Animație faină de un secundă
        lineChart.invalidate();
    }
}