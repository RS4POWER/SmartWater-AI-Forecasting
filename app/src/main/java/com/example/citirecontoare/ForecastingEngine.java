package com.example.citirecontoare;

import java.util.Calendar;
import java.util.List;

public class ForecastingEngine {

    /**
     * Predicție bazată pe Regresie Liniară Simplă (y = ax + b)
     * historylist Lista cu consumurile lunare anterioare
     * @return Valoarea prezisă pentru luna următoare
     */
    // 1. Obiect care ține rezultatul matematic
    public static class RegressionModel {
        public double slope;
        public double intercept;

        public RegressionModel(double slope, double intercept) {
            this.slope = slope;
            this.intercept = intercept;
        }

        public double predict(double x) {
            return slope * x + intercept;
        }
    }

    // 2. Metoda centralizată de calcul (O scriem o singură dată!)
    public static RegressionModel getLinearRegression(List<Double> history) {
        int n = history.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            double x = i + 1; // 🔥 Prima lună este 1, nu 0
            double y = history.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double denominator = (n * sumX2 - sumX * sumX);
        if (denominator == 0) return new RegressionModel(0, history.get(n - 1));

        double slope = (n * sumXY - sumX * sumY) / denominator;
        double intercept = (sumY - slope * sumX) / n;

        return new RegressionModel(slope, intercept);
    }

    // 3. Metoda de predicție devine acum foarte scurtă
    public static double predictNextConsumption(List<Double> historyList) {
        if (historyList == null || historyList.isEmpty()) return 0.0;

        // 🔥 FALLBACK: Dacă avem doar o lună (după filtrarea avariilor),
        // predicția este egală cu acea lună.
        if (historyList.size() < 2) {
            return applySeasonalAdjustment(historyList.get(0));
        }

        RegressionModel model = getLinearRegression(historyList);
        double prediction = model.predict(historyList.size() + 1);

        // 🔥 LIMITARE INTELIGENTĂ (Anti-Spike)
        // Nu lăsăm predicția să sară de 2x față de ultima lună reală
        double lastValue = historyList.get(historyList.size() - 1);
        double maxAllowed = lastValue * 2.0;

        prediction = Math.max(0, prediction); // Nu poate fi negativ
        prediction = Math.min(prediction, maxAllowed); // Nu poate fi absurd de mare

        return applySeasonalAdjustment(prediction);
    }

    public static double applySeasonalAdjustment(double basePrediction) {
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