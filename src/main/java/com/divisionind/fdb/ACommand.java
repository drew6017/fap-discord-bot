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

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public abstract class ACommand {

    public abstract void execute(MessageReceivedEvent event, String[] args);

    public abstract String[] aliases();

    public abstract String desc();

    public boolean isHidden() {
        return false;
    }

    protected boolean matchesAlias(String a) {
        for (String alias : aliases()) {
            if (alias.equalsIgnoreCase(a)) return true;
        }

        return false;
    }

    public static void respond(MessageReceivedEvent event, String msg) {
        event.getChannel().sendMessage(msg).queue();
    }

    public static boolean hasRole(Member member, User user, String role) {
        if (member == null) {
            if (user == null) return false;
            for (Guild guild : user.getMutualGuilds()) {
                if (hasRole(guild.getMember(user), null, role)) return true;
            }
            return false;
        }
        for (Role r : member.getRoles()) {
            if (r.getName().equals(role)) return true;
        }
        return false;
    }
}
