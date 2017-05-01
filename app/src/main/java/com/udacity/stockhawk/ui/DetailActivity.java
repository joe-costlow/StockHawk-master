package com.udacity.stockhawk.ui;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.EntryXComparator;
import com.udacity.stockhawk.ChartUtils.XAxisLabelFormatting;
import com.udacity.stockhawk.ChartUtils.YAxisLabelFormatting;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import yahoofinance.Stock;

public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String LOG_TAG = DetailActivity.class.getSimpleName();

    public static final int CHART_LOADER_ID = 1;
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.linechart)
    LineChart mLineChart;
    @BindView(R.id.progress_bar_detail)
    ProgressBar mProgressBar;
    private List<Entry> mEntries;
    private String symbol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        ButterKnife.bind(this);

        mProgressBar.setVisibility(View.VISIBLE);
        mLineChart.setVisibility(View.GONE);

//        Get and set URI data sent intent into string variable
        Intent intentFromMainActivity = getIntent();
        Uri stockUri = intentFromMainActivity.getData();
        symbol = stockUri.getLastPathSegment();

//        Create new ArrayList for data point entries for graph
        mEntries = new ArrayList<>();

//        Initialize or restart loader
        LoaderManager loaderManager = getSupportLoaderManager();
        Loader<Stock> loader = loaderManager.getLoader(CHART_LOADER_ID);

        if (loader == null) {
            loaderManager.initLoader(CHART_LOADER_ID, null, this).forceLoad();
        } else {
            loaderManager.restartLoader(CHART_LOADER_ID, null, this).forceLoad();
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        mProgressBar.setVisibility(View.VISIBLE);

//        Set URI and projection for cursor to be returned
        Uri stockUri = Contract.Quote.makeUriForStock(symbol);
        String[] projection = {Contract.Quote.COLUMN_HISTORY};

        return new CursorLoader(
                this,
                stockUri,
                projection,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        mLineChart.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.GONE);

        String[] dataCoord;
        String dateString;
        String priceString;
        float dateFloat;
        float priceFloat;
        Long referenceTime;

//        Move to first position of cursor
        data.moveToFirst();

//        Retrieve stock history data from cursor
        String history = data.getString(0);

//        Stock history data for each day is first separated by a new line. Split history string
//        into different days by new lines
        String[] infoPairs = history.split("\n");

//        Within each line of stock history data, split into a date string and closing price string
        for (String infoSet : infoPairs) {
            dataCoord = infoSet.split(", ");
            dateString = dataCoord[0];
            priceString = dataCoord[1];

//            Reformat date and closing price strings into float
            dateFloat = Float.parseFloat(dateString);
            priceFloat = Float.parseFloat(priceString);

//            add each entry into ArrayList to be plotted on graph
            mEntries.add(new Entry(dateFloat, priceFloat));
        }

        data.close();

        referenceTime = Long.valueOf(infoPairs.length - 1);

//        Sort and compare entries to be plotted on graph
        Collections.sort(mEntries, new EntryXComparator());

//        Set chart legend properties
        Legend legend = mLineChart.getLegend();
        legend.setFormSize(10);
        legend.setForm(Legend.LegendForm.CIRCLE);
        legend.setTextSize(12);
        legend.setTextColor(Color.CYAN);

//        Set X-Axis properties of chart
        XAxis xAxis = mLineChart.getXAxis();
        xAxis.setValueFormatter(new XAxisLabelFormatting(referenceTime));
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);
        xAxis.setAxisLineWidth(3);
        xAxis.setEnabled(true);

//        Set Y-Axis properties of chart
        YAxis yAxis = mLineChart.getAxisLeft();
        yAxis.setTextColor(Color.WHITE);
        yAxis.setDrawGridLines(false);
        yAxis.setValueFormatter(new YAxisLabelFormatting());
        yAxis.setAxisLineWidth(3);
        yAxis.setEnabled(true);

//        Set line properties, including data to be used from ArrayList
        LineDataSet dataSet = new LineDataSet(mEntries, getString(R.string.chart_legend_data_description));
        LineData lineData = new LineData(dataSet);
        lineData.setValueTextColor(Color.YELLOW);
        lineData.setValueTextSize(12);

//        Set description properties of chart
        Description description = new Description();
        description.setTextColor(Color.RED);
        description.setTextSize(24);
        description.setText(symbol);

//        Set chart properties
        mLineChart.setData(lineData);
        mLineChart.setScaleEnabled(true);
        mLineChart.setPinchZoom(false);
        mLineChart.setDoubleTapToZoomEnabled(false);
        mLineChart.getAxisRight().setEnabled(false);
        mLineChart.setBorderColor(Color.WHITE);
        mLineChart.setDescription(description);
        mLineChart.invalidate();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
