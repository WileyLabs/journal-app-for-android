/*  Journal App for Android
 *  Copyright (C) 2019 John Wiley & Sons, Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.wiley.wol.client.android.data.service;

import android.content.Context;
import android.util.Base64;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.wiley.wol.client.android.data.http.DownloadManager;
import com.wiley.wol.client.android.data.manager.FeedsInfo;
import com.wiley.wol.client.android.error.AppErrorCode;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.ParamsBuilder;
import com.wiley.wol.client.android.settings.Settings;
import com.wiley.wol.client.android.utils.NetUtils;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;
import org.solovyev.android.checkout.ActivityCheckout;
import org.solovyev.android.checkout.BillingRequests;
import org.solovyev.android.checkout.Checkout;
import org.solovyev.android.checkout.Inventory;
import org.solovyev.android.checkout.Purchase;
import org.solovyev.android.checkout.RequestListener;
import org.solovyev.android.checkout.Sku;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import static com.wiley.wol.client.android.data.http.DownloadManager.OK_CODE;
import static org.solovyev.android.checkout.ProductTypes.SUBSCRIPTION;

/**
 * Created by admin on 03/03/15.
 */
public class InAppBillingServiceImpl implements InAppBillingService {
    private static final String TAG = InAppBillingServiceImpl.class.getSimpleName();
    private static final String NO_SUBSCRIPTION = "";
    @Inject
    private Theme mTheme;
    @Inject
    private NotificationCenter mNotificationCenter;
    @Inject
    private Settings mSettings;
    @Inject
    private DownloadManager mDownloadManager;
    @Inject
    private Context context;

    private ActivityCheckout mCheckout;
    private Inventory mInventory;
    private FeedsInfo feedsInfo;

    @Inject
    public InAppBillingServiceImpl(final Provider<FeedsInfo> feedsInfoProvider) {


        feedsInfo = feedsInfoProvider.get();
    }

    @Override
    public void setCheckout(final ActivityCheckout checkout) {
        this.mCheckout = checkout;
        this.mCheckout.createPurchaseFlow(new PurchaseListener());
    }

    @Override
    public void buyJournalSubscription() {
        mInventory = mCheckout.loadInventory();
        mInventory.whenLoaded(new Inventory.Listener() {
            @Override
            public void onLoaded(Inventory.Products products) {

                // load SKUs
                final Inventory.Product product = products.get(SUBSCRIPTION);
                List<Sku> skus = product.getSkus();
                Sku skuFound = null;
                for (Sku sku : skus) {
                    Logger.d(TAG + ".InAppBilling", "loadSKUs() product = " + product.id
                            + "\nsku.title = " + sku.title
                            + "\nsku.description = " + sku.description
                            + "\nsku.id = " + sku.id
                            + "\nsku.price = " + sku.price
                            + "\nsku.product = " + sku.product);
                    if (mTheme.getNameOfPaidSubscription().equals(sku.id)) {
                        skuFound = sku;
                        break;
                    }
                }

                // purchase
                if (null != skuFound) {
                    final Sku skuPurchase = skuFound;
                    Logger.d(TAG + ".InAppBilling", "purchases() available");
                    mCheckout.whenReady(new Checkout.ListenerAdapter() {
                        @Override
                        public void onReady(@Nonnull BillingRequests requests) {
                            requests.purchase(skuPurchase, null, mCheckout.getPurchaseFlow());
                        }
                    });
                } else {
                    final ParamsBuilder paramsBuilder = new ParamsBuilder();
                    paramsBuilder.withError(-1);
                    mNotificationCenter.sendNotification(EventList.IN_APP_BILLING_PURCHASE_SUBS_ERROR.getEventName(), paramsBuilder.get());
                }
            }
        });
    }

    @Override
    public void loadPurchases() {


        mInventory = mCheckout.loadInventory();
        mInventory.whenLoaded(new Inventory.Listener() {
            @Override
            public void onLoaded(Inventory.Products products) {
                final Inventory.Product product = products.get(SUBSCRIPTION);
                List<Purchase> purchases = product.getPurchases();
                Logger.d(TAG + ".InAppBilling", "loadPurchases() completed. purchases.size() = " + purchases.size());
                Purchase purchaseFound = null;
                for (Purchase purchase : purchases) {
                    Logger.d(TAG + ".InAppBilling", "loadPurchases() product = " + product.id + "\n purchase = " + purchase.toJson(true));
                    if (mTheme.getNameOfPaidSubscription().equals(purchase.sku)) {
                        purchaseFound = purchase;
                        break;
                    }
                }

                final ParamsBuilder paramsBuilder = new ParamsBuilder();
                if (null != purchaseFound) {
                    mSettings.setSubscriptionReceipt(purchaseFound.toJson(true));
                    paramsBuilder
                            .succeed(true);
                } else {
                    paramsBuilder
                            .succeed(false)
                            .withAppErrorCode(AppErrorCode.NO_PREVIOUS_SUBSCRIPTION);
                }
                mNotificationCenter.sendNotification(EventList.IN_APP_BILLING_LOAD_PURCHASES_COMPLETED.getEventName(), paramsBuilder.get());
            }
        });
    }

