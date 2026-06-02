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
    public static double predictNextConsumption(List<Double> historyList, int targetMonthIndex) {
        if (historyList == null || historyList.isEmpty()) return 0.0;

        if (historyList.size() < 2) {
            return applySeasonalAdjustment(historyList.get(0), targetMonthIndex, historyList.size());
        }

        RegressionModel model = getLinearRegression(historyList);
        double prediction = model.predict(historyList.size() + 1);

        double lastValue = historyList.get(historyList.size() - 1);
        double maxAllowed = lastValue * 2.0;

        prediction = Math.max(0, prediction);
        prediction = Math.min(prediction, maxAllowed);

        return applySeasonalAdjustment(prediction, targetMonthIndex, historyList.size());
    }

    public static double applySeasonalAdjustment(double basePrediction, int targetMonthIndex, int historySize) {
        if (historySize < 6) {
            return Math.max(0, basePrediction);
        }

        double[] seasonalFactors = {
                0.82,
                0.85,
                0.95,
                1.00,
                1.10,
                1.30,
                1.40,
                1.35,
                1.05,
                0.95,
                0.90,
                0.85
        };

        if (targetMonthIndex < 0 || targetMonthIndex > 11) {
            targetMonthIndex = Calendar.getInstance().get(Calendar.MONTH);
        }

        double multiplier = seasonalFactors[targetMonthIndex];
        return Math.max(0, basePrediction * multiplier);
    }
}