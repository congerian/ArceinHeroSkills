package ru.arcein.plugins.heroes.skills.iceaura;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.CompatSound;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillIceAura extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillIceAura(Heroes plugin) {
        super(plugin, "IceAura");
        this.setDescription("Переключаемая аура льда, наносящая $1 урона и замедляющая противников в радиусе $2 блоков.");
        this.setUsage("/skill iceaura");
        this.setArgumentRange(0, 0);
        this.setIdentifiers(new String[]{"skill iceaura"});
        this.setTypes(new SkillType[]{SkillType.ABILITY_PROPERTY_ICE, SkillType.SILENCEABLE,
                SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.DEBUFFING, SkillType.MOVEMENT_SLOWING});
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("on-text", "%hero% охлаждает воздух с помощью %skill%!");
        node.set("off-text", "%hero% останавливает %skill%!");
        node.set("tick-damage", 5.0D);
        node.set("period-ms", 1000L);
        node.set("radius", 10);
        node.set("slow-multiplier", 1);
        node.set("mana", 50.0D);
        node.set("mana-per-tick", 10.0D);
        return node;
    }

    public String getDescription(Hero hero) {

        double damage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 5.0D, false);
        double radius = SkillConfigManager.getUseSetting(hero, this, "radius", 10.0D, false);

        String description = this.getDescription().replace("$1", damage + "").replace("$2", radius + "");

        return description;
    }

    public void init() {
        super.init();

        this.applyText = SkillConfigManager.getRaw(this, "on-text",
                ChatComponents.GENERIC_SKILL + "%hero% охлаждает воздух с помощью %skill%!")
                .replace("%hero%", "$1")
                .replace("%skill%", "$2");

        this.expireText = SkillConfigManager.getRaw(this, "off-text",
                ChatComponents.GENERIC_SKILL + "%hero% stops his %skill%!")
                .replace("%hero%", "$1")
                .replace("%skill%", "$2");
    }

    public SkillResult use(Hero hero, String[] args) {
        if (hero.hasEffect("IceAura")) {
            hero.removeEffect(hero.getEffect("IceAura"));
            return SkillResult.REMOVED_EFFECT;
        } else {

            long period = (long)SkillConfigManager.getUseSetting(hero, this, "period-ms", 1000L, false);
            double tickDamage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 5.0D, false);
            double radius = SkillConfigManager.getUseSetting(hero, this, "radius", 10.0D, false);
            double manaPerTick = SkillConfigManager.getUseSetting(hero, this, "mana-per-tick", 10.0D, false);
            int slowMultiplier = (int)SkillConfigManager.getUseSetting(hero, this, "slow-multiplier", 1, false);

            hero.addEffect(new SkillIceAura.IcyAuraEffect(this, period, tickDamage, hero.getPlayer(), radius, manaPerTick, slowMultiplier));
        }

        return SkillResult.NORMAL;
    }

    public class IcyAuraEffect extends PeriodicEffect {

        private SkillIceAura skill;

        private double tickDamage;
        private double radius;
        private double manaPerTick;
        private boolean firstTime = true;
        private long period;
        private int slowMultiplier;

        public IcyAuraEffect(SkillIceAura skill, long period, double tickDamage, Player player,
                             double radius, double manaPerTick, int slowMultiplier) {
            super(skill, "IceAura", player, period);

            this.skill = skill;

            this.tickDamage = tickDamage;
            this.radius = radius;
            this.manaPerTick = manaPerTick;
            this.period = period;
            this.slowMultiplier = slowMultiplier;

            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.ICE);
        }

        public ArrayList<Location> circle(Location centerPoint, int particleAmount, double circleRadius) {
            World world = centerPoint.getWorld();
            double increment = 6.283185307179586D / (double)particleAmount;
            ArrayList<Location> locations = new ArrayList();

            for(int i = 0; i < particleAmount; ++i) {
                double angle = (double)i * increment;
                double x = centerPoint.getX() + circleRadius * Math.cos(angle);
                double z = centerPoint.getZ() + circleRadius * Math.sin(angle);
                locations.add(new Location(world, x, centerPoint.getY(), z));
            }

            return locations;
        }

        public void applyToHero(Hero hero) {
            this.firstTime = true;
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            this.broadcast(player.getLocation(), skill.applyText, new Object[]{player.getDisplayName(), "IceAura"});
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            this.broadcast(player.getLocation(), skill.expireText, new Object[]{player.getDisplayName(), "IceAura"});
        }

        public void tickHero(Hero hero) {
            super.tickHero(hero);

            Player player = hero.getPlayer();

            ArrayList<Location> particleLocations = this.circle(player.getLocation(), 36, radius);

            for(Location loc : particleLocations){
                player.getWorld().spigot().playEffect(loc, Effect.TILE_BREAK,
                        Material.ICE.getId(), 0, 0.2F, 1.0F, 0.2F, 0.01F, 10, 16);
            }

            player.getWorld().playSound(player.getLocation(), CompatSound.BLOCK_GLASS_BREAK.value(), 7.0F, 0.7F);

            List<Entity> entities = player.getNearbyEntities(radius, radius, radius);

            for(Entity entity : entities){
                if (entity instanceof LivingEntity) {

                    LivingEntity target = (LivingEntity) entity;

                    if (!Skill.damageCheck(player, target)) {
                        continue;
                    }

                    target.setFireTicks(0);

                    if(slowMultiplier > 0){
                        SlowEffect iceSlowEffect = new SlowEffect(skill, player, period, slowMultiplier,
                                "", "");

                        iceSlowEffect.types.add(EffectType.DISPELLABLE);
                        iceSlowEffect.types.add(EffectType.ICE);
                        iceSlowEffect.types.add(EffectType.HARMFUL);
                        iceSlowEffect.types.add(EffectType.SLOW);

                        if(entity instanceof Player){
                            skill.plugin.getCharacterManager().getHero((Player) entity).addEffect(iceSlowEffect);
                        }
                    }

                    skill.addSpellTarget(target, hero);
                    skill.damageEntity(target, player, tickDamage, DamageCause.MAGIC);
                }
            }

            if (this.manaPerTick > 0 && !this.firstTime) {
                if (hero.getMana() - manaPerTick < 0) {
                    hero.setMana(0);
                } else {
                    hero.setMana((int)(hero.getMana() - manaPerTick));
                }
            } else if (this.firstTime) {
                this.firstTime = false;
            }

            if (hero.getMana() < this.manaPerTick) {
                hero.removeEffect(this);
            }

        }
    }
}
