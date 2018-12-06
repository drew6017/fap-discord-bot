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

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Commands {
    protected static class Help extends ACommand {
        @Override
        public void execute(MessageReceivedEvent event, String[] args) {
            StringBuilder sb = new StringBuilder();
            sb.append("Commands often have several aliases separated by commas for quick usage. Here is a list of commands:\n");
            for (ACommand c : FapBot.commands) {
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
            if (key == 0 && event.getAuthor().getId().equals("246069907467403264")) key = 1; // prevents drew6017 from getting bad messages
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
            if (hasRole(event.getMember(), "Verified Fat Cock")) {
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
                            task.remove(event.getMember());
                            respond(event, "This feature is not yet ready lol. Sorry."); // TODO
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
            return new String[] {"groupfap, gf"};
        }

        @Override
        public String desc() {
            return "create and manage group faps";
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
}
