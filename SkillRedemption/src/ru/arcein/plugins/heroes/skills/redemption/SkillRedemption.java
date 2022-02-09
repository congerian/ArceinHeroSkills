package ru.arcein.plugins.heroes.skills.redemption;

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

public class SkillRedemption extends TargettedSkill {

    public SkillRedemption(Heroes plugin) {
        super(plugin, "Redemption");
        this.setDescription("Вы жертвуете частью здоровья, нанося урон цели.");
        this.setUsage("/skill redemption <target>");
        this.setArgumentRange(0, 1);
        this.setIdentifiers(new String[]{"skill redemption"});
        this.setTypes(new SkillType[]{SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.SILENCEABLE, SkillType.NO_SELF_TARGETTING});
    }

    public String getDescription(Hero hero) { return this.getDescription(); }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 15);

        node.set("self-damage", 15.0);
        node.set("target-damage", 10.0);

        return node;
    }

    public void init() { super.init(); }

    public SkillResult use(Hero hero, LivingEntity target, String[] strings) {
        Player player = hero.getPlayer();

        if (!(target instanceof Player)) return SkillResult.INVALID_TARGET;
        if (!damageCheck(player, target)) return SkillResult.INVALID_TARGET;

        Player attackerPlayer = hero.getPlayer();
        Player attackedPlayer = (Player)target;

        Hero attackerHero = hero;
        Hero attackedHero = this.plugin.getCharacterManager().getHero(attackedPlayer);

        double selfDamage = SkillConfigManager.getUseSetting(hero, this, "self-damage", 15.0, false);
        double targetDamage = SkillConfigManager.getUseSetting(hero, this, "target-damage", 10.0, false);

        attackerPlayer.damage(selfDamage);
        this.addSpellTarget(attackedPlayer, attackerHero);
        this.damageEntity(attackedPlayer, attackerPlayer, targetDamage, DamageCause.MAGIC);

        return SkillResult.NORMAL;
    }

}