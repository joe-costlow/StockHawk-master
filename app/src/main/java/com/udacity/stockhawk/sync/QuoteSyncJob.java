package com.udacity.stockhawk.sync;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import timber.log.Timber;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;
import yahoofinance.quotes.stock.StockQuote;

public final class QuoteSyncJob {

    public static final String LOG_TAG = QuoteSyncJob.class.getSimpleName();

    public static final String ACTION_DATA_UPDATED = "com.udacity.stockhawk.ACTION_DATA_UPDATED";
    private static final int ONE_OFF_ID = 2;
    private static final int PERIOD = 300000;
    private static final int INITIAL_BACKOFF = 10000;
    private static final int PERIODIC_ID = 1;
    private static final int WEEK_HISTORY = 6;
    static String[] stockArray;

    private QuoteSyncJob() {
    }

    /**
     * Retrieve stock history, if valid, and write to local database. Then, sends a broadcast to
     * indicate a change to the database, if necessary
     *
     * @param context
     */
    static void getQuotes(final Context context) {

        Timber.d("Running sync job");

//        Set the time frame of history data to be retrieved for a stock
        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        from.add(Calendar.DATE, -WEEK_HISTORY);

        try {

//            Retrieves the default stock list, if first app run, or inputted symbol
            Set<String> stockPref = PrefUtils.getStocks(context);

//            Return if stocks list is empty
            if (stockPref.size() == 0) {
                return;
            }

//            Make a copy of the list and populate copy list
            Set<String> stockCopy = new HashSet<>();
            stockCopy.addAll(stockPref);
            stockArray = stockPref.toArray(new String[stockPref.size()]);

            Timber.d(stockCopy.toString());

//            Create iterator for copy list
            Iterator<String> iterator = stockCopy.iterator();

//            Create new ArrayList for possible new entries into local database
            ArrayList<ContentValues> quoteCVs = new ArrayList<>();

            while (iterator.hasNext()) {

                final String symbol = iterator.next();

//                Get stock data of current stock from YahooFinance
                Stock stock = YahooFinance.get(symbol);

//                If current stock symbol is valid, get quote and write data to ContentValues
                if (stock.isValid()) {
                    StockQuote quote = stock.getQuote();

                    float price = quote.getPrice().floatValue();
                    float change = quote.getChange().floatValue();
                    float percentChange = quote.getChangeInPercent().floatValue();

                    // WARNING! Don't request historical data for a stock that doesn't exist!
                    // The request will hang forever X_x
                    List<HistoricalQuote> history = stock.getHistory(from, to, Interval.DAILY);

                    StringBuilder historyBuilder = new StringBuilder();

                    for (HistoricalQuote it : history) {
                        historyBuilder.append(it.getDate().getTimeInMillis());
                        historyBuilder.append(", ");
                        historyBuilder.append(it.getClose());
                        historyBuilder.append("\n");
                    }

                    ContentValues quoteCV = new ContentValues();
                    quoteCV.put(Contract.Quote.COLUMN_SYMBOL, symbol);
                    quoteCV.put(Contract.Quote.COLUMN_PRICE, price);
                    quoteCV.put(Contract.Quote.COLUMN_PERCENTAGE_CHANGE, percentChange);
                    quoteCV.put(Contract.Quote.COLUMN_ABSOLUTE_CHANGE, change);
                    quoteCV.put(Contract.Quote.COLUMN_HISTORY, historyBuilder.toString());
                    quoteCVs.add(quoteCV);
                } else {

//                    If current list item is invalid, display a toast message indicating so
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, symbol + context.getString(R.string.toast_invalid_symbol), Toast.LENGTH_SHORT).show();
                        }
                    });

//                    Remove current list item from SharedPreferences list
                    PrefUtils.removeStock(context, symbol);
                }
            }

//            After iteration of list items, bulk insert data into local database and send broadcast
            context.getContentResolver()
                    .bulkInsert(
                            Contract.Quote.URI,
                            quoteCVs.toArray(new ContentValues[quoteCVs.size()]));

            Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED);
            context.sendBroadcast(dataUpdatedIntent);

        } catch (IOException exception) {
            Timber.e(exception, "Error fetching stock quotes");
            exception.printStackTrace();
        }
    }

    /**
     * Schedule a period sync of data for stocks from SharedPreferences list
     *
     * @param context
     */
    private static void schedulePeriodic(Context context) {
        Timber.d("Scheduling a periodic task");

        JobInfo.Builder builder = new JobInfo.Builder(PERIODIC_ID, new ComponentName(context, QuoteJobService.class));

        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(PERIOD)
                .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);

        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        scheduler.schedule(builder.build());
    }

    public static synchronized void initialize(final Context context) {

        schedulePeriodic(context);
        syncImmediately(context);

    }

    /**
     * Immediately sync data of SharedPreferences stock list, on start-up or other refreshes
     *
     * @param context
     */
    public static synchronized void syncImmediately(Context context) {

//        Check for network connectivity and start service, or schedule periodic sync
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            Intent nowIntent = new Intent(context, QuoteIntentService.class);
            context.startService(nowIntent);
        } else {
            JobInfo.Builder builder = new JobInfo.Builder(ONE_OFF_ID, new ComponentName(context, QuoteJobService.class));

            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);

            JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            scheduler.schedule(builder.build());
        }
    }
}
