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
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public abstract class ACommand {

    public abstract void execute(MessageReceivedEvent event, String[] args);

    public abstract String[] aliases();

    public abstract String desc();

    protected boolean matchesAlias(String a) {
        for (String alias : aliases()) {
            if (alias.equalsIgnoreCase(a)) return true;
        }

        return false;
    }

    public static void respond(MessageReceivedEvent event, String msg) {
        event.getChannel().sendMessage(msg).queue();
    }

    public static boolean hasRole(Member m, String role) {
        if (m == null) return false;
        for (Role r : m.getRoles()) {
            if (r.getName().equals(role)) return true;
        }
        return false;
    }
}
