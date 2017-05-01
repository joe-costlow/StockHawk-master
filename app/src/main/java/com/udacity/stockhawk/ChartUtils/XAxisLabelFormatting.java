package com.udacity.stockhawk.ChartUtils;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

/**
 * Created by Joseph Costlow on 23-Apr-17.
 */

public class XAxisLabelFormatting implements IAxisValueFormatter {

    private SimpleDateFormat mSimpleDateFormat;
    private Long referenceTime;
    private DecimalFormat mDecimalFormat;

    public XAxisLabelFormatting(Long referenceTime) {
        mSimpleDateFormat = new SimpleDateFormat("MM/dd");
        this.referenceTime = referenceTime;
        mDecimalFormat = new DecimalFormat("#.##");
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        return mSimpleDateFormat.format(value + referenceTime);
    }
}
