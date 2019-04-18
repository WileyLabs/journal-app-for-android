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

import android.util.Log;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

/**
 * Created by dfedorov
 * on 03/07/14.
 */
@Root(name = "citation")
@DatabaseTable(tableName = "citation")
public class CitationMO {
    private static final String TAG = CitationMO.class.getSimpleName();

    @DatabaseField(canBeNull = false, columnName = "uid", id = true)
    private String id;
    @DatabaseField(foreign = true, columnName = "reference_uid")
    private ReferenceMO reference;
    @Attribute(name = "id")
    @DatabaseField(columnName = "cit_id")
    private String citId;
    @Element(data = true)
    @DatabaseField(columnName = "text")
    private String text;
    @DatabaseField(dataType = DataType.BYTE_ARRAY)
    private byte[] links;
    @DatabaseField(columnName = "link_to_wol")
    private String linkToWOL;
    @DatabaseField(columnName = "sort_index")
    private int sortIndex;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCitId() {
        return citId;
    }

    public void setCitId(String citId) {
        this.citId = citId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ReferenceMO getReference() {
        return reference;
    }

    public void setReference(ReferenceMO reference) {
        this.reference = reference;
    }

    public byte[] getLinks() {
        return links;
    }

    public void setLinks(byte[] links) {
        this.links = links;
    }

    public void setLinksMap(final Map<String, Object> links) {
        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(byteOut);
            out.writeObject(links);
            this.links = byteOut.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public Map<String, Object> getLinksMap() {
        if (links == null)
            return null;

        try {
            ByteArrayInputStream byteIn = new ByteArrayInputStream(links);
            ObjectInputStream in = new ObjectInputStream(byteIn);
            return (Map<String, Object>) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }


    public String getLinkToWOL() {
        return linkToWOL;
    }

    public void setLinkToWOL(String linkToWOL) {
        this.linkToWOL = linkToWOL;
    }

    public int getSortIndex() {
        return sortIndex;
    }

    public void setSortIndex(int sortIndex) {
        this.sortIndex = sortIndex;
    }
}
