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
package com.wiley.wol.client.android.data.dao;

import com.wiley.wol.client.android.domain.entity.TPSSiteMO;

import java.util.List;

/**
 * Created by alobachev on 7/9/14.
 */
public interface TPSSiteDao {

    List<TPSSiteMO> findAll();

    void save(List<TPSSiteMO> tpsSites);

}
