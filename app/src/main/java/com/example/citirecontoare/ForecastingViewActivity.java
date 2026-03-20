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
        List<Entry> entryPredictionPoint = new ArrayList<>(); // 🔥 Set nou pentru bulină

        // 1. Punctele Reale (Indexate de la 1)
        for (int i = 0; i < history.size(); i++) {
            entriesReal.add(new Entry(i + 1, history.get(i).floatValue()));
        }

        ForecastingEngine.RegressionModel model = ForecastingEngine.getLinearRegression(history);
        int n = history.size();

        // 2. Linia de Trend (de la luna 1 până la n + 1)
        for (int i = 1; i <= n + 1; i++) {
            entriesRegression.add(new Entry(i, (float) model.predict(i)));
        }

        // 3. Punctul de Predicție (Bulina „Wow”)
        float predX = n + 1;
        double rawPrediction = model.predict(predX);

        // 🔥 APLICĂM SEZONALITATEA ȘI PE GRAFIC
        float predY = (float) ForecastingEngine.applySeasonalAdjustment(rawPrediction);

        entryPredictionPoint.add(new Entry(predX, predY));
        // --- CONFIGURARE VIZUALĂ ---

        // Set Real (Albastru)
        LineDataSet dataSetReal = new LineDataSet(entriesReal, "Consum Real");
        dataSetReal.setColor(Color.BLUE);
        dataSetReal.setCircleColor(Color.BLUE);
        dataSetReal.setLineWidth(0f);
        dataSetReal.setCircleRadius(6f); // Folosește dp pentru scalare corectă

        // Set Trend (Roșu întrerupt)
        LineDataSet dataSetReg = new LineDataSet(entriesRegression, "Trend AI");
        dataSetReg.setColor(Color.RED);
        dataSetReg.enableDashedLine(10f, 10f, 0f);
        dataSetReg.setDrawCircles(false);

        // 🔥 Set Bulină Predicție (Galben/Auriu)
        LineDataSet dataSetPred = new LineDataSet(entryPredictionPoint, "Predicție Luna Viitoare");
        dataSetPred.setCircleColor(Color.parseColor("#FFD700")); // Gold
        dataSetPred.setColor(Color.parseColor("#FFD700"));
        dataSetPred.setCircleRadius(8f);
        dataSetPred.setDrawCircleHole(true);
        dataSetPred.setCircleHoleRadius(4f);
        dataSetPred.setLineWidth(0f); // Fără linie, doar punctul

        dataSetPred.setValueTextColor(Color.BLACK);
        dataSetPred.setValueTextSize(10f);
// Afișăm valoarea exactă deasupra bulinei
        dataSetPred.setValueFormatter(new com.github.mikephil.charting.formatter.DefaultValueFormatter(1));

        // Aplicare pe grafic
        LineData lineData = new LineData(dataSetReal, dataSetReg, dataSetPred);
        lineChart.setData(lineData);
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f); // Doar numere întregi (Luna 1, 2, 3...)
        xAxis.setLabelCount(history.size() + 1); // Câte label-uri să apară
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getDescription().setText("Evoluție Consum per Lună");
        lineChart.animateX(1000); // Animație faină de un secundă
        lineChart.invalidate();
    }

}