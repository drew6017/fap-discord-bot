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

import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LevelSystem implements Runnable {

    private static final String PLAYER_LEVEL_DATA = "leveldata";

    public LevelSystem() {
        FapBot.getScheduler().repeating(this, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public void run() {
        Guild guild = FapBot.getJDA().getGuildById(FapBot.DISCORD_GUILD_ID);

        for (Member m : guild.getMembers()) {
            User u = m.getUser();
            if (u.isBot()) continue;
            if (m.getOnlineStatus() == OnlineStatus.OFFLINE) continue;
            if (m.getGame() != null) {
                // TODO award points -> Gamer Level
            }
        }

        for (VoiceChannel channel : guild.getVoiceChannels()) {
            List<Member> members = channel.getMembers();
            int num_members = members.size();

            if (num_members > 1)
            for (Member m : channel.getMembers()) {
                User u = m.getUser();
                if (u.isBot()) continue;
                // TODO award points -> Server Level
            }
        }
    }

    public void awardPoints(Level level, long points) {

    }

    public BufferedImage prepareImage(Member member) {
        return null; // TODO
    }

    public enum Level {

        SERVER("server_level"),
        GAMER("gamer_level");

        private String db_discriptor;

        Level(String db_discriptor) {
            this.db_discriptor = db_discriptor;
        }
    }
}
