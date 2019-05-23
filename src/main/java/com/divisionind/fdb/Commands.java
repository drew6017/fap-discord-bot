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

import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.managers.Presence;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Commands {
    protected static class Help extends ACommand {
        @Override
        public void execute(MessageReceivedEvent event, String[] args) {
            StringBuilder sb = new StringBuilder();
            sb.append("Commands often have several aliases separated by commas for quick usage. Here is a list of commands:\n");
            boolean isFatCock = hasRole(event.getMember(), event.getAuthor(), "Verified Fat Cock");
            for (ACommand c : FapBot.commands) {
                if (!isFatCock) {
                    if (c.isHidden()) continue;
                }
                String[] aliases = c.aliases();
                sb.append("**");
                for (int i = 0;i<aliases.length;i++) {
                    sb.append(aliases[i]);
                    if ((i + 1) != aliases.length) sb.append(", ");
                }
                sb.append("** - ").append(c.desc()).append("\n");
            }
            respond(event, sb.toString());
        }

        @Override
        public String[] aliases() {
            return new String[] {"help", "h"};
        }

        @Override
        public String desc() {
            return "shows this help message";
        }
    }

    protected static class Fap extends ACommand {
        @Override
        public void execute(MessageReceivedEvent event, String[] args) {
            Random randy = new Random();
            String msg;
            int key = randy.nextInt(3);
            if (key == 0 && event.getAuthor().getIdLong() == 246069907467403264L) key = 3; // prevents drew6017 from getting bad messages
            switch (key) {
                case 0:
                    msg = "Leave me alone...";
                    break;
                case 1:
                    msg = "Fapping is good for the body, proven by science. https://www.latlmes.com/breaking/fapping-is-good-for-you-1";
                    break;
                default:
                    msg = "*fap fap fap fap fap fap fap fap fap*... Oh, sorry, didn't see you there.";
            }
            respond(event, msg);
        }

        @Override
        public String[] aliases() {
            return new String[] {"fap"};
        }

        @Override
        public String desc() {
            return "prints out a funny message";
        }
    }

    protected static class GroupFap extends ACommand {

        private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy hh:mm aaa z");
        private static HashMap<Member, GroupFapTask> buildingFaps = new HashMap<>();

        @Override
        public void execute(MessageReceivedEvent event, String[] args) {
            if (hasRole(event.getMember(), event.getAuthor(), "Verified Fat Cock")) {
                if (args.length > 1) {
                    if (args[1].equalsIgnoreCase("time")) {
                        if (args.length == 2) {
                            respond(event, "Correct time format is MM/DD/YYYY HH:MM(PM/AM) or e.g. 01/09/2019 12:00 PM CST");
                        } else {
                            GroupFapTask task = getOrCreate(event.getMember());
                            StringBuilder sb = new StringBuilder();
                            for (int i = 2;i<args.length;i++) sb.append(args[i]).append(" ");
                            try {
                                task.datetime = DATE_FORMAT.parse(sb.toString());
                                respond(event, "Time set.");
                            } catch (ParseException e) {
                                respond(event, "Correct time format is MM/DD/YYYY HH:MM(PM/AM) or e.g. 01/09/2019 12:00 PM CST");
                            }
                        }
                    } else
                    if (args[1].equalsIgnoreCase("msg")) {
                        GroupFapTask task = getOrCreate(event.getMember());
                        StringBuilder sb = new StringBuilder();
                        for (int i = 2;i<args.length;i++) sb.append(args[i]).append(" ");
                        task.msg = sb.toString();
                        respond(event, "Message set.");
                    } else
                    if (args[1].equalsIgnoreCase("reminders")) { // sets the reminders string e.g. 1D,2D,30M which are relative to prior
                        if (args.length == 3) {
                            GroupFapTask task = getOrCreate(event.getMember());
                            task.reminders = args[2];
                            respond(event, "Reminder times set.");
                        } else respond(event, "Incorrect usage of command.");
                    } else
                    if (args[1].equalsIgnoreCase("publish")) {
                        GroupFapTask task = buildingFaps.get(event.getMember());
                        if (task == null || task.reminders == null || task.msg == null || task.datetime == null) {
                            respond(event, "You must first finish building the group fap in order to publish it.");
                        } else {
                            try {
                                Connection con = DB.getConnection();
                                PreparedStatement ps = con.prepareStatement("INSERT INTO group_faps VALUES (?,?,?)");
                                ps.setTimestamp(1, new java.sql.Timestamp(task.datetime.getTime()));
                                ps.setShort(2, (short)0);
                                ps.setString(3, task.reminders);
                                ps.executeUpdate();
                                ps.close();
                                con.close();
                            } catch (SQLException e) {
                                respond(event, "Error creating group fap in database.");
                                e.printStackTrace();
                                return;
                            }
                            if (!task.msg.equals("")) FapBot.getJDA().getGuildById(FapBot.DISCORD_GUILD_ID).getTextChannelById(FapBot.DISCORD_GROUP_FAP_TC_ID).sendMessage(task.msg).queue();
                            task.remove(event.getMember());
                            FapBot.getAnnouncer().run(); // updates announcer with newly created event
                            respond(event, "Group fap has been created!");
                        }
                    }
                } else respond(event, String.format("Incorrect usage. Correct usage: %sgf <subcommand>. Valid subcommands are: **time, msg, reminders, publish**", FapBot.PREFIX));
            } else {
                respond(event, "You do not have access to this command. You must at least be a: Verified Fat Cock.");
            }
        }

        private GroupFapTask getOrCreate(Member m) {
            GroupFapTask task = buildingFaps.get(m);
            if (task == null) {
                task = new GroupFapTask();
                buildingFaps.put(m, task);
            }
            task.repopRemoveTask(m);
            return task;
        }

        @Override
        public String[] aliases() {
            return new String[] {"groupfap", "gf"};
        }

        @Override
        public String desc() {
            return "create and manage group faps";
        }

        @Override
        public boolean isHidden() {
            return true;
        }

        private class GroupFapTask {
            private Date datetime;
            private String msg;
            private String reminders;
            private ScheduledFuture removeTask;

            GroupFapTask() {
                this.datetime = null;
                this.msg = null;
                this.reminders = null;
                this.removeTask = null;
            }

            private void repopRemoveTask(Member m) {
                if (removeTask != null && !(removeTask.isCancelled() || removeTask.isDone())) removeTask.cancel(true);
                removeTask = FapBot.getScheduler().delay(() -> buildingFaps.remove(m), 10, TimeUnit.MINUTES);
            }

            private void remove(Member m) {
                buildingFaps.remove(m);
                removeTask.cancel(true);
            }
        }
    }

    protected static class Unsubscribe extends ACommand {
        @Override
        public void execute(MessageReceivedEvent event, String[] args) {
            User author = event.getAuthor();
            String toggle_status;
            try {
                Connection con = DB.getConnection();
                PreparedStatement st = con.prepareStatement("SELECT discord_id FROM unsubscribed WHERE discord_id=?");
                st.setLong(1, author.getIdLong());
                ResultSet rs = st.executeQuery();
                boolean existed = rs.next();
                rs.close();
                st.close();
                if (existed) {
                    st = con.prepareStatement("DELETE FROM unsubscribed WHERE discord_id=?");
                    st.setLong(1, author.getIdLong());
                    st.executeUpdate();
                    st.close();
                    toggle_status = "on";
                } else {
                    PreparedStatement ps = con.prepareStatement("INSERT INTO unsubscribed VALUES(?,?,?)");
                    ps.setLong(1, author.getIdLong());
                    ps.setString(2, author.getName());
                    ps.setTimestamp(3, new java.sql.Timestamp(System.currentTimeMillis()));
                    ps.executeUpdate();
                    ps.close();
                    toggle_status = "off";
                }
                con.close();
            } catch (SQLException e) {
                FapBot.log.warning("Could not access database. Was there an error connecting?");
                e.printStackTrace();
                toggle_status = "error";
            }
            respond(event, String.format("Toggled receive private messages from bot to **%s**", toggle_status));
        }

        @Override
        public String[] aliases() {
            return new String[] {"unsubscribe", "unsub"};
        }

        @Override
        public String desc() {
            return "toggles private messages from this bot";
        }
    }

    protected static class Info extends ACommand {
        @Override
        public void execute(MessageReceivedEvent event, String[] args) {
            respond(event, "A group fap is a meet-and-greet session between members of the Fun and Partying (FAP) discord server.");
        }

        @Override
        public String[] aliases() {
            return new String[] {"info", "i"};
        }

        @Override
        public String desc() {
            return "tells you what the heck a group fap is";
        }
    }

    protected static class PrivateMessage extends ACommand {
        @Override
        public void execute(MessageReceivedEvent event, String[] args) {
            if (event.getAuthor().getIdLong() == FapBot.SERVER_OWNERS_DC_ID) { // drew6017's user id
                if (args.length < 2) {
                    respond(event, "You must specify a message to send to everyone.");
                    return;
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 2;i<args.length;i++) sb.append(args[i]).append(" ");
                try {
                    Announcer.massPrivateMessage(sb.toString(), FapBot.getJDA().getGuilds());
                } catch (SQLException e) {
                    respond(event, String.format("An error occurred whilst sending the mass private message: %s", e.getLocalizedMessage()));
                    e.printStackTrace();
                }
            } else respond(event, "This command is reserved for drew6017 only. Sorry.");
        }

        @Override
        public String[] aliases() {
            return new String[] {"privatemsg", "pm"};
        }

        @Override
        public String desc() {
            return "sends out a mass private message to all members of all connected discords";
        }

        @Override
        public boolean isHidden() {
            return true;
        }
    }

    protected static class When extends ACommand {
        @Override
        public void execute(MessageReceivedEvent event, String[] args) {
            try {
                Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT time, MIN(time) FROM group_faps WHERE time>?");
                ps.setLong(1, System.currentTimeMillis());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    try {
                        Date date = new Date(rs.getTimestamp("time").getTime());
                        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm aaa z");
                        dateFormat.setTimeZone(TimeZone.getTimeZone("CST6CDT"));
                        respond(event, String.format("The next group fap is %s", dateFormat.format(date)));
                    } catch (Exception e) { respond(event, "There are no upcoming group faps. Sorry D:"); }
                } else respond(event, "There are no upcoming group faps. Sorry D:");
                rs.close();
                ps.close();
                conn.close();
            } catch (SQLException e) {
                respond(event, String.format("Error loading group faps from database: %s", e.getLocalizedMessage()));
                e.printStackTrace();
            }
        }

        @Override
        public String[] aliases() {
            return new String[] {"when", "w"};
        }

        @Override
        public String desc() {
            return "tells you when the next group fap is";
        }
    }

    protected static class SetPlaying extends ACommand {
        @Override
        public void execute(MessageReceivedEvent event, String[] args) {
            if (hasRole(event.getMember(), event.getAuthor(), "Verified Fat Cock")) {
                StringBuilder sb = new StringBuilder();
                Presence presence = FapBot.getJDA().getPresence();

                if (args.length < 2) {
                    respond(event, String.format("Correct usage: %ssp <p:s:w:l> <msg>", FapBot.PREFIX));
                    return;
                }

                try {
                    if (args[1].equalsIgnoreCase("p")) {
                        readMessage(2, args, sb);
                        presence.setGame(Game.playing(sb.toString()));
                    } else
                    if (args[1].equalsIgnoreCase("s")) {
                        readMessage(3, args, sb);
                        presence.setGame(Game.streaming(sb.toString(), args[2]));
                    } else
                    if (args[1].equalsIgnoreCase("w")) {
                        readMessage(2, args, sb);
                        presence.setGame(Game.watching(sb.toString()));
                    } else
                    if (args[1].equalsIgnoreCase("l")) {
                        readMessage(2, args, sb);
                        presence.setGame(Game.listening(sb.toString()));
                    } else {
                        respond(event, "Status code incorrect. Valid codes are: p (playing), s (streaming), w (watching), l (listening)");
                        return;
                    }
                    respond(event, "Playing message has been set.");
                } catch (IllegalStateException e) {
                    respond(event, "Parameters are incorrect. Please review your syntax. Note: The \"s\" code requires the parameters <url> <msg>");
                }
            } else respond(event, "You do not have permission to use this command. Sorry D:");
        }

        private void readMessage(int start, String[] args, StringBuilder sb) throws IllegalStateException {
            if (args.length <= start) throw new IllegalStateException();
            for (int i = start;i<args.length;i++) sb.append(args[i]).append(" ");
        }

        @Override
        public String[] aliases() {
            return new String[] {"setplaying", "sp"};
        }

        @Override
        public String desc() {
            return "temporarily sets the \"Playing X\" tag for the bot";
        }

        @Override
        public boolean isHidden() {
            return true;
        }
    }

    protected static class Xp extends ACommand {
        @Override
        public void execute(MessageReceivedEvent event, String[] args) {
            if (args.length == 1) {
                sendStats(event, event.getAuthor());
            } else {
                List<Member> memberList = FapBot.getJDA().getGuildById(FapBot.DISCORD_GUILD_ID).getMembersByEffectiveName(args[1], false);
                if (memberList.size() == 0) {
                    respond(event, String.format("Member \"%s\" not found. Please check the spelling of the effective name provided.", args[1]));
                } else
                if (memberList.size() == 1) {
                    sendStats(event, memberList.get(0).getUser());
                } else {
                    if (args.length > 2) {
                        for (Member m : memberList) {
                            if (m.getUser().getDiscriminator().equals(args[2])) {
                                sendStats(event, m.getUser());
                                break;
                            }
                        }
                    } else {
                        StringBuilder sb = new StringBuilder("There are multiple users by this name. Which one do you mean:");
                        for (Member m : memberList) sb.append("\n").append(m.getEffectiveName()).append(" ").append(m.getUser().getDiscriminator());
                        respond(event, sb.toString());
                    }
                }
            }
        }

        private void sendStats(MessageReceivedEvent event, User user) {
            Member member = FapBot.getJDA().getGuildById(FapBot.DISCORD_GUILD_ID).getMember(user);
            long discord_id = user.getIdLong();
            try {
                Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM leveldata WHERE discord_id=?");
                ps.setLong(1, discord_id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    short server_level = rs.getShort("server_level");
                    short gamer_level = rs.getShort("gamer_level");
                    long server_xp = rs.getLong("server_xp");
                    long game_xp = rs.getLong("game_xp");

                    ByteArrayOutputStream bao = new ByteArrayOutputStream();
                    ImageIO.write(FapBot.getLevelSystem().prepareImage(member, server_level, gamer_level, game_xp, server_xp), "png", bao);
                    NumberFormat numFor = NumberFormat.getNumberInstance();
                    LevelSystem levelSystem = FapBot.getLevelSystem();
                    event.getChannel().sendFile(bao.toByteArray(), "faplevel.png", new MessageBuilder(
                            String.format("**Server Level: ** %s\n**Server Xp:** %s / 960\n**Server Rank:** #%s\n**Gamer Level:** %s\n**Gamer Xp:** %s / 96,000\n**Gamer Rank:** #%s",
                                    server_level, server_xp, numFor.format(levelSystem.getRank(LevelSystem.Level.SERVER, discord_id)), gamer_level, numFor.format(game_xp), numFor.format(levelSystem.getRank(LevelSystem.Level.GAMER, discord_id))
                            )).build()).queue();
                } else {
                    respond(event, "You do not have a level. Go play games or chat with some people from FAP to gain xp and level up.");
                }
                rs.close();
                ps.close();
                conn.close();
            } catch (SQLException | IOException | FontFormatException e) {
                e.printStackTrace();
                respond(event, "Oops, sorry about that. We seem to be having technical difficulties. Please try again later.");
            }
        }

        @Override
        public String[] aliases() {
            return new String[] {"xp"};
        }

        @Override
        public String desc() {
            return "shows your current xp/level statistics";
        }
    }

    protected static class Leaderboard extends ACommand {
        @Override
        public void execute(MessageReceivedEvent event, String[] args) {
            if (args.length == 1) {
                respond(event, String.format("Invalid syntax, the correct usage is %slb [server:gamer]", FapBot.PREFIX));
            } else
            if (args.length == 2) {
                try {
                    Connection conn = DB.getConnection();
                    Guild guild = FapBot.getJDA().getGuildById(FapBot.DISCORD_GUILD_ID);
                    if (args[1].equalsIgnoreCase("server")) {
                        // lists top 5 for server level
                        // note: this does not factor in prestige because I havent added anything for that. TODO later (maybe remove prestige)
                        StringBuilder sb = prepareFromStatement(conn.prepareStatement("SELECT * FROM leveldata ORDER BY (server_xp + server_level * 960) DESC LIMIT 5"), guild, LevelSystem.Level.SERVER, 2, 5);
                        respond(event, sb.toString());

                    } else
                    if (args[1].equalsIgnoreCase("gamer")) {
                        // lists top 5 for gamer level
                        StringBuilder sb = prepareFromStatement(conn.prepareStatement("SELECT * FROM leveldata ORDER BY game_xp DESC LIMIT 5"), guild, LevelSystem.Level.GAMER, 3, 6);
                        respond(event, sb.toString());

                    } else respond(event, "Please specify \"server\" or \"gamer\" to show the top people in either of these categories.");
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    respond(event, "Sorry, the database is currently down. Try again later.");
                }
            } else respond(event, String.format("Invalid syntax, the correct usage is %slb [server:gamer]", FapBot.PREFIX));
        }

        private class LeaderboardUser {

            private String name;
            private short level;
            private long xp;

            public LeaderboardUser(String name, short level, long xp) {
                this.name = name;
                this.level = level;
                this.xp = xp;
            }
        }

        private StringBuilder prepareFromStatement(PreparedStatement ps, Guild guild, LevelSystem.Level level, int i1, int i2) throws SQLException {
            ResultSet rs = ps.executeQuery();
            StringBuilder sb = new StringBuilder();
            List<LeaderboardUser> leaderboardUsers = new ArrayList<>();
            int longestName = 0;
            while (rs.next()) {
                Member member = guild.getMemberById(rs.getLong(1));
                String name;
                if (member == null) name = "<left>"; else name = member.getEffectiveName();
                int nameLength = name.length();
                if (nameLength > longestName) longestName = nameLength;
                leaderboardUsers.add(new LeaderboardUser(name, rs.getShort(i1), rs.getLong(i2)));
            }

            int i = 1;
            longestName++;
            sb.append("Leaderboard for ").append(level == LevelSystem.Level.SERVER ? "Server" : "Gamer").append(" Level:```\n");
            for (LeaderboardUser user : leaderboardUsers) {
                sb.append("\n").append(i++).append(". ").append(user.name);
                addSpaces(longestName - user.name.length(), sb);
                sb.append("Level: ");
                sb.append(user.level);
                addSpaces(8 - Long.toString(user.level).length(), sb);
                sb.append("Xp: ");
                if (level == LevelSystem.Level.SERVER) {
                    addSpaces(3 - Long.toString(user.xp).length(), sb);
                    sb.append(user.xp).append(" / 960");
                } else {
                    String xpFormatted = NumberFormat.getNumberInstance().format(user.xp);
                    addSpaces(6 - xpFormatted.length(), sb);
                    sb.append(xpFormatted).append(" / 96,000");
                }
            }
            sb.append("```");

            rs.close();
            ps.close();
            return sb;
        }

        private void addSpaces(int spaces, StringBuilder sb) {
            for (int i = 0;i<spaces;i++) sb.append(" ");
        }

        @Override
        public String[] aliases() {
            return new String[] {"leaderboard", "lb"};
        }

        @Override
        public String desc() {
            return "shows a list of the top five people based on server or gamer level";
        }
    }

    protected static class RewardPoints extends ACommand {
        @Override
        public void execute(MessageReceivedEvent event, String[] args) {
            if (event.getAuthor().getIdLong() == FapBot.SERVER_OWNERS_DC_ID) {
                if (args.length == 4) {
                    long points;
                    Member member;
                    // attempt to parse points
                    try {
                        points = Long.parseLong(args[3]);
                        if (points < 1) throw new NumberFormatException();
                    } catch (NumberFormatException e) {
                        respond(event, "Invalid number specified for points. Note: can not be negative or zero");
                        return;
                    }

                    // attempt to resolve name to member
                    List<Member> membersByEffectiveName = FapBot.getJDA().getGuildById(FapBot.DISCORD_GUILD_ID).getMembersByEffectiveName(args[1], false);
                    if (membersByEffectiveName.size() == 0) {
                        respond(event, "No user by that name was found. Is this their name as it appears in the discord server?");
                        return;
                    }
                    if (membersByEffectiveName.size() > 1) {
                        respond(event, "Multiple users by this name found. Sorry, you can't give xp to this person.");
                        return;
                    }
                    member = membersByEffectiveName.get(0);

                    // assign points based on type
                    try {
                        if (args[2].equalsIgnoreCase("server") || args[2].equalsIgnoreCase("gamer")) {
                            FapBot.getLevelSystem().awardPoints(member, args[2].equalsIgnoreCase("server") ? LevelSystem.Level.SERVER : LevelSystem.Level.GAMER, points);
                            respond(event, String.format("%s points where awarded towards %s's %s level.", NumberFormat.getNumberInstance().format(points), member.getEffectiveName(), args[2].toLowerCase()));
                        } else respond(event, "The correct value for argument 2 is server or gamer.");
                    } catch (SQLException e) {
                        e.printStackTrace();
                        respond(event, "The database is currently down. Sorry.");
                    }
                } else respond(event, String.format("Correct usage: %sap <name> [server:gamer] <points>", FapBot.PREFIX));
            } else respond(event, "Sorry. This command can only be used by drew6017.");
        }

        @Override
        public String[] aliases() {
            return new String[] {"awardpoints", "ap"};
        }

        @Override
        public String desc() {
            return "awards points to users";
        }

        @Override
        public boolean isHidden() {
            return true;
        }
    }
}
