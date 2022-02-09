package ru.arcein.plugins.heroes.skills.holyaura;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;

import com.herocraftonline.heroes.chat.ChatComponents;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SkillHolyAura extends ActiveSkill {

    public SkillHolyAura(Heroes plugin) {
        super(plugin, "HolyAura");
        this.setDescription("Переключаемая аура лечения, лечащая сюзников в пати.");
        this.setUsage("/skill HolyAura");
        this.setArgumentRange(0, 0);
        this.setIdentifiers(new String[]{"skill HolyAura"});
        this.setTypes(new SkillType[]{SkillType.ABILITY_PROPERTY_LIGHT, SkillType.SILENCEABLE,
                SkillType.HEALING});
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("on-text", "%hero% начинает испускать %skill%!");
        node.set("off-text", "%hero% останавливает %skill%!");

        node.set("healing-per-tick", 5.0D);
        node.set("period-ms", 1000L);

        node.set("mana", 50.0D);
        node.set("mana-per-tick", 10);

        return node;
    }

    public String getDescription(Hero hero) {
        return this.getDescription();
    }

    public void init() {
        super.init();
    }

    public SkillResult use(Hero hero, String[] args) {

        if (hero.hasEffect("HolyAura")) {
            hero.removeEffect(hero.getEffect("HolyAura"));
            return SkillResult.REMOVED_EFFECT;
        } else {

            double healingPerTick = SkillConfigManager.getUseSetting(hero, this, "healing-per-tick", 5.0D, false);
            double radius         = SkillConfigManager.getUseSetting(hero, this, "radius", 10.0D, false);
            int    manaPerTick    = SkillConfigManager.getUseSetting(hero, this, "mana-per-tick", 10, false);
            long   periodMs       = SkillConfigManager.getUseSetting(hero, this, "period-ms", 1000, false);

            String applyText = SkillConfigManager.getRaw(this, "on-text",
                    ChatComponents.GENERIC_SKILL + "%hero% охлаждает воздух с помощью %skill%!")
                    .replace("%hero%", "$1")
                    .replace("%skill%", "$2");

            String expireText = SkillConfigManager.getRaw(this, "off-text",
                    ChatComponents.GENERIC_SKILL + "%hero% stops his %skill%!")
                    .replace("%hero%", "$1")
                    .replace("%skill%", "$2");

            hero.addEffect(new SkillHolyAuraEffect(this, hero.getPlayer(),
                            periodMs, manaPerTick,
                            radius, healingPerTick,
                            applyText, expireText));
        }

        return SkillResult.NORMAL;
    }

    public class SkillHolyAuraEffect extends PeriodicEffect {

        private double healingPerTick;
        private double radius;
        private int manaPerTick;



        public SkillHolyAuraEffect( SkillHolyAura skill, Player player,
                                    long period, int manaPerTick,
                                    double radius, double healingPerTick,
                                    String applyText, String expireText) {
            super(skill, "HolyAura", player, period, applyText, expireText);

            this.healingPerTick = healingPerTick;
            this.radius = radius;
            this.manaPerTick = manaPerTick;

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.HEALING);
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
        }


        public void tickHero(Hero hero) {
            super.tickHero(hero);

            Player player = hero.getPlayer();

            if (hero.hasParty()){
                for (Hero target : hero.getParty().getMembers()){
                    if(player.getLocation().distance(target.getPlayer().getLocation()) > radius) continue;
                    target.tryHeal(healingPerTick);
                }
            }

            hero.tryHeal(healingPerTick);

            if (hero.getMana() - manaPerTick < 0) {
                hero.setMana(0);
                hero.removeEffect(this);
            } else {
                hero.setMana((int)(hero.getMana() - manaPerTick));
            }

        }
    }
}
