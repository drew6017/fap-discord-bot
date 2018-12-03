/*
 * DIVISION INDUSTRIES CONFIDENTIAL
 * __________________________________
 *
 *  2015-2018 Division Industries LLC
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

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;

import javax.security.auth.login.LoginException;
import java.util.logging.Logger;

public class FapBot {

    public static final Logger log = Logger.getLogger("FapBot");
    public static final String PREFIX = "$";

    private static final String TOKEN = System.getenv("DISCORD_BOT_TOKEN");

    private static JDA jda;

    public static void main(String[] args) {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tT] [%3$s/%4$s] %5$s %n");
        log.info("Initializing...");

        // start JDA
        try {
            jda = new JDABuilder()
                    .setToken(TOKEN)
                    .addEventListener(new BotEvent())
                    .setAutoReconnect(true)
                    .build();
        } catch (LoginException e) {
            log.severe("An error occurred when attempting to login");
            jda = null;
            e.printStackTrace();
        }

        log.info("Created shutdown hook");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (jda != null) jda.shutdownNow();
        }));
    }
}
