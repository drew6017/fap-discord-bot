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

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.utils.AttachmentOption;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.HashMap;
import java.util.List;

public class LevelSystem implements Runnable {

    private static final long BASE_GAMER_POINTS_PM = 2; // max = 96000 (around 800 hours of playing games)
    private static final double BASE_SERVER_POINTS_PM = 2;
    private static final double SERVER_POINTS_MULTIPLIER = 0.5; // effective multiplier (2 players = 1x, 4 players = 2x)
    private static final int MAX_XP_BAR_LENGTH = 534;
    private static final HashMap<Integer, MilestoneEvent> milestones = new HashMap<>();
    private static final String RANK_QUERY = "SELECT rank FROM (" +
                                               "SELECT %s, discord_id, @rnum := @rnum + 1 AS rank " +
                                               "FROM leveldata ORDER BY %s DESC" +
                                             ") AS lul WHERE discord_id=%s"; // lul is here cause i couldnt get rank() to work and this wouldnt work without it here for some reason

    public LevelSystem() {
        MilestoneEvent incrementOf10 = (member, level_type, level, trigger_level) -> {
            // post image showing the persons name/level
            TextChannel tc = member.getGuild().getTextChannelById(FapBot.DISCORD_MAIN_TC_ID);
            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            try {
                Connection conn = DB.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM leveldata WHERE discord_id=?");
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) throw new SQLException("Did not find user by provided discord id.");
                long game_xp = rs.getLong("game_xp");
                long server_xp = rs.getLong("server_xp");
                int gamer_level = (int)rs.getShort("gamer_level");
                rs.close();
                ps.close();
                conn.close();
                if (level_type == Level.SERVER) {
                    ImageIO.write(prepareImage(member, trigger_level, gamer_level, game_xp, server_xp), "png", bao);
                } else {
                    ImageIO.write(prepareImage(member, (int)level, gamer_level, game_xp, server_xp), "png", bao);
                }
            } catch (IOException | SQLException | FontFormatException e) {
                FapBot.log.warning("Could not render image for user " + member.getEffectiveName() + "'s level up message.");
                e.printStackTrace();
                return;
            }
            tc.sendFile(bao.toByteArray(), "faplevelup.png", new AttachmentOption[0])
                    .append(member.getEffectiveName() + " just leveled up their " + level_type.name().toLowerCase() + " level!")
                    .queue();
        };

