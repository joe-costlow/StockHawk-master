package com.udacity.stockhawk.widget;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Binder;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Created by Joseph Costlow on 25-Apr-17.
 */

public class StockListWidgetRemoteViewService extends RemoteViewsService {

    private static final String LOG_TAG = StockListWidgetRemoteViewService.class.getSimpleName();

    private static final String[] PROJECTION = {
            Contract.Quote._ID,
            Contract.Quote.COLUMN_SYMBOL,
            Contract.Quote.COLUMN_PRICE,
            Contract.Quote.COLUMN_ABSOLUTE_CHANGE};

    private static final int INDEX_ID = 0;
    private static final int INDEX_SYMBOL = 1;
    private static final int INDEX_PRICE = 2;
    private static final int INDEX_ABS_CHANGE = 3 ;

    private final DecimalFormat dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
    private final DecimalFormat dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {

        return new RemoteViewsFactory() {

            private Cursor cursor = null;

            @Override
            public void onCreate() {

            }

            @Override
            public void onDataSetChanged() {

                if (cursor != null) {
                    cursor.close();
                }

                final long IdToken = Binder.clearCallingIdentity();

                Uri stocksTableUri = Contract.Quote.URI;

                cursor = getContentResolver().query(
                        stocksTableUri,
                        PROJECTION,
                        null,
                        null,
                        Contract.Quote.COLUMN_SYMBOL + " ASC");

                Binder.restoreCallingIdentity(IdToken);
            }

            @Override
            public void onDestroy() {

                if (cursor != null) {
                    cursor.close();
                    cursor = null;
                }
            }

            @Override
            public int getCount() {
                return cursor == null ? 0 : cursor.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {

                if (position == AdapterView.INVALID_POSITION ||
                        cursor == null || !cursor.moveToPosition(position)) {
                    return null;
                }

                RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.widget_stock_list_item);

                String symbol = cursor.getString(INDEX_SYMBOL);
                float rawPrice = cursor.getFloat(INDEX_PRICE);
                float rawAbsChange = cursor.getFloat(INDEX_ABS_CHANGE);

                dollarFormatWithPlus.setPositivePrefix("+$");

                String price = String.valueOf(rawPrice);
                String absChange = String.valueOf(rawAbsChange);

                remoteViews.setTextViewText(R.id.widget_symbol, symbol);
                remoteViews.setTextViewText(R.id.widget_price, getString(R.string.widget_stock_dollar_sign) + price);
                remoteViews.setTextViewText(R.id.widget_change, absChange);

                if (Float.valueOf(absChange) > 0) {
                    remoteViews.setTextColor(R.id.widget_change, Color.GREEN);
                } else {
                    remoteViews.setTextColor(R.id.widget_change, Color.RED);
                }

                remoteViews.setTextColor(R.id.widget_symbol, Color.LTGRAY);
                remoteViews.setTextColor(R.id.widget_price, Color.LTGRAY);


                final Intent fillInIntent = new Intent();
                Uri stockUri = Contract.Quote.makeUriForStock(symbol);
                fillInIntent.setData(stockUri);

                remoteViews.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);
                return remoteViews;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_stock_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {

                if (cursor.moveToPosition(position)) {
                    return cursor.getLong(INDEX_ID);
                }

                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };

//        return null;
    }
}
