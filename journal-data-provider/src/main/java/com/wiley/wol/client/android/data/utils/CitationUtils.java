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

/**
 * Created by Andrey Rylov on 18/07/14.
 */
public class CitationUtils {

    public enum LinkType {
        ADS,
        CAS,
        PMED,
        XREF,
        ISI
    }

    public static String getLinkString(LinkType linkType) {
        switch (linkType) {
            case ADS:
                return "ADS";
            case CAS:
                return "CAS";
            case PMED:
                return "PMED";
            case XREF:
                return "XREF";
            case ISI:
                return "ISI";
            default:
                return null;
        }
    }

    public static String getLinkTitle(LinkType linkType) {
        switch (linkType) {
            case ADS:
                return "ADS";
            case CAS:
                return "CAS";
            case PMED:
                return "PubMed";
            case XREF:
                return "CrossRef";
            case ISI:
                return "Web of Knowledge";
            default:
                return null;
        }
    }

}
