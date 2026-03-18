package com.example.citirecontoare;

public class PerformanceLog {
    public String dataFinalizare;
    public String casaInceput;
    public String casaSfarsit;
    public long durataMinute;
    public String operatorEmail;

    // Constructor gol necesar pentru Firebase
    public PerformanceLog() {}

    public PerformanceLog(String dataFinalizare, String casaInceput, String casaSfarsit, long durataMinute, String operatorEmail) {
        this.dataFinalizare = dataFinalizare;
        this.casaInceput = casaInceput;
        this.casaSfarsit = casaSfarsit;
        this.durataMinute = durataMinute;
        this.operatorEmail = operatorEmail;
    }
}