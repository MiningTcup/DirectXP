package me.miningtcup.directXP;

import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.bukkit.Bukkit.getPluginManager;
import static org.bukkit.Bukkit.getScheduler;

public final class DirectXP extends JavaPlugin implements Listener {
    private final FileConfiguration config = getConfig();
    private final Map<Player, Integer> remainingDings = new HashMap<>();
    private final Map<Player, Boolean> isPlaying = new HashMap<>();
    private final Map<Player, Long> lastDingTime = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.getLogger().info("DirectXP is enabled.");
        getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        reloadConfig();
        if (!config.getBoolean("entity-death", true)) return;
        if (event.getDamageSource().getCausingEntity() instanceof Player player) {
            int exp = event.getDroppedExp();
            player.giveExp(exp);
            event.setDroppedExp(0);
            ding(player, exp);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        reloadConfig();
        if (!config.getBoolean("break-block", true)) return;
        int exp = event.getExpToDrop();
        if (exp > 0) {
            Player player = event.getPlayer();
            player.giveExp(exp);
            event.setExpToDrop(0);
            ding(player, exp);
        }
    }

    public void ding(Player player, int exp) {
        reloadConfig();
        if (config.getBoolean("multi-sounds")) {
            int newDings = calculateOrbs(exp).size();
            remainingDings.put(player, remainingDings.getOrDefault(player, 0) + newDings);
            startPlaying(player);
        } else {
            reloadConfig();
            long currentTime = System.currentTimeMillis();
            int soundDelayMillis = config.getInt("sound-delay", 2) * 50;
            long lastTime = lastDingTime.getOrDefault(player, 0L);

            if (currentTime - lastTime >= soundDelayMillis) {
                lastDingTime.put(player, currentTime);
                player.playSound(player.getLocation(),
                        Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.1F, (float) (0.55 + Math.random() * 0.7));
            }
        }
    }

    private void startPlaying(Player player) {
        reloadConfig();
        if (isPlaying.getOrDefault(player, false)) return;
        isPlaying.put(player, true);

        getScheduler().runTaskTimer(this, task -> {
            int dingsLeft = remainingDings.getOrDefault(player, 0);
            if (dingsLeft > 0) {
                player.playSound(player.getLocation(),
                        Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.1F, (float) (0.55 + Math.random() * 0.7));
                remainingDings.put(player, dingsLeft - 1);
            } else {
                task.cancel();
                isPlaying.put(player, false);
            }
        }, 0, config.getInt("sound-delay", 2));
    }

    private static final int[] ORB_VALUES = {2477, 1237, 617, 307, 149, 73, 37, 17, 7, 3, 1};

    public static List<Integer> calculateOrbs(int totalExperience) {
        List<Integer> orbs = new ArrayList<>();

        for (int value : ORB_VALUES) {
            while (totalExperience >= value) {
                totalExperience -= value;
                orbs.add(value);
            }
        }

        return orbs;
    }
}
