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

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Commands {
    public static class Help extends ACommand {
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

    public static class Fap extends ACommand {
        @Override
        public void execute(MessageReceivedEvent event, String[] args) {
            respond(event, "*fap fap fap fap fap fap fap fap fap*... Oh, sorry, didn't see you there.");
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
}
