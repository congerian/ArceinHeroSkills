package ru.arcein.plugins.heroes.skills.paranoia;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.BlindEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SkillParanoia extends ActiveSkill {

    public SkillParanoia(Heroes plugin) {
        super(plugin, "Paranoia");
        this.setDescription("Вы начинаете испускать аура страха, ослепляя противников вокруг вас.");
        this.setUsage("/skill paranoia");
        this.setArgumentRange(0, 1);
        this.setIdentifiers(new String[]{"skill paranoia"});
        this.setTypes(new SkillType[]{SkillType.DEBUFFING, SkillType.AGGRESSIVE, SkillType.SILENCEABLE, SkillType.BLINDING});
    }

    public String getDescription(Hero hero) { return this.getDescription(); }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 10000);

        node.set("aura-radius", 5.0);
        node.set("blindness-period", 1000);
        node.set("blindness-duration", 2000);

        node.set(SkillSetting.APPLY_TEXT.node(), "%hero% начинает испускать ауру ужаса!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero% перестаёт испускать ауру ужаса!");
        return node;
    }

    public void init() { super.init(); }

    public SkillResult use(Hero hero, String[] strings) {
        Player player = hero.getPlayer();

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        long blindnessPeriod = SkillConfigManager.getUseSetting(hero, this, "blindness-period", 1000, false);
        long blindnessDuration = SkillConfigManager.getUseSetting(hero, this, "blindness-duration", 2000, false);
        double auraRadius = SkillConfigManager.getUseSetting(hero, this, "aura-radius", 5.0, false);

        String applyText = SkillConfigManager.getUseSetting(hero, this, SkillSetting.APPLY_TEXT.node(),
                "%hero% начинает испускать ауру ужаса!")
                .replace("%hero%", hero.getName());

        String expireText = SkillConfigManager.getUseSetting(hero, this, SkillSetting.EXPIRE_TEXT.node(),
                "%hero% перестаёт испускать ауру ужаса!")
                .replace("%hero%", hero.getName());

        ParanoiaAuraEffect effect = new ParanoiaAuraEffect(
                this, player,
                duration, blindnessPeriod,
                applyText, expireText,
                blindnessDuration, auraRadius);

        hero.addEffect(effect);

        return SkillResult.NORMAL;

    }

    public class ParanoiaAuraEffect extends PeriodicExpirableEffect {
        private SkillParanoia skill;
        private Player caster;

        private long blindnessDuration;
        private double auraRadius;

        public ParanoiaAuraEffect(SkillParanoia skill, Player caster,
                            long duration, long period,
                            String applyText, String expireText,
                            long blindnessDuration, double auraRadius) {

            super(skill, "ParanoiaAura", caster, period, duration, applyText, expireText);

            this.skill = skill;
            this.caster = caster;
            this.blindnessDuration = blindnessDuration;
            this.auraRadius = auraRadius;

            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
        }

        public void tickHero(Hero hero) {
            Player casterPlayer = hero.getPlayer();

            List<Player> affectedPlayers = new ArrayList<>();

            for(Entity entity : casterPlayer.getNearbyEntities(auraRadius, auraRadius, auraRadius)){
                if(!(entity instanceof Player)) continue;
                if(!damageCheck(casterPlayer, (LivingEntity) entity)) continue;
                affectedPlayers.add((Player) entity);
            }

            BlindEffect effect = new BlindEffect(
                    skill, caster,
                    blindnessDuration,
                    "", ""
            );

            for(Player player : affectedPlayers){
                Heroes.getInstance().getCharacterManager().getHero(player).addEffect(effect);
            }
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
        }

        public void tickMonster(Monster mnstr) {
            return;
        }
    }

}