    @Override
    public void checkMcsSubscription() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                HttpResponse httpResponse = getHttpResponseForIdentityOnGooglePlay(feedsInfo.getMcsIdentityFeed());
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                Logger.d(TAG + ".InAppBilling", "statusCode: " + statusCode);

                final ParamsBuilder paramsBuilder = new ParamsBuilder();
                if (OK_CODE == statusCode) {
                    try {
                        String content = IOUtils.toString(httpResponse.getEntity().getContent());
                        Logger.d(TAG + ".InAppBilling", "content = " + content);

                        if (content.contains("test name=\"signature\" result=\"true\"")
                                && content.contains("test name=\"google-play\" result=\"true\"")) {
                            mSettings.setNeedShowGetAccessScreenOnStart(false);
                            paramsBuilder.succeed(true);
                        } else {

                            mSettings.setNeedShowGetAccessScreenOnStart(true);
                            mSettings.setSubscriptionReceipt(NO_SUBSCRIPTION);
                            paramsBuilder
                                    .succeed(false)
                                    .withAppErrorCode(AppErrorCode.CONTENT_LOCKED_FOR_ANDROID);
                        }

                    } catch (IOException e) {
                        Logger.d(TAG + ".InAppBilling", "IOException: " + e.getMessage());
                        paramsBuilder
                                .succeed(false)
                                .withAppErrorCode(AppErrorCode.IO_EXCEPTION);
                    }

                } else {
                    AppErrorCode errorCode = NetUtils.isOnline(context) ? AppErrorCode.SERVER_ERROR : AppErrorCode.NO_CONNECTION_AVAILABLE;

                    String reasonPhrase = httpResponse.getStatusLine().getReasonPhrase();
                    paramsBuilder
                            .succeed(false)
                            .withAppErrorCode(errorCode)
                            .withErrorMessage("" + statusCode + " " + (reasonPhrase != null ? reasonPhrase : ""));
                }
                mNotificationCenter.sendNotification(EventList.IN_APP_BILLING_CHECK_MCS_PURCHASES_COMPLETED.getEventName(), paramsBuilder.get());

            }
        }).start();
    }

    private HttpResponse getHttpResponseForIdentityOnGooglePlay(String url) {
        final ArrayList<Header> headers = new ArrayList<>();
        final String receiptString = mSettings.getSubscriptionReceipt();
        try {
            final JSONObject json = new JSONObject(receiptString);
            final String productId = json.getString("productId");
            final String packageName = json.optString("packageName");
            final int purchaseState = json.optInt("purchaseState", 0);
            final String token = json.optString("token", json.optString("purchaseToken"));

            final String identity = String.format("%s,%s,%s", packageName, productId, token);
            Logger.d(TAG + ".InAppBilling", "getHttpResponseForIdentityOnGooglePlay(): url = " + url
                    + "; purchaseState = " + purchaseState
                    + "; identity = identity");

            final String identityBase64 = new String(Base64.encode(String.format("%s", identity).getBytes(), Base64.NO_WRAP));
            headers.add(mDownloadManager.createAuthHeaderWithIdentityAndType(url, identityBase64, "google-play"));

        } catch (JSONException e) {
            Logger.s(TAG, e);
        }

        HttpResponse response = null;
        try {
            response = mDownloadManager.connectTo(url, headers);
        } catch (IOException e) {
            Logger.s(TAG, e);
        }
        return response;
    }

    private class PurchaseListener implements RequestListener<Purchase> {
        @Override
        public void onSuccess(@Nonnull Purchase purchase) {
            Logger.d(TAG + ".InAppBilling", "PurchaseListener.onSuccess()");
            if (null != purchase) {
                mSettings.setSubscriptionReceipt(purchase.toJson(true));
            }
            mNotificationCenter.sendNotification(EventList.IN_APP_BILLING_PURCHASE_SUBS_SUCCESS.getEventName());
        }

        @Override
        public void onError(int response, @Nonnull Exception e) {
            Logger.d(TAG + ".InAppBilling", "PurchaseListener.onError():  response = " + response);
            // it is possible that our data is not synchronized with data on Google Play => need to handle some errors
            final ParamsBuilder paramsBuilder = new ParamsBuilder();
            paramsBuilder.withError(new Integer(response));
            mNotificationCenter.sendNotification(EventList.IN_APP_BILLING_PURCHASE_SUBS_ERROR.getEventName(), paramsBuilder.get());
        }
    }
}
