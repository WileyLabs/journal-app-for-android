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
package com.wiley.wol.client.android.utils;

import android.text.TextUtils;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by taraskreknin on 08.08.14.
 */
public class EncryptionUtils {

    private static final String TAG = EncryptionUtils.class.getSimpleName();

    public static String encryptKey(final String dataToEncrypt, String recipe) {
        if (TextUtils.isEmpty(dataToEncrypt)) {
            return null;
        }

        byte[] encryptedData = encryptData(dataToEncrypt.getBytes(Charset.forName("UTF-8")), recipe);
        final StringBuilder encryptedKey = new StringBuilder();
        for (byte encryptedDataByte : encryptedData) {
            encryptedKey.append(String.format("%02x", encryptedDataByte));
        }

        return encryptedKey.toString().toUpperCase();
    }

    public static String decryptKey(String encryptedKey, String recipe) {
        if (TextUtils.isEmpty(encryptedKey)) {
            return null;
        }

        byte[] encryptedData = hexStringToByteArray(encryptedKey);
        byte[] keyData = encryptData(encryptedData, recipe);

        return new String(keyData);
    }

    private static byte[] encryptData(byte[] dataToEncrypt, String recipe) {
        byte[] saltData = hexStringToByteArray(recipe);

        byte[] encryptedData = new byte[dataToEncrypt.length];

        for(int i = 0; i<dataToEncrypt.length; ++i) {
            byte b = (byte) (dataToEncrypt[i] ^ saltData[i % saltData.length]);
            encryptedData[i] = b;
        }
        return encryptedData;
    }

    public static byte[] hexStringToByteArray(String s) {
        // TODO check s is odd
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static String md5Hash(final String s) {
        if (TextUtils.isEmpty(s)) {
            return null;
        }

        String result = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] hash = md5.digest((s).getBytes());

            StringBuilder sb = new StringBuilder(2*hash.length);
            for (byte b : hash) {
                sb.append(String.format("%02x", b&0xff));
            }
            result = sb.toString().toUpperCase();
        }
        catch (NoSuchAlgorithmException ignored) {
        }

        return result;
    }

}
