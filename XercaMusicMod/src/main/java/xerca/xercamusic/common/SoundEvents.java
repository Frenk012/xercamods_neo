package xerca.xercamusic.common;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import xerca.xercamusic.common.item.IItemInstrument;
import xerca.xercamusic.common.item.IItemInstrument.Pair;
import xerca.xercamusic.common.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Mod.MODID);

    // Core sounds
    public static final Supplier<SoundEvent> TICK = registerSound("tick");
    public static final Supplier<SoundEvent> METRONOME_SET = registerSound("metronome_set");
    public static final Supplier<SoundEvent> OPEN_SCROLL = registerSound("open_scroll");
    public static final Supplier<SoundEvent> CLOSE_SCROLL = registerSound("close_scroll");

    // Instrument sounds - registered dynamically
    public static ArrayList<Pair<Integer, SoundEvent>> cymbals;
    public static ArrayList<Pair<Integer, SoundEvent>> drum_kits;
    public static ArrayList<Pair<Integer, SoundEvent>> guitars;
    public static ArrayList<Pair<Integer, SoundEvent>> lyres;
    public static ArrayList<Pair<Integer, SoundEvent>> drums;
    public static ArrayList<Pair<Integer, SoundEvent>> flutes;
    public static ArrayList<Pair<Integer, SoundEvent>> banjos;
    public static ArrayList<Pair<Integer, SoundEvent>> saxophones;
    public static ArrayList<Pair<Integer, SoundEvent>> gods;
    public static ArrayList<Pair<Integer, SoundEvent>> harp_mcs;
    public static ArrayList<Pair<Integer, SoundEvent>> sansulas;
    public static ArrayList<Pair<Integer, SoundEvent>> tubular_bells;
    public static ArrayList<Pair<Integer, SoundEvent>> violins;
    public static ArrayList<Pair<Integer, SoundEvent>> xylophones;
    public static ArrayList<Pair<Integer, SoundEvent>> cellos;
    public static ArrayList<Pair<Integer, SoundEvent>> pianos;
    public static ArrayList<Pair<Integer, SoundEvent>> oboes;
    public static ArrayList<Pair<Integer, SoundEvent>> redstone_guitars;
    public static ArrayList<Pair<Integer, SoundEvent>> french_horns;
    public static ArrayList<Pair<Integer, SoundEvent>> bass_guitars;

    private static final String NAME_GUITAR = "guitar";
    private static final String NAME_DRUM_KIT = "drum_kit";
    private static final String NAME_LYRE = "lyre";
    private static final String NAME_BANJO = "banjo";
    private static final String NAME_DRUM = "drum";
    private static final String NAME_CYMBAL = "cymbal";
    private static final String NAME_XYLOPHONE = "xylophone";
    private static final String NAME_SANSULA = "sansula";
    private static final String NAME_TUBULAR_BELL = "tubular_bell";
    private static final String NAME_CELLO = "cello";
    private static final String NAME_VOICE_OF_GOD = "god";
    private static final String NAME_VIOLIN = "violin";
    private static final String NAME_FLUTE = "flute";
    private static final String NAME_SAXOPHONE = "saxophone";
    private static final String NAME_PIANO = "piano";
    private static final String NAME_OBOE = "oboe";
    private static final String NAME_REDSTONE_GUITAR = "redstone_guitar";
    private static final String NAME_FRENCH_HORN = "french_horn";
    private static final String NAME_BASS_GUITAR = "bass_guitar";
    private static final String NAME_HARP_MC = "harp_mc";

    private static Supplier<SoundEvent> registerSound(String name) {
        ResourceLocation id = Mod.id(name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    private static SoundEvent registerInstrumentSound(String soundName) {
        final ResourceLocation soundID = Mod.id(soundName);
        final SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(soundID);
        // Register to deferred register
        SOUND_EVENTS.register(soundName, () -> soundEvent);
        return soundEvent;
    }

    private static void addSound(List<Pair<Integer, SoundEvent>> array, String insName, int note) {
        array.add(Pair.of(note, registerInstrumentSound(insName + note)));
    }

    private static void addRange(List<Pair<Integer, SoundEvent>> array, String insName, int start, int end) {
        addRange(array, insName, start, end, 1);
    }

    private static void addRange(List<Pair<Integer, SoundEvent>> array, String insName, int start, int end, int step) {
        for (int i = start; i <= end; i += step) {
            addSound(array, insName, i);
        }
    }

    private static void addFixed(List<Pair<Integer, SoundEvent>> target, String instrumentName, int... notes) {
        for (int note : notes) {
            addSound(target, instrumentName, note);
        }
    }

    // Static block to register all instrument sounds during class loading
    static {
        // init lists
        cymbals = new ArrayList<>(48);
        drum_kits = new ArrayList<>(48);
        french_horns = new ArrayList<>(11);
        guitars = new ArrayList<>(48);
        drums = new ArrayList<>(48);
        redstone_guitars = new ArrayList<>(11);
        lyres = new ArrayList<>(48);
        flutes = new ArrayList<>(48);
        banjos = new ArrayList<>(48);
        saxophones = new ArrayList<>(48);
        gods = new ArrayList<>(48);
        oboes = new ArrayList<>(21);
        harp_mcs = new ArrayList<>(48);
        sansulas = new ArrayList<>(48);
        tubular_bells = new ArrayList<>(48);
        violins = new ArrayList<>(48);
        bass_guitars = new ArrayList<>(8);
        xylophones = new ArrayList<>(48);
        cellos = new ArrayList<>(48);
        pianos = new ArrayList<>(48);

        // ranges
        addRange(drum_kits, NAME_DRUM_KIT, 21, 116);
        addRange(harp_mcs, NAME_HARP_MC, 27, 111, 6);

        addFixed(guitars, NAME_GUITAR,
                24, 28, 34, 40, 48, 54, 55, 59, 65, 72, 78, 84, 90, 96, 102);

        addFixed(lyres, NAME_LYRE,
                33, 39, 45, 51, 57, 63, 69, 75, 81, 87, 93);

        addFixed(gods, NAME_VOICE_OF_GOD,
                27, 33, 39, 45, 51, 57, 63, 69, 75, 81, 87);

        addFixed(banjos, NAME_BANJO,
                27, 33, 39, 45, 51, 57, 63, 69, 75, 81);

        addFixed(drums, NAME_DRUM,
                33, 39, 43, 50, 55, 58, 63, 69, 75, 81);

        addFixed(cymbals, NAME_CYMBAL,
                27, 33, 39, 45, 51, 57, 63, 69, 75, 81);

        addFixed(xylophones, NAME_XYLOPHONE,
                27, 33, 39, 45, 51, 57, 63, 69, 75, 81, 87);

        addFixed(sansulas, NAME_SANSULA,
                33, 39, 45, 51, 57, 63, 69, 75, 81, 87);

        addFixed(tubular_bells, NAME_TUBULAR_BELL,
                33, 39, 45, 51, 57, 63, 69, 75, 81);

        addFixed(cellos, NAME_CELLO,
                25, 31, 37, 43, 49, 52, 55, 58, 61, 64, 67, 70, 73, 76, 79, 82, 85, 88, 91, 94, 97, 100);

        addFixed(violins, NAME_VIOLIN,
                37, 43, 49, 55, 58, 61, 64, 67, 70, 73, 76, 79, 82, 85, 88, 91);

        addFixed(flutes, NAME_FLUTE,
                35, 41, 51, 61, 63, 65, 68, 73, 76, 78, 80, 85, 88, 90, 100);

        addFixed(saxophones, NAME_SAXOPHONE,
                24, 30, 36, 38, 41, 45, 48, 50, 53, 57, 60, 65, 72, 78);

        addFixed(pianos, NAME_PIANO,
                27, 36, 40, 45, 56, 61, 66, 70, 73, 77, 82, 86, 90, 95, 103, 109);

        addFixed(oboes, NAME_OBOE,
                23, 29, 35, 37, 39, 40, 42, 44, 46, 48, 50, 52, 54, 55, 57, 59, 60, 62, 64, 65, 71);

        addFixed(redstone_guitars, NAME_REDSTONE_GUITAR,
                27, 33, 39, 45, 51, 57, 63, 69, 75, 81, 87);

        addFixed(french_horns, NAME_FRENCH_HORN,
                27, 33, 39, 45, 51, 57, 63, 69, 75, 81, 87);

        addFixed(bass_guitars, NAME_BASS_GUITAR,
                33, 39, 45, 51, 57, 63, 69, 75);
    }

    // Called during FMLCommonSetupEvent to set sounds on instruments
    public static void initInstrumentSounds() {
        ((IItemInstrument) Items.CYMBAL.get()).setSounds(cymbals);
        ((IItemInstrument) Items.DRUM_KIT.get()).setSounds(drum_kits);
        ((IItemInstrument) Items.GUITAR.get()).setSounds(guitars);
        ((IItemInstrument) Items.LYRE.get()).setSounds(lyres);
        ((IItemInstrument) Items.DRUM.get()).setSounds(drums);
        ((IItemInstrument) Items.FLUTE.get()).setSounds(flutes);
        ((IItemInstrument) Items.BANJO.get()).setSounds(banjos);
        ((IItemInstrument) Items.SAXOPHONE.get()).setSounds(saxophones);
        ((IItemInstrument) Items.GOD.get()).setSounds(gods);
        ((IItemInstrument) Items.SANSULA.get()).setSounds(sansulas);
        ((IItemInstrument) Items.TUBULAR_BELL.get()).setSounds(tubular_bells);
        ((IItemInstrument) Items.VIOLIN.get()).setSounds(violins);
        ((IItemInstrument) Items.XYLOPHONE.get()).setSounds(xylophones);
        ((IItemInstrument) Items.CELLO.get()).setSounds(cellos);
        ((IItemInstrument) Items.PIANO.get()).setSounds(pianos);
        ((IItemInstrument) Items.OBOE.get()).setSounds(oboes);
        ((IItemInstrument) Items.REDSTONE_GUITAR.get()).setSounds(redstone_guitars);
        ((IItemInstrument) Items.FRENCH_HORN.get()).setSounds(french_horns);
        ((IItemInstrument) Items.BASS_GUITAR.get()).setSounds(bass_guitars);

        ((IItemInstrument) Items.HARP_MC.get()).setSounds(harp_mcs);
    }
}
