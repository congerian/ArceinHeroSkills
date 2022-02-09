package ru.arcein.plugins.heroes.skills.manastorm;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;

public class SkillManaStorm extends ActiveSkill {
    public SkillManaStorm(Heroes plugin) {
        super(plugin, "ManaStorm");
        this.setDescription("Вы вызываете бурю в течении маны, нарушая потоки маны противников и нанося им ментальный урон.");
        this.setUsage("/skill ManaStorm");
        this.setArgumentRange(0, 0);
        this.setIdentifiers(new String[]{"skill ManaStorm"});
        this.setTypes(new SkillType[]{SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.MANA_DECREASING, SkillType.AGGRESSIVE});
    }

    public String getDescription(Hero hero) {
        return this.getDescription();
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 15);
        node.set("manastorm-radius", 5.0);
        node.set("min-mana-burst", 0.2);
        node.set("max-mana-burst", 0.5);
        node.set("burnt-mana-to-damage", 1.0);

        return node;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        Location loc = player.getLocation();

        int distance             = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 15, false);
        double manastormRadius   = SkillConfigManager.getUseSetting(hero, this, "manastorm-radius", 5.0, false);
        double minManaBurst      = SkillConfigManager.getUseSetting(hero, this, "min-mana-burst", 0.2, false);
        double maxManaBurst      = SkillConfigManager.getUseSetting(hero, this, "max-mana-burst", 0.5, false);
        double burntManaToDamage = SkillConfigManager.getUseSetting(hero, this, "burnt-mana-to-damage", 1.0, false);

        Block validFinalBlock = null;;
        BlockIterator iter = null;

        try {
            iter = new BlockIterator(player, distance);
        } catch (IllegalStateException err) {
            player.sendMessage("Не удалось вычислить ваше положение!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        while(iter.hasNext()) {
            Block currentBlock = iter.next();
            Material currentBlockType = currentBlock.getType();
            if (!Util.transparentBlocks.contains(currentBlockType)) {
                break;
            }

            validFinalBlock = currentBlock;
        }

        if (validFinalBlock == null) {
            player.sendMessage("Некорректное место для способности.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        Location targetLoc = validFinalBlock.getLocation().clone();

        Collection<Entity> entities = targetLoc.getWorld().getNearbyEntities(targetLoc, manastormRadius, manastormRadius, manastormRadius);

        for (Entity entity : entities){
            if (!(entity instanceof Player)) continue;

            Player targetPlayer = (Player) entity;
            Hero targetHero = Heroes.getInstance().getCharacterManager().getHero(targetPlayer);

            if (!damageCheck(player, (LivingEntity) targetPlayer)) continue;

            double manaBurst = minManaBurst + (maxManaBurst - minManaBurst) * Math.random();

            double manaBurnt = targetHero.getMana() * manaBurst;

            this.addSpellTarget(targetPlayer, hero);
            targetHero.setMana((int)(targetHero.getMana() - manaBurnt));
            this.damageEntity(targetPlayer, hero.getPlayer(), manaBurnt*burntManaToDamage, EntityDamageEvent.DamageCause.MAGIC);
        }

        return SkillResult.NORMAL;
    }
}
