package cc.polyfrost.coloredbedsdata;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.LocrawEvent;
import cc.polyfrost.oneconfig.events.event.ReceivePacketEvent;
import cc.polyfrost.oneconfig.events.event.WorldLoadEvent;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;
import cc.polyfrost.oneconfig.libs.universal.ChatColor;
import cc.polyfrost.oneconfig.libs.universal.UChat;
import cc.polyfrost.oneconfig.utils.Multithreading;
import cc.polyfrost.oneconfig.utils.NetworkUtils;
import cc.polyfrost.oneconfig.utils.hypixel.LocrawInfo;
import cc.polyfrost.oneconfig.utils.hypixel.LocrawUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBed;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class BedLocationHandler {
    public static final BedLocationHandler INSTANCE = new BedLocationHandler();
    private static final HashMap<String, Integer> COLORS = new HashMap<String, Integer>() {{
        put("yellow", 0);
        put("aqua", 1);
        put("white", 2);
        put("pink", 3);
        put("gray", 4);
        put("red", 5);
        put("blue", 6);
        put("green", 7);
    }};
    public static final HashMap<Integer, String> COLORS_REVERSE = new HashMap<Integer, String>() {{
        put(0, "yellow");
        put(1, "aqua");
        put(2, "white");
        put(3, "pink");
        put(4, "gray");
        put(5, "red");
        put(6, "blue");
        put(7, "green");
    }};
    private int[] defaultBedLocations = new int[]{0, 1, 2, 3, 4, 5, 6, 7};
    private boolean setDefault = false;
    private int[] bedLocations = new int[]{0, 1, 2, 3, 4, 5, 6, 7};
    private JsonObject locations = null;

    private final HashMap<Integer, EnumDyeColor> worldBedColors = new HashMap<>(10);

    private BedLocationHandler() {
        EventManager.INSTANCE.register(this);
    }

    public void initialize() {
        Multithreading.runAsync(() -> locations = NetworkUtils.getJsonElement("https://data.polyfrost.org/bed_locations.json").getAsJsonObject());
    }

    private int[] processColors(JsonArray array) {
        int[] colors = new int[8];
        int i = -1;
        for (JsonElement element : array) {
            i++;
            JsonPrimitive value = element.getAsJsonPrimitive();
            colors[i] = value.isString() ? COLORS.getOrDefault(value.getAsString(), 5) : value.getAsInt();
        }
        return colors;
    }

    public int[] getBedLocations() {
        LocrawInfo locrawInfo = LocrawUtil.INSTANCE.getLocrawInfo();
        if (locations == null || locrawInfo == null || locrawInfo.getGameType() != LocrawInfo.GameType.BEDWARS) {
            return (this.bedLocations = null);
        }
        if (!setDefault) {
            setDefault = true;
            COLORS_REVERSE.clear();
            int i = -1;
            for (JsonElement entry : locations.get("default").getAsJsonArray()) {
                i++;
                COLORS.put(entry.getAsString(), i);
                COLORS_REVERSE.put(i, entry.getAsString());
            }
            defaultBedLocations = COLORS_REVERSE.keySet().stream().mapToInt(Integer::intValue).toArray();
        }
        if (this.bedLocations != null) {
            return this.bedLocations;
        }
        JsonArray overrides = locations.getAsJsonArray("overrides");
        for (JsonElement override : overrides) {
            JsonObject overrideObject = override.getAsJsonObject();
            if (overrideObject.has("maps") && locrawInfo.getMapName() != null) {
                JsonArray maps = overrideObject.getAsJsonArray("maps");
                for (JsonElement map : maps) {
                    if (map.getAsString().equalsIgnoreCase(locrawInfo.getMapName())) {
                        return (this.bedLocations = processColors(overrideObject.getAsJsonArray("locations")));
                    }
                }
            }
            if (overrideObject.has("modes") && locrawInfo.getGameMode() != null) {
                JsonArray modes = overrideObject.getAsJsonArray("modes");
                for (JsonElement mode : modes) {
                    if (mode.getAsString().equalsIgnoreCase(locrawInfo.getGameMode())) {
                        this.bedLocations = processColors(overrideObject.getAsJsonArray("locations"));
                        return (this.bedLocations = processColors(overrideObject.getAsJsonArray("locations")));
                    }
                }
            }
        }
        return (this.bedLocations = defaultBedLocations);
    }

    @Subscribe
    private void onWorldLoad(WorldLoadEvent event) {
        worldBedColors.clear();
    }

    @Subscribe
    private void onSPacketUpdateTileEntity(ReceivePacketEvent event) {
        if (event.packet instanceof SPacketUpdateTileEntity) {
            SPacketUpdateTileEntity packet = (SPacketUpdateTileEntity) event.packet;
            if (Minecraft.getMinecraft().world.isBlockLoaded(packet.getPos())) {
                TileEntity tileentity = Minecraft.getMinecraft().world.getTileEntity(packet.getPos());
                if (tileentity instanceof TileEntityBed) {
                    TileEntityBed bed = (TileEntityBed) tileentity;
                    if (packet.getNbtCompound().hasKey("color")) {
                        worldBedColors.put((int)((Math.atan2(bed.getPos().getZ(), bed.getPos().getX()) + Math.PI * 4) / Math.toRadians(45)) % 8, EnumDyeColor.byMetadata(packet.getNbtCompound().getInteger("color")));
                    }
                }
            }
        }
    }

    @Subscribe
    private void onLocraw(LocrawEvent event) {
        if (event.info != LocrawUtil.INSTANCE.getLastLocrawInfo() && event.info.getGameType() == LocrawInfo.GameType.BEDWARS) {
            bedLocations = null;
            if (getBedLocations() != null) {
                for (Map.Entry<Integer, EnumDyeColor> entry : worldBedColors.entrySet()) {
                    int color = bedLocations[entry.getKey()];
                    if (!Objects.equals(Objects.equals(entry.getValue().getDyeColorName(), "cyan") ? "aqua" : entry.getValue().getDyeColorName(), COLORS_REVERSE.get(color))) {
                        UChat.chat(ChatColor.RED + "Bed color mismatch: " +
                                (Objects.equals(entry.getValue().getDyeColorName(), "cyan") ? "aqua" : entry.getValue().getDyeColorName()) +
                                " != " + COLORS_REVERSE.get(color));
                    }
                }
            }
        }
    }
}
