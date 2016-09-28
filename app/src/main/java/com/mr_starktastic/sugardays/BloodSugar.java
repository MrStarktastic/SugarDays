package com.mr_starktastic.sugardays;

public class BloodSugar {
    private static final int MGDL_IDX = 0, MMOLL_IDX = 1;
    private static final int CONVERSION_FACTOR = 18;

    private Number[] arr;

    public BloodSugar(int mgdl) {
        arr = new Number[]{mgdl, convert(mgdl)};
    }

    public BloodSugar(float mmoll) {
        arr = new Number[]{convert(mmoll), mmoll};
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
}
