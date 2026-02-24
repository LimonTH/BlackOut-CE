package bodevelopment.client.blackout.module.modules.combat.defensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.enums.RotationType;
import bodevelopment.client.blackout.enums.SwingHand;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.BlockStateEvent;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.randomstuff.Pair;
import bodevelopment.client.blackout.randomstuff.PlaceData;
import bodevelopment.client.blackout.randomstuff.timers.TimerList;
import bodevelopment.client.blackout.util.*;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Blocker extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgProtection = this.addGroup("Protection");
    private final SettingGroup sgSpeed = this.addGroup("Speed");
    private final SettingGroup sgBlocks = this.addGroup("Blocks");
    private final SettingGroup sgAttack = this.addGroup("Attack");
    private final SettingGroup sgDamage = this.addGroup("Damage");
    private final SettingGroup sgRender = this.addGroup("Render");

    private final Setting<Boolean> pauseEat = this.sgGeneral.booleanSetting("Pause Eat", false, "Pauses when eating.");
    private final Setting<SwitchMode> switchMode = this.sgGeneral.enumSetting("Switch Mode", SwitchMode.Silent, "Method of switching. Silent is the most reliable.");
    private final Setting<Double> mineTime = this.sgGeneral
            .doubleSetting("Mine Time", 1.0, 0.0, 10.0, 0.1, "How long do we let enemies mine our surround for before protecting it.");
    private final Setting<Double> maxMineTime = this.sgGeneral.doubleSetting("Max Mine Time", 5.0, 0.0, 10.0, 0.1, "Ignores mining after x seconds.");
    private final Setting<Boolean> packet = this.sgGeneral.booleanSetting("Packet", false, ".");
    private final Setting<Boolean> onlyHole = this.sgGeneral.booleanSetting("Only Hole", false, "Only protects when you are in a hole..");
    private final Setting<Boolean> surroundFloor = this.sgProtection.booleanSetting("Surround Floor", true, "Places blocks around surround floor blocks");
    private final Setting<Boolean> surroundFloorBottom = this.sgProtection.booleanSetting("Surround Floor Bottom", true, "Places blocks under surround floor blocks");
    private final Setting<Boolean> surroundSides = this.sgProtection.booleanSetting("Surround Sides", true, "Places blocks next to surround blocks");
    private final Setting<Boolean> surroundTop = this.sgProtection.booleanSetting("Surround Side Top", true, "Places blocks on top of surround blocks");
    private final Setting<Boolean> surroundBottom = this.sgProtection.booleanSetting("Surround Side Bottom", true, "Places blocks under surround blocks");
    private final Setting<Boolean> trapCev = this.sgProtection.booleanSetting("Trap Cev", true, "Places on top of trap side block");
    private final Setting<Boolean> cev = this.sgProtection.booleanSetting("Cev", true, "Places on top of trap top blocks");
    private final Setting<PlaceDelayMode> placeDelayMode = this.sgSpeed.enumSetting("Place Delay Mode", PlaceDelayMode.Ticks, ".");
    private final Setting<Integer> placeDelayT = this.sgSpeed
            .intSetting("Place Delay Ticks", 1, 0, 20, 1, "Tick delay between places.", () -> this.placeDelayMode.get() == PlaceDelayMode.Ticks);
    private final Setting<Double> placeDelayS = this.sgSpeed
            .doubleSetting("Place Delay", 0.1, 0.0, 1.0, 1.0, "Delay between places.", () -> this.placeDelayMode.get() == PlaceDelayMode.Seconds);
    private final Setting<Integer> places = this.sgSpeed.intSetting("Places", 1, 0, 20, 1, "How many blocks to place each time.");
    private final Setting<Double> cooldown = this.sgSpeed.doubleSetting("Cooldown", 0.5, 0.0, 1.0, 1.0, "Waits x seconds before trying to place at the same position.");
    private final Setting<List<Block>> blocks = this.sgBlocks.blockListSetting("Blocks", "Blocks to use.", Blocks.OBSIDIAN);
    private final Setting<Boolean> attack = this.sgAttack.booleanSetting("Attack", true, "Attacks crystals blocking.");
    private final Setting<Double> attackSpeed = this.sgAttack.doubleSetting("Attack Speed", 4.0, 0.0, 20.0, 0.1, "How many times to attack every second.");
    private final Setting<Boolean> always = this.sgDamage.booleanSetting("Always", true, "Doesn't check for min damage when placing.");
    private final Setting<Double> minDmg = this.sgDamage
            .doubleSetting("Min Damage", 6.0, 0.0, 20.0, 0.1, "Doesn't place if you would take less damage than this.", () -> !this.always.get());
    private final Setting<Boolean> placeSwing = this.sgRender.booleanSetting("Place Swing", true, "Renders swing animation when placing a block.");
    private final Setting<SwingHand> placeHand = this.sgRender.enumSetting("Place Hand", SwingHand.RealHand, "Which hand should be swung.", this.placeSwing::get);
    private final Setting<Boolean> attackSwing = this.sgRender.booleanSetting("Attack Swing", true, "Renders swing animation when attacking a crystal.");
    private final Setting<SwingHand> attackHand = this.sgRender.enumSetting("Attack Hand", SwingHand.RealHand, "Which hand should be swung.", this.attackSwing::get);
    private final Setting<RenderShape> renderShape = this.sgRender.enumSetting("Render Shape", RenderShape.Full, "Which parts should be rendered.");
    private final Setting<BlackOutColor> lineColor = this.sgRender.colorSetting("Line Color", new BlackOutColor(255, 0, 0, 255), ".");
    private final Setting<BlackOutColor> sideColor = this.sgRender.colorSetting("Side Color", new BlackOutColor(255, 0, 0, 50), ".");
    private final TimerList<Pair<BlockPos, Integer>> mining = new TimerList<>(true);
    private final List<ProtectBlock> toProtect = new ArrayList<>();
    private final List<BlockPos> placePositions = new ArrayList<>();
    private final List<Render> render = Collections.synchronizedList(new ArrayList<>());
    private final TimerList<BlockPos> placed = new TimerList<>(false);
    private int blocksLeft = 0;
    private int placesLeft = 0;
    private FindResult result = null;
    private boolean switched = false;
    private Hand hand = null;
    private int tickTimer = 0;
    private double timer = 0.0;
    private long lastTime = 0L;
    private long lastAttack = 0L;

    public Blocker() {
        super("Blocker", "Covers your surround blocks if any enemy tries to mine them.", SubCategory.DEFENSIVE, true);
    }

    @Event
    public void onBlock(BlockStateEvent event) {
        if (event.previousState.getBlock() != event.state.getBlock() && !OLEPOSSUtils.replaceable(event.pos) && this.placePositions.contains(event.pos)) {
            this.render.add(new Render(event.pos, System.currentTimeMillis()));
        }
    }

    @Event
    public void onTickPre(TickEvent.Pre event) {
        this.tickTimer++;
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        this.placed.update();
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            this.timer = this.timer + (System.currentTimeMillis() - this.lastTime) / 1000.0;
            this.lastTime = System.currentTimeMillis();
            this.updateBlocks();
            this.updatePlacing();
            synchronized (this.render) {
                this.render
                        .removeIf(
                                r -> {
                                    if (System.currentTimeMillis() - r.time > 1000L) {
                                        return true;
                                    } else {
                                        double progress = 1.0 - Math.min(System.currentTimeMillis() - r.time, 500L) / 500.0;
                                        Render3DUtils.box(
                                                BoxUtils.get(r.pos), this.sideColor.get().alphaMulti(progress), this.lineColor.get().alphaMulti(progress), this.renderShape.get()
                                        );
                                        return false;
                                    }
                                }
                        );
            }
        }
    }

    @Event
    public void onReceive(PacketEvent.Receive.Pre event) {
        if (event.packet instanceof BlockBreakingProgressS2CPacket p) {
            this.mining.remove(timer -> timer.value.getRight() == p.getEntityId());
            this.mining.add(new Pair<>(p.getPos(), p.getEntityId()), this.maxMineTime.get());
        }
    }

    private void updatePlacing() {
        if (!this.pauseEat.get() || !BlackOut.mc.player.isUsingItem()) {
            this.updateResult();
            this.updatePlaces();
            this.blocksLeft = Math.min(this.placesLeft, this.result.amount());
            this.hand = OLEPOSSUtils.getHand(this::valid);
            this.switched = false;
            this.placePositions.clear();
            this.toProtect.stream().filter(this::shouldProtect).forEach(this::addPlacePositions);
            this.updateAttack();
            this.placePositions
                    .stream()
                    .filter(
                            pos -> !EntityUtils.intersects(BoxUtils.get(pos), entity -> entity instanceof EndCrystalEntity && System.currentTimeMillis() - this.lastAttack > 100L)
                    )
                    .forEach(this::place);
            if (this.switched) {
                this.switchMode.get().swapBack();
            }
        }
    }

    private void addPlacePositions(ProtectBlock p) {
        switch (p.type) {
            case 0:
            case 1:
                for (Direction dir : Direction.values()) {
                    if (p.type == 1
                            ? (this.surroundSides.get() || !dir.getAxis().isHorizontal())
                            && (this.surroundTop.get() || dir != Direction.UP)
                            && (this.surroundBottom.get() || dir != Direction.DOWN)
                            : dir != Direction.UP
                            && (this.surroundFloor.get() || !dir.getAxis().isHorizontal())
                            && (this.surroundFloorBottom.get() || dir != Direction.DOWN)) {
                        BlockPos posx = p.pos.offset(dir);
                        if (OLEPOSSUtils.replaceable(posx)) {
                            PlaceData datax = SettingUtils.getPlaceData(posx);
                            if (datax.valid() && SettingUtils.inPlaceRange(datax.pos()) && !EntityUtils.intersects(BoxUtils.get(posx), this::validForIntersects)) {
                                this.placePositions.add(posx);
                            }
                        }
                    }
                }
                break;
            case 2:
            case 3:
                BlockPos pos = p.pos.up();
                if (!OLEPOSSUtils.replaceable(pos)) {
                    return;
                }

                PlaceData data = SettingUtils.getPlaceData(pos);
                if (!data.valid()) {
                    return;
                }

                if (!SettingUtils.inPlaceRange(data.pos())) {
                    return;
                }

                if (EntityUtils.intersects(BoxUtils.get(pos), this::validForIntersects)) {
                    return;
                }

                this.placePositions.add(pos);
        }
    }

    private void updateAttack() {
        if (this.attack.get()) {
            if (!(System.currentTimeMillis() - this.lastAttack < 1000.0 / this.attackSpeed.get())) {
                Entity blocking = this.getBlocking();
                if (blocking != null) {
                    if (!SettingUtils.shouldRotate(RotationType.Attacking) || this.attackRotate(blocking.getBoundingBox(), 0.1, "attacking")) {
                        this.attackEntity(blocking);
                        if (SettingUtils.shouldRotate(RotationType.Attacking)) {
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

    private Entity getBlocking() {
        Entity crystal = null;
        double lowest = 1000.0;

        for (Entity entity : BlackOut.mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity && !(BlackOut.mc.player.distanceTo(entity) > 5.0F) && SettingUtils.inAttackRange(entity.getBoundingBox())) {
                for (BlockPos pos : this.placePositions) {
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

    private void updateResult() {
        this.result = this.switchMode.get().find(this::valid);
    }

    private boolean valid(ItemStack stack) {
        return stack.getItem() instanceof BlockItem block && this.blocks.get().contains(block.getBlock());
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
                if (this.placesLeft >= this.places.get() || this.timer >= this.placeDelayS.get()) {
                    this.placesLeft = this.places.get();
                    this.timer = 0.0;
                }
        }
    }

    private void place(BlockPos pos) {
        if (this.blocksLeft > 0) {
            PlaceData data = SettingUtils.getPlaceData(pos, (p, d) -> this.placed.contains(p), null);
            if (data != null && data.valid()) {
                if (!SettingUtils.shouldRotate(RotationType.BlockPlace) || this.rotateBlock(data, RotationType.BlockPlace, "placing")) {
                    if (!this.switched && this.hand == null) {
                        this.switched = this.switchMode.get().swap(this.result.slot());
                    }

                    if (this.switched || this.hand != null) {
                        this.placeBlock(this.hand, data.pos().toCenterPos(), data.dir(), data.pos());
                        if (this.placeSwing.get()) {
                            this.clientSwing(this.placeHand.get(), this.hand);
                        }

                        if (!this.packet.get()) {
                            this.setBlock(pos);
                        }

                        this.placed.add(pos, this.cooldown.get());
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

    private void setBlock(BlockPos pos) {
        if (BlackOut.mc.player.getInventory().getStack(this.result.slot()).getItem() instanceof BlockItem block) {
            Managers.PACKET
                    .addToQueue(
                            handler -> {
                                BlackOut.mc.world.setBlockState(pos, block.getBlock().getDefaultState());
                                BlackOut.mc
                                        .world
                                        .playSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
                            }
                    );
        }
    }

    private boolean shouldProtect(ProtectBlock p) {
        BlockPos pos = p.pos;
        switch (p.type) {
            case 1:
                if (!OLEPOSSUtils.solid2(pos) || BlackOut.mc.world.getBlockState(pos).getBlock() == Blocks.BEDROCK) {
                    return false;
                }
                break;
            case 2:
            case 3:
                if (BlackOut.mc.world.getBlockState(p.pos).getBlock() != Blocks.OBSIDIAN) {
                    return false;
                }

                if (!(BlackOut.mc.world.getBlockState(p.pos.up()).getBlock() instanceof AirBlock)) {
                    return false;
                }

                if (SettingUtils.oldCrystals() && !(BlackOut.mc.world.getBlockState(p.pos.up(2)).getBlock() instanceof AirBlock)) {
                    return false;
                }
        }

        return this.mining
                .contains(
                        timer -> timer.value.getLeft().equals(p.pos) && System.currentTimeMillis() - timer.startTime >= this.mineTime.get() * 1000.0
                ) && this.damageCheck(pos, p.type);
    }

    private void updateBlocks() {
        this.toProtect.clear();
        if (!this.onlyHole.get() || HoleUtils.inHole(BlackOut.mc.player.getBlockPos())) {
            BlockPos e = BlockPos.ofFloored(
                    BlackOut.mc.player.getX(), BlackOut.mc.player.getBoundingBox().maxY, BlackOut.mc.player.getZ()
            );
            BlockPos pos = new BlockPos(
                    BlackOut.mc.player.getBlockX(), (int) Math.round(BlackOut.mc.player.getY()), BlackOut.mc.player.getBlockZ()
            );
            int[] size = new int[4];
            double xOffset = BlackOut.mc.player.getX() - BlackOut.mc.player.getBlockX();
            double zOffset = BlackOut.mc.player.getZ() - BlackOut.mc.player.getBlockZ();
            if (xOffset < 0.3) {
                size[0] = -1;
            }

            if (xOffset > 0.7) {
                size[1] = 1;
            }

            if (zOffset < 0.3) {
                size[2] = -1;
            }

            if (zOffset > 0.7) {
                size[3] = 1;
            }

            this.updateSurround(pos, size);
            if (this.trapCev.get()) {
                this.updateEyes(e, size);
            }

            if (this.cev.get()) {
                this.updateTop(e.up());
            }
        }
    }

    private void updateTop(BlockPos pos) {
        this.toProtect.add(new ProtectBlock(pos, 3));
    }

    private void updateEyes(BlockPos pos, int[] size) {
        for (int x = size[0] - 1; x <= size[1] + 1; x++) {
            for (int z = size[2] - 1; z <= size[3] + 1; z++) {
                if (x != size[0] - 1 && x != size[1] + 1 || z != size[2] - 1 && z != size[3] + 1) {
                    this.toProtect.add(new ProtectBlock(pos.add(x, 0, z), 2));
                }
            }
        }
    }

    private void updateSurround(BlockPos pos, int[] size) {
        for (int y = -1; y <= 0; y++) {
            for (int x = size[0] - 1; x <= size[1] + 1; x++) {
                for (int z = size[2] - 1; z <= size[3] + 1; z++) {
                    boolean bx = x == size[0] - 1 || x == size[1] + 1;
                    boolean by = y == -1;
                    boolean bz = z == size[2] - 1 || z == size[3] + 1;
                    if (by) {
                        if (!bx && !bz) {
                            this.toProtect.add(new ProtectBlock(pos.add(x, y, z), 0));
                        }
                    } else if (!bx || !bz) {
                        this.toProtect.add(new ProtectBlock(pos.add(x, y, z), 1));
                    }
                }
            }
        }
    }

    private boolean validForIntersects(Entity entity) {
        return !(entity instanceof ItemEntity) && !(entity instanceof EndCrystalEntity);
    }

    private boolean damageCheck(BlockPos blockPos, int type) {
        if (this.always.get()) {
            return true;
        } else {
            switch (type) {
                case 1:
                    for (int x = -2; x <= 2; x++) {
                        for (int y = -2; y <= 2; y++) {
                            for (int z = -2; z <= 2; z++) {
                                BlockPos pos = blockPos.add(x, y, z);
                                if (BlackOut.mc.world.getBlockState(pos).getBlock() instanceof AirBlock
                                        && (!SettingUtils.oldCrystals() || BlackOut.mc.world.getBlockState(pos.up()).getBlock() instanceof AirBlock)
                                        && !EntityUtils.intersects(
                                        BoxUtils.crystalSpawnBox(pos), entity -> !(entity instanceof ExperienceOrbEntity) && !(entity instanceof ExperienceBottleEntity)
                                )
                                        && DamageUtils.crystalDamage(BlackOut.mc.player, BlackOut.mc.player.getBoundingBox(), this.feet(pos), blockPos)
                                        >= this.minDmg.get()) {
                                    return true;
                                }
                            }
                        }
                    }
                    break;
                case 2:
                case 3:
                    if (DamageUtils.crystalDamage(BlackOut.mc.player, BlackOut.mc.player.getBoundingBox(), this.feet(blockPos.up()), blockPos)
                            >= this.minDmg.get()) {
                        return true;
                    }
            }

            return false;
        }
    }

    private Vec3d feet(BlockPos pos) {
        return new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }

    public enum PlaceDelayMode {
        Ticks,
        Seconds
    }

    private record ProtectBlock(BlockPos pos, int type) {
    }

    public record Render(BlockPos pos, long time) {
    }
}
