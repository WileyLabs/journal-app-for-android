package org.solovyev.android.checkout;

import android.app.Activity;
import android.app.Service;
import android.content.Context;

import java.util.concurrent.Executor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface Billing {
    @Nonnull
    Context getContext();

    @Nonnull
    Configuration getConfiguration();

    void connect();

    void disconnect();

    void cancel(int requestId);

    void cancelAll();

    @Nonnull
    BillingImpl.RequestsBuilder newRequestsBuilder();

    @Nonnull
    BillingRequests getRequests(@Nonnull Context context);

    @Nonnull
    BillingRequests getRequests(@Nonnull Activity activity);

    @Nonnull
    BillingRequests getRequests(@Nonnull Service service);

    @Nonnull
    BillingRequests getRequests();

    @Nonnull
    PurchaseFlow createPurchaseFlow(@Nonnull Activity activity, int requestCode, @Nonnull RequestListener<Purchase> listener);

    void onCheckoutStarted();

    void onCheckoutStopped();

    /**
     * Service connection state
     */
    public enum State {
        /**
         * Service is not connected, no requests can be done, initial state
         */
        INITIAL,
        /**
         * Service is connecting
         */
        CONNECTING,
        /**
         * Service is connected, requests can be executed
         */
        CONNECTED,
        /**
         * Service is disconnecting
         */
        DISCONNECTING,
        /**
         * Service is disconnected
         */
        DISCONNECTED,
        /**
         * Service failed to connect
         */
        FAILED,
    }

    public static interface ServiceConnector {
        boolean connect();

        void disconnect();
    }

    public static interface Configuration {
        /**
         * @return application's public key, encoded in base64.
         * This is used for verification of purchase signatures. You can find app's base64-encoded
         * public key in application's page on Google Play Developer Console. Note that this
         * is NOT "developer public key".
         */
        @Nonnull
        String getPublicKey();

        /**
         * @return cache instance to be used for caching, null for no caching
         * @see org.solovyev.android.checkout.BillingImpl#newCache()
         */
        @Nullable
        Cache getCache();

        /**
         * @return {@link org.solovyev.android.checkout.PurchaseVerifier} to be used to validate the purchases
         * @see PurchaseVerifier
         */
        @Nonnull
        PurchaseVerifier getPurchaseVerifier();

        /**
         * @return inventory to be used if Billing v.3 is not supported
         * @param checkout checkout
         * @param onLoadExecutor executor to be used to call {@link org.solovyev.android.checkout.Inventory.Listener} methods
         */
        @Nullable
        Inventory getFallbackInventory(@Nonnull Checkout checkout, @Nonnull Executor onLoadExecutor);

        /**
         * Return true if you want Billing to connect to/disconnect from Billing API Service
         * automatically. If this method returns true then there is not need in calling {@link org.solovyev.android.checkout.Billing#connect()}
         * or {@link org.solovyev.android.checkout.Billing#disconnect()} manually.
         *
         * @return true if Billing should connect to/disconnect from Billing API service automatically
         * according to the number of started Checkouts
         */
        boolean isAutoConnect();
    }

    /**
     * Dummy listener, used if user didn't provide {@link org.solovyev.android.checkout.RequestListener}
     *
     * @param <R> type of result
     */
    public static class EmptyListener<R> implements RequestListener<R> {
        @Override
        public void onSuccess(@Nonnull R result) {
        }

        @Override
        public void onError(int response, @Nonnull Exception e) {
        }
    }

    /**
     * Gets public key only once, all other methods are called from original configuration
     */
    public static final class StaticConfiguration implements Configuration {
        @Nonnull
        private final Configuration original;

        @Nonnull
        private final String publicKey;

        public StaticConfiguration() {
            original = null;
            publicKey = "";
        }

        protected StaticConfiguration(@Nonnull Configuration original) {
            this.original = original;
            this.publicKey = original.getPublicKey();
        }

        @Nonnull
        @Override
        public String getPublicKey() {
            return publicKey;
        }

        @Nullable
        @Override
        public Cache getCache() {
            return original.getCache();
        }

        @Nonnull
        @Override
        public PurchaseVerifier getPurchaseVerifier() {
            return original.getPurchaseVerifier();
        }

        @Nullable
        @Override
        public Inventory getFallbackInventory(@Nonnull Checkout checkout, @Nonnull Executor onLoadExecutor) {
            return original.getFallbackInventory(checkout, onLoadExecutor);
        }

        @Override
        public boolean isAutoConnect() {
            return original.isAutoConnect();
        }
    }
}