        milestones.put(10, incrementOf10);
        milestones.put(20, incrementOf10);
        milestones.put(30, incrementOf10);
        milestones.put(40, incrementOf10);
        milestones.put(50, incrementOf10);
        milestones.put(60, incrementOf10);
        milestones.put(70, incrementOf10);
        milestones.put(80, incrementOf10);
        milestones.put(90, incrementOf10);
        milestones.put(100, (member, level_type, level, trigger_level) -> {
            long role = 0;
            String rank = null;
            if (level_type == Level.GAMER) {
                // promote to Master Gamer
                role = FapBot.ROLE_ID_MASTER_GAMER;
                rank = "Mastur Gamer";
            } else
            if (level_type == Level.SERVER) {
                // promote to Verified Fat Cock
                role = FapBot.ROLE_ID_VERIFIED_FAT_COCK;
                rank = "Verified Fat Cock";
            }
            if (role != 0) {
                for (Role r : member.getRoles()) {
                    if (r.getIdLong() == role) return;
                }

                member.getGuild().addRoleToMember(member, member.getGuild().getRoleById(role)).queue(); // maybe add .reason() here
                member.getGuild().getTextChannelById(FapBot.DISCORD_MAIN_TC_ID).sendMessage(String.format("@everyone %s just became a %s for reaching a %s level of 100!", member.getEffectiveName(), rank, level_type.name().toLowerCase())).queue();
            }
        });
    }

    @Override
    public void run() {
        Guild guild = FapBot.getJDA().getGuildById(FapBot.DISCORD_GUILD_ID);

        for (Member m : guild.getMembers()) {
            User u = m.getUser();
            if (u.isBot()) continue;
            if (m.getOnlineStatus() == OnlineStatus.OFFLINE) continue;
            if (m.getActivities().size() != 0) {
                // award points -> Gamer Level
                try {
                    awardPoints(m, Level.GAMER, BASE_GAMER_POINTS_PM);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        for (VoiceChannel channel : guild.getVoiceChannels()) {
            List<Member> members = channel.getMembers();
            int num_members = members.size();

            if (num_members > 1)
            for (Member m : channel.getMembers()) {
                User u = m.getUser();
                if (u.isBot()) continue;
                // award points -> Server Level
                double points = (SERVER_POINTS_MULTIPLIER * (double)num_members) * BASE_SERVER_POINTS_PM;
                BigDecimal bd = new BigDecimal(points, new MathContext(1, RoundingMode.HALF_UP));
                try {
                    awardPoints(m, Level.SERVER, bd.longValue());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void awardPoints(Member member, Level level, long points) throws SQLException {
        awardPoints(member, level, points, false);
    }

    public long getRank(Level level, long discord_id) throws SQLException {
        Connection conn = DB.getConnection();

        String fromCols;
        String orderer;

        if (level.equals(Level.GAMER)) {
            orderer = fromCols = "game_xp";
        } else
        if (level.equals(Level.SERVER)) {
            fromCols = "server_xp, server_level";
            orderer = "(server_xp + server_level * 960)";
        } else {
            conn.close();
            throw new SQLException("Invalid level type. Must be gamer or server.");
        }


        Statement stmt = conn.createStatement();
        stmt.execute("SET @rnum := 0");
        ResultSet rs = stmt.executeQuery(String.format(RANK_QUERY, fromCols, orderer, discord_id));
        long rank;
        if (rs.next()) {
            rank = rs.getLong("rank");
        } else {
            // user was not in db, therefore had no rank
            rank = -1;
        }
        rs.close();
        stmt.close();
        conn.close();
        return rank;
    }

    private void awardPoints(Member member, Level level, long points, boolean recursive) throws SQLException {
        User user = member.getUser();
        Connection conn = DB.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM leveldata WHERE discord_id=?");
        ps.setLong(1, member.getUser().getIdLong());
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            // has user data, update (did level up occur)
            if (level == Level.GAMER) {
                // award gamer xp
                short old_gamer_level = rs.getShort("gamer_level");
                long newxp_value = rs.getLong("game_xp") + points;
                rs.close();
                ps.close();
                if (newxp_value <= 96000) { // allows value to go over 96000 as a way to track further progress (maybe do something with it later)
                    BigDecimal bd = new BigDecimal(newxp_value).divide(new BigDecimal(960), 1, RoundingMode.DOWN);
                    short new_gamer_level = bd.shortValue();
                    if (old_gamer_level < new_gamer_level) {
                        // award new level
                        // e.g. update new level in db, check if passed any level milestones
                        ps = conn.prepareStatement("UPDATE leveldata SET gamer_level = ?, game_xp = game_xp + ? WHERE discord_id=?");
                        ps.setShort(1, new_gamer_level);
                        ps.setLong(2, points);
                        ps.setLong(3, user.getIdLong());
                        ps.executeUpdate();
                        ps.close();
                        conn.close();
                        awardMilestones(member, level, old_gamer_level, new_gamer_level);
                        return;
                    }
                }
                // update points in db
                ps = conn.prepareStatement("UPDATE leveldata SET game_xp = game_xp + ? WHERE discord_id=?");
                ps.setLong(1, points);
                ps.setLong(2, user.getIdLong());
                ps.executeUpdate();
            } else
            if (level == Level.SERVER) {
                // award server xp
                short old_server_level = rs.getShort("server_level");
                long newxp_value = rs.getLong("server_xp") + points;
                rs.close();
                ps.close();
                if (old_server_level >= 100) {
                    conn.close();
                    return;
                }
                if (newxp_value >= 960) {
                    // there are enough points to level up

                    long increase_levels;
                    long sparePoints;

                    if (newxp_value > 960) {
                        increase_levels = newxp_value / 960L;
                        sparePoints = newxp_value - (increase_levels * 960);
                    } else {
                        increase_levels = 1;
                        sparePoints = 0;
                    }

                    if ((old_server_level + increase_levels) >= 100) {
                        increase_levels = 100 - old_server_level;
                        sparePoints = 960;
                    }

                    ps = conn.prepareStatement("UPDATE leveldata SET server_level = server_level + ?, server_xp = ? WHERE discord_id=?");
                    ps.setShort(1, (short)increase_levels);
                    ps.setLong(2, sparePoints);
                    ps.setLong(3, user.getIdLong());
                    ps.executeUpdate();
                    ps.close();
                    conn.close();
                    awardMilestones(member, level, old_server_level, (short)(old_server_level + (short)increase_levels));
                    return;
                }
                // update points in db
                ps = conn.prepareStatement("UPDATE leveldata SET server_xp = server_xp + ? WHERE discord_id=?");
                ps.setLong(1, points);
                ps.setLong(2, user.getIdLong());
                ps.executeUpdate();
            }
            ps.close();
            conn.close();
        } else {
            // no user data, create (does this amount cause level up)
            // loop this method?
            if (recursive) {
                // already did this, dont try again or could get stuck in loop
                throw new IllegalStateException("Failed to award points to user " + user.getName() + ". Potential recursion imminent.");
            }
            rs.close();
            ps.close();
            ps = conn.prepareStatement("INSERT INTO leveldata VALUES(?,0,0,0,0,0)");
            ps.setLong(1, user.getIdLong());
            ps.executeUpdate();
            ps.close();
            conn.close();
            awardPoints(member, level, points, true);
        }
    }

    private void awardMilestones(Member member, Level level, short old_level, short new_level) {
        milestones.forEach((k, v) -> {
            if (inRangeOnce(k, old_level, new_level)) v.call(member, level, new_level, k);
        });
    }

    public BufferedImage prepareImage(Member member, int level, int gamerlevel, long gamer_xp, long server_xp) throws IOException, FontFormatException {
        BufferedImage img = ImageIO.read(getClass().getResource("/com/divisionind/fdb/assets/level-up-template.png"));
        Graphics g = img.getGraphics();
        Graphics2D g2d = (Graphics2D)g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        // server_xp bar
        g2d.setColor(new Color(0, 118, 177));
        BigDecimal bd = new BigDecimal(server_xp).divide(new BigDecimal(960), 2, RoundingMode.HALF_UP).multiply(new BigDecimal(MAX_XP_BAR_LENGTH));
        g2d.fillRect(214, 34, bd.intValue(), 10);

        // gamer_xp bar
        g2d.setColor(new Color(34, 227, 0));
        if (gamer_xp > 96000) gamer_xp = 96000; // ensures the bar does not exceed the image
        bd = new BigDecimal(gamer_xp).divide(new BigDecimal(96000), 2, RoundingMode.HALF_UP).multiply(new BigDecimal(MAX_XP_BAR_LENGTH));
        g2d.fillRect(214, 44, bd.intValue(), 10);

        // users name
        g2d.setColor(Color.WHITE);
        Font font = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("/com/divisionind/fdb/assets/phagspab.ttf"));
        g2d.setFont(font.deriveFont(70F));
        g2d.drawString(member.getEffectiveName(), 214, 135);

        // server level
        g2d.setColor(new Color(0, 99, 148));
        g2d.setFont(font.deriveFont(45F));
        String slevel = Integer.toString(level);
        g2d.drawString(slevel, 668 - g2d.getFontMetrics().stringWidth(slevel), 187);

        // gamer level
        g2d.setColor(new Color(0, 119, 73));
        g2d.setFont(font.deriveFont(45F));
        String glevel = Integer.toString(gamerlevel);
        g2d.drawString(glevel, 812 - g2d.getFontMetrics().stringWidth(glevel), 187);

        // avatar image
        // https://cdn.discordapp.com/avatars/246069907467403264/9a227866ccdd06169c8742eff9a48a25.png returns 128x128 image
        // note: discord started throwing a 403 so I changed the agent
        URL url = new URL(member.getUser().getEffectiveAvatarUrl());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:67.0) Gecko/20100101 Firefox/67.0");

        int scale = 170;
        Image avatar = ImageIO.read(conn.getInputStream()).getScaledInstance(scale, scale, Image.SCALE_AREA_AVERAGING);
        g2d.drawImage(applyAlphaMask(toBufferedImage(avatar), getCircleMask(scale, scale)), 30, 111 - (scale / 2), null);

        g2d.dispose();

        return img;
    }

    private BufferedImage applyAlphaMask(BufferedImage image, BufferedImage mask) {
        BufferedImage maskedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = maskedImage.createGraphics();

        // draw original
        g2.drawImage(image, 0, 0, null);

        // set "masking settings"
        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.DST_IN, 1.0F);
        g2.setComposite(ac);

        // draw mask
        g2.drawImage(mask, 0, 0, null);

        g2.dispose();

        return maskedImage;
    }

    private BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) return (BufferedImage) img;
        BufferedImage bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = bi.createGraphics();
        g2d.drawImage(img, 0, 0, null);
        g2d.dispose();

        return bi;
    }

    private BufferedImage getCircleMask(int imageSize, int circleSize) {
        BufferedImage img = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D)img.getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // draw circle
        g2d.setColor(Color.BLACK);
        g2d.fillOval(0, 0, circleSize, circleSize);
        g2d.dispose();

        return img;
    }

    // in range once via increment (e.g. i = 10, min = 9, max = 10 yields true while i = 10, min = 10, max = 11 yields false
    private boolean inRangeOnce(int i, int min, int max) { // note: min MUST NOT EQUAL max, e.g. i = 100, min = 100, max = 100 yields true (so the award would be given again)
        if (i == max) return true;
        return i > min && i < max;
    }

    public enum Level {

        SERVER("server_level"),
        GAMER("gamer_level");

        private String db_descriptor;

        Level(String db_descriptor) {
            this.db_descriptor = db_descriptor;
        }
    }

    private interface MilestoneEvent {
        void call(Member member, Level level_type, short level, int trigger_level);
    }
}
