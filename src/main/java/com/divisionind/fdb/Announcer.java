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

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Announcer implements Runnable {

    private static final String UNSUBSCRIBE_END_TAG = String.format("*To stop receiving messages like this, reply %sunsub*", FapBot.PREFIX);

    private Timer alarm;
    private List<TimerTask> tasks;

    public Announcer() {
        this.alarm = new Timer("Group Fap Alarm", false);
        this.tasks = new ArrayList<>();
    }

    @Override
    public void run() {
        for (TimerTask t : tasks) t.cancel();
        this.alarm.purge();
        this.tasks.clear();
        try {
            Connection con = FapBot.newConnection();
            PreparedStatement ps = con.prepareStatement("SELECT time, reminders FROM group_faps");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Date date = new Date(rs.getTimestamp("time").getTime());
                String[] reminders = rs.getString("reminders").split(",");
                Random randy = new Random();
                for (String reminder : reminders) {
                    TimerTask t = new AnnounceTask(randy, date);
                    char[] rca = reminder.toCharArray();
                    char unit = rca[rca.length-1];
                    long amount = Long.parseLong(reminder.substring(0, reminder.length() - 1));
                    switch (unit) {
                        case 'D': // day
                            amount = TimeUnit.DAYS.toMillis(amount);
                            break;
                        case 'M': // minute
                            amount = TimeUnit.MINUTES.toMillis(amount);
                            break;
                        case 'H': // hour
                            amount = TimeUnit.HOURS.toMillis(amount);
                    }
                    tasks.add(t);
                    this.alarm.schedule(t, date.getTime() - amount);
                }
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException e) {
            FapBot.log.severe("Could not refresh group fap alarms.");
            e.printStackTrace();
        }
        FapBot.log.info("Updated announcement events.");
    }

    private class AnnounceTask extends TimerTask {

        private final SimpleDateFormat DATE_FORMAT_FAR = new SimpleDateFormat("MM/dd/yyyy hh:mmaaa z");
        private final SimpleDateFormat DATE_FORMAT_SAME_WEEK = new SimpleDateFormat("EEEE hh:mmaaa z");
        private final SimpleDateFormat DATE_FORMAT_SAME_DAY = new SimpleDateFormat("hh:mmaaa z");

        private Random randy;
        private Date event;

        private AnnounceTask(Random randy, Date event) {
            this.randy = randy;
            this.event = event;
        }

        @Override
        public void run() {
            ReminderMessage rm = ReminderMessage.values()[randy.nextInt(ReminderMessage.values().length)];
            long timeTill = event.getTime() - System.currentTimeMillis();
            SimpleDateFormat dateFormat;
            if (timeTill > TimeUnit.DAYS.toMillis(6L)) dateFormat = DATE_FORMAT_FAR; else
            if (timeTill > TimeUnit.HOURS.toMillis(12L)) dateFormat = DATE_FORMAT_SAME_WEEK; else dateFormat = DATE_FORMAT_SAME_DAY;

            String msg;
            if (dateFormat.equals(DATE_FORMAT_SAME_DAY)) {
                msg = rm.getText("today at " + dateFormat.format(event));
            } else msg = rm.getText(dateFormat.format(event));

            // TODO announce message, this is to test for stability before mass sending messages
            // send to massPrivateMessage and the group-faps channel in F.A.P.
            FapBot.log.info(msg);
        }
    }

    public enum ReminderMessage {

        MESSAGE_1("Just a quick reminder that the group fap is %s. %s"),
        MESSAGE_2("Did you forget? Make sure you show up to the Fun and Partying's discord server meet up %s. %s"),
        MESSAGE_3("Make sure your ready for the next group fap %s. It's a great chance to meet people and make friends. %s");

        private String text;

        ReminderMessage(String text) {
            this.text = text;
        }

        public String getText(String date) {
            return String.format(text, date, UNSUBSCRIBE_END_TAG);
        }

        public String getText() {
            return text;
        }
    }

    public static void massPrivateMessage(String msg, List<Guild> guilds) throws SQLException {
        Connection con = FapBot.newConnection();
        PreparedStatement ps = con.prepareStatement("SELECT discord_id FROM unsubscribed WHERE discord_id=?");
        if (guilds == null) guilds = FapBot.getJDA().getGuilds();
        for (Guild g : guilds) {
            for (Member member : g.getMembers()) {
                User user = member.getUser(); // possibly cache this to a blacklist ArrayList to reduce database calls. This was not done to reduce ram consumption if the list grows large
                ps.setLong(1, user.getIdLong());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) user.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage(msg).queue());
                rs.close();
            }
        }

        ps.close();
        con.close();
    }
}
