package com.example.jalkster.ui.util;

import android.text.InputFilter;
import android.text.Spanned;

public class RangeInputFilter implements InputFilter {

    private final int min;
    private final int max;

    public RangeInputFilter(int min, int max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        StringBuilder builder = new StringBuilder(dest);
        builder.replace(dstart, dend, source.subSequence(start, end).toString());
        String newValue = builder.toString();

        if (newValue.isEmpty()) {
            return null;
        }

        if (newValue.length() > 2) {
            return "";
        }

        try {
            int input = Integer.parseInt(newValue);
            if (input >= min && input <= max) {
                return null;
            }
        } catch (NumberFormatException ignored) {
        }

        return "";
    }
}
