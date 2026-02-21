package bodevelopment.client.blackout.module.modules.visual.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.*;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.manager.managers.StatsManager;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.timers.TimerList;
import bodevelopment.client.blackout.randomstuff.timers.TimerMap;
import bodevelopment.client.blackout.util.BoxUtils;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import bodevelopment.client.blackout.util.render.WireframeRenderer;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LogoutSpots extends Module {
    private static LogoutSpots INSTANCE;
    private final SettingGroup sgRendering = this.addGroup("Rendering");
    private final SettingGroup sgInfo = this.addGroup("Info");
    private final Setting<Boolean> model = this.sgRendering.b("Model", false, ".");
    private final Setting<RenderShape> renderShape = this.sgRendering.e("Render Shape", RenderShape.Full, ".");
    private final Setting<BlackOutColor> lineColor = this.sgRendering.c("Line Color", new BlackOutColor(255, 0, 0, 255), ".");
    private final Setting<BlackOutColor> sideColor = this.sgRendering.c("Side Color", new BlackOutColor(255, 0, 0, 50), ".");
    private final Setting<Double> maxTime = this.sgRendering.d("Max Time", 60.0, 0.0, 100.0, 1.0, ".");
    private final Setting<Double> fadeTime = this.sgRendering.d("Fade Time", 20.0, 0.0, 100.0, 1.0, ".");
    private final Setting<Double> infoScale = this.sgRendering.d("Info Scale", 1.0, 0.0, 2.0, 0.1, ".");
    private final Setting<Boolean> name = this.sgInfo.b("Names", true, ".");
    private final Setting<Boolean> armor = this.sgInfo.b("Armor", false, ".");
    private final Setting<Boolean> items = this.sgInfo.b("Items", false, ".");
    private final Setting<Boolean> health = this.sgInfo.b("Health", false, ".");
    private final Setting<Boolean> ping = this.sgInfo.b("Ping", false, ".");
    private final Setting<Boolean> pops = this.sgInfo.b("Pops", false, ".");
    private final Setting<Boolean> time = this.sgInfo.b("Time", false, ".");
    private final List<Spot> spots = new ArrayList<>();
    private final TimerMap<UUID, ItemStack[]> prevItems = new TimerMap<>(true);
    private final TimerList<UUID> removedUUIDs = new TimerList<>(true);
    private final TimerList<AbstractClientPlayerEntity> removedEntities = new TimerList<>(true);
    private final MatrixStack matrixStack = new MatrixStack();
    private float alphaMulti;

    public LogoutSpots() {
        super("Logout Spots", "Traces to other entities", SubCategory.MISC_VISUAL, true);
        INSTANCE = this;
    }

    public static LogoutSpots getInstance() {
        return INSTANCE;
    }

    @Event
    public void onGameJoin(GameJoinEvent event) {
        this.spots.clear();
    }

    @Override
    public void onEnable() {
        this.spots.clear();
    }

    @Event
    public void onJoin(GameJoinS2CPacket event) {
        this.spots.clear();
    }

    @Event
    public void onEntityRemove(PacketEvent.Receive.Pre event) {
        if (event.packet instanceof PlayerRemoveS2CPacket(List<UUID> profileIds)) {
            profileIds.stream().filter(this::checkMatchingEntities).forEach(uuid -> this.removedUUIDs.add(uuid, 1.0));
        }
    }

    @Event
    public void onRemove(RemoveEvent event) {
        if (event.entity instanceof AbstractClientPlayerEntity player && this.checkMatchingUUIDs(player)) {
            this.removedEntities.add(player, 1.0);
        }
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.world != null) {
            BlackOut.mc.world.getPlayers().forEach(player -> {
                UUID uuid = player.getGameProfile().getId();
                if (!this.itemsEmpty(player)) {
                    ItemStack[] stacks;
                    if (this.prevItems.containsKey(uuid)) {
                        stacks = this.prevItems.get(uuid);
                    } else {
                        stacks = new ItemStack[6];
                    }

                    for (int i = 0; i < 4; i++) {
                        stacks[i + 1] = player.getInventory().getArmorStack(3 - i);
                    }

                    stacks[0] = player.getMainHandStack();
                    stacks[5] = player.getOffHandStack();
                    this.prevItems.removeKey(uuid);
                    this.prevItems.add(uuid, stacks, 0.3);
                }
            });
        }
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            this.spots
                    .removeIf(
                            spot -> {
                                if (!spot.seen && this.anyPlayerMatches(spot.player.getGameProfile().getId())) {
                                    spot.setSeen();
                                }

                                this.setAlpha(spot);
                                if (this.alphaMulti <= 0.0F) {
                                    return true;
                                } else {
                                    if (this.model.get()) {
                                        WireframeRenderer.renderModel(
                                                spot.player,
                                                this.lineColor.get().alphaMulti(this.alphaMulti),
                                                this.sideColor.get().alphaMulti(this.alphaMulti),
                                                this.renderShape.get(),
                                                spot.tickDelta
                                        );
                                    } else {
                                        Render3DUtils.box(
                                                spot.player.getBoundingBox(),
                                                this.sideColor.get().alphaMulti(this.alphaMulti),
                                                this.lineColor.get().alphaMulti(this.alphaMulti),
                                                this.renderShape.get()
                                        );
                                    }

                                    return System.currentTimeMillis() - spot.logTime > (this.maxTime.get() + this.fadeTime.get()) * 1000.0;
                                }
                            }
                    );
        }
    }

    @Event
    public void onRender2D(RenderEvent.Hud.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            GlStateManager._disableDepthTest();
            GlStateManager._enableBlend();
            GlStateManager._disableCull();
            this.matrixStack.push();
            RenderUtils.unGuiScale(this.matrixStack);
            this.spots.forEach(this::renderInfo);
            this.matrixStack.pop();
        }
    }

    private boolean anyPlayerMatches(UUID uuid) {
        for (AbstractClientPlayerEntity player : BlackOut.mc.world.getPlayers()) {
            if (player.getGameProfile().getId().equals(uuid)) {
                return true;
            }
        }

        return false;
    }

    private boolean itemsEmpty(AbstractClientPlayerEntity player) {
        for (int i = 0; i < 4; i++) {
            ItemStack armorStack = player.getInventory().getArmorStack(3 - i);
            if (!armorStack.isEmpty()) {
                return false;
            }
        }

        return player.getMainHandStack().isEmpty() && player.getOffHandStack().isEmpty();
    }

    private void setAlpha(Spot spot) {
        float time = (float) (System.currentTimeMillis() - spot.logTime) / 1000.0F;
        if (time <= this.maxTime.get()) {
            this.alphaMulti = 1.0F;
        } else {
            this.alphaMulti = 1.0F - (time - this.maxTime.get().floatValue()) / this.fadeTime.get().floatValue();
        }

        if (spot.seen) {
            this.alphaMulti = this.alphaMulti * Math.max(1.0F - (float) (System.currentTimeMillis() - spot.seenSince) / 300.0F, 0.0F);
        }
    }

    private void renderInfo(Spot spot) {
        this.setAlpha(spot);
        int textColor = ColorUtils.withAlpha(-1, (int) (this.alphaMulti * 255.0F));
        Vec3d pos = this.infoPos(spot);
        Vec2f coords = RenderUtils.getCoords(pos.getX(), pos.getY(), pos.getZ(), true);
        if (coords != null) {
            this.matrixStack.push();
            this.matrixStack.translate(coords.x, coords.y, 0.0F);
            float scale = this.infoScale(pos) * this.infoScale.get().floatValue();
            this.matrixStack.scale(scale, scale, 1.0F);
            this.matrixStack.translate(0.0, -this.infoHeight(spot) / 2.0, 0.0);
            if (this.itemsFound(spot)) {
                this.renderArmorAndItems(spot, textColor);
            }

            if (this.name.get()) {
                this.renderName(spot, textColor);
            }

            if (this.health.get() || this.ping.get() || this.pops.get()) {
                this.renderInfoText(spot, textColor);
            }

            if (this.time.get()) {
                this.renderTime(spot, textColor);
            }

            this.matrixStack.pop();
        }
    }

    private void renderTime(Spot spot, int textColor) {
        BlackOut.FONT
                .text(this.matrixStack, String.format("%.1fs", (System.currentTimeMillis() - spot.logTime) / 1000.0), 1.0F, 0.0F, 0.0F, textColor, true, false);
    }

    private void renderInfoText(Spot spot, int textColor) {
        List<String> strings = new ArrayList<>();
        if (this.health.get()) {
            strings.add(String.format("%.1f", spot.health));
        }

        if (this.ping.get()) {
            strings.add(spot.ping + " ms");
        }

        if (this.pops.get()) {
            strings.add("[" + spot.pops + "]");
        }

        double width = (strings.size() - 1) * 3;

        for (String string : strings) {
            width += BlackOut.FONT.getWidth(string);
        }

        this.matrixStack.push();
        this.matrixStack.translate(-width / 2.0, 0.0, 0.0);

        for (String string : strings) {
            BlackOut.FONT.text(this.matrixStack, string, 1.0F, 0.0F, 0.0F, textColor, false, false);
            this.matrixStack.translate(BlackOut.FONT.getWidth(string) + 3.0F, 0.0F, 0.0F);
        }

        this.matrixStack.pop();
        this.matrixStack.translate(0.0F, BlackOut.FONT.getHeight(), 0.0F);
    }

    private void renderName(Spot spot, int textColor) {
        BlackOut.FONT.text(this.matrixStack, spot.player.getName().getString(), 1.0F, 0.0F, 0.0F, textColor, true, false);
        this.matrixStack.translate(0.0F, BlackOut.FONT.getHeight(), 0.0F);
    }

    private void renderArmorAndItems(Spot spot, int textColor) {
        int size = 0;

        for (ItemComponent item : spot.items) {
            if (item.armor() && this.armor.get() || !item.armor() && this.items.get()) {
                size++;
            }
        }

        this.matrixStack.push();
        this.matrixStack.translate(-size * 8, 0.0F, 0.0F);
        spot.items.forEach(itemx -> {
            if (itemx.armor() && this.armor.get() || !itemx.armor() && this.items.get()) {
                this.renderComponent(itemx, textColor);
            }
        });
        this.matrixStack.pop();
        this.matrixStack.translate(0.0F, 20.0F, 0.0F);
    }

    private void renderComponent(ItemComponent component, int textColor) {
        ItemStack itemStack = component.itemStack();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alphaMulti);
        RenderUtils.renderItem(this.matrixStack, itemStack.getItem(), 0.0F, 0.0F, 16.0F);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (component.armor()) {
            boolean isUnbreakable = itemStack.get(DataComponentTypes.UNBREAKABLE) != null;

            if (!isUnbreakable && itemStack.isDamageable()) {
                float maxDamage = (float) itemStack.getMaxDamage();
                if (maxDamage > 0) {
                    int durabilityPercentage = Math.round((maxDamage - itemStack.getDamage()) * 100.0F / maxDamage);
                    BlackOut.FONT.text(this.matrixStack, durabilityPercentage + "%", 0.8F, 8.0F, 16.0F, textColor, true, true);
                }
            }
        } else if (itemStack.isStackable() || itemStack.getCount() > 1) {
            BlackOut.FONT.text(this.matrixStack, String.valueOf(itemStack.getCount()), 0.8F, 8.0F, 16.0F, textColor, true, true);
        }

        this.matrixStack.translate(16.0F, 0.0F, 0.0F);
    }

    private double infoHeight(Spot spot) {
        double height = 0.0;
        if (this.name.get()) {
            height += BlackOut.FONT.getHeight();
        }

        if (this.itemsFound(spot)) {
            height += 20.0;
        }

        if (this.health.get() || this.ping.get() || this.pops.get()) {
            height += BlackOut.FONT.getHeight();
        }

        if (this.time.get()) {
            height += BlackOut.FONT.getHeight();
        }

        return height;
    }

    private boolean itemsFound(Spot spot) {
        for (ItemComponent itemComponent : spot.items) {
            if ((!itemComponent.armor() || this.armor.get()) && (itemComponent.armor() || this.items.get())) {
                return true;
            }
        }

        return false;
    }

    private float infoScale(Vec3d pos) {
        double distance = BlackOut.mc.gameRenderer.getCamera().getPos().subtract(pos).length();
        float distSqrt = (float) Math.sqrt(distance);
        return 8.0F / distSqrt + 0.05F * distSqrt;
    }

    private Vec3d infoPos(Spot spot) {
        return BoxUtils.middle(spot.player.getBoundingBox());
    }

    private boolean checkMatchingEntities(UUID uuid) {
        return !this.removedEntities.removeAll(timer -> {
            AbstractClientPlayerEntity player = timer.value;
            GameProfile profile = player.getGameProfile();
            if (uuid.equals(profile.getId())) {
                this.spots.add(new Spot(player));
                return true;
            } else {
                return false;
            }
        });
    }

    private boolean checkMatchingUUIDs(AbstractClientPlayerEntity player) {
        GameProfile profile = player.getGameProfile();
        return !this.removedUUIDs.removeAll(timer -> {
            UUID uuid = timer.value;
            if (uuid.equals(profile.getId())) {
                this.spots.add(new Spot(player));
                return true;
            } else {
                return true;
            }
        });
    }

    private record ItemComponent(ItemStack itemStack, boolean armor) {
    }

    private static class Spot {
        private final AbstractClientPlayerEntity player;
        private final float tickDelta = BlackOut.mc.getRenderTickCounter().getTickDelta(true);
        private final List<ItemComponent> items = new ArrayList<>();
        private final float health;
        private final int ping;
        private final int pops;
        private final long logTime = System.currentTimeMillis();
        private boolean seen = false;
        private long seenSince = 0L;

        public Spot(AbstractClientPlayerEntity player) {
            this.player = player;
            this.fillItems();
            this.health = player.getHealth() + player.getAbsorptionAmount();
            PlayerListEntry entry = BlackOut.mc.getNetworkHandler().getPlayerListEntry(player.getGameProfile().getId());
            if (entry != null) {
                this.ping = entry.getLatency();
            } else {
                this.ping = -1;
            }

            StatsManager.TrackerData trackerData = Managers.STATS.getStats(player);
            if (trackerData == null) {
                this.pops = 0;
            } else {
                this.pops = trackerData.pops;
            }
        }

        private void fillItems() {
            UUID uuid = this.player.getGameProfile().getId();
            TimerMap<UUID, ItemStack[]> map = LogoutSpots.getInstance().prevItems;
            if (map.containsKey(uuid)) {
                ItemStack[] arr = map.get(uuid);

                for (int i = 0; i < 6; i++) {
                    ItemStack stack = arr[i];
                    if (!stack.isEmpty()) {
                        this.items.add(new ItemComponent(stack, i != 0 && i != 5));
                    }
                }
            }
        }

        private void setSeen() {
            this.seen = true;
            this.seenSince = System.currentTimeMillis();
        }
    }
}
