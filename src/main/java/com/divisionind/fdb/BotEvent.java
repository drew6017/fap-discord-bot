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
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class BotEvent extends ListenerAdapter {
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // prevents possible infinite loops
        if (event.getAuthor().isBot()) return;

        // check for bot command prefix
        String raw = event.getMessage().getContentRaw();
        if (raw.startsWith(FapBot.PREFIX)) {
            event.getChannel().sendMessage("Yep, got your message!").queue();
            return;
        }

        if (raw.toLowerCase().contains("fap")) {
            event.getChannel().sendMessage("Did someone say fap? I like to have fun and party!").queue();
        }
    }
}
