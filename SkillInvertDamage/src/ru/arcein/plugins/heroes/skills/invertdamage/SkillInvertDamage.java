package ru.arcein.plugins.heroes.skills.invertdamage;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public class SkillInvertDamage extends ActiveSkill {

    public SkillInvertDamage(Heroes plugin) {
        super(plugin, "InvertDamage");

        this.setDescription("Вы обращаете весь получаемый урон в леченье и наоборот.");

        this.setIdentifiers(new String[]{"skill invertdamage"});

        this.setArgumentRange(0, 0);

        this.setTypes(new SkillType[]{SkillType.BUFFING, SkillType.HEALING});

        Bukkit.getPluginManager().registerEvents(new SkillInvertDamageListener(this), plugin);

    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 10000);

        node.set(SkillSetting.APPLY_TEXT.node(), "%hero% оборачивает урон в лечение и лечение в урон!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero% перестаёт манипулировать здоровьем!");

        return node;
    }

    public String getDescription(Hero hero) { return this.getDescription(); }

    public SkillResult use(Hero hero, String[] args) {

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);

        String applyText = SkillConfigManager.getUseSetting(hero, this, SkillSetting.APPLY_TEXT.node(),
                "%hero% оборачивает урон в лечение и лечение в урон!")
                .replace("%hero%", hero.getName());

        String expireText = SkillConfigManager.getUseSetting(hero,this, SkillSetting.EXPIRE_TEXT.node(),
                "%hero% перестаёт манипулировать здоровьем!")
                .replace("%hero%", hero.getName());

        InvertDamageEffect effect = new InvertDamageEffect(
                this, hero.getPlayer(),
                duration,
                applyText, expireText);

        hero.addEffect(effect);

        return SkillResult.NORMAL;
    }

    public class SkillInvertDamageListener implements Listener {

        private SkillInvertDamage skill;

        public SkillInvertDamageListener(SkillInvertDamage skill) {
            this.skill = skill;
        }

        @EventHandler(
                priority = EventPriority.LOWEST
        )
        public void onEntityDamage(EntityDamageEvent event) {
            if(event.isCancelled()) return;

            Entity entity = event.getEntity();

            if(!(entity instanceof Player)) return;

            Player player = (Player) entity;
            Hero hero = Heroes.getInstance().getCharacterManager().getHero(player);

            if(!hero.hasEffect("InvertDamage")) return;

            double damage = event.getDamage();
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + damage));
            event.setCancelled(true);
        }

        @EventHandler(
                priority = EventPriority.LOWEST
        )
        public void onEntityRegen(EntityRegainHealthEvent event) {
            if(event.isCancelled()) return;

            Entity entity = event.getEntity();

            if(!(entity instanceof Player)) return;

            Player player = (Player) entity;
            Hero hero = Heroes.getInstance().getCharacterManager().getHero(player);

            if(!hero.hasEffect("InvertDamage")) return;

            double regen = event.getAmount();
            player.setHealth(Math.max(0.0, player.getHealth() - regen));
            event.setCancelled(true);
        }
    }

    public class InvertedRegainHealthEvent extends EntityRegainHealthEvent{
        public InvertedRegainHealthEvent (Entity entity, double amount, RegainReason regainReason) {
            super(entity, amount, regainReason);
        }
    }

    public class InvertedEntityDamageEvent extends EntityDamageEvent{
        public InvertedEntityDamageEvent(Entity entity, DamageCause cause, double damage) {
            super(entity, cause, damage);
        }
    }

    public class InvertDamageEffect extends ExpirableEffect {

        private SkillInvertDamage skill;

        private double damageDealt;

        public InvertDamageEffect(SkillInvertDamage skill, Player player,
                            long duration,
                            String applyText, String expireText) {

            super(skill, "InvertDamage", player, duration, applyText, expireText);
            this.skill = skill;
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
        }

        public void removeFromHero(Hero hero) { super.removeFromHero(hero); }
    }


}