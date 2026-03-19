package com.example.citirecontoare;

import java.util.Calendar;
import java.util.List;

public class ForecastingEngine {

    /**
     * Predicție bazată pe Regresie Liniară Simplă (y = ax + b)
     * @param historyList Lista cu consumurile lunare anterioare
     * @return Valoarea prezisă pentru luna următoare
     */
    public static double predictNextConsumption(List<Double> historyList) {
        if (historyList == null || historyList.size() < 2) {
            // Dacă avem prea puține date (sub 2 luni), nu putem face regresie
            // Returnăm ultima valoare sau o medie sigură
            return (historyList != null && !historyList.isEmpty()) ? historyList.get(historyList.size() - 1) : 0.0;
        }

        int n = historyList.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        // X reprezintă timpul (luna 1, 2, 3...)
        // Y reprezintă consumul (mc)
        for (int i = 0; i < n; i++) {
            double x = i + 1;
            double y = historyList.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        // Formula pentru pantă (a) și intersecție (b)
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        // Predicția brută pentru luna următoare (n + 1)
        double prediction = slope * (n + 1) + intercept;

        // Aplicăm AJUSTAREA SEZONIERĂ (Factor de inteligență)
        return applySeasonalAdjustment(prediction);
    }

    private static double applySeasonalAdjustment(double basePrediction) {
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH); // 0=Ian, 11=Dec

        // Tabel de coeficienți lunari (Indexat 0-11)
        // Valorile sub 1.0 scad predicția (iarna), peste 1.0 o cresc (vara)
        double[] seasonalFactors = {
                0.82, // Ianuarie (Minim)
                0.85, // Februarie
                0.95, // Martie (Începe primăvara)
                1.00, // Aprilie
                1.10, // Mai
                1.30, // Iunie (Vârf de vară)
                1.40, // Iulie (Vârf de vară)
                1.35, // August
                1.05, // Septembrie (Scădere - "September Drop")
                0.95, // Octombrie
                0.90, // Noiembrie
                0.85  // Decembrie
        };

        double multiplier = seasonalFactors[currentMonth];
        double finalPrediction = basePrediction * multiplier;

        return Math.max(0, finalPrediction);
    }
}