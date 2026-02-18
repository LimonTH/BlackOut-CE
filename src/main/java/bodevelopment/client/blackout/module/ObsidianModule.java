package bodevelopment.client.blackout.module;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.*;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.BlockStateEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.combat.defensive.Surround;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.BoxMultiSetting;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.randomstuff.PlaceData;
import bodevelopment.client.blackout.randomstuff.Rotation;
import bodevelopment.client.blackout.randomstuff.timers.RenderList;
import bodevelopment.client.blackout.randomstuff.timers.TimerList;
import bodevelopment.client.blackout.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ObsidianModule extends Module {
    public final SettingGroup sgGeneral = this.addGroup("General");
    public final SettingGroup sgBlocks = this.addGroup("Blocks");
    public final SettingGroup sgSpeed = this.addGroup("Speed");
    public final SettingGroup sgAttack = this.addGroup("Attack");
    public final SettingGroup sgRender = this.addGroup("Render");
    public final Setting<List<Block>> blocks = this.sgBlocks.bl("Blocks", "Blocks to use.", Blocks.OBSIDIAN);
    public final Setting<List<Block>> supportBlocks = this.sgBlocks.bl("Support Blocks", "Blocks to use for support.", Blocks.OBSIDIAN);
    public final List<BlockPos> blockPlacements = new ArrayList<>();
    protected final Setting<Double> cooldown = this.sgSpeed
            .d("Cooldown", 0.3, 0.0, 1.0, 0.01, "Waits x seconds before trying to place at the same position if there is more than 1 missing block.");
    protected final Setting<Boolean> attack = this.sgAttack.b("Attack", true, "Attacks crystals blocking surround.");
    private final Setting<Double> attackSpeed = this.sgAttack
            .d("Attack Speed", 4.0, 0.0, 20.0, 0.05, "How many times to attack every second.", this.attack::get);
    private final Setting<Boolean> alwaysAttack = this.sgAttack
            .b("Always Attack", false, "Attacks crystals even when surround block isn't broken.", this.attack::get);
    protected final List<BlockPos> insideBlocks = new ArrayList<>();
    protected final List<BlockPos> valids = new ArrayList<>();
    private final Setting<Boolean> pauseEat = this.sgGeneral.b("Pause Eat", false, "Pauses when eating.");
    private final Setting<Boolean> packet = this.sgGeneral.b("Packet", false, ".");
    private final Setting<Boolean> allowSneak = this.sgGeneral.b("Allow Sneak", false, ".");
    private final Setting<SwitchMode> switchMode = this.sgGeneral
            .e("Switch Mode", SwitchMode.Silent, "Method of switching. Silent is the most reliable but delays crystals on some servers.");
    private final Setting<Boolean> onlyOnGround = this.sgGeneral.b("Only On Ground", false, ".");
    private final Setting<RotationMode> rotationMode = this.sgGeneral.e("Rotation Mode", RotationMode.Normal, ".");
    private final Setting<Surround.PlaceDelayMode> placeDelayMode = this.sgSpeed.e("Place Delay Mode", Surround.PlaceDelayMode.Ticks, ".");
    private final Setting<Integer> placeDelayT = this.sgSpeed
            .i("Place Tick Delay", 1, 0, 20, 1, "Tick delay between places.", () -> this.placeDelayMode.get() == Surround.PlaceDelayMode.Ticks);
    private final Setting<Double> placeDelayS = this.sgSpeed
            .d("Place Delay", 0.1, 0.0, 1.0, 0.01, "Delay between places.", () -> this.placeDelayMode.get() == Surround.PlaceDelayMode.Seconds);
    private final Setting<Integer> places = this.sgSpeed.i("Places", 1, 1, 20, 1, "How many blocks to place each time.");
    private final Setting<Boolean> placeSwing = this.sgRender.b("Place Swing", true, "Renders swing animation when placing a block.");
    private final Setting<SwingHand> placeHand = this.sgRender.e("Place Swing Hand", SwingHand.RealHand, "Which hand should be swung.", this.placeSwing::get);
    private final Setting<Boolean> attackSwing = this.sgRender.b("Attack Swing", true, "Renders swing animation when attacking a block.");
    private final Setting<SwingHand> attackHand = this.sgRender.e("Attack Swing Hand", SwingHand.RealHand, "Which hand should be swung.", this.attackSwing::get);
    private final BoxMultiSetting normalRendering = BoxMultiSetting.of(this.sgRender);
    private final BoxMultiSetting supportRendering = BoxMultiSetting.of(this.sgRender, "Support");
    private final List<BlockPos> supportPositions = new ArrayList<>();
    private final TimerList<BlockPos> placed = new TimerList<>(false);
    private final RenderList<BlockPos> render = RenderList.getList(true);
    private final RenderList<BlockPos> supportRender = RenderList.getList(true);
    public boolean placing = false;
    private int tickTimer = 0;
    private double timer = 0.0;
    private boolean support = false;
    private Hand hand = null;
    private int blocksLeft = 0;
    private int placesLeft = 0;
    private FindResult result = null;
    private boolean switched = false;
    private long lastAttack = 0L;
    private boolean firstCalc = true;

    public ObsidianModule(String name, String description, SubCategory category) {
        super(name, description, category, true);
    }

    @Override
    public void onEnable() {
        this.tickTimer = this.placeDelayT.get();
        this.timer = this.placeDelayS.get();
        this.placesLeft = this.places.get();
        this.firstCalc = true;
    }

    @Override
    public void onDisable() {
        this.blockPlacements.stream().filter(OLEPOSSUtils::replaceable).forEach(pos -> this.render.add(pos, 0.5));
        this.supportPositions.forEach(pos -> this.supportRender.add(pos, 0.5));
    }

    @Override
    public boolean shouldSkipListeners() {
        return false;
    }

    @Event
    public void onBlock(BlockStateEvent event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null && this.enabled) {
            if (event.previousState.getBlock() != event.state.getBlock() && !OLEPOSSUtils.replaceable(event.pos)) {
                if (this.blockPlacements.contains(event.pos)) {
                    this.render.add(event.pos, 0.5);
                }

                if (this.supportPositions.contains(event.pos)) {
                    this.supportRender.add(event.pos, 0.5);
                }
            }
        }
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        this.tickTimer++;
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (BlackOut.mc.player == null || BlackOut.mc.world == null) return;

        MatrixStack stack = event.stack;

        if (!this.enabled) {
            this.render.update((pos, time, delta) ->
                    this.normalRendering.render(BoxUtils.get(pos), (float) (1.0 - delta), 1.0F)
            );
        } else {
            this.placed.update();

            this.timer += event.tickDelta;

            if (!this.preCalc()) {
                this.updateBlocks();
                this.updateSupport();

                this.blockPlacements.stream()
                        .filter(OLEPOSSUtils::replaceable)
                        .forEach(block -> {
                            this.normalRendering.render(BoxUtils.get(block));
                            if (this.firstCalc) {
                                this.render.remove(block);
                            }
                        });

                this.supportPositions.forEach(block -> {
                    this.supportRendering.render(BoxUtils.get(block));
                    if (this.firstCalc) {
                        this.supportRender.remove(block);
                    }
                });

                this.render.update((pos, time, delta) ->
                        this.normalRendering.render(BoxUtils.get(pos), (float) (1.0 - delta), 1.0F)
                );
                this.supportRender.update((pos, time, delta) ->
                        this.supportRendering.render(BoxUtils.get(pos), (float) (1.0 - delta), 1.0F)
                );

                this.firstCalc = false;

                if (!this.pauseEat.get() || !BlackOut.mc.player.isUsingItem()) {
                    if (!this.onlyOnGround.get() || BlackOut.mc.player.isOnGround()) {
                        this.placeBlocks();
                    }
                }
            }
        }
    }

    protected double getCooldown() {
        return 0.0;
    }

    protected boolean preCalc() {
        return false;
    }

    private void updateAttack() {
        if (this.attack.get()) {
            if (!(System.currentTimeMillis() - this.lastAttack < 1000.0 / this.attackSpeed.get())) {
                Entity blocking = this.getBlocking();
                if (blocking != null) {
                    if (!SettingUtils.shouldRotate(RotationType.Attacking) || this.attackRotate(blocking.getBoundingBox(), -0.1, "attacking")) {
                        SettingUtils.swing(SwingState.Pre, SwingType.Attacking, Hand.MAIN_HAND);
                        this.sendPacket(PlayerInteractEntityC2SPacket.attack(blocking, BlackOut.mc.player.isSneaking()));
                        SettingUtils.swing(SwingState.Post, SwingType.Attacking, Hand.MAIN_HAND);
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
            if (entity instanceof EndCrystalEntity
                    && !(BlackOut.mc.player.distanceTo(entity) > 5.0F)
                    && SettingUtils.inAttackRange(entity.getBoundingBox())
                    && this.validForBlocking(entity)) {
                double dmg = Math.max(10.0, DamageUtils.crystalDamage(BlackOut.mc.player, BlackOut.mc.player.getBoundingBox(), entity.getPos()));
                if (dmg < lowest) {
                    lowest = dmg;
                    crystal = entity;
                }
            }
        }

        return crystal;
    }

    protected boolean validForBlocking(Entity entity) {
        for (BlockPos pos : this.alwaysAttack.get() ? this.blockPlacements : this.valids) {
            if (BoxUtils.get(pos).intersects(entity.getBoundingBox())) {
                return true;
            }
        }

        return false;
    }

    private void placeBlocks() {
        List<BlockPos> positions = new ArrayList<>();
        this.setSupport();
        if (this.support) {
            positions.addAll(this.supportPositions);
        } else {
            positions.addAll(this.blockPlacements);
        }

        this.valids.clear();
        positions.stream().filter(this::validBlock).forEach(this.valids::add);
        this.updateAttack();
        this.updatePlaces();
        if ((this.result = this.switchMode.get().find(this::valid)).wasFound()) {
            this.blocksLeft = Math.min(this.placesLeft, this.result.amount());
            this.hand = OLEPOSSUtils.getHand(this::valid);
            this.switched = false;
            this.valids
                    .stream()
                    .filter(pos -> !EntityUtils.intersects(BoxUtils.get(pos), this::validEntity))
                    .sorted(Comparator.comparingDouble(RotationUtils::getYaw))
                    .forEach(this::place);
            if (this.switched && this.hand == null) {
                this.switchMode.get().swapBack();
            }
        }
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

    private boolean validBlock(BlockPos pos) {
        if (!OLEPOSSUtils.replaceable(pos)) {
            return false;
        } else {
            PlaceData data = SettingUtils.getPlaceData(pos, (p, d) -> this.placed.contains(p), null, !this.allowSneak.get());
            if (!data.valid()) {
                return false;
            } else {
                return SettingUtils.inPlaceRange(data.pos()) && !this.placed.contains(pos);
            }
        }
    }

    private void place(BlockPos pos) {
        if (this.result.amount() > 0) {
            PlaceData data = SettingUtils.getPlaceData(pos, (p, d) -> this.placed.contains(p), null, !this.allowSneak.get());
            if (data.valid()) {
                this.placing = true;
                if (SettingUtils.shouldRotate(RotationType.BlockPlace)) {
                    switch (this.rotationMode.get()) {
                        case Normal:
                            if (!this.rotateBlock(data, RotationType.BlockPlace, "placing")) {
                                return;
                            }
                            break;
                        case Instant:
                            if (!this.rotateBlock(data, RotationType.InstantBlockPlace, "placing")) {
                                return;
                            }
                    }
                }

                if (this.blocksLeft > 0) {
                    if (!this.switched && this.hand == null) {
                        this.switched = this.switchMode.get().swap(this.result.slot());
                    }

                    if (this.switched || this.hand != null) {
                        if (SettingUtils.shouldRotate(RotationType.BlockPlace) && this.rotationMode.get() == RotationMode.Packet) {
                            Rotation rotation = SettingUtils.getRotation(data.pos(), data.dir(), data.pos().toCenterPos(), RotationType.BlockPlace);
                            this.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(rotation.yaw(), rotation.pitch(), Managers.PACKET.isOnGround()));
                        }

                        this.placeBlock(this.hand, data.pos().toCenterPos(), data.dir(), data.pos());
                        if (this.placeSwing.get()) {
                            this.clientSwing(this.placeHand.get(), this.hand);
                        }

                        if (!this.packet.get()) {
                            this.setBlock(pos);
                        }

                        this.placed.add(pos, this.getCooldown());
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
            Managers.PACKET.addToQueue(handler -> {
                BlackOut.mc.world.setBlockState(pos, block.getBlock().getDefaultState());
                this.blockPlaceSound(pos, this.result.stack());
            });
        }
    }

    private void setSupport() {
        this.support = false;
        double min = 10000.0;

        for (BlockPos pos : this.blockPlacements) {
            if (this.validBlock(pos)) {
                double y = RotationUtils.getYaw(pos.toCenterPos());
                if (y < min) {
                    this.support = false;
                    min = y;
                }
            }
        }

        for (BlockPos posx : this.supportPositions) {
            if (this.validBlock(posx)) {
                double y = RotationUtils.getYaw(posx.toCenterPos());
                if (y < min) {
                    this.support = true;
                    min = y;
                }
            }
        }
    }

    private boolean valid(ItemStack stack) {
        return stack.getItem() instanceof BlockItem block && (this.support ? this.supportBlocks : this.blocks).get().contains(block.getBlock());
    }

    private void updateSupport() {
        this.supportPositions.clear();
        this.blockPlacements.forEach(pos -> {
            BlockPos support = this.findSupport(pos);
            if (support != null) {
                this.supportPositions.add(support);
            }
        });
    }

    protected BlockPos findSupport(BlockPos pos) {
        if (!OLEPOSSUtils.replaceable(pos)) {
            return null;
        } else if (this.hasSupport(pos, true)) {
            return null;
        } else {
            for (Direction dir : Direction.values()) {
                if (dir != Direction.UP) {
                    BlockPos pos2 = pos.offset(dir);
                    if (!this.blockPlacements.contains(pos2)
                            && !this.insideBlocks.contains(pos2)
                            && !EntityUtils.intersects(BoxUtils.get(pos2), entity -> entity instanceof PlayerEntity && !entity.isSpectator())
                            && SettingUtils.getPlaceData(pos2, !this.allowSneak.get()).valid()
                            && SettingUtils.inPlaceRange(pos2)
                            && SettingUtils.getPlaceData(pos, (p, d) -> d == dir, null, !this.allowSneak.get()).valid()) {
                        return pos2;
                    }
                }
            }

            return null;
        }
    }

    protected boolean hasSupport(BlockPos pos, boolean check) {
        PlaceData data = SettingUtils.getPlaceData(pos, !this.allowSneak.get());
        if (data.valid()) {
            return true;
        } else {
            for (Direction dir : Direction.values()) {
                PlaceData data2 = SettingUtils.getPlaceData(pos, (p, d) -> d == dir, null, !this.allowSneak.get());
                if (data2.valid()) {
                    BlockPos offsetPos = pos.offset(dir);
                    if (this.supportPositions.contains(offsetPos)) {
                        return true;
                    }

                    if (check && this.blockPlacements.contains(offsetPos) && this.hasSupport(offsetPos, false)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    private void updateBlocks() {
        this.updateInsideBlocks();
        this.blockPlacements.clear();
        this.addPlacements();
    }

    private void updateInsideBlocks() {
        this.insideBlocks.clear();
        this.addInsideBlocks();
    }

    protected void addInsideBlocks() {
    }

    protected void addPlacements() {
    }

    protected void addBlocks(Entity entity, int[] size) {
    }

    protected boolean validEntity(Entity entity) {
        return (!(entity instanceof EndCrystalEntity) || System.currentTimeMillis() - this.lastAttack >= 100L) && !(entity instanceof ItemEntity);
    }

    protected boolean intersects(PlayerEntity player) {
        Box playerBox = player.getBoundingBox().contract(1.0E-4, 1.0E-4, 1.0E-4);

        for (BlockPos pos : this.blockPlacements) {
            if (playerBox.intersects(BoxUtils.get(pos))) {
                return true;
            }
        }

        return false;
    }

    protected int[] getSize(PlayerEntity player) {
        int[] size = new int[4];
        double x = player.getX() - player.getBlockX();
        double z = player.getZ() - player.getBlockZ();
        if (x < 0.3) {
            size[0] = -1;
        }

        if (x > 0.7) {
            size[1] = 1;
        }

        if (z < 0.3) {
            size[2] = -1;
        }

        if (z > 0.7) {
            size[3] = 1;
        }

        return size;
    }

    protected BlockPos getPos() {
        return BlackOut.mc.player == null
                ? BlockPos.ORIGIN
                : new BlockPos(BlackOut.mc.player.getBlockX(), (int) Math.round(BlackOut.mc.player.getY()), BlackOut.mc.player.getBlockZ());
    }

    public enum RotationMode {
        Normal,
        Instant,
        Packet
    }
}
