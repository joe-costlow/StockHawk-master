package com.udacity.stockhawk.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.udacity.stockhawk.R;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class PrefUtils {

    private PrefUtils() {
    }

    /**
     * Retrieves default list of stock symbols on first run and returns this list, using boolean
     * to indicate whether or not app has been ran since install. If already ran, returns a new
     * HashSet
     *
     * @param context
     * @return
     */
    public static Set<String> getStocks(Context context) {
        String stocksKey = context.getString(R.string.pref_stocks_key);
        String initializedKey = context.getString(R.string.pref_stocks_initialized_key);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean initialized = prefs.getBoolean(initializedKey, false);

        if (!initialized) {
            String[] defaultStocksList = context.getResources().getStringArray(R.array.default_stocks);
            HashSet<String> defaultStocks = new HashSet<>(Arrays.asList(defaultStocksList));

            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(initializedKey, true);
            editor.putStringSet(stocksKey, defaultStocks);
            editor.apply();
            return defaultStocks;
        }

        return prefs.getStringSet(stocksKey, new HashSet<String>());
    }

    /**
     * Retrieves the SharedPreferences of stocks for editing
     *
     * @param context
     * @param symbol  symbol retrieved by input dialog or swiped right to delete
     * @param add     boolean to determine whether wether the symbol is being added to, or removed from
     *                SharedPreferences
     */
    private static void editStockPref(Context context, String symbol, Boolean add) {

//        Retrieve saved stocks from SharedPreferences
        String key = context.getString(R.string.pref_stocks_key);
        Set<String> stocks = getStocks(context);

//        Make a copy of the stocks in SharedPreferences
        Set<String> updatedStocks = new HashSet<>();
        updatedStocks.addAll(stocks);

//        Determine whether the symbol, from parameter, is to be added, or removed,
//        from SharedPreferences
        if (add) {
            updatedStocks.add(symbol);
        } else {
            updatedStocks.remove(symbol);
        }

//        Edit copy of SharedPreferences and update SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(key, updatedStocks);
        editor.apply();
    }

    public static void addStock(Context context, String symbol) {
        editStockPref(context, symbol, true);
    }

    public static void removeStock(Context context, String symbol) {
        editStockPref(context, symbol, false);
    }

    /**
     * Retrieves display mode, in currency amount or percentage, of stock price from SharedPreferences
     *
     * @param context
     * @return
     */
    public static String getDisplayMode(Context context) {
        String key = context.getString(R.string.pref_display_mode_key);
        String defaultValue = context.getString(R.string.pref_display_mode_default);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(key, defaultValue);
    }

    /**
     * Retrieves and edits SharedPreference for display mode, currency change or percentage change,
     * of stock. Toggles between currency and percentage change when menu list is clicked
     *
     * @param context
     */
    public static void toggleDisplayMode(Context context) {
        String key = context.getString(R.string.pref_display_mode_key);
        String absoluteKey = context.getString(R.string.pref_display_mode_absolute_key);
        String percentageKey = context.getString(R.string.pref_display_mode_percentage_key);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String displayMode = getDisplayMode(context);

        SharedPreferences.Editor editor = prefs.edit();

        if (displayMode.equals(absoluteKey)) {
            editor.putString(key, percentageKey);
        } else {
            editor.putString(key, absoluteKey);
        }

        editor.apply();
    }
}
