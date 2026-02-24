package bodevelopment.client.blackout.module.modules.combat.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.*;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.combat.defensive.Surround;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.*;
import bodevelopment.client.blackout.randomstuff.timers.RenderList;
import bodevelopment.client.blackout.randomstuff.timers.TimerList;
import bodevelopment.client.blackout.util.*;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class HoleFill extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgSelf = this.addGroup("Self");
    private final SettingGroup sgPlacing = this.addGroup("Placing");
    private final SettingGroup sgRender = this.addGroup("Render");
    private final SettingGroup sgHole = this.addGroup("Hole");

    private final Setting<Boolean> near = this.sgGeneral.booleanSetting("Near", true, "Only fills holes if enemies are nearby.");
    private final Setting<Double> nearDistance = this.sgGeneral.doubleSetting("Near Distance", 3.0, 0.0, 10.0, 0.1, "Max distance between an enemy and a hole.");
    private final Setting<Integer> nearExt = this.sgGeneral.intSetting("Extrapolation", 5, 0, 20, 1, "Predicts enemy movement (in ticks).");
    private final Setting<Integer> selfExt = this.sgGeneral.intSetting("Self Extrapolation", 2, 0, 20, 1, "Predicts your own movement.");
    private final Setting<Boolean> above = this.sgGeneral.booleanSetting("Above", true, "Only places if the target is above the hole level.");
    private final Setting<Boolean> ignoreHole = this.sgGeneral.booleanSetting("Ignore Hole", true, "Won't waste blocks if the enemy is already inside a hole.");

    private final Setting<Boolean> ignoreSelfHole = this.sgSelf.booleanSetting("Ignore Self Hole", true, "Allows filling holes even when you are in one.");
    private final Setting<Boolean> selfAbove = this.sgSelf.booleanSetting("Self Above", true, "Allows filling near you if you aren't directly above the hole.");
    private final Setting<Double> selfDistance = this.sgSelf.doubleSetting("Self Distance", 3.0, 0.0, 10.0, 0.1, "Minimum safety buffer around you.");
    private final Setting<Boolean> efficient = this.sgSelf.booleanSetting("Efficient", true, "Only places if the hole is closer to the enemy than to you.");

    private final Setting<Boolean> pauseEat = this.sgPlacing.booleanSetting("Pause Eat", false, "Pauses filling while you are eating/gapping.");
    private final Setting<SwitchMode> switchMode = this.sgPlacing.enumSetting("Switch Mode", SwitchMode.Silent, "Method of switching to obsidian.");
    private final Setting<Surround.PlaceDelayMode> placeDelayMode = this.sgPlacing.enumSetting("Place Delay Mode", Surround.PlaceDelayMode.Ticks, "Timing unit (Ticks/Seconds).");
    private final Setting<Integer> placeDelayT = this.sgPlacing.intSetting("Place Tick Delay", 1, 0, 20, 1, "Delay in ticks between placements.", () -> this.placeDelayMode.get() == Surround.PlaceDelayMode.Ticks);
    private final Setting<Double> placeDelayS = this.sgPlacing.doubleSetting("Place Delay", 0.1, 0.0, 1.0, 0.01, "Delay in seconds.", () -> this.placeDelayMode.get() == Surround.PlaceDelayMode.Seconds);
    private final Setting<Integer> places = this.sgPlacing.intSetting("Places", 1, 1, 20, 1, "How many blocks to place per cycle.");
    private final Setting<Double> cooldown = this.sgPlacing.doubleSetting("Cooldown", 0.3, 0.0, 1.0, 0.01, "Cooldown before retrying the same position.");
    private final Setting<List<Block>> blocks = this.sgPlacing.blockListSetting("Blocks", "Blocks to use for filling.", Blocks.OBSIDIAN);
    private final Setting<Integer> boxExtrapolation = this.sgPlacing.intSetting("Box Extrapolation", 1, 0, 20, 1, "Inflates enemy hitbox for collision checks.");

    private final Setting<Boolean> single = this.sgHole.booleanSetting("Single", true, "Fill 1x1 holes.");
    private final Setting<Boolean> doubleHole = this.sgHole.booleanSetting("Double", true, "Fill 2x1 holes.");
    private final Setting<Boolean> quad = this.sgHole.booleanSetting("Quad", true, "Fill 2x2 holes.");

    private final Setting<Boolean> placeSwing = this.sgRender.booleanSetting("Swing", true, "Renders hand swing animation.");
    private final Setting<SwingHand> placeHand = this.sgRender.enumSetting("Swing Hand", SwingHand.RealHand, "Which hand to swing.", this.placeSwing::get);
    private final Setting<Double> renderTime = this.sgRender.doubleSetting("Render Time", 0.3, 0.0, 5.0, 0.1, "Time the box stays fully visible.");
    private final Setting<Double> fadeTime = this.sgRender.doubleSetting("Fade Time", 1.0, 0.0, 5.0, 0.1, "Time it takes for the box to fade out.");
    private final Setting<RenderShape> renderShape = this.sgRender.enumSetting("Render Shape", RenderShape.Full, "Style of the rendered box.");
    private final Setting<BlackOutColor> lineColor = this.sgRender.colorSetting("Line Color", new BlackOutColor(255, 0, 0, 255), "Line color of the box.");
    private final Setting<BlackOutColor> sideColor = this.sgRender.colorSetting("Side Color", new BlackOutColor(255, 0, 0, 50), "Side color of the box.");

    private final List<BlockPos> holes = new ArrayList<>();
    private final TimerList<BlockPos> timers = new TimerList<>(true);
    private final RenderList<BlockPos> render = RenderList.getList(false);
    private final ExtrapolationMap nearPosition = new ExtrapolationMap();
    private final ExtrapolationMap boxes = new ExtrapolationMap();
    private Hand hand = null;
    public static boolean placing = false;
    private int blocksLeft = 0;
    private int placesLeft = 0;
    private FindResult result = null;
    private boolean switched = false;
    private boolean shouldIgnoreSelf = false;
    private int tickTimer = 0;
    private long lastTime = 0L;
    private BlockPos prevPos = BlockPos.ORIGIN;
    private BlockPos prevHole = BlockPos.ORIGIN;
    private long holeTime = 0L;

    public HoleFill() {
        super("Hole Fill", "Automatically fills nearby holes with obsidian to keep enemies vulnerable on the surface.", SubCategory.MISC_COMBAT, true);
    }

    @Event
    public void onTick(TickEvent.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            BlockPos pos = new BlockPos(
                    BlackOut.mc.player.getBlockX(), (int) Math.round(BlackOut.mc.player.getY()), BlackOut.mc.player.getBlockZ()
            );
            if (!pos.equals(this.prevPos) && HoleUtils.inHole(this.prevPos) && !HoleUtils.inHole(pos)) {
                this.prevHole = this.prevPos;
                this.holeTime = System.currentTimeMillis();
            }

            this.prevPos = pos;
            this.update();
        }
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            this.render.update((pos, time, d) -> {
                double progress = 1.0 - Math.max(time - this.renderTime.get(), 0.0) / this.fadeTime.get();
                Render3DUtils.box(BoxUtils.get(pos), this.sideColor.get().alphaMulti(progress), this.lineColor.get().alphaMulti(progress), this.renderShape.get());
            });
        }
    }

    private void update() {
        this.tickTimer++;
        this.updateMaps();
        this.updateHoles();
        this.result = this.switchMode.get().find(this::valid);
        this.updatePlaces();
        this.updatePlacing();
    }

    private void updatePlacing() {
        this.blocksLeft = Math.min(this.placesLeft, this.result.amount());
        this.hand = OLEPOSSUtils.getHand(this::valid);
        this.switched = false;
        if (!BlackOut.mc.player.isUsingItem() || !this.pauseEat.get()) {
            this.holes
                    .stream()
                    .sorted(Comparator.comparingDouble(pos -> pos.toCenterPos().distanceTo(BlackOut.mc.player.getEyePos())))
                    .forEach(this::place);
            if (this.switched && this.hand == null) {
                this.switchMode.get().swapBack();
            }
        }
    }

    private boolean valid(ItemStack stack) {
        return stack.getItem() instanceof BlockItem block && this.blocks.get().contains(block.getBlock());
    }

    private void updateMaps() {
        this.nearPosition.update(player -> player == BlackOut.mc.player ? this.selfExt.get() : this.nearExt.get());
        this.boxes.update(player -> player == BlackOut.mc.player ? 0 : this.boxExtrapolation.get());
    }

    private void updateHoles() {
        this.holes.clear();
        int range = (int) Math.ceil(SettingUtils.maxPlaceRange() + 1.0);
        BlockPos p = BlockPos.ofFloored(BlackOut.mc.player.getEyePos());
        List<Hole> holeList = new ArrayList<>();

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    Hole hole = HoleUtils.getHole(p.add(x, y, z));
                    if (hole.type != HoleType.NotHole
                            && (this.single.get() || hole.type != HoleType.Single)
                            && (this.doubleHole.get() || hole.type != HoleType.DoubleX && hole.type != HoleType.DoubleZ)
                            && (this.quad.get() || hole.type != HoleType.Quad)) {
                        holeList.add(hole);
                    }
                }
            }
        }

        holeList.forEach(holex -> {
            if (this.validHole(holex)) {
                Arrays.stream(holex.positions).filter(this::validPos).forEach(this.holes::add);
            }
        });
    }

    private boolean validPos(BlockPos pos) {
        if (this.timers.contains(pos)) {
            return false;
        } else if (!OLEPOSSUtils.replaceable(pos)) {
            return false;
        } else {
            PlaceData data = SettingUtils.getPlaceData(pos);
            return data.valid() && SettingUtils.inPlaceRange(data.pos());
        }
    }

    private boolean validHole(Hole hole) {
        double pDist = (this.nearPosition.contains(BlackOut.mc.player)
                ? this.feet(this.nearPosition.get(BlackOut.mc.player))
                : BlackOut.mc.player.getPos())
                .distanceTo(hole.middle);
        if (this.selfNearCheck(hole)) {
            return false;
        } else {
            for (BlockPos pos : hole.positions) {
                if (EntityUtils.intersects(BoxUtils.get(pos), entity -> !entity.isSpectator() && !(entity instanceof ItemEntity))) {
                    return false;
                }
            }

            for (AbstractClientPlayerEntity player : BlackOut.mc.world.getPlayers()) {
                if (!player.isSpectator()
                        && player != BlackOut.mc.player
                        && !(player.getHealth() <= 0.0F)
                        && !Managers.FRIENDS.isFriend(player)
                        && this.nearCheck(player, hole, pDist)) {
                    return true;
                }
            }

            return false;
        }
    }

    private boolean selfNearCheck(Hole hole) {
        if (System.currentTimeMillis() - this.holeTime < 500L && OLEPOSSUtils.contains(hole.positions, this.prevHole)) {
            return false;
        } else {
            BlockPos pos = new BlockPos(
                    BlackOut.mc.player.getBlockX(), (int) Math.round(BlackOut.mc.player.getY()), BlackOut.mc.player.getBlockZ()
            );
            if (!this.ignoreSelfHole.get() || !HoleUtils.inHole(BlackOut.mc.player.getBlockPos()) && !OLEPOSSUtils.collidable(pos)) {
                if (this.selfAbove.get() && BlackOut.mc.player.getY() <= hole.middle.y) {
                    this.shouldIgnoreSelf = true;
                    return false;
                } else {
                    this.shouldIgnoreSelf = false;
                    return BlackOut.mc.player.getPos().distanceTo(hole.middle) <= this.selfDistance.get();
                }
            } else {
                this.shouldIgnoreSelf = true;
                return false;
            }
        }
    }

    private boolean nearCheck(AbstractClientPlayerEntity player, Hole hole, double pDist) {
        if (!this.near.get()) {
            return false;
        } else if (this.ignoreHole.get() && this.inHole(player.getPos()) && this.inHole(BoxUtils.feet(this.nearPosition.get(player)))) {
            return false;
        } else if (this.above.get() && this.nearPosition.get(player).minY <= hole.middle.y && player.getY() <= hole.middle.y) {
            return false;
        } else {
            double eDist = (this.nearPosition.contains(player) ? this.feet(this.nearPosition.get(player)) : player.getPos())
                    .distanceTo(hole.middle.add(0.0, 1.0, 0.0));
            return !(eDist > this.nearDistance.get()) && (System.currentTimeMillis() - this.holeTime < 500L && OLEPOSSUtils.contains(hole.positions, this.prevHole)
                    || this.shouldIgnoreSelf
                    || !this.efficient.get()
                    || !(pDist <= eDist));
        }
    }

    private boolean inHole(Vec3d vec) {
        BlockPos pos = new BlockPos((int) Math.floor(vec.getX()), (int) Math.round(vec.getY()), (int) Math.floor(vec.getZ()));
        return HoleUtils.inHole(pos) || OLEPOSSUtils.collidable(pos);
    }

    private void updatePlaces() {
        switch (this.placeDelayMode.get()) {
            case Ticks:
                if (this.placesLeft >= this.places.get() || this.tickTimer >= this.placeDelayT.get()) {
                    this.placesLeft = this.places.get();
                    this.tickTimer = 0;
                }
                break;
            case Seconds:
                if (this.placesLeft >= this.places.get() || System.currentTimeMillis() - this.lastTime >= this.placeDelayS.get() * 1000.0) {
                    this.placesLeft = this.places.get();
                    this.lastTime = System.currentTimeMillis();
                }
        }
    }

    private void place(BlockPos pos) {
        if (this.blocksLeft > 0) {
            PlaceData data = SettingUtils.getPlaceData(pos);
            if (data != null && data.valid()) {
                placing = true;
                if (!SettingUtils.shouldRotate(RotationType.BlockPlace) || this.rotateBlock(data, RotationType.BlockPlace, "placing")) {
                    if (this.switched || this.hand != null || (this.switched = this.switchMode.get().swap(this.result.slot()))) {
                        this.render.add(pos, this.renderTime.get() + this.fadeTime.get());
                        this.timers.add(pos, this.cooldown.get());
                        this.placeBlock(this.hand, data.pos().toCenterPos(), data.dir(), data.pos());
                        if (this.placeSwing.get()) {
                            this.clientSwing(this.placeHand.get(), this.hand);
                        }

                        this.blocksLeft--;
                        this.placesLeft--;
                        if (SettingUtils.shouldRotate(RotationType.BlockPlace)) {
                            this.end("placing");
                        }
                    }
                }
            }
        }
    }

    private Vec3d feet(Box box) {
        return new Vec3d((box.minX + box.maxX) / 2.0, box.minY, (box.minZ + box.maxZ) / 2.0);
    }
}
