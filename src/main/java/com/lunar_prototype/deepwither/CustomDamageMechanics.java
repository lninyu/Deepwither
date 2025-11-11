package com.lunar_prototype.deepwither;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.bukkit.BukkitAdapter;
import org.bukkit.entity.LivingEntity;

public class CustomDamageMechanics implements ITargetedEntitySkill {
    protected final int damage;
    protected final int multiplier;

    public CustomDamageMechanics(MythicLineConfig config) {
        this.damage = config.getInteger(new String[] {"damage", "d"}, 1);
        this.multiplier = config.getInteger(new String[] {"multiplier", "mp"}, 0);
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
        LivingEntity bukkitTarget = (LivingEntity) BukkitAdapter.adapt(target);

        double tempdamage = damage * multiplier;
        int final_damage = Integer.parseInt(String.valueOf(tempdamage));

        bukkitTarget.damage(final_damage,data.getCaster().getEntity().getBukkitEntity());

        return SkillResult.SUCCESS;
    }
}
