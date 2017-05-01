package com.udacity.stockhawk.ChartUtils;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.DecimalFormat;

/**
 * Created by Joseph Costlow on 23-Apr-17.
 */

public class YAxisLabelFormatting implements IAxisValueFormatter {

    DecimalFormat mDecimalFormat;

    public YAxisLabelFormatting() {
        mDecimalFormat = new DecimalFormat("#.##");
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        return mDecimalFormat.format(value);
    }
}
