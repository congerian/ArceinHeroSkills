package ru.arcein.plugins.heroes.skills.lullaby;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillLullaby extends TargettedSkill {

    public SkillLullaby(Heroes plugin) {
        super(plugin, "Lullaby");
        this.setDescription("Вы очаровываете цель песней, заставляя её уснуть на ходу.");
        this.setUsage("/skill lullaby <target>");
        this.setArgumentRange(0, 1);
        this.setIdentifiers(new String[]{"skill lullaby"});
        this.setTypes(new SkillType[]{SkillType.DISABLING, SkillType.INTERRUPTING, SkillType.STUNNING,
                SkillType.AGGRESSIVE, SkillType.SILENCEABLE, SkillType.BLINDING});
    }

    public String getDescription(Hero hero) { return this.getDescription(); }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set(SkillSetting.MAX_DISTANCE.node(), 5);
        node.set(SkillSetting.APPLY_TEXT.node(), "%hero% очаровывает песней %target%!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero% больше не очаровывает песней %target%!");
        return node;
    }

    public void init() { super.init(); }

    public SkillResult use(Hero hero, LivingEntity target, String[] strings) {
        Player player = hero.getPlayer();

        if (!(target instanceof Player)) return SkillResult.INVALID_TARGET;
        if (!damageCheck(player, target)) return SkillResult.INVALID_TARGET;

        Hero targetHero = this.plugin.getCharacterManager().getHero((Player)target);

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);

        String applyText = SkillConfigManager.getUseSetting(hero, this, SkillSetting.APPLY_TEXT.node(),
                "%hero% очаровывает песней %target%!")
                .replace("%hero%", hero.getName())
                .replace("%target%", targetHero.getName());

        String expireText = SkillConfigManager.getUseSetting(hero, this, SkillSetting.EXPIRE_TEXT.node(),
                "%hero% больше не очаровывает песней %target%!")
                .replace("%hero%", hero.getName())
                .replace("%target%", targetHero.getName());

        LullabyEffect effect = new LullabyEffect(
                this, player,
                duration,
                applyText, expireText);

        targetHero.addEffect(effect);

        return SkillResult.NORMAL;

    }

    public class LullabyEffect extends PeriodicExpirableEffect {
        private Location loc;

        public LullabyEffect(Skill skill, Player applier, long duration, String applyText, String expireText) {
            super(skill, "Lullaby", applier, 50L, duration, applyText, expireText);
            this.types.add(EffectType.STUN);
            this.types.add(EffectType.HARMFUL);
            this.types.add(EffectType.PHYSICAL);
            this.types.add(EffectType.DISABLE);
            this.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int)(20L * duration / 1000L), 127));
            this.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int)(20L * duration / 1000L), 0));
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();

            final int currentHunger = player.getFoodLevel();
            player.setFoodLevel(1);
            Bukkit.getServer().getScheduler().runTaskLater(this.plugin, new Runnable() {
                public void run() {
                    player.setFoodLevel(currentHunger);
                }
            }, 2L);

            this.loc = player.getLocation();
        }

        public void tickHero(Hero hero) {
            Player player = hero.getPlayer();
            if (!player.isDead() && !(player.getHealth() <= 0.0D)) {
                Location location = player.getLocation();
                if (location != null) {
                    if (location.getX() != this.loc.getX() || location.getY() != this.loc.getY()
                            || location.getZ() != this.loc.getZ() || location.getYaw() != this.loc.getYaw()
                            || location.getPitch() != this.loc.getPitch()) {
                        hero.getPlayer().teleport(this.loc);
                    }

                }
            } else {
                hero.removeEffect(this);
            }
        }

        public void tickMonster(Monster monster) {
        }
    }

}