package bodevelopment.client.blackout.module.modules.visual.world;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.BoxMultiSetting;
import bodevelopment.client.blackout.randomstuff.timers.RenderList;
import bodevelopment.client.blackout.util.BlockUtils;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ToolMaterials;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

public class MineESP extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgRender = this.addGroup("Render");

    private final Setting<Double> range = this.sgGeneral.doubleSetting("Visual Range", 10.0, 0.0, 50.0, 0.5, "The maximum distance at which mining indicators will be rendered.");
    private final Setting<Boolean> accurateTime = this.sgGeneral.booleanSetting("Predictive Timing", false, "Calculates the exact mining duration based on a simulated tool and efficiency level.");
    private final Setting<ToolMaterials> pickaxeMaterial = this.sgGeneral.enumSetting("Simulated Tool", ToolMaterials.NETHERITE, "The tool material used to calculate the predicted block breaking time.", this.accurateTime::get);
    private final Setting<Integer> pickaxeEfficiency = this.sgGeneral.intSetting("Efficiency Level", 5, 0, 5, 1, "The Efficiency enchantment level used for the predictive timing calculation.", this.accurateTime::get);
    private final Setting<Double> fadeIn = this.sgGeneral.doubleSetting("Static Scale Time", 2.0, 0.0, 20.0, 0.1, "The duration it takes for the box to scale to full size when predictive timing is disabled.", () -> !this.accurateTime.get());
    private final Setting<Double> renderTime = this.sgGeneral.doubleSetting("Dwell Time", 4.0, 0.0, 20.0, 0.1, "The amount of time the highlight remains at full size before beginning to fade.");
    private final Setting<Double> fadeOut = this.sgGeneral.doubleSetting("Opacity Fade Time", 2.0, 0.0, 20.0, 0.1, "The duration over which the highlight disappears after the mining action is complete.");

    private final BoxMultiSetting box = BoxMultiSetting.of(this.sgRender);

    private final RenderList<Triple<BlockPos, Integer, Double>> renders = RenderList.getList(false);

    public MineESP() {
        super("Mine ESP", "Interprets block-breaking packets from the server to highlight blocks currently being mined by other players.", SubCategory.WORLD, true);
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            this.renders
                    .update(
                            (triple, time, delta) -> {
                                double distanceDelta = MathHelper.clamp(
                                        MathHelper.getLerpProgress(
                                                BlackOut.mc.player.getEyePos().distanceTo(triple.getLeft().toCenterPos()),
                                                this.range.get() + 2.0,
                                                this.range.get()
                                        ),
                                        0.0,
                                        1.0
                                );
                                double fadeIn = triple.getRight();
                                double scaleDelta;
                                double fadeDelta;
                                if (time <= fadeIn) {
                                    scaleDelta = fadeDelta = time / fadeIn;
                                } else if (time >= fadeIn + this.renderTime.get()) {
                                    scaleDelta = 1.0;
                                    fadeDelta = 1.0 - (time - fadeIn - this.renderTime.get()) / this.fadeOut.get();
                                } else {
                                    scaleDelta = 1.0;
                                    fadeDelta = 1.0;
                                }

                                double colorDelta = MathHelper.clamp(fadeDelta * distanceDelta, 0.0, 1.0);

                                this.box.render(this.getBox(triple.getLeft(), scaleDelta), (float) colorDelta, (float) colorDelta);
                            }
                    );
        }
    }

    @Event
    public void onReceive(PacketEvent.Receive.Pre event) {
        if (BlackOut.mc.world != null && event.packet instanceof BlockBreakingProgressS2CPacket packet && !this.contains(packet) && BlockUtils.mineable(packet.getPos())) {
            this.renders.remove(timer -> timer.value.getMiddle() == packet.getEntityId());
            double fadeIn = this.getFadeIn(packet.getPos());
            this.renders.add(new ImmutableTriple<>(packet.getPos(), packet.getEntityId(), fadeIn), fadeIn + this.renderTime.get() + this.fadeOut.get());
        }
    }

    private double getFadeIn(BlockPos pos) {
        return !this.accurateTime.get() ? this.fadeIn.get() : 1.0 / BlockUtils.getBlockBreakingDelta(pos, this.getPickaxeStack(), false, false, false) / 20.0;
    }

    private ItemStack getPickaxeStack() {
        ItemStack stack = switch (this.pickaxeMaterial.get()) {
            case WOOD -> new ItemStack(Items.WOODEN_PICKAXE);
            case STONE -> new ItemStack(Items.STONE_PICKAXE);
            case IRON -> new ItemStack(Items.IRON_PICKAXE);
            case DIAMOND -> new ItemStack(Items.DIAMOND_PICKAXE);
            case GOLD -> new ItemStack(Items.GOLDEN_PICKAXE);
            case NETHERITE -> new ItemStack(Items.NETHERITE_PICKAXE);
        };

        int level = this.pickaxeEfficiency.get();
        if (level > 0) {
            var enchantmentRegistry = BlackOut.mc.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
            var efficiencyOption = enchantmentRegistry.getEntry(Enchantments.EFFICIENCY);

            efficiencyOption.ifPresent(entry -> stack.addEnchantment(entry, level));
        }

        return stack;
    }

    private boolean contains(BlockBreakingProgressS2CPacket packet) {
        return this.renders
                .contains(timer -> timer.value.getLeft().equals(packet.getPos()) && timer.value.getMiddle() == packet.getEntityId());
    }

    private Box getBox(BlockPos pos, double progress) {
        return new Box(
                pos.getX() + 0.5 - progress / 2.0,
                pos.getY() + 0.5 - progress / 2.0,
                pos.getZ() + 0.5 - progress / 2.0,
                pos.getX() + 0.5 + progress / 2.0,
                pos.getY() + 0.5 + progress / 2.0,
                pos.getZ() + 0.5 + progress / 2.0
        );
    }
}
