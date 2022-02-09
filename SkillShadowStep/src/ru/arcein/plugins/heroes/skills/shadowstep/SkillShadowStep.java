package ru.arcein.plugins.heroes.skills.shadowstep;

import com.comphenix.net.sf.cglib.core.Local;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SkillShadowStep extends TargettedSkill {

    public SkillShadowStep(Heroes plugin) {
        super(plugin, "ShadowStep");
        this.setDescription("Вы появляетесь в тени противника.");
        this.setUsage("/skill ShadowStep <target>");
        this.setArgumentRange(0, 1);
        this.setIdentifiers(new String[]{"skill ShadowStep"});
        this.setTypes(new SkillType[]{SkillType.TELEPORTING,
                SkillType.AGGRESSIVE, SkillType.SILENCEABLE});

    }

    public String getDescription(Hero hero) { return this.getDescription(); }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 10);

        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "");

        return node;
    }

    public void init() { super.init(); }

    public SkillResult use(Hero hero, LivingEntity target, String[] strings) {
        Player player = hero.getPlayer();

        if (!(target instanceof Player)) return SkillResult.INVALID_TARGET;
        if (!damageCheck(player, target)) return SkillResult.INVALID_TARGET;

        Hero targetHero = this.plugin.getCharacterManager().getHero((Player)target);

        String applyText = SkillConfigManager.getUseSetting(hero, this, SkillSetting.APPLY_TEXT.node(),
                "")
                .replace("%hero%", hero.getName())
                .replace("%target%", targetHero.getName());

        String expireText = SkillConfigManager.getUseSetting(hero, this, SkillSetting.EXPIRE_TEXT.node(),
                "")
                .replace("%hero%", hero.getName())
                .replace("%target%", targetHero.getName());


        Vector eyeDirection = target.getEyeLocation().getDirection().clone().setY(0).normalize();

        Location backLocation = target.getLocation().clone().add(eyeDirection.clone().multiply(-0.5));
        backLocation.setDirection(eyeDirection);

        player.teleport(backLocation);

        return SkillResult.NORMAL;

    }

}