package dev.nova.module;

import dev.nova.module.client.*;
import dev.nova.module.combat.*;
import dev.nova.module.render.*;
import dev.nova.module.utility.*;
import dev.nova.util.Config;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();

    private ModuleManager() {}

    public static void init() {
        register(new StorageFinder());
        register(new EntityEsp());
        register(new BaseFinder());
        register(new AncientDebrisFinder());
        register(new SpawnerFinder());
        register(new OreFinder());
        register(new RespawnAnchorFinder());
        register(new AutoTotem());
        register(new FastCrystal());
        register(new AutoSafeAnchor());
        register(new Freecam());
        register(new Trails());
        register(new AutoRtp());
        register(new DonutWheel());
        register(new RegionMap());
        register(new GuiSettings());
        Config.load();
    }

    private static void register(Module m) {
        MODULES.add(m);
        if (!m.toggleable()) m.setEnabled(true);
    }

    public static List<Module> all() { return MODULES; }

    public static List<Module> in(Module.Category category) {
        List<Module> out = new ArrayList<>();
        for (Module m : MODULES) if (m.category == category) out.add(m);
        return out;
    }

    public static <T extends Module> T get(Class<T> type) {
        for (Module m : MODULES) if (type.isInstance(m)) return type.cast(m);
        return null;
    }

    public static void tick(Minecraft mc) {
        for (Module m : MODULES) if (m.enabled()) m.onTick(mc);
    }

    public static void preTick(Minecraft mc) {
        for (Module m : new ArrayList<>(MODULES)) if (m.enabled()) m.onPreTick(mc);
    }

    public static void render(Minecraft mc) {
        for (Module m : new ArrayList<>(MODULES)) {
            if (!m.enabled()) continue;
            m.onFrame(mc);
            if (m instanceof EspModule esp) esp.render(mc);
        }
    }
}
