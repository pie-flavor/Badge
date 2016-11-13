package flavor.pie.badge;

import com.google.inject.Inject;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.Team;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Plugin(id = "badge", name = "Badge", version = "1.0.0", description = "Puts prefixes on peoples' heads.", authors = "pie_flavor")
public class Badge {
    @Inject
    Game game;
    @Inject
    Logger logger;
    @Inject @DefaultConfig(sharedRoot = true)
    ConfigurationLoader<CommentedConfigurationNode> loader;
    @Inject @DefaultConfig(sharedRoot = true)
    Path path;
    Config config;
    Scoreboard scoreboard;
    @Listener
    public void preInit(GamePreInitializationEvent e) throws IOException, ObjectMappingException {
        if (!Files.exists(path)) {
            try {
                game.getAssetManager().getAsset(this, "default.conf").get().copyToFile(path);
            } catch (IOException ex) {
                logger.error("Could not copy default config!");
                mapDefault();
                throw ex;
            }
        }
        ConfigurationNode root;
        try {
            root = loader.load();
        } catch (IOException ex) {
            logger.error("Could not load config!");
            mapDefault();
            throw ex;
        }
        try {
            config = root.getValue(Config.type);
        } catch (ObjectMappingException ex) {
            logger.error("Invalid config!");
            loadDefault();
            throw ex;
        }
    }

    private Scoreboard getScoreboard() {
        if (scoreboard == null) {
            List<Team> teams = config.prefixes.stream().map(prefix -> Team.builder()
                    .allowFriendlyFire(true)
                    .canSeeFriendlyInvisibles(false)
                    .name(prefix.name)
                    .prefix(TextSerializers.FORMATTING_CODE.deserialize(prefix.display))
                    .build()).collect(Collectors.toList());
            scoreboard = Scoreboard.builder().teams(teams).build();
        }
        return scoreboard;
    }

    @Listener
    public void started(GameStartedServerEvent e) {
        Task.builder()
                .intervalTicks(1)
                .delayTicks(1)
                .name("badge-S-PrefixAssigner")
                .execute(this::assignPrefixes)
                .submit(this);
    }

    private void assignPrefixes() {
        for (Player p : game.getServer().getOnlinePlayers()) {
            Optional<Config.Prefix> prefix_ = config.prefixes.stream().filter(pfx -> p.hasPermission("badge.prefix."+pfx.name.replace('.', '_'))).findFirst();
            if (prefix_.isPresent()) {
                getScoreboard().getTeam(prefix_.get().name).get().addMember(p.getTeamRepresentation());
            } else {
                getScoreboard().getMemberTeam(p.getTeamRepresentation()).ifPresent(t -> t.removeMember(p.getTeamRepresentation()));
            }
        }
    }

    @Listener
    public void join(ClientConnectionEvent.Join e) {
        e.getTargetEntity().setScoreboard(getScoreboard());
    }

    private ConfigurationNode loadDefault() throws IOException {
        return HoconConfigurationLoader.builder().setURL(game.getAssetManager().getAsset(this, "default.conf").get().getUrl()).build().load(loader.getDefaultOptions());
    }

    private void mapDefault() {
        try {
            config = loadDefault().getValue(Config.type);
        } catch (IOException | ObjectMappingException ex) {
            logger.error("Could not load internal config! Disabling plugin.");
            game.getEventManager().unregisterPluginListeners(this);
        }
    }
}
