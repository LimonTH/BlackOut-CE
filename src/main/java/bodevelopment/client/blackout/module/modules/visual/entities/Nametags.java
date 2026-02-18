package bodevelopment.client.blackout.module.modules.visual.entities;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.FilterMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.manager.managers.StatsManager;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.combat.misc.AntiBot;
import bodevelopment.client.blackout.module.modules.visual.misc.Freecam;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.BackgroundMultiSetting;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.EnchantmentNames;
import bodevelopment.client.blackout.util.render.RenderUtils;
import com.mojang.blaze3d.platform.GlStateManager;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Nametags extends Module {
    private static Nametags INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<Boolean> ping = this.sgGeneral.b("Show Ping", true, ".");
    public final Setting<Boolean> pops = this.sgGeneral.b("Show Pops", true, ".");
    public final Setting<Boolean> showId = this.sgGeneral.b("Show Id", false, "");
    public final Setting<Boolean> rounded = this.sgGeneral.b("Rounded", true, ".");
    public final Setting<Boolean> shadow = this.sgGeneral.b("Shadow", true, ".");
    private final SettingGroup sgItems = this.addGroup("Items");
    private final SettingGroup sgEnchantments = this.addGroup("Enchantments");
    private final SettingGroup sgColor = this.addGroup("Color");
    public final Setting<ColorMode> colorMode = this.sgColor.e("Mode", ColorMode.Dynamic, "What style to use");
    private final Setting<BlackOutColor> hp = this.sgColor
            .c("Health Color", new BlackOutColor(150, 150, 150, 255), ".", () -> this.colorMode.get() == ColorMode.Custom);
    private final Setting<Double> scale = this.sgGeneral.d("Scale", 1.0, 0.0, 10.0, 0.1, ".");
    private final Setting<Double> scaleInc = this.sgGeneral
            .d("Scale Increase", 1.0, 0.0, 5.0, 0.05, "How much should the scale increase when enemy is further away.");
    private final Setting<Double> yOffset = this.sgGeneral.d("Y", 0.0, 0.0, 1.0, 0.01, ".");
    private final Setting<NameMode> nameMode = this.sgGeneral.e("Name Mode", NameMode.EntityName, "");
    private final Setting<Boolean> blur = this.sgGeneral.b("Blur", true, ".");
    private final Setting<List<EntityType<?>>> entityTypes = this.sgGeneral.el("Entities", ".", EntityType.PLAYER);
    private final Setting<Boolean> armor = this.sgItems.b("Armor", false, ".");
    private final Setting<Boolean> hand = this.sgItems.b("Hands", false, ".");
    private final Setting<Double> itemScale = this.sgItems.d("Item Scale", 1.0, 0.0, 3.0, 0.03, ".");
    private final Setting<Double> itemOffset = this.sgItems.d("Item Offset", 1.0, 0.0, 2.0, 0.02, ".");
    private final Setting<Double> itemSeparation = this.sgItems.d("Item Separation", 0.0, 0.0, 5.0, 0.05, ".");
    private final Setting<FilterMode> filterMode = this.sgEnchantments.e("Enchantment Mode", FilterMode.Blacklist, ".");
    private final Setting<List<RegistryKey<Enchantment>>> enchantments = this.sgEnchantments
            .l("Enchantments", ".", EnchantmentNames.enchantments, EnchantmentNames::getLongName);
    private final Setting<Boolean> drawEnchants = this.sgEnchantments.b("Draw Enchants", false, ".");
    private final Setting<Boolean> enchantsAbove = this.sgEnchantments.b("Enchantments Above", true, ".", this.drawEnchants::get);
    private final Setting<Boolean> shortNames = this.sgEnchantments.b("Short Names", true, ".", this.drawEnchants::get);
    private final Setting<Double> enchantScale = this.sgEnchantments.d("Enchantment Scale", 0.5, 0.0, 2.0, 0.02, ".", this.drawEnchants::get);
    private final Setting<Double> compact = this.sgEnchantments.d("Compact Enchantments", 0.0, 0.0, 1.0, 0.01, ".", this.drawEnchants::get);
    private final Setting<Boolean> center = this.sgEnchantments.b("Center Enchantments", true, ".", this.drawEnchants::get);
    private final Setting<Double> enchantmentsOffset = this.sgEnchantments
            .d("Enchantments Offset", 0.0, 0.0, 1.0, 0.01, ".", () -> this.drawEnchants.get() && !this.center.get());
    private final Setting<BlackOutColor> txt = this.sgColor.c("Text Color", new BlackOutColor(255, 255, 255, 255), ".");
    private final BackgroundMultiSetting background = BackgroundMultiSetting.of(this.sgGeneral, "Nametag");
    private final Setting<BlackOutColor> friendColor = this.sgColor.c("Friend Color", new BlackOutColor(150, 150, 255, 255), ".");
    private final MatrixStack stack = new MatrixStack();
    private final List<Entity> entities = new ArrayList<>();
    private final List<Component> components = new ArrayList<>();
    private float length;
    private float offset;

    public Nametags() {
        super("Nametags", "Renders custom nametags", SubCategory.ENTITIES, true);
        INSTANCE = this;
    }

    public static Nametags getInstance() {
        return INSTANCE;
    }

    public static boolean shouldCancelLabel(Entity entity) {
        Nametags nametags = getInstance();
        if (nametags.enabled && nametags.shouldRender(entity)) {
            return true;
        } else {
            ESP esp = ESP.getInstance();
            return esp.enabled && esp.shouldRender(entity);
        }
    }

    @Event
    public void onTick(TickEvent.Post event) {
        if (BlackOut.mc.world != null && BlackOut.mc.player != null) {
            this.entities.clear();
            BlackOut.mc.world.entityList.forEach(entity -> {
                if (this.shouldRender(entity)) {
                    this.entities.add(entity);
                }
            });
            this.entities.sort(Comparator.comparingDouble(entity -> -BlackOut.mc.gameRenderer.getCamera().getPos().distanceTo(entity.getPos())));
        }
    }

    @Event
    public void onRender(RenderEvent.Hud.Post event) {
        if (BlackOut.mc.world != null && BlackOut.mc.player != null) {
            GlStateManager._disableDepthTest();
            GlStateManager._enableBlend();
            GlStateManager._disableCull();
            this.stack.push();
            RenderUtils.unGuiScale(this.stack);
            this.entities.forEach(entity -> this.renderNameTag(event.tickDelta, entity));
            this.stack.pop();
        }
    }

    public void renderNameTag(double tickDelta, Entity entity) {
        double x = MathHelper.lerp(tickDelta, entity.prevX, entity.getX());
        double y = MathHelper.lerp(tickDelta, entity.prevY, entity.getY());
        double z = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ());
        float d = (float) BlackOut.mc.gameRenderer.getCamera().getPos().subtract(x, y, z).length();
        float s = this.getScale(d);
        this.stack.push();
        Vec2f f = RenderUtils.getCoords(x, y + entity.getBoundingBox().getLengthY() + this.yOffset.get(), z, true);
        if (f == null) {
            this.stack.pop();
        } else {
            this.stack.translate(f.x, f.y, 0.0F);
            this.stack.scale(s, s, s);
            this.components.clear();
            Color color1;
            if (entity instanceof PlayerEntity && Managers.FRIENDS.isFriend((PlayerEntity) entity)) {
                color1 = this.friendColor.get().getColor();
            } else {
                color1 = this.txt.get().getColor();
            }

            this.components.add(new Component(this.nameMode.get().getName(entity), color1));
            if (entity instanceof ItemEntity itemEntity) {
                int count = itemEntity.getStack().getCount();
                if (count > 1) {
                    this.components.add(new Component(count + "x", Color.WHITE));
                }
            }

            if (entity instanceof AbstractClientPlayerEntity player) {
                if (this.ping.get()) {
                    PlayerListEntry entry = BlackOut.mc.getNetworkHandler().getPlayerListEntry(entity.getUuid());
                    if (entry != null) {
                        this.components.add(new Component(entry.getLatency() + "ms", color1));
                    }
                }

                StatsManager.TrackerData trackerData = Managers.STATS.getStats(player);
                if (this.pops.get() && trackerData != null && trackerData.pops > 0) {
                    this.components.add(new Component("[" + trackerData.pops + "]", color1));
                }
            }

            if (this.showId.get()) {
                this.components.add(new Component("id:" + entity.getId(), color1));
            }

            if (entity instanceof LivingEntity livingEntity) {
                this.components
                        .add(
                                new Component(
                                        String.format("%.1f", livingEntity.getHealth() + livingEntity.getAbsorptionAmount()),
                                        BlackOut.FONT.getWidth("20.0"),
                                        this.getColor(livingEntity, livingEntity.getHealth() + livingEntity.getAbsorptionAmount())
                                )
                        );
            }

            this.length = 0.0F;
            this.offset = 0.0F;
            this.components.forEach(component -> this.length = this.length + BlackOut.FONT.getWidth(component.text));
            this.length = this.length + (this.components.size() * 5 - 5);
            this.stack.push();
            this.stack.translate(-this.length / 2.0F, -9.0F, 0.0F);
            if (this.blur.get()) {
                RenderUtils.drawLoadedBlur(
                        "hudblur", this.stack, renderer -> renderer.rounded(-2.0F, -5.0F, this.length + 4.0F, 10.0F, this.rounded.get() ? 3.0F : 0.0F, 10)
                );
                Renderer.onHUDBlur();
            }

            this.background.render(this.stack, -2.0F, -5.0F, this.length + 4.0F, 10.0F, this.rounded.get() ? 3.0F : 0.0F, this.shadow.get() ? 3.0F : 0.0F);
            this.components.forEach(component -> {
                BlackOut.FONT.text(this.stack, component.text, 1.0F, this.offset, 0.0F, component.color, false, true);
                this.offset = this.offset + (component.width + 5.0F);
            });
            this.stack.pop();
            if (this.armor.get() || this.hand.get()) {
                this.stack.push();
                this.stack.translate(0.0, -16.0 * this.itemOffset.get() - 6.0, 0.0);
                this.stack.push();
                float wbg = 16.0F;
                float separation = (this.itemSeparation.get().floatValue() - 1.0F) * 4.0F;
                this.stack.scale(this.itemScale.get().floatValue(), this.itemScale.get().floatValue(), 1.0F);
                this.stack.translate((wbg * 4.0F + separation * 3.0F) / -2.0F, -16.0F, 0.0F);
                if (entity instanceof AbstractClientPlayerEntity livingEntity) {
                    if (this.hand.get()) {
                        this.renderHandItem(livingEntity.getMainHandStack(), wbg, separation, -1);
                        this.renderHandItem(livingEntity.getOffHandStack(), wbg, separation, 4);
                    }

                    if (this.armor.get()) {
                        for (int i = 0; i < 4; i++) {
                            ItemStack itemStack = livingEntity.getInventory().getArmorStack(3 - i);

                            if (!itemStack.isEmpty()) {
                                RenderUtils.renderItem(this.stack, itemStack.getItem(), i * (wbg + separation), 0.0F, 16.0F);
                                boolean isUnbreakable = itemStack.get(DataComponentTypes.UNBREAKABLE) != null;

                                if (!isUnbreakable && itemStack.isDamageable()) {
                                    float maxDamage = (float) itemStack.getMaxDamage();
                                    if (maxDamage > 0) {
                                        int durabilityPercentage = Math.round((maxDamage - itemStack.getDamage()) * 100.0F / maxDamage);
                                        this.drawItemText(durabilityPercentage + " %", wbg, separation, i);
                                    }
                                }
                                this.drawEnchantments(itemStack, wbg, separation, i);
                            }
                        }
                    }
                }

                this.stack.pop();
                this.stack.pop();
            }

            this.stack.pop();
        }
    }

    private void renderHandItem(ItemStack itemStack, float wbg, float separation, int i) {
        if (!itemStack.isEmpty()) {
            RenderUtils.renderItem(this.stack, itemStack.getItem(), i * (wbg + separation), 0.0F, 16.0F);
            if (itemStack.isStackable() || itemStack.getCount() > 1) {
                this.drawItemText(String.valueOf(itemStack.getCount()), wbg, separation, i);
            }

            this.drawEnchantments(itemStack, wbg, separation, i);
        }
    }

    private void drawItemText(String text, float wbg, float separation, int i) {
        BlackOut.FONT.text(this.stack, text, 0.6F, i * (wbg + separation) + wbg / 2.0F, 16.0F, this.txt.get().getRGB(), true, true);
    }

    private void drawEnchantments(ItemStack itemStack, float wbg, float separation, int i) {
        ItemEnchantmentsComponent enchantsComponent = EnchantmentHelper.getEnchantments(itemStack);

        if (!enchantsComponent.isEmpty() && this.drawEnchants.get()) {
            final int[] y = {0};

            for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchantsComponent.getEnchantmentEntries()) {
                RegistryEntry<Enchantment> enchantment = entry.getKey();
                int level = entry.getIntValue();

                enchantment.getKey().ifPresent(key -> {
                    if (this.filterMode.get().shouldAccept(key, this.enchantments.get())) {
                        String name = EnchantmentNames.getName(enchantment, this.shortNames.get());
                        String text = (level > 1) ? name + " " + level : name;

                        float cy = (this.enchantsAbove.get() ? -y[0] - 1 : y[0])
                                * BlackOut.FONT.getHeight()
                                * this.enchantScale.get().floatValue()
                                * (1.0F - this.compact.get().floatValue() / 3.0F);

                        BlackOut.FONT.text(
                                this.stack,
                                text,
                                this.enchantScale.get().floatValue(),
                                i * (wbg + separation) + (this.center.get() ? 1.0F : this.enchantmentsOffset.get().floatValue()) * wbg / 2.0F,
                                cy,
                                this.txt.get().getRGB(),
                                this.center.get(),
                                false
                        );

                        y[0]++;
                    }
                });
            }
        }
    }

    public boolean shouldRender(Entity entity) {
        AntiBot antiBot = AntiBot.getInstance();
        if (antiBot.enabled && antiBot.mode.get() == AntiBot.HandlingMode.Ignore && entity instanceof AbstractClientPlayerEntity player && antiBot.getBots().contains(player)) {
            return false;
        } else if (!this.entityTypes.get().contains(entity.getType())) {
            return false;
        } else {
            return entity != BlackOut.mc.player || Freecam.getInstance().enabled;
        }
    }

    private Color getColor(LivingEntity entity, float health) {
        return this.colorMode.get() == ColorMode.Custom
                ? this.hp.get().getColor()
                : ColorUtils.lerpColor(
                Math.min(health / (entity.getMaxHealth() + (entity instanceof PlayerEntity ? 16 : 0)), 1.0F), new Color(255, 0, 0, 255), new Color(0, 255, 0, 255)
        );
    }

    private float getScale(float d) {
        float distSqrt = (float) Math.sqrt(d);
        return this.scale.get().floatValue() * 8.0F / distSqrt + this.scaleInc.get().floatValue() / 20.0F * distSqrt;
    }

    public enum ColorMode {
        Custom,
        Dynamic
    }

    public enum NameMode {
        Display,
        EntityName;

        private String getName(Entity entity) {
            return this == Display ? entity.getDisplayName().getString() : entity.getName().getString();
        }
    }

    private static class Component {
        private final String text;
        private final float width;
        private final Color color;

        public Component(String text, Color color) {
            this.text = text;
            this.width = BlackOut.FONT.getWidth(text);
            this.color = color;
        }

        public Component(String text, float width, Color color) {
            this.text = text;
            this.width = width;
            this.color = color;
        }
    }
}
