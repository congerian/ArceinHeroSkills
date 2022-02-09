package ru.arcein.plugins.heroes.skills.equalize;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillEqualize extends TargettedSkill {

    public SkillEqualize(Heroes plugin) {
        super(plugin, "Equalize");
        this.setDescription("Восстанавливает здоровье вас или цели, до тех пор, пока ваше здоровье не сравняется.");
        this.setUsage("/skill Equalize <target>");
        this.setArgumentRange(0, 1);
        this.setIdentifiers(new String[]{"skill equalize"});
        this.setTypes(new SkillType[]{SkillType.HEALING, SkillType.SILENCEABLE});
    }

    public String getDescription(Hero hero) { return this.getDescription(); }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.MAX_DISTANCE.node(), 5);
        return node;
    }

    public void init() { super.init(); }

    public SkillResult use(Hero hero, LivingEntity target, String[] strings) {
        Player player = hero.getPlayer();

        if (!(target instanceof Player)) return SkillResult.INVALID_TARGET;

        Player targetPlayer = (Player) target;
        Hero targetHero = this.plugin.getCharacterManager().getHero((Player)target);

        if (!damageCheck(player, target)){
            if(!hero.hasParty()) return SkillResult.INVALID_TARGET;
            if(!hero.getParty().isPartyMember(targetPlayer)) return SkillResult.INVALID_TARGET;
        }

        if(target.getHealth() - player.getHealth() > 0.1){
            hero.tryHeal(target.getHealth() - player.getHealth());
        }
        else if(player.getHealth() - target.getHealth() > 0.1){
            targetHero.tryHeal(player.getHealth() - target.getHealth());
        }
        else{
            player.sendMessage("Ваше здоровье уже равняется здоровью цели.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        return SkillResult.NORMAL;

    }

}