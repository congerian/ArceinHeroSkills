package ru.arcein.plugins.heroes.skills.escape;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.InvulnerabilityEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;
import java.util.Iterator;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

public class SkillEscape extends ActiveSkill {
    public SkillEscape(Heroes plugin) {
        super(plugin, "Escape");
        this.setDescription("Неуязвимость на $1, следующий за телепортацией на $2 блоков.");
        this.setUsage("/skill escape");
        this.setArgumentRange(0, 0);
        this.setIdentifiers(new String[]{"skill escape"});
        this.setTypes(new SkillType[]{SkillType.TELEPORTING, SkillType.DISABLE_COUNTERING});
    }

    public String getDescription(Hero hero) {
        long duration = (long)((double)SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 1000, false) + SkillConfigManager.getUseSetting(hero, this, "duration-increase", 0.0D, false) * (double)hero.getSkillLevel(this)) / 1000L;
        duration = duration > 0L ? duration : 0L;
        int maxDistance = (int)((double)SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE.node(), 6, false) + SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE.node(), 0.0D, false) * (double)hero.getSkillLevel(this));
        maxDistance = maxDistance > 0 ? maxDistance : 0;
        String description = this.getDescription().replace("$1", duration + "").replace("$1", maxDistance + "");
        int cooldown = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN.node(), 0, false) - SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN_REDUCE.node(), 0, false) * hero.getSkillLevel(this)) / 1000;
        if (cooldown > 0) {
            description = description + " CD:" + cooldown + "s";
        }

        int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA.node(), 10, false) - SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA_REDUCE.node(), 0, false) * hero.getSkillLevel(this);
        if (mana > 0) {
            description = description + " M:" + mana;
        }

        int healthCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST, 0, false) - SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST_REDUCE, mana, true) * hero.getSkillLevel(this);
        if (healthCost > 0) {
            description = description + " HP:" + healthCost;
        }

        int delay = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DELAY.node(), 0, false) / 1000;
        if (delay > 0) {
            description = description + " W:" + delay + "s";
        }

        int exp = SkillConfigManager.getUseSetting(hero, this, SkillSetting.EXP.node(), 0, false);
        if (exp > 0) {
            description = description + " XP:" + exp;
        }

        return description;
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 1000);
        node.set("duration-increase", 0);
        node.set(SkillSetting.MAX_DISTANCE.node(), 6);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE.node(), 0);
        return node;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        int maxDistance = (int)((double)SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE.node(), 6, false) + SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE.node(), 0.0D, false) * (double)hero.getSkillLevel(this));
        maxDistance = maxDistance > 0 ? maxDistance : 0;
        Block prev = null;
        BlockIterator iter = null;

        try {
            iter = new BlockIterator(player, maxDistance);
        } catch (IllegalStateException var12) {
            Messaging.send(player, "There was an error getting your blink location!", new Object[0]);
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        while(iter.hasNext()) {
            Block b = iter.next();
            if (!Util.transparentBlocks.contains(b.getType()) || !Util.transparentBlocks.contains(b.getRelative(BlockFace.UP).getType()) && !Util.transparentBlocks.contains(b.getRelative(BlockFace.DOWN).getType())) {
                break;
            }

            prev = b;
        }

        if (prev != null) {
            this.broadcastExecuteText(hero);
            long duration = (long)((double)SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 1000, false) + SkillConfigManager.getUseSetting(hero, this, "duration-increase", 0.0D, false) * (double)hero.getSkillLevel(this));
            duration = duration > 0L ? duration : 0L;
            Iterator i$ = hero.getEffects().iterator();

            while(i$.hasNext()) {
                Effect effect = (Effect)i$.next();
                if (effect.isType(EffectType.HARMFUL)) {
                    hero.removeEffect(effect);
                }
            }

            hero.addEffect(new InvulnerabilityEffect(this, player, duration));
            Location teleport = prev.getLocation().clone();
            teleport.setPitch(player.getLocation().getPitch());
            teleport.setYaw(player.getLocation().getYaw());
            player.teleport(teleport);
            return SkillResult.NORMAL;
        } else {
            Messaging.send(player, "No location to blink to.", new Object[0]);
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
    }
}
