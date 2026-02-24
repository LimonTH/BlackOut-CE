package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RotationType;
import bodevelopment.client.blackout.enums.SwingHand;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.interfaces.mixin.IVec3d;
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
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Scaffold extends MoveUpdateModule {
    private static Scaffold INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<Boolean> safeWalk = this.sgGeneral.booleanSetting("Safe Walk", true, "Stops you from falling off.");
    private final SettingGroup sgPlacing = this.addGroup("Placing");
    private final SettingGroup sgAttack = this.addGroup("Attack");
    private final SettingGroup sgRender = this.addGroup("Render");
    private final Setting<Boolean> smart = this.sgGeneral.booleanSetting("Smart", true, "Only places on blocks that you can reach.");
    private final Setting<TowerMode> tower = this.sgGeneral.enumSetting("Tower", TowerMode.NCP, "Flies up with blocks.");
    private final Setting<Boolean> towerMoving = this.sgGeneral.booleanSetting("Moving Tower", false, "Allows you to move while towering.");
    private final Setting<Boolean> useTimer = this.sgGeneral.booleanSetting("Use Timer", false, "Should we use timer.");
    private final Setting<Double> timer = this.sgGeneral.doubleSetting("Timer", 1.088, 0.0, 10.0, 0.1, "Should we use timer.", this.useTimer::get);
    private final Setting<Boolean> constantRotate = this.sgGeneral.booleanSetting("Constant Rotate", false, "Stops you from falling off.");
    private final Setting<Double> rotationTime = this.sgGeneral.doubleSetting("Rotation Time", 1.0, 0.0, 1.0, 0.01, "Keeps rotations for x seconds.");
    private final Setting<Integer> support = this.sgPlacing.intSetting("Support", 3, 0, 5, 1, "Max amount of support blocks.");
    private final Setting<Boolean> keepY = this.sgGeneral.booleanSetting("Keep Y", false, ".");
    private final Setting<Boolean> allowTower = this.sgGeneral.booleanSetting("Allow Tower", true, "Doesn't keep y while standing still.", this.keepY::get);
    private final Setting<YawMode> smartYaw = this.sgGeneral.enumSetting("Yaw Mode", YawMode.Normal, ".");
    private final Setting<SwitchMode> switchMode = this.sgPlacing.enumSetting("Switch Mode", SwitchMode.Normal, "Method of switching. Silent is the most reliable.");
    private final Setting<List<Block>> blocks = this.sgPlacing
            .blockListSetting(
                    "Blocks",
                    "Blocks to use.",
                    Blocks.OBSIDIAN,
                    Blocks.CRYING_OBSIDIAN,
                    Blocks.NETHERITE_BLOCK,
                    Blocks.STONE,
                    Blocks.OAK_PLANKS,
                    Blocks.TNT
            );
    private final Setting<Surround.PlaceDelayMode> placeDelayMode = this.sgPlacing.enumSetting("Place Delay Mode", Surround.PlaceDelayMode.Ticks, ".");
    private final Setting<Integer> placeDelayT = this.sgPlacing
            .intSetting("Place Tick Delay", 1, 0, 20, 1, "Tick delay between places.", () -> this.placeDelayMode.get() == Surround.PlaceDelayMode.Ticks);
    private final Setting<Double> placeDelayS = this.sgPlacing
            .doubleSetting("Place Delay", 0.1, 0.0, 1.0, 0.01, "Delay between places.", () -> this.placeDelayMode.get() == Surround.PlaceDelayMode.Seconds);
    private final Setting<Integer> places = this.sgPlacing.intSetting("Places", 1, 1, 20, 1, "How many blocks to place each time.");
    private final Setting<Double> cooldown = this.sgPlacing
            .doubleSetting("Cooldown", 0.3, 0.0, 1.0, 0.01, "Waits x seconds before trying to place at the same position if there is more than 1 missing block.");
    private final Setting<Integer> extrapolation = this.sgPlacing.intSetting("Extrapolation", 3, 1, 20, 1, "Predicts movement.");
    private final Setting<Boolean> instantRotate = this.sgGeneral.booleanSetting("Instant Rotate", true, "Ignores rotation speed limit.");
    private final Setting<Boolean> attack = this.sgAttack.booleanSetting("Attack", true, "Attacks crystals blocking surround.");
    private final Setting<Double> attackSpeed = this.sgAttack
            .doubleSetting("Attack Speed", 4.0, 0.0, 20.0, 0.05, "How many times to attack every second.", this.attack::get);
    private final Setting<Double> renderTime = this.sgAttack.doubleSetting("Render Time", 1.0, 0.0, 5.0, 0.1, "How many times to attack every second.", this.attack::get);
    private final Setting<Boolean> drawBlocks = this.sgRender.booleanSetting("Show Blocks", true, "Draws the amount of blocks you have");
    private final Setting<BlackOutColor> customColor = this.sgRender.colorSetting("Text Color", new BlackOutColor(255, 255, 255, 255), "Text Color", this.drawBlocks::get);
    private final Setting<Boolean> bg = this.sgRender.booleanSetting("Background", true, "Draws a background", this.drawBlocks::get);
    private final Setting<BlackOutColor> bgColor = this.sgRender
            .colorSetting("Background Color", new BlackOutColor(0, 0, 0, 50), ".", () -> this.drawBlocks.get() && this.bg.get());
    private final Setting<Boolean> blur = this.sgRender.booleanSetting("Block Blur", true, ".", this.drawBlocks::get);
    private final Setting<Boolean> shadow = this.sgRender.booleanSetting("Shadow", true, ".", this.drawBlocks::get);
    private final Setting<BlackOutColor> shadowColor = this.sgRender
            .colorSetting("Shadow Color", new BlackOutColor(0, 0, 0, 100), ".", () -> this.drawBlocks.get() && this.bg.get() && this.shadow.get());
    private final Setting<Boolean> placeSwing = this.sgRender.booleanSetting("Place Swing", true, "Renders swing animation when placing a block.");
    private final Setting<SwingHand> placeHand = this.sgRender.enumSetting("Place Swing Hand", SwingHand.RealHand, "Which hand should be swung.", this.placeSwing::get);
    private final Setting<Boolean> attackSwing = this.sgRender.booleanSetting("Attack Swing", true, "Renders swing animation when attacking a block.");
    private final Setting<SwingHand> attackHand = this.sgRender.enumSetting("Attack Swing Hand", SwingHand.RealHand, "Which hand should be swung.", this.attackSwing::get);
    private final Setting<RenderMode> renderMode = this.sgRender.enumSetting("Render Mode", RenderMode.Placed, "Which parts should be rendered.");
    private final BoxMultiSetting rendering = BoxMultiSetting.of(this.sgRender);
    private final MatrixStack stack = new MatrixStack();
    private final TimerList<BlockPos> placed = new TimerList<>(true);
    private final List<BlockPos> positions = new ArrayList<>();
    private final List<BlockPos> valids = new ArrayList<>();
    private final List<Box> boxes = new ArrayList<>();
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
    private Vec3d movement = Vec3d.ZERO;
    private FindResult result = null;
    private boolean switched = false;
    private Hand hand = null;
    private boolean towerRotate = false;
    private int jumpProgress = -1;
    private double startY = 0.0;

    public Scaffold() {
        super("Scaffold", "Places blocks under your feet.", SubCategory.MOVEMENT);
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
                    double progress = 1.0 - MathHelper.clamp(MathHelper.getLerpProgress(System.currentTimeMillis(), timer.startTime, timer.endTime), 0.0, 1.0);
                    this.rendering.render(BoxUtils.get(timer.value), (float) progress, 1.0F);
                });
                break;
            case NotPlaced:
                this.positions.forEach(pos -> this.rendering.render(BoxUtils.get(pos), 1.0F, 1.0F));
                this.render.forEach(timer -> {
                    double progress = 1.0 - MathHelper.clamp(MathHelper.getLerpProgress(System.currentTimeMillis(), timer.startTime, timer.endTime), 0.0, 1.0);
                    this.rendering.render(BoxUtils.get(timer.value), (float) progress, 1.0F);
                });
        }
    }

    @Event
    public void onRenderHud(RenderEvent.Hud.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            this.updateResult();
            Hand hand = OLEPOSSUtils.getHand(this::valid);
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
                this.stack.push();
                RenderUtils.unGuiScale(this.stack);
                float anim = (float) AnimUtils.easeOutQuart(this.delta);
                this.stack
                        .translate(
                                BlackOut.mc.getWindow().getWidth() / 2.0F - width / 2.0F, BlackOut.mc.getWindow().getHeight() / 2.0F + height + 2.0F, 0.0F
                        );
                this.stack.scale(anim, anim, 1.0F);
                float prevAlpha = Renderer.getAlpha();
                Renderer.setAlpha(anim);
                this.stack.push();
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

                RenderUtils.renderItem(this.stack, itemStack, 3.0F, 3.0F, 24.0F);
                BlackOut.FONT.text(this.stack, text, textScale, 26.0F, 1.0F, this.customColor.get().getColor(), false, false);
                Renderer.setAlpha(prevAlpha);
                this.stack.pop();
                this.stack.pop();
            }
        }
    }

    @Override
    public void preTick() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
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
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            super.postMove();
        }
    }

    @Override
    public void postTick() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (BlackOut.mc.player.isOnGround()) {
                this.startY = BlackOut.mc.player.getY();
            }

            if (this.canTower() && this.towerRotate && SettingUtils.shouldRotate(RotationType.BlockPlace)) {
                PlaceData data = SettingUtils.getPlaceData(BlackOut.mc.player.getBlockPos(), null, null);
                if (data.valid()) {
                    this.rotateBlock(data, RotationType.BlockPlace, -0.1, "tower");
                }
            }

            super.postTick();
        }
    }

    @Event
    public void onMove(MoveEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            this.movement = event.movement;
            this.updateTower(event);
        }
    }

    private void updateTower(MoveEvent.Pre event) {
        if (this.canTower()) {
            switch (this.tower.get()) {
                case NCP:
                    if (BlackOut.mc.options.jumpKey.isPressed()
                            && (this.towerMoving.get() || BlackOut.mc.player.input.movementForward == 0.0F && BlackOut.mc.player.input.movementSideways == 0.0F)) {
                        this.towerRotate = true;
                        if (BlackOut.mc.player.isOnGround() || this.jumpProgress == 3) {
                            this.jumpProgress = 0;
                        }

                        if (this.jumpProgress > -1 && this.jumpProgress < 3) {
                            if (!this.towerMoving.get()) {
                                event.setXZ(this, 0.0, 0.0);
                            }

                            event.setY(this, this.velocities[this.jumpProgress]);
                            ((IVec3d) BlackOut.mc.player.getVelocity()).blackout_Client$setY(this.velocities[this.jumpProgress]);
                            this.jumpProgress++;
                        }
                    } else {
                        this.jumpProgress = -1;
                        this.towerRotate = false;
                    }
                    break;
                case SlowNCP:
                    if (BlackOut.mc.options.jumpKey.isPressed()
                            && (this.towerMoving.get() || BlackOut.mc.player.input.movementForward == 0.0F && BlackOut.mc.player.input.movementSideways == 0.0F)) {
                        this.towerRotate = true;
                        if (BlackOut.mc.player.isOnGround() || this.jumpProgress == 4) {
                            this.jumpProgress = 0;
                        }

                        if (this.jumpProgress > -1 && this.jumpProgress < 4) {
                            if (!this.towerMoving.get()) {
                                event.setXZ(this, 0.0, 0.0);
                            }

                            event.setY(this, this.slowVelocities[this.jumpProgress]);
                            ((IVec3d) BlackOut.mc.player.getVelocity()).blackout_Client$setY(this.slowVelocities[this.jumpProgress]);
                            this.jumpProgress++;
                        }
                    } else {
                        this.jumpProgress = -1;
                        this.towerRotate = false;
                    }
                    break;
                case TP:
                    if (BlackOut.mc.options.jumpKey.isPressed()
                            && (this.towerMoving.get() || BlackOut.mc.player.input.movementForward == 0.0F && BlackOut.mc.player.input.movementSideways == 0.0F)) {
                        this.towerRotate = true;
                        if (BlackOut.mc.player.isOnGround() || this.jumpProgress == 1) {
                            this.jumpProgress = 0;
                        }

                        if (this.jumpProgress == 0) {
                            if (!this.towerMoving.get()) {
                                event.setXZ(this, 0.0, 0.0);
                            }

                            event.setY(this, 1.0);
                            ((IVec3d) BlackOut.mc.player.getVelocity()).blackout_Client$setY(1.0);
                            this.jumpProgress++;
                        }
                    } else {
                        this.jumpProgress = -1;
                        this.towerRotate = false;
                    }
                    break;
                case Disabled:
                    if (BlackOut.mc.options.jumpKey.isPressed()
                            && (this.towerMoving.get() || BlackOut.mc.player.input.movementForward == 0.0F && BlackOut.mc.player.input.movementSideways == 0.0F)) {
                        this.towerRotate = true;
                        if (BlackOut.mc.player.isOnGround() || this.jumpProgress == 1) {
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

    private void updateBlocks(Vec3d motion) {
        this.boxes.clear();
        this.positions.clear();
        Direction[] directions = this.getDirections(motion);
        Box box = BlackOut.mc.player.getBoundingBox();
        if (this.shouldKeepY()) {
            double offset = box.minY - Math.min(this.startY, box.minY);
            box = box.withMaxY(box.maxY - offset);
            box = box.withMinY(box.minY - offset);
        }

        this.addBlocks(box, directions, this.support.get());
        double x = motion.x;
        double y = motion.y;
        double z = motion.z;
        boolean onGround = this.inside(box.offset(0.0, -0.04, 0.0));

        for (int i = 0; i < this.extrapolation.get(); i++) {
            if (!this.smart.get() || !this.inside(box.offset(x, 0.0, 0.0))) {
                box = box.offset(x, 0.0, 0.0);
            }

            if (!this.smart.get() || !this.inside(box.offset(0.0, 0.0, z))) {
                box = box.offset(0.0, 0.0, z);
            }

            if (!this.shouldKeepY()) {
                if (onGround) {
                    if (BlackOut.mc.options.jumpKey.isPressed()) {
                        y = 0.42;
                    } else {
                        y = 0.0;
                    }
                }

                if (!this.inside(box.offset(0.0, y, 0.0))) {
                    if (box.minY + y <= Math.floor(box.minY)) {
                        box = box.offset(0.0, -(box.minY % 1.0), 0.0);
                    } else {
                        box = box.offset(0.0, y, 0.0);
                    }
                }

                onGround = this.inside(box.offset(0.0, -0.04, 0.0)) || box.minY % 1.0 == 0.0;
                y = (y - 0.08) * 0.98;
            }

            this.boxes.add(box);
            this.addBlocks(box, directions, 1);
        }
    }

    private boolean shouldKeepY() {
        return (!this.allowTower.get() || !(this.movement.horizontalLength() < 0.1)) && this.keepY.get();
    }

    private boolean inside(Box box) {
        return OLEPOSSUtils.inside(BlackOut.mc.player, box);
    }

    private boolean addBlocks2(Box box, Direction[] directions, int b) {
        BlockPos feetPos = BlockPos.ofFloored(BoxUtils.feet(box).add(0.0, -0.5, 0.0));
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
                            pos = pos.offset(dir);
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
            this.positions.add(0, pos);
        }
    }

    private void addBlocks(Box box, Direction[] directions, int max) {
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
                pos = pos.offset(dir);
            }
        }

        return !this.positions.contains(pos) && OLEPOSSUtils.replaceable(pos) && !this.intersects(pos) && SettingUtils.getPlaceData(pos, (p, d) -> this.placed.contains(p) || this.positions.contains(p), null).valid();
    }

    private boolean intersects(BlockPos pos) {
        Box box = BoxUtils.get(pos);

        for (Box bb : this.boxes) {
            if (bb.intersects(box)) {
                return true;
            }
        }

        return EntityUtils.intersects(BoxUtils.get(pos), entity -> !(entity instanceof ItemEntity));
    }

    private Direction[] getDirections(Vec3d motion) {
        double dir = RotationUtils.getYaw(new Vec3d(0.0, 0.0, 0.0), motion, 0.0);
        Direction moveDir = Direction.fromRotation(dir);
        return new Direction[]{moveDir.getOpposite(), moveDir, moveDir.rotateYCounterclockwise(), moveDir.rotateYClockwise(), Direction.UP, Direction.DOWN};
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
                                this.clientSwing(this.attackHand.get(), Hand.MAIN_HAND);
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
                        Rotation rotation = SettingUtils.getRotation(data.pos(), data.dir(), data.pos().toCenterPos(), RotationType.BlockPlace);
                        Vec3d vec = this.getRotationVec(data.pos(), rotation.pitch());
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
                            this.placeBlock(this.hand, data.pos().toCenterPos(), data.dir(), data.pos());
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

    private Vec3d getRotationVec(BlockPos pos, double pitch) {
        double yaw;
        if (this.movement.horizontalLengthSquared() > 0.0) {
            switch (this.smartYaw.get()) {
                case SemiLocked:
                    yaw = Math.round(RotationUtils.getYaw(this.movement, Vec3d.ZERO, 0.0) / 45.0) * 45L;
                    break;
                case Locked:
                    yaw = Math.round(RotationUtils.getYaw(this.movement, Vec3d.ZERO, 0.0) / 90.0) * 90L;
                    break;
                case Back:
                    yaw = RotationUtils.getYaw(this.movement, Vec3d.ZERO, 0.0);
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
                RotationUtils.rotationVec(yaw, pitch, BlackOut.mc.player.getEyePos(), BlackOut.mc.player.getEyePos().distanceTo(pos.toCenterPos())),
                BoxUtils.get(pos)
        );
    }

    private void setBlock(BlockPos pos) {
        if (BlackOut.mc.player.getInventory().getStack(this.result.slot()).getItem() instanceof BlockItem block) {
            Managers.PACKET.addToQueue(handler -> {
                BlackOut.mc.world.setBlockState(pos, block.getBlock().getDefaultState());
                this.blockPlaceSound(pos, block);
            });
        }
    }

    private boolean validEntity(Entity entity) {
        return (!(entity instanceof EndCrystalEntity) || System.currentTimeMillis() - this.lastAttack >= 100L) && !(entity instanceof ItemEntity);
    }

    private Entity getBlocking() {
        Entity crystal = null;
        double lowest = 1000.0;

        for (Entity entity : BlackOut.mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity && !(BlackOut.mc.player.distanceTo(entity) > 5.0F) && SettingUtils.inAttackRange(entity.getBoundingBox())) {
                for (BlockPos pos : this.valids) {
                    if (BoxUtils.get(pos).intersects(entity.getBoundingBox())) {
                        double dmg = DamageUtils.crystalDamage(BlackOut.mc.player, BlackOut.mc.player.getBoundingBox(), entity.getPos());
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

    private Hand getHand() {
        if (this.valid(Managers.PACKET.getStack())) {
            return Hand.MAIN_HAND;
        } else {
            return this.valid(BlackOut.mc.player.getOffHandStack()) ? Hand.OFF_HAND : null;
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
