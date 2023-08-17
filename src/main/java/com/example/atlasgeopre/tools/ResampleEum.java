package com.example.atlasgeopre.tools;

public enum ResampleEum {
    NEAR("near"),
    BILINEAR("bilinear"),
    CUBIC("cubic"),
    CUBICSPLINE("cubicspline"),
    LANCZOS("lanczos"),
    AVERAGE("average"),
    RMS("rms"),
    MODE("mode"),
    MAX("max"),
    MIN("min"),
    MED("med"),
    Q1("q1"),
    Q3("q3"),
    SUM("sum");

    private String value;

    ResampleEum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
