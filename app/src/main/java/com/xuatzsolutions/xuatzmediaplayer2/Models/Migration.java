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
import android.util.Log;

import java.util.HashMap;

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

    private static final String TAG = "Migration";
    static int currentVersion = 0;

    public static RealmConfiguration getConfig(Context context) {
        return new RealmConfiguration.Builder(context)
                .schemaVersion(currentVersion)
                .migration(new Migration())
                .build();
    }

    @Override
    public long execute(Realm realm, long version) {
        Log.d(TAG, "execute() start");

        if (version == 0) {
            version++;
        }

        if (version == 1) {
            Log.d(TAG, "Upgrading to Schema v2");

            version++;
        }

        if (version == 2) {
            Log.d(TAG, "Upgrading to Schema v3");

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