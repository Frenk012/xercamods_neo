package xerca.xercapaint;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Mod.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> STROKE_LOOP =
            SOUND_EVENTS.register("stroke_loop", () -> SoundEvent.createVariableRangeEvent(Mod.id("stroke_loop")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MIX =
            SOUND_EVENTS.register("mix", () -> SoundEvent.createVariableRangeEvent(Mod.id("mix")));
    public static final DeferredHolder<SoundEvent, SoundEvent> COLOR_PICKER =
            SOUND_EVENTS.register("color_picker", () -> SoundEvent.createVariableRangeEvent(Mod.id("color_picker")));
    public static final DeferredHolder<SoundEvent, SoundEvent> COLOR_PICKER_SUCK =
            SOUND_EVENTS.register("color_picker_suck", () -> SoundEvent.createVariableRangeEvent(Mod.id("color_picker_suck")));
    public static final DeferredHolder<SoundEvent, SoundEvent> WATER =
            SOUND_EVENTS.register("water", () -> SoundEvent.createVariableRangeEvent(Mod.id("water")));
    public static final DeferredHolder<SoundEvent, SoundEvent> WATER_DROP =
            SOUND_EVENTS.register("water_drop", () -> SoundEvent.createVariableRangeEvent(Mod.id("water_drop")));
}
