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
package com.wiley.wol.client.android.data.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.openOutputStream;
import static org.apache.commons.io.IOUtils.closeQuietly;

public final class ZipUtils {

    private ZipUtils() {
    }

    public static void unzip(final String zipFilePathName, final String destinationFolderPathName) throws IOException {
        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFilePathName)));
            final byte[] buffer = new byte[1024];
            int readCount;

            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                final String entryFileName = zipEntry.getName();

                final File entryFile = new File(destinationFolderPathName, entryFileName);
                if (zipEntry.isDirectory()) {
                    forceMkdir(entryFile);
                    continue;
                }
                final FileOutputStream fos = openOutputStream(entryFile);
                while ((readCount = zis.read(buffer)) != -1) {
                    fos.write(buffer, 0, readCount);
                }
                fos.close();
                zis.closeEntry();
            }
        } finally {
            closeQuietly(zis);
        }
    }
}
