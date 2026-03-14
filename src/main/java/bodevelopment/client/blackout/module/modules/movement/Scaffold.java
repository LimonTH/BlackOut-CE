package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RotationType;
import bodevelopment.client.blackout.enums.SwingHand;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.interfaces.mixin.IVec3;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.MoveUpdateModule;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.combat.defensive.Surround;
import bodevelopment.client.blackout.module.modules.misc.Timer;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.BoxMultiSetting;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.randomstuff.PlaceData;
import bodevelopment.client.blackout.randomstuff.Rotation;
import bodevelopment.client.blackout.randomstuff.timers.TimerList;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.*;
import bodevelopment.client.blackout.util.render.AnimUtils;
import bodevelopment.client.blackout.util.render.RenderLayer;
import bodevelopment.client.blackout.util.render.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Scaffold extends MoveUpdateModule {
    private static Scaffold INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgPlacing = this.addGroup("Placing");
    private final SettingGroup sgAttack = this.addGroup("Attack");
    private final SettingGroup sgRender = this.addGroup("Render");

    public final Setting<Boolean> safeWalk = this.sgGeneral.booleanSetting("Edge Protection", true, "Prevents the player from walking off block edges during bridge construction.");
    private final Setting<Boolean> smart = this.sgGeneral.booleanSetting("Reach Validation", true, "Only attempts to place blocks within the server-side interaction range.");
    private final Setting<TowerMode> tower = this.sgGeneral.enumSetting("Tower Logic", TowerMode.NCP, "The vertical movement method used for rapid upward building.");
    private final Setting<Boolean> towerMoving = this.sgGeneral.booleanSetting("Omni-Tower", false, "Allows horizontal movement while simultaneously building vertically.");
    private final Setting<Boolean> useTimer = this.sgGeneral.booleanSetting("Timer Synchronization", false, "Engages the client timer to accelerate the placement cycle.");
    private final Setting<Double> timer = this.sgGeneral.doubleSetting("Timer Value", 1.088, 0.0, 10.0, 0.1, "The multiplier applied to the game speed.", this.useTimer::get);
    private final Setting<Boolean> constantRotate = this.sgGeneral.booleanSetting("Rotation Persistence", false, "Maintains the looking direction even when not actively placing blocks.");
    private final Setting<Double> rotationTime = this.sgGeneral.doubleSetting("Interpolation Duration", 1.0, 0.0, 1.0, 0.01, "The time window for smoothly transitioning between placement rotations.");
    private final Setting<Boolean> keepY = this.sgGeneral.booleanSetting("Level Lock", false, "Forces the module to maintain the initial Y-level during horizontal construction.");
    private final Setting<Boolean> allowTower = this.sgGeneral.booleanSetting("Stationary Tower", true, "Suspends the Y-level lock when the player is not moving horizontally.", this.keepY::get);
    private final Setting<YawMode> smartYaw = this.sgGeneral.enumSetting("Orientation Mode", YawMode.Normal, "The logic used to determine the player's yaw during placement.");
    private final Setting<Boolean> instantRotate = this.sgGeneral.booleanSetting("Instant Pivot", true, "Bypasses rotation speed limits to snap immediately to the target placement vector.");

    private final Setting<Integer> support = this.sgPlacing.intSetting("Support Depth", 3, 0, 5, 1, "The maximum number of auxiliary blocks to place to connect to a valid surface.");
    private final Setting<SwitchMode> switchMode = this.sgPlacing.enumSetting("Hotbar Switch", SwitchMode.Normal, "The method used to select blocks from the hotbar (Silent, Normal, or Packet).");
    private final Setting<List<Block>> blocks = this.sgPlacing.blockListSetting("Filter List", "A priority-ordered list of blocks the module is permitted to use.", Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.NETHERITE_BLOCK, Blocks.STONE, Blocks.OAK_PLANKS, Blocks.TNT);
    private final Setting<Surround.PlaceDelayMode> placeDelayMode = this.sgPlacing.enumSetting("Delay Unit", Surround.PlaceDelayMode.Ticks, "Determines if placement speed is calculated in game ticks or real-world seconds.");
    private final Setting<Integer> placeDelayT = this.sgPlacing.intSetting("Tick Interval", 1, 0, 20, 1, "The number of game ticks to wait between placements.", () -> this.placeDelayMode.get() == Surround.PlaceDelayMode.Ticks);
    private final Setting<Double> placeDelayS = this.sgPlacing.doubleSetting("Second Interval", 0.1, 0.0, 1.0, 0.01, "The time in seconds to wait between placements.", () -> this.placeDelayMode.get() == Surround.PlaceDelayMode.Seconds);
    private final Setting<Integer> places = this.sgPlacing.intSetting("Burst Count", 1, 1, 20, 1, "The number of blocks to attempt to place in a single execution cycle.");
    private final Setting<Double> cooldown = this.sgPlacing.doubleSetting("Retry Cooldown", 0.3, 0.0, 1.0, 0.01, "The wait time before re-attempting a placement at a position that failed.");
    private final Setting<Integer> extrapolation = this.sgPlacing.intSetting("Prediction Window", 3, 1, 20, 1, "The number of future ticks to calculate to stay ahead of movement.");

    private final Setting<Boolean> attack = this.sgAttack.booleanSetting("Anti-Obstruction", true, "Automatically destroys entities like crystals that prevent block placement.");
    private final Setting<Double> attackSpeed = this.sgAttack.doubleSetting("Attack Frequency", 4.0, 0.0, 20.0, 0.05, "The number of attack attempts per second.", this.attack::get);
    private final Setting<Double> renderTime = this.sgAttack.doubleSetting("Render Duration", 1.0, 0.0, 5.0, 0.1, "How long the visualization of the target remains visible.", this.attack::get);

    private final Setting<Boolean> drawBlocks = this.sgRender.booleanSetting("Inventory Overlay", true, "Displays the remaining block count on the HUD.");
    private final Setting<BlackOutColor> customColor = this.sgRender.colorSetting("Text Accent", new BlackOutColor(255, 255, 255, 255), "Color of the block count text.", this.drawBlocks::get);
    private final Setting<Boolean> bg = this.sgRender.booleanSetting("Overlay Background", true, "Renders a background behind the HUD element.", this.drawBlocks::get);
    private final Setting<BlackOutColor> bgColor = this.sgRender.colorSetting("Background Fill", new BlackOutColor(0, 0, 0, 50), "Color of the HUD background.", () -> this.drawBlocks.get() && this.bg.get());
    private final Setting<Boolean> blur = this.sgRender.booleanSetting("Glow Effect", true, "Adds a blur effect to the inventory overlay.", this.drawBlocks::get);
    private final Setting<Boolean> shadow = this.sgRender.booleanSetting("Drop Shadow", true, "Adds a depth shadow to the overlay.", this.drawBlocks::get);
    private final Setting<BlackOutColor> shadowColor = this.sgRender.colorSetting("Shadow Tint", new BlackOutColor(0, 0, 0, 100), "Color of the drop shadow.", () -> this.drawBlocks.get() && this.bg.get() && this.shadow.get());
    private final Setting<Boolean> placeSwing = this.sgRender.booleanSetting("Placement Animation", true, "Triggers a client-side hand swing when a block is placed.");
    private final Setting<SwingHand> placeHand = this.sgRender.enumSetting("Placement Hand", SwingHand.RealHand, "The hand used for the placement animation.", this.placeSwing::get);
    private final Setting<Boolean> attackSwing = this.sgRender.booleanSetting("Attack Animation", true, "Triggers a client-side hand swing when clearing obstructions.");
    private final Setting<SwingHand> attackHand = this.sgRender.enumSetting("Attack Hand", SwingHand.RealHand, "The hand used for the attack animation.", this.attackSwing::get);
    private final Setting<RenderMode> renderMode = this.sgRender.enumSetting("Visual Mode", RenderMode.Placed, "Controls which target positions are highlighted in the world.");
    private final BoxMultiSetting rendering = BoxMultiSetting.of(this.sgRender);

    private final PoseStack stack = new PoseStack();
    private final TimerList<BlockPos> placed = new TimerList<>(true);
    private final List<BlockPos> positions = new ArrayList<>();
    private final List<BlockPos> valids = new ArrayList<>();
    private final List<AABB> boxes = new ArrayList<>();
    private final TimerList<BlockPos> render = new TimerList<>(false);
    private final float[] velocities = new float[]{0.42F, 0.3332F, 0.2468F};
    private final float[] slowVelocities = new float[]{0.42F, 0.3332F, 0.2468F, 0.0F};
    float delta = 0.0F;
    private int placeTickTimer = 0;
    private double placeTimer = 0.0;
    private long lastAttack = 0L;
    private int placesLeft = 0;
    private int blocksLeft = 0;
    private boolean changedTimer = false;
    private Vec3 movement = Vec3.ZERO;
    private FindResult result = null;
    private boolean switched = false;
    private InteractionHand hand = null;
    private boolean towerRotate = false;
    private int jumpProgress = -1;
    private double startY = 0.0;
    public Scaffold() {
        super("Scaffold", "Constructs a walking surface beneath the player in real-time to facilitate rapid travel and bridging.", SubCategory.MOVEMENT);
        INSTANCE = this;
    }

    public static Scaffold getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        if (!this.constantRotate.get()) {
            this.end("placing");
        }

        this.startY = BlackOut.mc.player.getY();
    }

    @Override
    public void onDisable() {
        this.placeTimer = 0.0;
        this.delta = 0.0F;
        this.placesLeft = this.places.get();
        if (this.changedTimer) {
            Timer.reset();
            this.changedTimer = false;
        }
    }

    @Override
    protected double getRotationTime() {
        return this.rotationTime.get();
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        this.render.update();
        switch (this.renderMode.get()) {
            case Placed:
                this.render.forEach(timer -> {
                    double progress = 1.0 - Mth.clamp(Mth.inverseLerp(System.currentTimeMillis(), timer.startTime, timer.endTime), 0.0, 1.0);
                    this.rendering.render(BoxUtils.get(timer.value), (float) progress, 1.0F);
                });
                break;
            case NotPlaced:
                this.positions.forEach(pos -> this.rendering.render(BoxUtils.get(pos), 1.0F, 1.0F));
                this.render.forEach(timer -> {
                    double progress = 1.0 - Mth.clamp(Mth.inverseLerp(System.currentTimeMillis(), timer.startTime, timer.endTime), 0.0, 1.0);
                    this.rendering.render(BoxUtils.get(timer.value), (float) progress, 1.0F);
                });
        }
    }

    @Event
    public void onRenderHud(RenderEvent.Hud.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.level != null) {
            this.updateResult();
            InteractionHand hand = OLEPOSSUtils.getHand(this::valid);
            ItemStack itemStack;
            if (hand != null) {
                itemStack = Managers.PACKET.stackInHand(hand);
            } else {
                if (!this.result.wasFound()) {
                    return;
                }

                itemStack = this.result.stack();
            }

            String text = String.valueOf(itemStack.getCount());
            float textScale = 3.0F;
            float width = BlackOut.FONT.getWidth(text) * textScale + 26.0F;
            float height = BlackOut.FONT.getHeight() * textScale;
            if (this.enabled) {
                this.delta = (float) Math.min(this.delta + event.frameTime * 4.0, 1.0);
            } else {
                this.delta = (float) Math.max(this.delta - event.frameTime * 4.0, 0.0);
            }

            if (this.drawBlocks.get()) {
                this.stack.pushPose();
                RenderUtils.unGuiScale(this.stack);
                float anim = (float) AnimUtils.easeOutQuart(this.delta);
                this.stack
                        .translate(
                                BlackOut.mc.getWindow().getScreenWidth() / 2.0F - width / 2.0F, BlackOut.mc.getWindow().getScreenHeight() / 2.0F + height + 2.0F, 0.0F
                        );
                this.stack.scale(anim, anim, 1.0F);
                float prevAlpha = Renderer.getAlpha();
                Renderer.setAlpha(anim);
                this.stack.pushPose();
                this.stack.translate(width / -2.0F + width / 2.0F, (height + 2.0F) / -2.0F + height / 2.0F, 0.0F);
                if (this.blur.get()) {
                    RenderUtils.drawLoadedBlur("hudblur", this.stack, renderer -> renderer.rounded(0.0F, 0.0F, width, height, 6.0F, 10));
                    Renderer.onHUDBlur();
                }

                if (this.bg.get()) {
                    RenderUtils.rounded(
                            this.stack, 0.0F, 0.0F, width, height, 6.0F, this.shadow.get() ? 6.0F : 0.0F, this.bgColor.get().getRGB(), this.shadowColor.get().getRGB()
                    );
                }

                RenderUtils.renderItem(this.stack, itemStack, 3.0F, 3.0F, 24.0F, RenderLayer.HUD, false);
                BlackOut.FONT.text(this.stack, text, textScale, 26.0F, 1.0F, this.customColor.get().getColor(), false, false);
                Renderer.setAlpha(prevAlpha);
                this.stack.popPose();
                this.stack.popPose();
            }
        }
    }

    @Override
    public void preTick() {
        if (BlackOut.mc.player != null && BlackOut.mc.level != null) {
            this.placeTickTimer++;
            if (this.useTimer.get()) {
                Timer.set(this.timer.get().floatValue());
                this.changedTimer = true;
            }

            super.preTick();
        }
    }

    @Override
    public void postMove() {
        if (BlackOut.mc.player != null && BlackOut.mc.level != null) {
            super.postMove();
        }
    }

    @Override
    public void postTick() {
        if (BlackOut.mc.player != null && BlackOut.mc.level != null) {
            if (BlackOut.mc.player.onGround()) {
                this.startY = BlackOut.mc.player.getY();
            }

            if (this.canTower() && this.towerRotate && SettingUtils.shouldRotate(RotationType.BlockPlace)) {
                PlaceData data = SettingUtils.getPlaceData(BlackOut.mc.player.blockPosition(), null, null);
                if (data.valid()) {
                    this.rotateBlock(data, RotationType.BlockPlace, -0.1, "tower");
                }
            }

            super.postTick();
        }
    }

    @Event
    public void onMove(MoveEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.level != null) {
            this.movement = event.movement;
            this.updateTower(event);
        }
    }

    private void updateTower(MoveEvent.Pre event) {
        if (this.canTower()) {
            switch (this.tower.get()) {
                case NCP:
                    if (BlackOut.mc.options.keyJump.isDown()
                            && (this.towerMoving.get() || BlackOut.mc.player.input.forwardImpulse == 0.0F && BlackOut.mc.player.input.leftImpulse == 0.0F)) {
                        this.towerRotate = true;
                        if (BlackOut.mc.player.onGround() || this.jumpProgress == 3) {
                            this.jumpProgress = 0;
                        }

                        if (this.jumpProgress > -1 && this.jumpProgress < 3) {
                            if (!this.towerMoving.get()) {
                                event.setXZ(this, 0.0, 0.0);
                            }

                            event.setY(this, this.velocities[this.jumpProgress]);
                            ((IVec3) BlackOut.mc.player.getDeltaMovement()).blackout_Client$setY(this.velocities[this.jumpProgress]);
                            this.jumpProgress++;
                        }
                    } else {
                        this.jumpProgress = -1;
                        this.towerRotate = false;
                    }
                    break;
                case SlowNCP:
                    if (BlackOut.mc.options.keyJump.isDown()
                            && (this.towerMoving.get() || BlackOut.mc.player.input.forwardImpulse == 0.0F && BlackOut.mc.player.input.leftImpulse == 0.0F)) {
                        this.towerRotate = true;
                        if (BlackOut.mc.player.onGround() || this.jumpProgress == 4) {
                            this.jumpProgress = 0;
                        }

                        if (this.jumpProgress > -1 && this.jumpProgress < 4) {
                            if (!this.towerMoving.get()) {
                                event.setXZ(this, 0.0, 0.0);
                            }

                            event.setY(this, this.slowVelocities[this.jumpProgress]);
                            ((IVec3) BlackOut.mc.player.getDeltaMovement()).blackout_Client$setY(this.slowVelocities[this.jumpProgress]);
                            this.jumpProgress++;
                        }
                    } else {
                        this.jumpProgress = -1;
                        this.towerRotate = false;
                    }
                    break;
                case TP:
                    if (BlackOut.mc.options.keyJump.isDown()
                            && (this.towerMoving.get() || BlackOut.mc.player.input.forwardImpulse == 0.0F && BlackOut.mc.player.input.leftImpulse == 0.0F)) {
                        this.towerRotate = true;
                        if (BlackOut.mc.player.onGround() || this.jumpProgress == 1) {
                            this.jumpProgress = 0;
                        }

                        if (this.jumpProgress == 0) {
                            if (!this.towerMoving.get()) {
                                event.setXZ(this, 0.0, 0.0);
                            }

                            event.setY(this, 1.0);
                            ((IVec3) BlackOut.mc.player.getDeltaMovement()).blackout_Client$setY(1.0);
                            this.jumpProgress++;
                        }
                    } else {
                        this.jumpProgress = -1;
                        this.towerRotate = false;
                    }
                    break;
                case Disabled:
                    if (BlackOut.mc.options.keyJump.isDown()
                            && (this.towerMoving.get() || BlackOut.mc.player.input.forwardImpulse == 0.0F && BlackOut.mc.player.input.leftImpulse == 0.0F)) {
                        this.towerRotate = true;
                        if (BlackOut.mc.player.onGround() || this.jumpProgress == 1) {
                            this.jumpProgress = 0;
                        }

                        if (this.jumpProgress == 0) {
                            this.jumpProgress++;
                        }
                    } else {
                        this.jumpProgress = -1;
                        this.towerRotate = false;
                    }
            }
        }
    }

    @Override
    protected void update(boolean allowAction, boolean fakePos) {
        if (fakePos) {
            this.updateBlocks(this.movement);
        }

        this.placeBlocks(allowAction);
    }

    private boolean canTower() {
        return (!this.shouldKeepY() || !(BlackOut.mc.player.getY() >= this.startY)) && (OLEPOSSUtils.getHand(this::valid) != null || this.updateResult().wasFound());
    }

    private void placeBlocks(boolean allowAction) {
        this.valids.clear();
        this.valids.addAll(this.positions.stream().filter(this::validBlock).toList());
        this.updateAttack(allowAction);
        this.updateResult();
        this.updatePlaces();
        this.blocksLeft = Math.min(this.placesLeft, this.result.amount());
        this.hand = this.getHand();
        this.switched = false;
        this.positions
                .stream()
                .filter(pos -> !EntityUtils.intersects(BoxUtils.get(pos), this::validEntity))
                .sorted(Comparator.comparingDouble(RotationUtils::getYaw))
                .forEach(pos -> this.place(pos, allowAction));
        if (this.switched && this.hand == null) {
            this.switchMode.get().swapBack();
        }
    }

    private FindResult updateResult() {
        return this.result = this.switchMode.get().find(this::valid);
    }

    private void updateBlocks(Vec3 motion) {
        this.boxes.clear();
        this.positions.clear();
        Direction[] directions = this.getDirections(motion);
        AABB box = BlackOut.mc.player.getBoundingBox();
        if (this.shouldKeepY()) {
            double offset = box.minY - Math.min(this.startY, box.minY);
            box = box.setMaxY(box.maxY - offset);
            box = box.setMinY(box.minY - offset);
        }

        this.addBlocks(box, directions, this.support.get());
        double x = motion.x;
        double y = motion.y;
        double z = motion.z;
        boolean onGround = this.inside(box.move(0.0, -0.04, 0.0));

        for (int i = 0; i < this.extrapolation.get(); i++) {
            if (!this.smart.get() || !this.inside(box.move(x, 0.0, 0.0))) {
                box = box.move(x, 0.0, 0.0);
            }

            if (!this.smart.get() || !this.inside(box.move(0.0, 0.0, z))) {
                box = box.move(0.0, 0.0, z);
            }

            if (!this.shouldKeepY()) {
                if (onGround) {
                    if (BlackOut.mc.options.keyJump.isDown()) {
                        y = 0.42;
                    } else {
                        y = 0.0;
                    }
                }

                if (!this.inside(box.move(0.0, y, 0.0))) {
                    if (box.minY + y <= Math.floor(box.minY)) {
                        box = box.move(0.0, -(box.minY % 1.0), 0.0);
                    } else {
                        box = box.move(0.0, y, 0.0);
                    }
                }

                onGround = this.inside(box.move(0.0, -0.04, 0.0)) || box.minY % 1.0 == 0.0;
                y = (y - 0.08) * 0.98;
            }

            this.boxes.add(box);
            this.addBlocks(box, directions, 1);
        }
    }

    private boolean shouldKeepY() {
        return (!this.allowTower.get() || !(this.movement.horizontalDistance() < 0.1)) && this.keepY.get();
    }

    private boolean inside(AABB box) {
        return OLEPOSSUtils.inside(BlackOut.mc.player, box);
    }

    private boolean addBlocks2(AABB box, Direction[] directions, int b) {
        BlockPos feetPos = BlockPos.containing(BoxUtils.feet(box).add(0.0, -0.5, 0.0));
        if (OLEPOSSUtils.replaceable(feetPos) && !this.positions.contains(feetPos) && !this.intersects(feetPos)) {
            if (b < 1 && this.validSupport(feetPos, true)) {
                this.positions.add(feetPos);
                return true;
            } else {
                int l = directions.length;
                Direction[] drr = new Direction[b];

                for (int i = 0; i < Math.pow(l, b); i++) {
                    for (int j = 0; j < b; j++) {
                        Direction dir = directions[i / (int) Math.pow(l, j) % l];
                        drr[b - j - 1] = dir;
                    }

                    if (this.validSupport(feetPos, false, drr)) {
                        BlockPos pos = feetPos;
                        this.addPos(feetPos);

                        for (Direction dir : drr) {
                            pos = pos.relative(dir);
                            this.addPos(pos);
                        }

                        return true;
                    }
                }

                return false;
            }
        } else {
            return true;
        }
    }

    private void addPos(BlockPos pos) {
        if (OLEPOSSUtils.replaceable(pos) && !this.positions.contains(pos)) {
            this.positions.addFirst(pos);
        }
    }

    private void addBlocks(AABB box, Direction[] directions, int max) {
        for (int i = 0; i < max; i++) {
            if (this.addBlocks2(box, directions, i)) {
                return;
            }
        }
    }

    private boolean validSupport(BlockPos feet, boolean useFeet, Direction... dirs) {
        BlockPos pos = feet;
        if (!useFeet) {
            for (Direction dir : dirs) {
                pos = pos.relative(dir);
            }
        }

        return !this.positions.contains(pos) && OLEPOSSUtils.replaceable(pos) && !this.intersects(pos) && SettingUtils.getPlaceData(pos, (p, d) -> this.placed.contains(p) || this.positions.contains(p), null).valid();
    }

    private boolean intersects(BlockPos pos) {
        AABB box = BoxUtils.get(pos);

        for (AABB bb : this.boxes) {
            if (bb.intersects(box)) {
                return true;
            }
        }

        return EntityUtils.intersects(BoxUtils.get(pos), entity -> !(entity instanceof ItemEntity));
    }

    private Direction[] getDirections(Vec3 motion) {
        double dir = RotationUtils.getYaw(new Vec3(0.0, 0.0, 0.0), motion, 0.0);
        Direction moveDir = Direction.fromYRot(dir);
        return new Direction[]{moveDir.getOpposite(), moveDir, moveDir.getCounterClockWise(), moveDir.getClockWise(), Direction.UP, Direction.DOWN};
    }

    private boolean validBlock(BlockPos pos) {
        if (!OLEPOSSUtils.replaceable(pos)) {
            return false;
        } else {
            PlaceData data = SettingUtils.getPlaceData(pos, (p, d) -> this.placed.contains(p), null);
            if (!data.valid()) {
                return false;
            } else {
                return SettingUtils.inPlaceRange(data.pos()) && !this.placed.contains(pos);
            }
        }
    }

    private void updateAttack(boolean allowAction) {
        if (this.attack.get()) {
            if (!(System.currentTimeMillis() - this.lastAttack < 1000.0 / this.attackSpeed.get())) {
                Entity blocking = this.getBlocking();
                if (blocking != null) {
                    if (!SettingUtils.shouldRotate(RotationType.Attacking) || this.attackRotate(blocking.getBoundingBox(), -0.1, "attacking")) {
                        if (allowAction) {
                            this.attackEntity(blocking);
                            if (SettingUtils.shouldRotate(RotationType.Attacking) && this.constantRotate.get()) {
                                this.end("attacking");
                            }

                            if (this.attackSwing.get()) {
                                this.clientSwing(this.attackHand.get(), InteractionHand.MAIN_HAND);
                            }

                            this.lastAttack = System.currentTimeMillis();
                        }
                    }
                }
            }
        }
    }

    private void place(BlockPos pos, boolean allowAction) {
        if (this.validBlock(pos)) {
            if (this.result.amount() > 0) {
                if (this.blocksLeft > 0) {
                    PlaceData data = SettingUtils.getPlaceData(pos, (p, d) -> this.placed.contains(p), null);
                    if (SettingUtils.shouldRotate(RotationType.BlockPlace)) {
                        Rotation rotation = SettingUtils.getRotation(data.pos(), data.dir(), data.pos().getCenter(), RotationType.BlockPlace);
                        Vec3 vec = this.getRotationVec(data.pos(), rotation.pitch());
                        if (vec != null) {
                            if (!this.rotateBlock(data.pos(), data.dir(), vec, RotationType.BlockPlace.withInstant(this.instantRotate.get()), "placing")) {
                                return;
                            }
                        } else if (!this.rotateBlock(data, RotationType.BlockPlace.withInstant(this.instantRotate.get()), "placing")) {
                            return;
                        }
                    }

                    if (allowAction) {
                        if (this.switched || this.hand != null || (this.switched = this.switchMode.get().swap(this.result.slot()))) {
                            this.placeBlock(this.hand, data.pos().getCenter(), data.dir(), data.pos());
                            this.setBlock(pos);
                            this.render.add(pos, this.renderTime.get());
                            if (this.placeSwing.get()) {
                                this.clientSwing(this.placeHand.get(), this.hand);
                            }

                            this.placed.add(pos, this.cooldown.get());
                            this.blocksLeft--;
                            this.placesLeft--;
                            if (SettingUtils.shouldRotate(RotationType.BlockPlace) && !this.constantRotate.get()) {
                                this.end("placing");
                            }
                        }
                    }
                }
            }
        }
    }

    private Vec3 getRotationVec(BlockPos pos, double pitch) {
        double yaw;
        if (this.movement.horizontalDistanceSqr() > 0.0) {
            switch (this.smartYaw.get()) {
                case SemiLocked:
                    yaw = Math.round(RotationUtils.getYaw(this.movement, Vec3.ZERO, 0.0) / 45.0) * 45L;
                    break;
                case Locked:
                    yaw = Math.round(RotationUtils.getYaw(this.movement, Vec3.ZERO, 0.0) / 90.0) * 90L;
                    break;
                case Back:
                    yaw = RotationUtils.getYaw(this.movement, Vec3.ZERO, 0.0);
                    break;
                default:
                    return null;
            }
        } else {
            yaw = Managers.ROTATION.prevYaw;
            if (pitch == 90.0) {
                pitch--;
            }
        }

        return BoxUtils.clamp(
                RotationUtils.rotationVec(yaw, pitch, BlackOut.mc.player.getEyePosition(), BlackOut.mc.player.getEyePosition().distanceTo(pos.getCenter())),
                BoxUtils.get(pos)
        );
    }

    private void setBlock(BlockPos pos) {
        if (BlackOut.mc.player.getInventory().getItem(this.result.slot()).getItem() instanceof BlockItem block) {
            Managers.PACKET.addToQueue(handler -> {
                BlackOut.mc.level.setBlockAndUpdate(pos, block.getBlock().defaultBlockState());
                this.blockPlaceSound(pos, block);
            });
        }
    }

    private boolean validEntity(Entity entity) {
        return (!(entity instanceof EndCrystal) || System.currentTimeMillis() - this.lastAttack >= 100L) && !(entity instanceof ItemEntity);
    }

    private Entity getBlocking() {
        Entity crystal = null;
        double lowest = 1000.0;

        for (Entity entity : BlackOut.mc.level.entitiesForRendering()) {
            if (entity instanceof EndCrystal && !(BlackOut.mc.player.distanceTo(entity) > 5.0F) && SettingUtils.inAttackRange(entity.getBoundingBox())) {
                for (BlockPos pos : this.valids) {
                    if (BoxUtils.get(pos).intersects(entity.getBoundingBox())) {
                        double dmg = DamageUtils.crystalDamage(BlackOut.mc.player, BlackOut.mc.player.getBoundingBox(), entity.position());
                        if (dmg < lowest) {
                            crystal = entity;
                            lowest = dmg;
                        }
                    }
                }
            }
        }

        return crystal;
    }

    private void updatePlaces() {
        switch (this.placeDelayMode.get()) {
            case Ticks:
                if (this.placesLeft >= this.places.get() || this.placeTickTimer >= this.placeDelayT.get()) {
                    this.placesLeft = this.places.get();
                    this.placeTickTimer = 0;
                }
                break;
            case Seconds:
                if (this.placesLeft >= this.places.get() || this.placeTimer >= this.placeDelayS.get()) {
                    this.placesLeft = this.places.get();
                    this.placeTimer = 0.0;
                }
        }
    }

    private boolean valid(ItemStack stack) {
        return stack.getItem() instanceof BlockItem block && this.blocks.get().contains(block.getBlock());
    }

    private InteractionHand getHand() {
        if (this.valid(Managers.PACKET.getStack())) {
            return InteractionHand.MAIN_HAND;
        } else {
            return this.valid(BlackOut.mc.player.getOffhandItem()) ? InteractionHand.OFF_HAND : null;
        }
    }

    public enum RenderMode {
        Placed,
        NotPlaced
    }

    public enum TowerMode {
        Disabled,
        NCP,
        SlowNCP,
        TP
    }

    public enum YawMode {
        Normal,
        SemiLocked,
        Locked,
        Back
    }

    public record Render(BlockPos pos, long time) {
    }
}
