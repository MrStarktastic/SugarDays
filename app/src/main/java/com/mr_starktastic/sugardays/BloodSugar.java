package com.mr_starktastic.sugardays;

import java.text.DecimalFormat;

public class BloodSugar {
    public static final DecimalFormat mgdlFormat = new DecimalFormat("0");
    public static final DecimalFormat mmollFormat = new DecimalFormat("0.0");
    private static final int MGDL_IDX = 0, MMOLL_IDX = 1;
    private static final int CONVERSION_FACTOR = 18;
    private Number[] arr;

    private BloodSugar() {
    }

    public BloodSugar(int mgdl) {
        arr = new Number[]{mgdl, convert(mgdl)};
    }

    public BloodSugar(float mmoll) {
        arr = new Number[]{convert(mmoll), mmoll};
    }

    public static BloodSugar[] getDiscreteExtremeValues() {
        final BloodSugar minVal = new BloodSugar();
        minVal.arr = new Number[]{10, 0.5f};
        final BloodSugar maxVal = new BloodSugar();
        maxVal.arr = new Number[]{270, 15f};

        return new BloodSugar[]{minVal, maxVal};
    }

    public static BloodSugar[] getExtremeValues() {
        final BloodSugar minVal = new BloodSugar();
        minVal.arr = new Number[]{10, 0.5f};
        final BloodSugar maxVal = new BloodSugar();
        maxVal.arr = new Number[]{900, 50f};

        return new BloodSugar[]{minVal, maxVal};
    }

    public static DecimalFormat getFormat(int bgUnit) {
        return bgUnit == MGDL_IDX ? mgdlFormat : mmollFormat;
    }

    private static float convert(int mgdl) {
        return mgdl / CONVERSION_FACTOR;
    }

    private static int convert(float mmoll) {
        return (int) (mmoll * CONVERSION_FACTOR);
    }

    public int getMgdl() {
        return (int) arr[MGDL_IDX];
    }

    public float getMmoll() {
        return (float) arr[MMOLL_IDX];
    }

    public float get(int index) {
        return arr[index].floatValue();
    }
}
