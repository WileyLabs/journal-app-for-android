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
package com.wiley.wol.client.android.domain.entity;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Map;

/**
 * Created by alobachev on 7/9/14.
 */

@Root(name = "tpsSite")
@DatabaseTable(tableName = "tpssite")
public class TPSSiteMO {

    @Path("form")
    @ElementMap(entry="script", key="type", attribute=true, inline=true, data=true)
    private Map<String, String> scripts;

    @DatabaseField(generatedId = true, canBeNull = false, columnName = "uid")
    private Integer uid;

    @DatabaseField(dataType = DataType.DATE, columnName = "importing_date")
    private Date importingDate;

    @Attribute(name="url")
    @DatabaseField(columnName = "tps_site_url")
    private String tpsSiteUrl;

    @Attribute(name="name")
    @DatabaseField(columnName = "tps_name")
    private String tpsName;

    @Attribute(name="shortName")
    @DatabaseField(columnName = "tps_short_name")
    private String tpsShortName;

    @Attribute(name="help")
    @DatabaseField(columnName = "help")
    private String help;

    @Element(name="instructions",data=true)
    @DatabaseField(columnName = "instructions")
    private String instructions;

    @Path("form")
    @Attribute(name="action")
    @DatabaseField(columnName = "form_url")
    private String formUrl;

    @Path("form")
    @Attribute(name="usernameLabel")
    @DatabaseField(columnName = "username_label")
    private String usernameLabel;

    @Path("form")
    @Attribute(name = "passwordLabel")
    @DatabaseField(columnName = "password_label")
    private String passwordLabel;

    @DatabaseField(columnName = "hash_string")
    private String hashString;

    @DatabaseField(columnName = "login_script")
    private String loginScript;


    @DatabaseField(columnName = "response_script")
    private String responseScript;

    @DatabaseField(columnName = "sort_index")
    private int sortIndex;

    public TPSSiteMO() {}

    public TPSSiteMO(
            @Attribute(name="url")final String tpsSiteUrl,
            @Attribute(name="name")final String tpsName,
            @Path("form")@ElementMap(entry="script", key="type", attribute=true, inline=true, data=true) final Map<String, String> scripts) {

        if (null == tpsSiteUrl || null == tpsName) {
            this.tpsSiteUrl = "";
            this.tpsName = "";
            this.hashString = "";
        }
        else {
            this.tpsSiteUrl = tpsSiteUrl;
            this.tpsName = tpsName;

            // calculate hashString
            String result = null;
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                byte[] hash = md5.digest((tpsSiteUrl+tpsName).getBytes());

                StringBuilder sb = new StringBuilder(2*hash.length);
                for (byte b : hash) {
                    sb.append(String.format("%02x", b&0xff));
                }
                result = sb.toString().toUpperCase();
            }
            catch (NoSuchAlgorithmException ignored) {
            }

            this.hashString = null == result ? "" : result;
        }

        this.scripts = null;
        this.loginScript = !scripts.containsKey("login") ? "" : scripts.get("login");
        this.responseScript = !scripts.containsKey("response") ? "" : scripts.get("response");
    }

    public Date getImportingDate() {
        return importingDate;
    }
    public void setImportingDate(final Date importingDate) {
        this.importingDate = importingDate;
    }

    public int getSortIndex() {
        return sortIndex;
    }
    public void setSortIndex(final int sortIndex) {
        this.sortIndex = sortIndex;
    }

    public String getTPSSiteUrl() {
        return tpsSiteUrl;
    }

    public String getTPSName() {
        return tpsName;
    }

    public String getTPSShortName() {
        return tpsShortName;
    }

    public String getHelp() {
        return help;
    }

    public String getInstructions() {
        return instructions;
    }

    public String getFormUrl() {
        return formUrl;
    }

    public String getUsernameLabel() {
        return usernameLabel;
    }

    public String getPasswordLabel() {
        return passwordLabel;
    }

    public String getLoginScript() {
        return loginScript;
    }

    public String getResponseScript() {
        return responseScript;
    }

    public String getHashString() {
        return hashString;
    }
}
