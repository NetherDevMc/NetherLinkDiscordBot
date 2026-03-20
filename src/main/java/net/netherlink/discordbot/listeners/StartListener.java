/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/GeyserDiscordBot
 */

package net.netherlink.discordbot.listeners;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.netherlink.discordbot.util.BotColors;
import net.netherlink.discordbot.util.FeaturedServers;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import pw.chew.chewbotcca.util.RestClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StartListener extends ListenerAdapter {
    public static final String FEATURED_SERVERS_URL =
            "https://raw.githubusercontent.com/NetherDevMc/NetherLinkData/main/featured/featured-servers";
    public static final String CHANNEL_ID = "1484678070030827560";

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        var channel = event.getJDA().getTextChannelById(CHANNEL_ID);
        if (channel == null) {
            System.err.println("Kanaal niet gevonden!");
            return;
        }
        List<FeaturedServers> servers = fetchFeaturedServers();
        if (servers.isEmpty()) {
            System.err.println("Geen servers gevonden in de JSON!");
            return;
        }
        int day = LocalDate.now().getDayOfYear();
        FeaturedServers s = servers.get(day % servers.size());

        var eb = new EmbedBuilder()
                .setTitle("🌟 Server of the Day: " + s.name(), s.websiteUrl())
                .setDescription(String.format(
                        """
                                **%s**  
                                %s
                                
                                🌐 **[Website](%s)**  
                                """,
                        s.name(),
                        s.description(),
                        s.websiteUrl() != null ? s.websiteUrl() : " "
                ))
                .addField("IP / Port", String.format("```%s:%s```", s.address(), s.port()), false)
                .setColor(BotColors.SUCCESS.getColor())
                .setTimestamp(java.time.OffsetDateTime.now())
                .setFooter("Featured daily • NetherDev", null);

        if (s.iconUrl() != null && !s.iconUrl().isBlank()) {
            eb.setThumbnail(s.iconUrl());
        }

        channel.getHistory().retrievePast(10).queue(history -> {
            Message botMsg = history.stream()
                    .filter(m -> m.getAuthor().isBot() && m.getEmbeds().size() > 0)
                    .findFirst()
                    .orElse(null);

            if (botMsg != null) {
                botMsg.editMessageEmbeds(eb.build()).queue();
            } else {
                channel.sendMessageEmbeds(eb.build()).queue();
            }
        });
    }

    @NotNull
    public static List<FeaturedServers> fetchFeaturedServers() {
        JSONArray arr = RestClient.get(FEATURED_SERVERS_URL).asJSONArray();
        List<FeaturedServers> servers = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            servers.add(new FeaturedServers(
                    obj.getString("name"),
                    obj.getString("description"),
                    obj.getString("address"),
                    obj.getInt("port"),
                    obj.optString("iconUrl", null),
                    obj.optString("websiteUrl", null)
            ));
        }
        return servers;
    }
}