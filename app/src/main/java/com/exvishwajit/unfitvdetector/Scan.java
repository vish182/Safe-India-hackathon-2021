package com.exvishwajit.unfitvdetector;

public class Scan {
    String result;
    String open;
    String date;
    String state;
    String model;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    Scan(){}

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    Scan(String result, String open, String date, String state, String model){
        this.result = result;
        this.open = open;
        this.date = date;
        this.state = state;
        this.model = model;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }

    public String getOpen() {
        return open;
    }

    public void setOpen(String open) {
        this.open = open;
    }
}
