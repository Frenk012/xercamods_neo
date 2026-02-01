package xerca.xercamusic.common.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import xerca.xercamusic.common.Mod;

import java.util.function.Supplier;

public class Entities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Mod.MODID);

    public static final Supplier<EntityType<EntityMusicSpirit>> MUSIC_SPIRIT = ENTITY_TYPES.register("music_spirit",
            () -> EntityType.Builder.<EntityMusicSpirit>of(EntityMusicSpirit::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .updateInterval(10)
                    .build("music_spirit"));
}
