package ru.arcein.plugins.heroes.skills.batswarm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Bat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;

public class SkillBatSwarm extends TargettedSkill
{
    public static SkillBatSwarm skill;

    public SkillBatSwarm(Heroes instance)
    {
        super(instance, "BatSwarm");
        skill=this;
        setDescription("You summon a swarm of bats, surrounding your enemy, dealing damage every second.");
        setUsage("/skill batswarm");
        setArgumentRange(0,0);
        setIdentifiers(new String[] { "skill batswarm" });
        setTypes(new SkillType[] { SkillType.AGGRESSIVE, SkillType.SUMMONING, SkillType.DEBUFFING, SkillType.DAMAGING });
        //Bukkit.getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    public String getDescription(Hero hero)
    {
        return super.getDescription();
    }

    public ConfigurationSection getDefaultConfig()
    {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), 70);
        node.set(SkillSetting.DURATION.node(), 15000);
        return node;
    }

    @Override
    public SkillResult use(final Hero hero, final LivingEntity target, String[] args)
    {
        if (!damageCheck(hero.getPlayer(), target)) return SkillResult.INVALID_TARGET;

        double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 70, false);
        double duration = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DURATION, 15000, false);

        plugin.getCharacterManager().getCharacter(target).addEffect(new BatSwarmEffect(this, (long)duration, damage, hero.getPlayer(), target));

        return SkillResult.NORMAL;
    }

    public class BatSwarmEffect extends ExpirableEffect
    {
        List<LivingEntity> bats = new ArrayList<LivingEntity>();
        private final Player applier;
        private final int id;
        private final int id2;

        public BatSwarmEffect(Skill skill, long duration, final double damage, final Player applier, final LivingEntity target)
        {
            super(skill, "BatSwarm", applier, duration);
            this.applier = applier;
            for(int i=0;i<10;++i)
            {
                Bat bat = (Bat)applier.getWorld().spawnEntity(applier.getEyeLocation(), EntityType.BAT);
                bat.setMaxHealth(4095D);
                bat.setHealth(4095D);
                bats.add(bat);
            }
            id = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        boolean doneDamage = false;
                        Iterator<LivingEntity> itr = bats.iterator();
                        while(itr.hasNext())
                        {
                            LivingEntity bat = itr.next();
                            if(bat == null)
                                continue;
                            if(bat.isDead())
                                continue;

                            if(!doneDamage && bat.getNearbyEntities(0.5, 0.5, 0.5).contains(target))
                            {
                                doneDamage = true;
                                damageEntity(target, applier, damage);
                            }
                        }
                    }
                    catch(Exception e)
                    {
                    }
                }
            },0,20L);
            id2 = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable()
            {
                @Override
                public void run()
                {
                    Iterator<LivingEntity> itr = bats.iterator();
                    while(itr.hasNext())
                    {
                        LivingEntity bat = itr.next();
                        if(bat == null)
                            continue;
                        if(bat.isDead())
                            continue;
                        double dist = bat.getLocation().distance(target.getLocation());
                        if(target instanceof Player)
                        {
                            Player pTarget = (Player)target;
                            double percent = 1 / dist;
                            final double x = ((bat.getLocation().getX() - pTarget.getEyeLocation().getX()) * percent) * -1;
                            final double y = ((bat.getLocation().getY() - pTarget.getEyeLocation().getY()) * percent) * -1;
                            final double z = ((bat.getLocation().getZ() - pTarget.getEyeLocation().getZ()) * percent) * -1;
                            bat.teleport(bat.getLocation().add(x,y,z));
                        }
                        else
                        {
                            double percent = 1 / dist;
                            final double x = ((bat.getLocation().getX() - target.getLocation().getX()) * percent) * -1;
                            final double y = ((bat.getLocation().getY() - (target.getLocation().getY()+1)) * percent) * -1;
                            final double z = ((bat.getLocation().getZ() - target.getLocation().getZ()) * percent) * -1;
                            bat.teleport(bat.getLocation().add(x,y,z));
                        }
                    }
                }
            },3,1L);
        }

        @Override
        public void applyToHero(Hero hero)
        {
            //Messaging.send(hero.getPlayer(), ChatColor.DARK_RED + applier.getName() + ChatColor.GRAY + " is draining your life!");
            super.applyToHero(hero);
        }

        @Override
        public void removeFromHero(Hero hero)
        {
            Bukkit.getScheduler().cancelTask(id);
            Bukkit.getScheduler().cancelTask(id2);
            Iterator<LivingEntity> itr = bats.iterator();
            while(itr.hasNext())
            {
                LivingEntity bat = itr.next();
                if(bat == null)
                    continue;
                if(bat.isDead())
                    continue;
                bat.remove();
                itr.remove();
            }
            broadcast(hero.getPlayer().getLocation(), ChatColor.GRAY + "[" + ChatColor.DARK_RED + "Skill" + ChatColor.GRAY + "] The " + ChatColor.WHITE + "Bat Swarm " + ChatColor.GRAY +
                    "summoned by " + ChatColor.DARK_RED + applier.getName() + ChatColor.GRAY + " has perished!");
            super.removeFromHero(hero);
        }

        @Override
        public void removeFromMonster(Monster monster)
        {
            Bukkit.getScheduler().cancelTask(id);
            Bukkit.getScheduler().cancelTask(id2);
            Iterator<LivingEntity> itr = bats.iterator();
            while(itr.hasNext())
            {
                LivingEntity bat = itr.next();
                if(bat == null)
                    continue;
                if(bat.isDead())
                    continue;
                bat.remove();
                itr.remove();
            }
            broadcast(monster.getEntity().getLocation(), ChatColor.GRAY + "[" + ChatColor.DARK_RED + "Skill" + ChatColor.GRAY + "] The " + ChatColor.WHITE + "Bat Swarm " + ChatColor.GRAY +
                    "summoned by " + ChatColor.DARK_RED + applier.getName() + ChatColor.GRAY + " has perished!");
            super.removeFromMonster(monster);
        }

		/*private Vector getTargetVector(Location shooter, Location target)
		{
			Location first_location = shooter.add(0, 1, 0);
			Location second_location = target.add(0, 1, 0);
			Vector vector = second_location.toVector().subtract(first_location.toVector());
			return vector;
		}*/
    }
}