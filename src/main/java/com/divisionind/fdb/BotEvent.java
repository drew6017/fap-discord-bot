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

import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
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
            raw = raw.substring(1); // remove prefix

            // split into arguments
            String[] args;
            if (raw.contains(FapBot.DELIMITER)) {
                args = raw.split(FapBot.DELIMITER);
            } else args = new String[] {raw};

            // search for and run command
            for (ACommand cmd : FapBot.commands) {
                if (cmd.matchesAlias(args[0])) {
                    try {
                        cmd.execute(event, args);
                    } catch (Exception e) {
                        FapBot.log.severe(String.format("An error occurred whilst attempting to run the command: %s", args[0]));
                        e.printStackTrace();
                    }
                    return;
                }
            }
            event.getChannel().sendMessage(String.format("Command not found. See a full list of commands with %shelp", FapBot.PREFIX)).queue();
            return;
        }

        if (raw.toLowerCase().contains("fap") && event.getAuthor().getIdLong() == 377291553913438220L) { // datpepper
            event.getChannel().sendMessage("Did someone say fap? I like to have fun and party!").queue();
        }
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
        if (event.getUser().isBot()) return;
        event.getGuild().getTextChannelById(FapBot.DISCORD_HELLO_GOODBYE_TC_ID).sendMessage(String.format("**%s#%s** has left the discord for some stupid reason.", event.getMember().getEffectiveName(), event.getUser().getDiscriminator())).queue();
        event.getUser().openPrivateChannel().queue(pmc -> pmc.sendMessage(String.format("We are sad to see you leave :(. You were invited to FAP because you are truly awesome, and the door is always open to people like you. In that spirit, here is a link that will never expire for you to rejoin FAP should you ever choose to do so: %s. Thanks for being you!",
                        FapBot.PERMANENT_INVITE_LINK)).queue());
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (event.getUser().isBot()) return;
        event.getUser().openPrivateChannel().queue(pmc -> pmc.sendMessage("Hi and welcome to the Fun and partying (F.A.P) discord server. As our name implies, we just like to have fun here (and party). So feel free to reach out and make friends with anyone on the discord.").queue());
    }
}
