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
import java.util.Calendar;
import java.util.List;

public class ForecastingViewActivity extends AppCompatActivity {

    private LineChart lineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecasting_view);

        lineChart = findViewById(R.id.reportingChart);
        findViewById(R.id.btnCloseChart).setOnClickListener(v -> finish());

        ArrayList<Double> history =
                (ArrayList<Double>) getIntent().getSerializableExtra("HISTORY_DATA");

        int targetMonthIndex = getIntent().getIntExtra(
                "TARGET_MONTH_INDEX",
                Calendar.getInstance().get(Calendar.MONTH)
        );

        if (history != null && !history.isEmpty()) {
            setupChart(history, targetMonthIndex);
        }
    }

    private void setupChart(List<Double> history, int targetMonthIndex) {
        List<Entry> entriesReal = new ArrayList<>();
        List<Entry> entriesRegression = new ArrayList<>();
        List<Entry> entryPredictionPoint = new ArrayList<>();

        for (int i = 0; i < history.size(); i++) {
            entriesReal.add(new Entry(i + 1, history.get(i).floatValue()));
        }

        ForecastingEngine.RegressionModel model = ForecastingEngine.getLinearRegression(history);
        int n = history.size();

        for (int i = 1; i <= n + 1; i++) {
            entriesRegression.add(new Entry(i, (float) model.predict(i)));
        }

        float predX = n + 1;
        float predY = (float) ForecastingEngine.predictNextConsumption(history, targetMonthIndex);
        entryPredictionPoint.add(new Entry(predX, predY));

        LineDataSet dataSetReal = new LineDataSet(entriesReal, "Consum real");
        dataSetReal.setColor(Color.BLUE);
        dataSetReal.setCircleColor(Color.BLUE);
        dataSetReal.setLineWidth(0f);
        dataSetReal.setCircleRadius(6f);

        LineDataSet dataSetReg = new LineDataSet(entriesRegression, "Trend liniar brut");
        dataSetReg.setColor(Color.RED);
        dataSetReg.enableDashedLine(10f, 10f, 0f);
        dataSetReg.setDrawCircles(false);

        LineDataSet dataSetPred = new LineDataSet(entryPredictionPoint, "Predicție ajustată");
        dataSetPred.setCircleColor(Color.parseColor("#FFD700"));
        dataSetPred.setColor(Color.parseColor("#FFD700"));
        dataSetPred.setCircleRadius(8f);
        dataSetPred.setDrawCircleHole(true);
        dataSetPred.setCircleHoleRadius(4f);
        dataSetPred.setLineWidth(0f);
        dataSetPred.setValueTextColor(Color.BLACK);
        dataSetPred.setValueTextSize(10f);
        dataSetPred.setValueFormatter(
                new com.github.mikephil.charting.formatter.DefaultValueFormatter(1)
        );

        LineData lineData = new LineData(dataSetReal, dataSetReg, dataSetPred);
        lineChart.setData(lineData);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(history.size() + 1);

        lineChart.getDescription().setText("Evoluție consum per lună");
        lineChart.animateX(1000);
        lineChart.invalidate();
    }
}