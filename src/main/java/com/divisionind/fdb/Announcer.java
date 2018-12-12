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
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Announcer implements Runnable {

    private static final String UNSUBSCRIBE_END_TAG = String.format("*To stop receiving messages like this, reply %sunsub*", FapBot.PREFIX);

    private List<AnnounceTask> tasks;

    public Announcer() {
        this.tasks = new ArrayList<>();
    }

    @Override
    public void run() {
        for (AnnounceTask t : tasks) t.future.cancel(false);
        this.tasks.clear();
        try {
            Connection con = FapBot.newConnection();
            PreparedStatement ps = con.prepareStatement("SELECT time, reminders FROM group_faps");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Date date = new Date(rs.getTimestamp("time").getTime());
                String rem = rs.getString("reminders");
                rem = rem.toUpperCase();
                String[] reminders;
                if (rem.contains(",")) reminders = rem.split(","); else reminders = new String[] {rem};
                Random randy = new Random();
                for (String reminder : reminders) {
                    char[] rca = reminder.toCharArray();
                    if (rca.length < 2) continue;
                    char unit = rca[rca.length-1];
                    long amount = Long.parseLong(reminder.substring(0, reminder.length() - 1));
                    try {
                        switch (unit) {
                            case 'D': // day
                                amount = TimeUnit.DAYS.toMillis(amount);
                                break;
                            case 'M': // minute
                                amount = TimeUnit.MINUTES.toMillis(amount);
                                break;
                            case 'H': // hour
                                amount = TimeUnit.HOURS.toMillis(amount);
                                break;
                            default:
                                throw new IllegalStateException("Unit of time not specified in reminder.");
                        }
                    } catch (IllegalStateException e) {
                        FapBot.log.warning(e.getLocalizedMessage());
                        continue;
                    }
                    AnnounceTask t = new AnnounceTask(randy, date, amount);
                    if (t.getTimeTill() > 0) tasks.add(t.schedule());
                }
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException e) {
            FapBot.log.severe("Could not refresh group fap alarms.");
            e.printStackTrace();
        }
        AnnounceTask soonest = null;
        for (AnnounceTask t : tasks) {
            if (soonest == null) {
                if (System.currentTimeMillis() < t.triggerTime) soonest = t;
            } else
            if (soonest.triggerTime > t.triggerTime && System.currentTimeMillis() < soonest.triggerTime) soonest = t;
        }
        String time = soonest == null ? "never." : String.format("in %s minutes.", NumberFormat.getNumberInstance().format(TimeUnit.MILLISECONDS.toMinutes(soonest.getTimeTill())));
        FapBot.log.info(String.format("Updated announcement events. Next announcement is %s", time));
    }

    private class AnnounceTask implements Runnable {

        private final SimpleDateFormat DATE_FORMAT_FAR = new SimpleDateFormat("MM/dd/yyyy hh:mmaaa z");
        private final SimpleDateFormat DATE_FORMAT_SAME_WEEK = new SimpleDateFormat("EEEE hh:mmaaa z");
        private final SimpleDateFormat DATE_FORMAT_SAME_DAY = new SimpleDateFormat("hh:mmaaa z");

        private Random randy;
        private Date event;
        private long triggerTime;
        private ScheduledFuture future;

        private AnnounceTask(Random randy, Date event, long amount) {
            this.randy = randy;
            this.event = event;
            this.triggerTime = event.getTime() - amount;
        }

        @Override
        public void run() {
            ReminderMessage rm = ReminderMessage.values()[randy.nextInt(ReminderMessage.values().length)];
            long timeTill = event.getTime() - System.currentTimeMillis();
            SimpleDateFormat dateFormat;
            if (timeTill > TimeUnit.DAYS.toMillis(6L)) dateFormat = DATE_FORMAT_FAR; else
            if (timeTill > TimeUnit.HOURS.toMillis(12L)) dateFormat = DATE_FORMAT_SAME_WEEK; else dateFormat = DATE_FORMAT_SAME_DAY;

            String msg;
            dateFormat.setTimeZone(TimeZone.getTimeZone("CST6CDT"));
            if (dateFormat.equals(DATE_FORMAT_SAME_DAY)) {
                msg = rm.getText("today at " + dateFormat.format(event));
            } else msg = rm.getText(dateFormat.format(event));

            try {
                massPrivateMessage(msg, FapBot.getJDA().getGuilds());
            } catch (SQLException e) {
                FapBot.log.warning("Error sending out mass private message. " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }

        private AnnounceTask schedule() {
            this.future = FapBot.getScheduler().delay(this, getTimeTill());
            return this;
        }

        private long getTimeTill() { // time till event in millis
            return this.triggerTime - System.currentTimeMillis();
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
                if (!rs.next()) user.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage(msg).queue());
                rs.close();
            }
        }

        ps.close();
        con.close();
    }
}
