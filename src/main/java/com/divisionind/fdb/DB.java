/*
 * DIVISION INDUSTRIES CONFIDENTIAL
 * __________________________________
 *
 *  2015-2019 Division Industries LLC
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of Division Industries LLC
 * and its suppliers, if any. The intellectual and technical concepts contained herein are proprietary
 * to Division Industries LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents
 * in process, and are protected by trade secret or copyright law. Dissemination of this information
 * or reproduction of this material is strictly forbidden unless prior written permission is obtained
 * from Division Industries LLC.
 */

package com.divisionind.fdb;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DB {

    /*
        Database structure must be pre-defined to run FapBot!

        DB:
            Tables:
            - group_faps
            - leveldata
            - unsubscribed

            Table: group_faps
            - time (DATETIME), attending (SMALLINT), reminders (TEXT)

            Table: leveldata
            - discord_id (BIGINT), server_level (SMALLINT), gamer_level (SMALLINT), prestige (SMALLINT), server_xp (BIGINT), game_xp (BIGINT)

            Table: unsubscribed
            - discord_id (BIGINT), discord_name (TINYTEXT), unsub_date (DATETIME)
     */

    private static DB instance;

    private final HikariDataSource ds;

    DB(String url, String user, String pass, String driver) {
        ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(user);
        ds.setPassword(pass);
        ds.setMaximumPoolSize(4);
        ds.setDriverClassName(driver);
    }

    static void __init__(DB db) {
        instance = db;
    }

    public static Connection getConnection() throws SQLException {
        return instance.ds.getConnection();
    }
}
