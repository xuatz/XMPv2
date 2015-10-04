package com.xuatzsolutions.xuatzmediaplayer2.Models;/*
 * Copyright 2014 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;

import hirondelle.date4j.DateTime;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmMigration;
import io.realm.internal.ColumnType;
import io.realm.internal.Table;

/***************************** NOTE: *********************************************
 * The API for migration is currently using internal lower level classes that will
 * be replaced by a new API very soon! Until then you will have to explore and use
 * below example as inspiration.
 *********************************************************************************
 */


public class Migration implements RealmMigration {

    static int currentVersion = 1;

    public static RealmConfiguration getConfig(Context context) {
        return new RealmConfiguration.Builder(context)
                .schemaVersion(currentVersion)
                .migration(new Migration())
                .build();
    }

    @Override
    public long execute(Realm realm, long version) {
        // Migrate from version 0 to version 1
        if (version == 0) {
            Table trackTable = realm.getTable(Track.class);

            long completedCountIndex = trackTable.addColumn(ColumnType.INTEGER, "completedCount");
            long skippedCountIndex = trackTable.addColumn(ColumnType.INTEGER, "skippedCount");
            long selectedCountIndex = trackTable.addColumn(ColumnType.INTEGER, "selectedCount");
            long likedCountIndex = trackTable.addColumn(ColumnType.INTEGER, "likedCount");
            long dislikedCountIndex = trackTable.addColumn(ColumnType.INTEGER, "dislikedCount");

            long statsUpdatedAtIndex = trackTable.addColumn(ColumnType.STRING, "statsUpdatedAt");

            for (int i = 0; i < trackTable.size(); i++) {
                trackTable.setLong(completedCountIndex, i, 0);
                trackTable.setLong(skippedCountIndex, i, 0);
                trackTable.setLong(selectedCountIndex, i, 0);
                trackTable.setLong(likedCountIndex, i, 0);
                trackTable.setLong(dislikedCountIndex, i, 0);

                DateTime random = new DateTime(2000, 12, 22, null, null, null, null);
                trackTable.setString(statsUpdatedAtIndex, i, random.toString());
            }

            version++;
        }
        return version;
    }

    private long getIndexForProperty(Table table, String name) {
        for (int i = 0; i < table.getColumnCount(); i++) {
            if (table.getColumnName(i).equals(name)) {
                return i;
            }
        }
        return -1;
    }
}