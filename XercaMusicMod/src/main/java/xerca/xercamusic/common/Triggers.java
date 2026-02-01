package xerca.xercamusic.common;

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class Triggers {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(BuiltInRegistries.TRIGGER_TYPES, Mod.MODID);

    public static final Supplier<CustomTrigger> BECOME_MUSICIAN = TRIGGERS.register("become_musician", CustomTrigger::new);
}
