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

import com.divisionind.fdb.scheduler.AtomicScheduler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.Compression;

import javax.security.auth.login.LoginException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class FapBot {

    public static final Logger log = Logger.getLogger("FapBot");
    public static final String PREFIX = "$";
    public static final String DELIMITER = " ";
    public static final String PERMANENT_INVITE_LINK = "https://discord.gg/QBAeD6u";
    public static final long DISCORD_GUILD_ID = 425464794368442371L;
    public static final long DISCORD_GROUP_FAP_TC_ID = 505460272115351552L;
    public static final long DISCORD_MAIN_TC_ID = 425464794368442375L;
    public static final long DISCORD_HELLO_GOODBYE_TC_ID = 558749269478080523L;
    public static final long ROLE_ID_VERIFIED_FAT_COCK = 457718136091705355L;
    public static final long ROLE_ID_MASTER_GAMER = 578487579544911872L;
    public static final long SERVER_OWNERS_DC_ID = 246069907467403264L;
    public static final int SCHEDULER_THREADS = 2;

    private static final String TOKEN = System.getenv("DISCORD_BOT_TOKEN");
    private static final String JDBC_DRIVER = "org.mariadb.jdbc.Driver";

    private static JDA jda;
    private static AtomicScheduler scheduler;
    private static Announcer announcer;
    private static LevelSystem levelSystem;
    //private static ThreadPoolExecutor threadCluster;

    protected static List<ACommand> commands;

    public static void main(String[] args) {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tT] [%3$s/%4$s] %5$s %n");
        log.info("Initializing...");

        scheduler = new AtomicScheduler(SCHEDULER_THREADS);

        // register all default commands
        commands = new ArrayList<>();
        registerCMDS(new Commands.Help(),
                new Commands.Fap(),
                new Commands.GroupFap(),
                new Commands.Unsubscribe(),
                new Commands.Info(),
                new Commands.PrivateMessage(),
                new Commands.When(),
                new Commands.SetPlaying(),
                new Commands.Xp(),
                new Commands.Leaderboard(),
                new Commands.RewardPoints(),
                new Commands.Teamspeak());

        // start JDA
        try {
            jda = JDABuilder.createDefault(TOKEN)
                    .addEventListeners(new BotEvent())
                    .setAutoReconnect(true)
//                    .setActivity(Activity.of(Activity.ActivityType.CUSTOM_STATUS, String.format("type %shelp for help!", PREFIX)))
                    .setCompression(Compression.ZLIB)
                    .setLargeThreshold(250)
                    .enableIntents(Arrays.asList(GatewayIntent.values())) // just enable all intents
                    .build();

            jda.awaitReady(); // wait
        } catch (LoginException | InterruptedException e) {
            log.severe("An error occurred when attempting to login");
            jda = null;
            e.printStackTrace();
        }

        jda.getPresence().setActivity(Activity.of(Activity.ActivityType.CUSTOM_STATUS, String.format("type %shelp", PREFIX)));

        log.info("Creating shutdown hook...");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (jda != null) jda.shutdownNow();
            //if (threadCluster != null) threadCluster.shutdown();
        }));

        // load db driver
        log.info("Loading JDBC (MySQL) driver...");
        try {
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            log.severe("An error occurred whilst loading the database driver. The specified driver could not be found.");
            e.printStackTrace();
        }

        // setup db connection info
        log.info("Setting up Hikari connection pool...");
        try {
            URI maria = new URI(System.getenv("JAWSDB_MARIA_URL"));
            String[] userPass = maria.getUserInfo().split(":");
            String db_user = userPass[0];
            String db_pass = userPass[1];
            String db_url = String.format("jdbc:mysql://%s:%s%s", maria.getHost(), maria.getPort(), maria.getPath());
            DB.__init__(new DB(db_url, db_user, db_pass, JDBC_DRIVER));
        } catch (URISyntaxException e) {
            log.severe("Could not parse JAWSDB_MARIA_URL information. The application will NOT have database access.");
            e.printStackTrace();
        }

        // creating announcement events from db
        announcer = new Announcer();
        scheduler.repeating(announcer, 0L, 30L, TimeUnit.MINUTES);

        // initialize level system
        levelSystem = new LevelSystem();
        scheduler.repeating(levelSystem, 1L, 1L, TimeUnit.MINUTES);

        // create cluster msg in handler
        //threadCluster = (ThreadPoolExecutor)Executors.newCachedThreadPool(DaemonThreadFactory.getThreadFactory());

        log.info("FapBot is now running.");
    }

    public static void registerCMDS(ACommand... cmds) {
        commands.addAll(Arrays.asList(cmds));
    }

    public static AtomicScheduler getScheduler() {
        return scheduler;
    }

//    public static ThreadPoolExecutor getThreadCluster() {
//        return threadCluster;
//    }

    public static Announcer getAnnouncer() {
        return announcer;
    }

    public static LevelSystem getLevelSystem() {
        return levelSystem;
    }

    public static JDA getJDA() {
        return jda;
    }
}
