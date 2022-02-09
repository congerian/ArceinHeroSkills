package ru.arcein.plugins.heroes.skills.chaser;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillChaser extends TargettedSkill {

    public SkillChaser(Heroes plugin) {
        super(plugin, "Chaser");
        this.setDescription("Наносит урон цели, если она остаётся на месте.");
        this.setUsage("/skill chaser <target>");
        this.setArgumentRange(0, 1);
        this.setIdentifiers(new String[]{"skill chaser"});
        this.setTypes(new SkillType[]{SkillType.DEBUFFING, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.SILENCEABLE});
    }

    public String getDescription(Hero hero) { return this.getDescription(); }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set("damage-period", 1000);
        node.set("damage-per-tick", 1.0D);
        node.set(SkillSetting.MAX_DISTANCE.node(), 15);
        node.set(SkillSetting.APPLY_TEXT.node(), "%hero% раскаляет землю под ногами %target%!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero% перестаёт раскалять землю под ногами %target%!");
        return node;
    }

    public void init() { super.init(); }

    public SkillResult use(Hero hero, LivingEntity target, String[] strings) {
        Player player = hero.getPlayer();

        if (!(target instanceof Player)) return SkillResult.INVALID_TARGET;
        if (!damageCheck(player, target)) return SkillResult.INVALID_TARGET;


        Hero targetHero = this.plugin.getCharacterManager().getHero((Player)target);

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        long damagePeriod = SkillConfigManager.getUseSetting(hero, this, "damage-period", 1000, false);
        double damagePerTick = SkillConfigManager.getUseSetting(hero, this, "damage-per-tick", 1.0D, false);

        String applyText = SkillConfigManager.getUseSetting(hero, this, SkillSetting.APPLY_TEXT.node(),
                "%hero% раскаляет землю под ногами %target%!")
                .replace("%hero%", hero.getName())
                .replace("%target%", targetHero.getName());

        String expireText = SkillConfigManager.getUseSetting(hero, this, SkillSetting.EXPIRE_TEXT.node(),
                "%hero% перестаёт раскалять землю под ногами %target%!")
                .replace("%hero%", hero.getName())
                .replace("%target%", targetHero.getName());

         ChaserEffect effect = new ChaserEffect(
                this, player,
                 duration, damagePeriod,
                 applyText, expireText,
                 damagePerTick);

        targetHero.addEffect(effect);

        return SkillResult.NORMAL;

    }

    public class ChaserEffect extends PeriodicExpirableEffect {
        private SkillChaser skill;
        private Player caster;

        private Location prevLocation;
        private double damagePerTick;

        public ChaserEffect(SkillChaser skill, Player caster,
                             long duration, long period,
                             String applyText, String expireText,
                             double damagePerTick) {

            super(skill, "Chaser", caster, period, duration, applyText, expireText);

            this.skill = skill;
            this.caster = caster;
            this.damagePerTick = damagePerTick;

            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.HARMFUL);
        }

        public void tickHero(Hero hero) {
            if(!skill.damageCheck(hero.getEntity(), caster)) return;
            if (    Math.abs(hero.getPlayer().getLocation().getX() - this.prevLocation.getX()) < 1.0D
                    && Math.abs(hero.getPlayer().getLocation().getZ() - this.prevLocation.getZ()) < 1.0D)
            {
                skill.addSpellTarget(hero.getPlayer(), Heroes.getInstance().getCharacterManager().getHero(caster));
                skill.damageEntity(hero.getPlayer(), this.caster, this.damagePerTick, DamageCause.MAGIC, false);
            }

            this.prevLocation = hero.getPlayer().getLocation();
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            this.prevLocation = hero.getPlayer().getLocation();
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
        }

        public void tickMonster(Monster mnstr) {
            return;
        }
    }
}