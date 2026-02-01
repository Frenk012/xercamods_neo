package xerca.xercapaint.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import xerca.xercapaint.Mod;

public class Entities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Mod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<EntityCanvas>> CANVAS =
            ENTITY_TYPES.register("canvas", () -> EntityType.Builder.<EntityCanvas>of(EntityCanvas::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(10)
                    .updateInterval(Integer.MAX_VALUE)
                    .build(Mod.id("canvas").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<EntityEasel>> EASEL =
            ENTITY_TYPES.register("easel", () -> EntityType.Builder.<EntityEasel>of(EntityEasel::new, MobCategory.MISC)
                    .sized(0.8f, 1.975f)
                    .clientTrackingRange(10)
                    .build(Mod.id("easel").toString()));
}
