package ru.arcein.plugins.heroes.skills.tether;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.classes.HeroClass.ExperienceType;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

public class SkillTether extends ActiveSkill {
    private String applyText;
    private String expireText;
    private HashMap<Player, Player> affectedPlayers = new HashMap();

    public SkillTether(Heroes plugin) {
        super(plugin, "Tether");
        this.setDescription("Привязывает противников вокруг вас к вам на $1 секунд.");
        this.setUsage("/skill tether");
        this.setArgumentRange(0, 0);
        this.setIdentifiers(new String[]{"skill tether"});
        this.setTypes(new SkillType[]{SkillType.DEBUFFING, SkillType.DISABLE_COUNTERING, SkillType.MOVEMENT_PREVENTING, SkillType.ABILITY_PROPERTY_PHYSICAL});
    }

    public String getDescription(Hero hero) {
        long duration = (long)((double)SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 10000, false) + SkillConfigManager.getUseSetting(hero, this, "duration-increase", 0.0D, false) * (double)hero.getSkillLevel(this)) / 1000L;
        duration = duration > 0L ? duration : 0L;
        String description = this.getDescription().replace("$1", duration + "");
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

        int staminaCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA.node(), 0, false) - SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA_REDUCE.node(), 0, false) * hero.getSkillLevel(this);
        if (staminaCost > 0) {
            description = description + " FP:" + staminaCost;
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
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set("duration-increase", 0);
        node.set("exp-per-creature-tethered", 0);
        node.set("exp-per-player-tethered", 0);
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% was tethered!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% got away!");
        return node;
    }

    public void init() {
        super.init();
        this.applyText = SkillConfigManager.getUseSetting((Hero)null, this, SkillSetting.APPLY_TEXT.node(), "%target% was tethered!").replace("%target%", "$1");
        this.expireText = SkillConfigManager.getUseSetting((Hero)null, this, SkillSetting.EXPIRE_TEXT.node(), "%target% got away!").replace("%target%", "$1");
    }

    public SkillResult use(Hero hero, String[] args) {
        List<Entity> entities = hero.getPlayer().getNearbyEntities(3.0D, 3.0D, 3.0D);
        Player player = hero.getPlayer();
        double expCreature = (double)SkillConfigManager.getUseSetting(hero, this, "exp-per-creature-tethered", 0, false);
        double expPlayer = (double)SkillConfigManager.getUseSetting(hero, this, "exp-per-player-tethered", 0, false);
        double exp = 0.0D;
        long duration = (long)((double)SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 10000, false) + SkillConfigManager.getUseSetting(hero, this, "duration-increase", 0.0D, false) * (double)hero.getSkillLevel(this));
        duration = duration > 0L ? duration : 0L;
        Iterator i$ = entities.iterator();

        while(i$.hasNext()) {
            Entity n = (Entity)i$.next();
            if (n instanceof Monster) {
                ((Monster)n).setTarget(hero.getPlayer());
                exp += expCreature;
            } else if (n instanceof Player && n != player && Skill.damageCheck(player, (LivingEntity)n)) {
                SkillTether.CurseEffect cEffect = new SkillTether.CurseEffect(this, duration, hero.getPlayer());
                Hero tHero = this.plugin.getCharacterManager().getHero((Player)n);
                tHero.addEffect(cEffect);
                exp += expPlayer;
            }
        }

        if (exp > 0.0D) {
            if (hero.hasParty()) {
                hero.getParty().gainExp(exp, ExperienceType.SKILL, player.getLocation());
            } else {
                hero.gainExp(exp, ExperienceType.SKILL, player.getLocation());
            }
        }

        i$ = hero.getEffects().iterator();

        while(true) {
            Effect e;
            do {
                if (!i$.hasNext()) {
                    this.broadcastExecuteText(hero);
                    return SkillResult.NORMAL;
                }

                e = (Effect)i$.next();
            } while(!e.isType(EffectType.DISABLE) && !e.isType(EffectType.ROOT) && !e.isType(EffectType.STUN));

            hero.removeEffect(e);
        }
    }

    public class CurseEffect extends PeriodicExpirableEffect {
        private Player caster;

        public CurseEffect(Skill skill, long duration, Player caster) {
            super(skill, "Tether", caster, 20L, duration);
            this.types.add(EffectType.HARMFUL);
            this.types.add(EffectType.PHYSICAL);
            this.caster = caster;
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            SkillTether.this.affectedPlayers.put(player, this.caster);
            this.broadcast(player.getLocation(), SkillTether.this.applyText, new Object[]{player.getDisplayName()});
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            if (SkillTether.this.affectedPlayers.containsKey(player)) {
                SkillTether.this.affectedPlayers.remove(player);
                this.broadcast(player.getLocation(), SkillTether.this.expireText, new Object[]{player.getDisplayName()});
            }

        }

        public void tickHero(Hero hero) {
            Player player = hero.getPlayer();

            try {
                if (player.getLocation().distance(this.caster.getLocation()) > 5.0D) {
                    player.teleport(this.caster);
                }
            } catch (IllegalArgumentException var4) {
            }

        }

        public void tickMonster(com.herocraftonline.heroes.characters.Monster mnstr) {
        }
    }
}