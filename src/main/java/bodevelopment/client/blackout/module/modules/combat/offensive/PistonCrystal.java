package bodevelopment.client.blackout.module.modules.combat.offensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.*;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.Notifications;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.randomstuff.PlaceData;
import bodevelopment.client.blackout.randomstuff.timers.TimerList;
import bodevelopment.client.blackout.util.BoxUtils;
import bodevelopment.client.blackout.util.EntityUtils;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import bodevelopment.client.blackout.util.SettingUtils;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import net.minecraft.block.*;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PistonCrystal extends Module {
    public static AbstractClientPlayerEntity targetedPlayer = null;
    private static PistonCrystal INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgDelay = this.addGroup("Delay");
    private final SettingGroup sgSwitch = this.addGroup("Switch");
    private final SettingGroup sgToggle = this.addGroup("Toggle");
    private final SettingGroup sgSwing = this.addGroup("Swing");
    private final SettingGroup sgRender = this.addGroup("Render");
    private final Setting<Boolean> pauseEat = this.sgGeneral.booleanSetting("Pause Eat", false, "Pauses when eating.");
    private final Setting<Boolean> fire = this.sgGeneral.booleanSetting("Fire", false, "Uses fire to blow up the crystal.");
    private final Setting<Redstone> redstone = this.sgGeneral.enumSetting("Redstone", Redstone.Torch, "What kind of redstone to use.");
    private final Setting<Boolean> alwaysAttack = this.sgGeneral.booleanSetting("Always Attack", false, "Attacks all crystals blocking crystal placing.");
    private final Setting<Double> attackSpeed = this.sgGeneral.doubleSetting("Attack Speed", 4.0, 0.0, 20.0, 0.1, "Attacks all crystals blocking crystal placing.");
    private final Setting<Boolean> pauseOffGround = this.sgGeneral.booleanSetting("Pause Off Ground", true, ".");
    private final Setting<Double> pcDelay = this.sgDelay
            .doubleSetting("Piston > Crystal", 0.0, 0.0, 1.0, 0.01, "How many seconds to wait between placing piston and crystal.");
    private final Setting<Double> cfDelay = this.sgDelay
            .doubleSetting("Crystal > Fire", 0.25, 0.0, 1.0, 0.01, "How many seconds to wait between placing a crystal and placing fire.");
    private final Setting<Double> crDelay = this.sgDelay
            .doubleSetting("Crystal > Redstone", 0.25, 0.0, 1.0, 0.01, "How many seconds to wait between placing a crystal and placing redstone.");
    private final Setting<Double> rmDelay = this.sgDelay
            .doubleSetting("Redstone > Mine", 0.25, 0.0, 1.0, 0.01, "How many seconds to wait between placing redstone and starting to mine.");
    private final Setting<Double> raDelay = this.sgDelay
            .doubleSetting("Redstone > Attack", 0.1, 0.0, 1.0, 0.01, "How many seconds to wait between placing redstone and attacking a crystal.");
    private final Setting<Double> mpDelay = this.sgDelay
            .doubleSetting("Mine > Piston", 0.25, 0.0, 1.0, 0.01, "How many seconds to wait after mining the redstone before starting a new cycle.");
    private final Setting<SwitchMode> crystalSwitch = this.sgSwitch.enumSetting("Crystal Switch", SwitchMode.Normal, "Method of switching. Silent is the most reliable.");
    private final Setting<SwitchMode> pistonSwitch = this.sgSwitch.enumSetting("Piston Switch", SwitchMode.Normal, "Method of switching. Silent is the most reliable.");
    private final Setting<SwitchMode> redstoneSwitch = this.sgSwitch
            .enumSetting("Redstone Switch", SwitchMode.Normal, "Method of switching. Silent is the most reliable.");
    private final Setting<SwitchMode> fireSwitch = this.sgSwitch.enumSetting("Fire Switch", SwitchMode.Normal, "Method of switching. Silent is the most reliable.");
    private final Setting<Boolean> toggleMove = this.sgToggle.booleanSetting("Toggle Move", false, "Disables when moved.");
    private final Setting<Boolean> toggleEnemyMove = this.sgToggle.booleanSetting("Toggle Enemy Move", false, "Disables when enemy moved.");
    private final Setting<Boolean> crystalSwing = this.sgSwing.booleanSetting("Crystal Swing", false, "Renders swing animation when placing a crystal.");
    private final Setting<SwingHand> crystalHand = this.sgSwing.enumSetting("Crystal Hand", SwingHand.RealHand, "Which hand should be swung.", this.crystalSwing::get);
    private final Setting<Boolean> attackSwing = this.sgSwing.booleanSetting("Attack Swing", false, "Renders swing animation when attacking a crystal.");
    private final Setting<SwingHand> attackHand = this.sgSwing.enumSetting("Attack Hand", SwingHand.RealHand, "Which hand should be swung.", this.attackSwing::get);
    private final Setting<Boolean> pistonSwing = this.sgSwing.booleanSetting("Piston Swing", false, "Renders swing animation when placing a piston.");
    private final Setting<SwingHand> pistonHand = this.sgSwing.enumSetting("Piston Hand", SwingHand.RealHand, "Which hand should be swung.", this.pistonSwing::get);
    private final Setting<Boolean> redstoneSwing = this.sgSwing.booleanSetting("Redstone Swing", false, "Renders swing animation when placing redstone.");
    private final Setting<SwingHand> redstoneHand = this.sgSwing.enumSetting("Redstone Hand", SwingHand.RealHand, "Which hand should be swung.", this.redstoneSwing::get);
    private final Setting<Boolean> fireSwing = this.sgSwing.booleanSetting("Fire Swing", false, "Renders swing animation when placing fire.");
    private final Setting<SwingHand> fireHand = this.sgSwing.enumSetting("Fire Hand", SwingHand.RealHand, "Which hand should be swung.", this.fireSwing::get);
    private final Setting<Double> crystalHeight = this.sgRender.doubleSetting("Crystal Height", 0.25, -1.0, 1.0, 0.05, "Height of crystal render.");
    private final Setting<RenderShape> crystalShape = this.sgRender.enumSetting("Crystal Shape", RenderShape.Full, "Which parts should be rendered.");
    private final Setting<BlackOutColor> crystalLineColor = this.sgRender.colorSetting("Crystal Line Color", new BlackOutColor(255, 0, 0, 255), ".");
    private final Setting<BlackOutColor> crystalSideColor = this.sgRender.colorSetting("Crystal Side Color", new BlackOutColor(255, 0, 0, 50), ".");
    private final Setting<Double> pistonHeight = this.sgRender.doubleSetting("Piston Height", 1.0, -1.0, 1.0, 0.05, "Height of crystal render.");
    private final Setting<RenderShape> pistonShape = this.sgRender.enumSetting("Pistonl Shape", RenderShape.Full, "Which parts should be rendered.");
    private final Setting<BlackOutColor> pistonLineColor = this.sgRender.colorSetting("Piston Line Color", new BlackOutColor(255, 255, 255, 255), ".");
    private final Setting<BlackOutColor> pistonSideColor = this.sgRender.colorSetting("Piston Side Color", new BlackOutColor(255, 255, 255, 50), ".");
    private final Setting<Double> redstoneHeight = this.sgRender.doubleSetting("Redstone Height", 1.0, -1.0, 1.0, 0.05, "Height of crystal render.");
    private final Setting<RenderShape> redstoneShape = this.sgRender.enumSetting("Redstone Shape", RenderShape.Full, "Which parts should be rendered.");
    private final Setting<BlackOutColor> redstoneLineColor = this.sgRender.colorSetting("Redstone Line Color", new BlackOutColor(255, 0, 0, 255), ".");
    private final Setting<BlackOutColor> redstoneSideColor = this.sgRender.colorSetting("Redstone Side Color", new BlackOutColor(255, 0, 0, 50), ".");
    private final TimerList<Entity> attacked = new TimerList<>(true);
    public BlockPos crystalPos = null;
    private long lastAttack = 0L;
    private BlockPos pistonPos = null;
    private BlockPos firePos = null;
    private BlockPos redstonePos = null;
    private BlockPos lastCrystalPos = null;
    private BlockPos lastPistonPos = null;
    private BlockPos lastRedstonePos = null;
    private Entity prevTarget = null;
    private Direction pistonDir = null;
    private PlaceData pistonData = null;
    private Direction crystalPlaceDir = null;
    private Direction crystalDir = null;
    private PlaceData redstoneData = null;
    private PlaceData fireData = null;
    private Entity target = null;
    private BlockPos closestCrystalPos = null;
    private BlockPos closestPistonPos = null;
    private BlockPos closestRedstonePos = null;
    private Direction closestPistonDir = null;
    private PlaceData closestPistonData = null;
    private Direction closestCrystalPlaceDir = null;
    private Direction closestCrystalDir = null;
    private PlaceData closestRedstoneData = null;
    private BlockPos closestFirePos = null;
    private PlaceData closestFireData = null;
    private long pistonTime = 0L;
    private long redstoneTime = 0L;
    private long mineTime = 0L;
    private long crystalTime = 0L;
    private boolean minedThisTick = false;
    private boolean pistonPlaced = false;
    private boolean redstonePlaced = false;
    private boolean mined = false;
    private boolean crystalPlaced = false;
    private boolean firePlaced = false;
    private boolean startedMining = false;
    private boolean prevBlocking = false;
    private boolean redstoneBlocking = false;
    private boolean entityBlocking = false;
    private boolean pistonBlocking = false;
    private double closestDistance;
    private double currentDistance;
    private BlockPos prevPos = null;
    private BlockPos prevEnemyPos = null;
    private long prevNotification = 0L;

    public PistonCrystal() {
        super("Piston Crystal", "Pushes crystals into your enemies to deal massive damage.", SubCategory.OFFENSIVE, true);
        INSTANCE = this;
    }

    public static PistonCrystal getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        this.resetPos();
        this.lastCrystalPos = null;
        this.lastPistonPos = null;
        this.lastRedstonePos = null;
        this.pistonPlaced = false;
        this.redstonePlaced = false;
        this.mined = false;
        this.crystalPlaced = false;
        this.firePlaced = false;
        this.startedMining = false;
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        this.minedThisTick = false;
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            String string = this.checkToggle();
            targetedPlayer = null;
            if (string != null) {
                this.disable(string);
            } else {
                this.updatePos();
                if (this.crystalPos != null) {
                    Render3DUtils.box(
                            this.getBox(this.crystalPos, this.crystalHeight.get()), this.crystalSideColor.get(), this.crystalLineColor.get(), this.crystalShape.get()
                    );
                    Render3DUtils.box(
                            this.getBox(this.pistonPos, this.pistonHeight.get()), this.pistonSideColor.get(), this.pistonLineColor.get(), this.pistonShape.get()
                    );
                    Render3DUtils.box(
                            this.getBox(this.redstonePos, this.redstoneHeight.get()),
                            this.redstoneSideColor.get(),
                            this.redstoneLineColor.get(),
                            this.redstoneShape.get()
                    );
                }

                if (this.crystalPos != null) {
                    if (System.currentTimeMillis() - this.mineTime > this.mpDelay.get() * 1000.0
                            && this.crystalPlaced
                            && this.redstonePlaced
                            && this.pistonPlaced
                            && this.mined
                            && (this.firePlaced || !this.canFire(false))) {
                        this.resetProgress();
                    }

                    if (!this.pauseEat.get() || !BlackOut.mc.player.isUsingItem()) {
                        if (!this.pauseOffGround.get() || BlackOut.mc.player.isOnGround()) {
                            if (this.target instanceof AbstractClientPlayerEntity player) {
                                targetedPlayer = player;
                            }

                            if (this.isBlocked()) {
                                this.updateAttack(true);
                                if (this.redstoneBlocking) {
                                    this.mineUpdate(true);
                                }

                                if (this.pistonBlocking) {
                                    this.updateCrystal(true);
                                }

                                this.prevBlocking = true;
                            } else {
                                if (this.prevBlocking) {
                                    this.resetProgress();
                                    this.prevBlocking = false;
                                }

                                this.updateAttack(false);
                                this.updatePiston();
                                this.updateFire();
                                this.updateCrystal(false);
                                this.updateRedstone();
                                this.mineUpdate(false);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean canFire(boolean calc) {
        return this.fire.get() && this.fireSwitch.get().find(Items.FLINT_AND_STEEL).wasFound() && (calc || this.firePos != null);
    }

    private void resetProgress() {
        this.redstonePlaced = false;
        this.pistonPlaced = false;
        this.mined = false;
        this.firePlaced = false;
        this.crystalPlaced = false;
        this.pistonTime = 0L;
        this.redstoneTime = 0L;
        this.mineTime = 0L;
        this.crystalTime = 0L;
        this.lastAttack = 0L;
    }

    private String checkToggle() {
        BlockPos currentPos = BlackOut.mc.player.getBlockPos();
        BlockPos enemyPos = this.target == null ? null : this.target.getBlockPos();
        if (this.toggleMove.get() && !currentPos.equals(this.prevPos)) {
            return "moved";
        } else if (this.toggleEnemyMove.get() && enemyPos != null && !enemyPos.equals(this.prevEnemyPos)) {
            return "enemy moved";
        } else {
            this.prevPos = currentPos;
            this.prevTarget = this.target;
            this.prevEnemyPos = enemyPos;
            return null;
        }
    }

    private Box getBox(BlockPos pos, double height) {
        return new Box(
                pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + height, pos.getZ() + 1
        );
    }

    private boolean isBlocked() {
        BlockState pistonState = BlackOut.mc.world.getBlockState(this.pistonPos);
        this.redstoneBlocking = false;
        this.entityBlocking = false;
        this.pistonBlocking = false;
        if (pistonState.getBlock() != Blocks.PISTON) {
            this.redstoneBlocking = BlackOut.mc.world.getBlockState(this.redstonePos).getBlock() == this.redstone.get().b;
            this.entityBlocking = EntityUtils.intersects(BoxUtils.get(this.pistonPos), this::validForPistonIntersect);
        } else if (pistonState.get(FacingBlock.FACING) != this.pistonDir) {
            this.pistonBlocking = true;
        }

        if (EntityUtils.intersects(BoxUtils.get(this.crystalPos), this::validForCrystalIntersect)) {
            this.entityBlocking = true;
        }

        return this.redstoneBlocking || this.entityBlocking || this.pistonBlocking;
    }

    private boolean validForPistonIntersect(Entity entity) {
        if (entity.isSpectator()) {
            return false;
        } else if (entity instanceof ItemEntity) {
            return false;
        } else {
            return !(entity instanceof EndCrystalEntity) || !this.attacked.contains(entity);
        }
    }

    private boolean validForCrystalIntersect(Entity entity) {
        if (entity.isSpectator()) {
            return false;
        } else if (entity.age < 10) {
            return false;
        } else if (entity instanceof EndCrystalEntity) {
            return !entity.getBlockPos().equals(this.crystalPos) && !this.attacked.contains(entity);
        } else {
            return true;
        }
    }

    private void mineUpdate(boolean blocked) {
        if (!blocked) {
            if (System.currentTimeMillis() - this.redstoneTime < this.rmDelay.get() * 1000.0) {
                return;
            }

            if (!this.redstonePlaced || this.mined) {
                return;
            }
        }

        if (!this.minedThisTick) {
            AutoMine autoMine = AutoMine.getInstance();
            if (this.redstone.get() == Redstone.Torch) {
                Direction mineDir = SettingUtils.getPlaceOnDirection(this.redstonePos);
                if (mineDir != null) {
                    this.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, this.redstonePos, mineDir));
                    this.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, this.redstonePos, mineDir));
                }
            } else {
                if (!autoMine.enabled) {
                    if (System.currentTimeMillis() - this.prevNotification > 500L) {
                        Managers.NOTIFICATIONS.addNotification("Automine required for redstone block mode.", this.getDisplayName(), 1.0, Notifications.Type.Info);
                        this.prevNotification = System.currentTimeMillis();
                    }

                    return;
                }

                if (BlackOut.mc.world.getBlockState(this.redstonePos).getBlock() != Blocks.REDSTONE_BLOCK && this.mined) {
                    return;
                }

                if (this.redstonePos.equals(autoMine.minePos)) {
                    return;
                }

                autoMine.onStart(this.redstonePos);
            }

            if (!this.mined) {
                this.mineTime = System.currentTimeMillis();
            }

            this.mined = true;
            this.minedThisTick = true;
        }
    }

    private void updateAttack(boolean blocked) {
        if (!blocked) {
            if (!this.redstonePlaced) {
                return;
            }

            if (System.currentTimeMillis() - this.redstoneTime < this.raDelay.get() * 1000.0) {
                return;
            }
        }

        EndCrystalEntity crystal = null;
        double cd = 10000.0;

        for (Entity entity : BlackOut.mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity c
                    && (blocked || c.getX() != this.crystalPos.getX() + 0.5 || c.getZ() != this.crystalPos.getZ() + 0.5)
                    && (this.alwaysAttack.get() || blocked || c.getX() - c.getBlockX() != 0.5 || c.getZ() - c.getBlockZ() != 0.5)
                    && (c.getBoundingBox().intersects(BoxUtils.crystalSpawnBox(this.crystalPos)) || blocked && c.getBoundingBox().intersects(BoxUtils.get(this.pistonPos)))) {
                double d = BlackOut.mc.player.getEyePos().distanceTo(c.getPos());
                if (d < cd) {
                    cd = d;
                    crystal = c;
                }
            }
        }

        if (crystal != null) {
            if (!SettingUtils.shouldRotate(RotationType.Attacking) || this.attackRotate(crystal.getBoundingBox(), 0.1, "attacking")) {
                if (!(System.currentTimeMillis() - this.lastAttack < 1000.0 / this.attackSpeed.get())) {
                    SettingUtils.swing(SwingState.Pre, SwingType.Attacking, Hand.MAIN_HAND);
                    this.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, BlackOut.mc.player.isSneaking()));
                    SettingUtils.swing(SwingState.Post, SwingType.Attacking, Hand.MAIN_HAND);
                    if (SettingUtils.shouldRotate(RotationType.Attacking)) {
                        this.end("attacking");
                    }

                    if (this.attackSwing.get()) {
                        this.clientSwing(this.attackHand.get(), Hand.MAIN_HAND);
                    }

                    this.lastAttack = System.currentTimeMillis();
                    this.attacked.add(crystal, 0.25);
                }
            }
        }
    }

    private void updatePiston() {
        if (!this.pistonPlaced) {
            if (this.pistonData != null) {
                Hand hand = OLEPOSSUtils.getHand(Items.PISTON);
                boolean available = hand != null;
                FindResult result = this.pistonSwitch.get().find(Items.PISTON);
                if (!available) {
                    available = result.wasFound();
                }

                if (available) {
                    if (!SettingUtils.shouldRotate(RotationType.BlockPlace) || this.rotateBlock(this.pistonData, RotationType.BlockPlace, "piston")) {
                        boolean switched = false;
                        if (hand != null || (switched = this.pistonSwitch.get().swap(result.slot()))) {
                            this.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(this.pistonDir.getOpposite().asRotation(), Managers.ROTATION.nextPitch, Managers.PACKET.isOnGround()));
                            this.placeBlock(hand, this.pistonData.pos().toCenterPos(), this.pistonData.dir(), this.pistonData.pos());
                            if (SettingUtils.shouldRotate(RotationType.BlockPlace)) {
                                this.end("piston");
                            }

                            if (this.pistonSwing.get()) {
                                this.clientSwing(this.pistonHand.get(), hand);
                            }

                            this.pistonTime = System.currentTimeMillis();
                            this.pistonPlaced = true;
                            if (switched) {
                                this.pistonSwitch.get().swapBack();
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateCrystal(boolean blocked) {
        if (blocked || this.pistonPlaced && !this.crystalPlaced) {
            if (!(System.currentTimeMillis() - this.pistonTime < this.pcDelay.get() * 1000.0)) {
                if (this.crystalPlaceDir != null) {
                    if (!EntityUtils.intersects(BoxUtils.get(this.crystalPos), entity -> {
                        if (entity.isSpectator()) {
                            return false;
                        } else if (entity instanceof EndCrystalEntity) {
                            return !entity.getBlockPos().equals(this.crystalPos) && !this.attacked.contains(entity);
                        } else {
                            return true;
                        }
                    })) {
                        Hand hand = OLEPOSSUtils.getHand(Items.END_CRYSTAL);
                        boolean available = hand != null;
                        FindResult result = this.crystalSwitch.get().find(Items.END_CRYSTAL);
                        if (!available) {
                            available = result.wasFound();
                        }

                        if (available) {
                            if (!SettingUtils.shouldRotate(RotationType.Interact)
                                    || this.rotateBlock(this.crystalPos.down(), this.crystalPlaceDir, RotationType.Interact, "crystal")) {
                                boolean switched = false;
                                if (hand != null || (switched = this.crystalSwitch.get().swap(result.slot()))) {
                                    hand = hand == null ? Hand.MAIN_HAND : hand;
                                    this.interactBlock(hand, this.crystalPos.down().toCenterPos(), this.crystalPlaceDir, this.crystalPos.down());
                                    if (SettingUtils.shouldRotate(RotationType.Interact)) {
                                        this.end("crystal");
                                    }

                                    if (this.crystalSwing.get()) {
                                        this.clientSwing(this.crystalHand.get(), hand);
                                    }

                                    this.crystalTime = System.currentTimeMillis();
                                    this.crystalPlaced = true;
                                    if (switched) {
                                        this.crystalSwitch.get().swapBack();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateRedstone() {
        if (this.crystalPlaced && !this.redstonePlaced) {
            if (!(System.currentTimeMillis() - this.crystalTime < this.crDelay.get() * 1000.0)) {
                if (this.redstoneData != null) {
                    Hand hand = OLEPOSSUtils.getHand(this.redstone.get().i);
                    boolean available = hand != null;
                    FindResult result = this.redstoneSwitch.get().find(this.redstone.get().i);
                    if (!available) {
                        available = result.wasFound();
                    }

                    if (available) {
                        if (!SettingUtils.shouldRotate(RotationType.BlockPlace) || this.rotateBlock(this.redstoneData, RotationType.BlockPlace, "redstone")) {
                            boolean switched = false;
                            if (hand != null || (switched = this.redstoneSwitch.get().swap(result.slot()))) {
                                this.placeBlock(hand, this.redstoneData.pos().toCenterPos(), this.redstoneData.dir(), this.redstoneData.pos());
                                if (SettingUtils.shouldRotate(RotationType.BlockPlace)) {
                                    this.end("redstone");
                                }

                                if (this.redstoneSwing.get()) {
                                    this.clientSwing(this.redstoneHand.get(), hand);
                                }

                                this.redstoneTime = System.currentTimeMillis();
                                this.redstonePlaced = true;
                                if (switched) {
                                    this.redstoneSwitch.get().swapBack();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateFire() {
        if (this.canFire(true)) {
            if (this.crystalPlaced && !this.firePlaced) {
                if (!(System.currentTimeMillis() - this.crystalTime < this.cfDelay.get() * 1000.0)) {
                    if (this.firePos == null) {
                        this.firePlaced = true;
                    } else {
                        Hand hand = OLEPOSSUtils.getHand(Items.FLINT_AND_STEEL);
                        FindResult result = this.fireSwitch.get().find(Items.FLINT_AND_STEEL);
                        if (hand != null || result.wasFound()) {
                            if (!SettingUtils.shouldRotate(RotationType.Interact) || this.rotateBlock(this.fireData, RotationType.Interact, "fire")) {
                                boolean switched = false;
                                if (hand != null || (switched = this.fireSwitch.get().swap(result.slot()))) {
                                    this.interactBlock(hand, this.fireData.pos().toCenterPos(), this.fireData.dir(), this.fireData.pos());
                                    if (SettingUtils.shouldRotate(RotationType.Interact)) {
                                        this.end("fire");
                                    }

                                    if (this.fireSwing.get()) {
                                        this.clientSwing(this.fireHand.get(), hand);
                                    }

                                    this.firePlaced = true;
                                    if (switched) {
                                        this.fireSwitch.get().swapBack();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean getFirePos(BlockPos posC, BlockPos posP, BlockPos posR, Direction dirC, Direction dirP) {
        BlockPos bestPos = null;
        PlaceData bestData = null;
        double closestDistance = 0.0;

        for (int x = dirC.getOpposite().getOffsetX() == 0 ? -1 : Math.min(0, dirC.getOffsetX());
             x <= (dirC.getOpposite().getOffsetX() == 0 ? 1 : Math.max(0, dirC.getOpposite().getOffsetX()));
             x++
        ) {
            for (int y = 0; y <= 1; y++) {
                for (int z = dirC.getOpposite().getOffsetZ() == 0 ? -1 : Math.min(0, dirC.getOffsetZ());
                     z <= (dirC.getOpposite().getOffsetZ() == 0 ? 1 : Math.max(0, dirC.getOpposite().getOffsetZ()));
                     z++
                ) {
                    BlockPos pos = posC.offset(dirC.getOpposite()).add(x, y, z);
                    if (!pos.equals(posC) && !pos.equals(posP) && !pos.equals(posR) && !pos.equals(posP.offset(dirP.getOpposite()))) {
                        if (BlackOut.mc.world.getBlockState(pos).getBlock() instanceof FireBlock) {
                            PlaceData data = SettingUtils.getPlaceData(pos);
                            if (data.valid() && SettingUtils.inPlaceRange(data.pos())) {
                                this.fireData = SettingUtils.getPlaceData(pos);
                                this.firePos = pos;
                                return true;
                            }
                        }

                        double d = pos.toCenterPos().distanceTo(BlackOut.mc.player.getEyePos());
                        if ((bestPos == null || !(d > closestDistance))
                                && OLEPOSSUtils.solid(pos.down())
                                && BlackOut.mc.world.getBlockState(pos).getBlock() instanceof AirBlock) {
                            PlaceData da = SettingUtils.getPlaceData(pos);
                            if (da.valid() && SettingUtils.inPlaceRange(da.pos())) {
                                closestDistance = d;
                                bestPos = pos;
                                bestData = da;
                            }
                        }
                    }
                }
            }
        }

        this.firePos = bestPos;
        this.fireData = bestData;
        return this.firePos != null;
    }

    private void updatePos() {
        this.lastCrystalPos = this.crystalPos;
        this.lastPistonPos = this.pistonPos;
        this.lastRedstonePos = this.redstonePos;
        this.closestCrystalPos = null;
        this.closestPistonPos = null;
        this.closestRedstonePos = null;
        this.closestPistonDir = null;
        this.closestPistonData = null;
        this.closestCrystalPlaceDir = null;
        this.closestCrystalDir = null;
        this.closestRedstoneData = null;
        this.resetPos();
        BlackOut.mc
                .world
                .getPlayers()
                .stream()
                .filter(
                        player -> player != BlackOut.mc.player
                                && player.getPos().distanceTo(BlackOut.mc.player.getPos()) < 10.0
                                && player.getHealth() > 0.0F
                                && !Managers.FRIENDS.isFriend(player)
                                && !player.isSpectator()
                )
                .sorted(Comparator.comparingDouble(i -> i.getPos().distanceTo(BlackOut.mc.player.getPos())))
                .forEach(player -> {
                    if (this.crystalPos == null) {
                        this.update(player, true);
                        if (this.crystalPos != null) {
                            return;
                        }

                        this.update(player, false);
                    }
                });
    }

    private void update(PlayerEntity player, boolean top) {
        this.closestDistance = 10000.0;

        for (Direction dir : Direction.Type.HORIZONTAL) {
            this.resetPos();
            BlockPos cPos = top
                    ? BlockPos.ofFloored(player.getEyePos()).offset(dir).up()
                    : BlockPos.ofFloored(player.getEyePos()).offset(dir);
            this.currentDistance = cPos.toCenterPos().distanceTo(BlackOut.mc.player.getPos());
            if (cPos.equals(this.lastCrystalPos) || !(this.currentDistance > this.closestDistance)) {
                Block b = BlackOut.mc.world.getBlockState(cPos).getBlock();
                if (b instanceof AirBlock || b == Blocks.PISTON_HEAD || b == Blocks.MOVING_PISTON) {
                    b = BlackOut.mc.world.getBlockState(cPos.up()).getBlock();
                    if ((!SettingUtils.oldCrystals() || b instanceof AirBlock || b == Blocks.PISTON_HEAD || b == Blocks.MOVING_PISTON)
                            && (
                            BlackOut.mc.world.getBlockState(cPos.down()).getBlock() == Blocks.OBSIDIAN
                                    || BlackOut.mc.world.getBlockState(cPos.down()).getBlock() == Blocks.BEDROCK
                    )
                            && !EntityUtils.intersects(BoxUtils.crystalSpawnBox(cPos), entity -> !entity.isSpectator() && entity instanceof PlayerEntity)
                            && SettingUtils.inInteractRange(cPos)) {
                        Direction cDir = SettingUtils.getPlaceOnDirection(cPos);
                        if (cDir != null) {
                            this.getPistonPos(cPos, dir);
                            if (this.pistonPos != null && (!this.canFire(true) || this.getFirePos(cPos, this.pistonPos, this.redstonePos, dir, this.pistonDir))) {
                                this.closestDistance = this.currentDistance;
                                this.crystalPos = cPos;
                                this.crystalPlaceDir = cDir;
                                this.crystalDir = dir;
                                this.closestCrystalPos = this.crystalPos;
                                this.closestPistonPos = this.pistonPos;
                                this.closestRedstonePos = this.redstonePos;
                                this.closestPistonDir = this.pistonDir;
                                this.closestPistonData = this.pistonData;
                                this.closestCrystalPlaceDir = this.crystalPlaceDir;
                                this.closestCrystalDir = this.crystalDir;
                                this.closestRedstoneData = this.redstoneData;
                                this.closestFirePos = this.firePos;
                                this.closestFireData = this.fireData;
                                if (this.crystalPos.equals(this.lastCrystalPos)) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        this.crystalPos = this.closestCrystalPos;
        this.pistonPos = this.closestPistonPos;
        this.redstonePos = this.closestRedstonePos;
        this.pistonDir = this.closestPistonDir;
        this.pistonData = this.closestPistonData;
        this.crystalPlaceDir = this.closestCrystalPlaceDir;
        this.crystalDir = this.closestCrystalDir;
        this.redstoneData = this.closestRedstoneData;
        this.firePos = this.closestFirePos;
        this.fireData = this.closestFireData;
        this.target = player;
    }

    private void getPistonPos(BlockPos pos, Direction dir) {
        List<BlockPos> pistonBlocks = this.pistonBlocks(pos, dir);
        this.closestDistance = 10000.0;
        BlockPos cPos = null;
        PlaceData cData = null;
        Direction cDir = null;
        BlockPos cRedstonePos = null;
        PlaceData cRedstoneData = null;

        for (BlockPos position : pistonBlocks) {
            this.currentDistance = BlackOut.mc.player.getEyePos().distanceTo(position.toCenterPos());
            if (position.equals(this.lastPistonPos) || !(this.closestDistance < this.currentDistance)) {
                PlaceData placeData = SettingUtils.getPlaceData(
                        position,
                        null,
                        (p, d) -> !this.isRedstone(p)
                                && !(BlackOut.mc.world.getBlockState(p).getBlock() instanceof PistonBlock)
                                && !(BlackOut.mc.world.getBlockState(p).getBlock() instanceof PistonHeadBlock)
                                && !(BlackOut.mc.world.getBlockState(p).getBlock() instanceof PistonExtensionBlock)
                                && BlackOut.mc.world.getBlockState(p).getBlock() != Blocks.MOVING_PISTON
                                && !(BlackOut.mc.world.getBlockState(p).getBlock() instanceof FireBlock)
                );
                if (placeData.valid() && SettingUtils.inPlaceRange(placeData.pos())) {
                    this.redstonePos(position, dir.getOpposite(), pos);
                    if (this.redstonePos != null) {
                        this.closestDistance = this.currentDistance;
                        cRedstonePos = this.redstonePos;
                        cRedstoneData = this.redstoneData;
                        cPos = position;
                        cDir = dir.getOpposite();
                        cData = placeData;
                        if (position.equals(this.lastPistonPos)) {
                            break;
                        }
                    }
                }
            }
        }

        this.pistonPos = cPos;
        this.pistonDir = cDir;
        this.pistonData = cData;
        this.redstonePos = cRedstonePos;
        this.redstoneData = cRedstoneData;
    }

    private List<BlockPos> pistonBlocks(BlockPos pos, Direction dir) {
        List<BlockPos> blocks = new ArrayList<>();

        for (int x = dir.getOffsetX() == 0 ? -1 : dir.getOffsetX(); x <= (dir.getOffsetX() == 0 ? 1 : dir.getOffsetX()); x++) {
            for (int z = dir.getOffsetZ() == 0 ? -1 : dir.getOffsetZ(); z <= (dir.getOffsetZ() == 0 ? 1 : dir.getOffsetZ()); z++) {
                for (int y = 0; y <= 1; y++) {
                    if ((x != 0 || y != 0 || z != 0) && (!SettingUtils.oldCrystals() || x != 0 || y != 1 || z != 0) && this.upCheck(pos.add(x, y, z))) {
                        blocks.add(pos.add(x, y, z));
                    }
                }
            }
        }

        return blocks.stream()
                .filter(
                        b -> {
                            if (this.blocked(b.offset(dir.getOpposite()))) {
                                return false;
                            } else if (EntityUtils.intersects(BoxUtils.get(b), entity -> !entity.isSpectator() && entity instanceof PlayerEntity)) {
                                return false;
                            } else {
                                return BlackOut.mc.world.getBlockState(b).getBlock() instanceof PistonBlock
                                        || BlackOut.mc.world.getBlockState(b).getBlock() == Blocks.MOVING_PISTON
                                        || BlackOut.mc.world.getBlockState(b).getBlock() instanceof FireBlock || OLEPOSSUtils.replaceable(b);
                            }
                        }
                )
                .toList();
    }

    private void redstonePos(BlockPos pos, Direction pDir, BlockPos cPos) {
        this.closestDistance = 10000.0;
        this.redstonePos = null;
        BlockPos cRedstonePos = null;
        PlaceData cRedstoneData = null;
        if (this.redstone.get() == Redstone.Torch) {
            for (Direction direction : Direction.values()) {
                if (direction != pDir && direction != Direction.DOWN) {
                    BlockPos position = pos.offset(direction);
                    this.currentDistance = position.toCenterPos().distanceTo(BlackOut.mc.player.getEyePos());
                    if ((position.equals(this.lastPistonPos) || !(this.closestDistance < this.currentDistance))
                            && !position.equals(cPos)
                            && (!SettingUtils.oldCrystals() || !position.equals(cPos.up()))
                            && (
                            OLEPOSSUtils.replaceable(position)
                                    || BlackOut.mc.world.getBlockState(position).getBlock() instanceof RedstoneTorchBlock
                                    || BlackOut.mc.world.getBlockState(position).getBlock() instanceof FireBlock
                    )) {
                        this.redstoneData = SettingUtils.getPlaceData(
                                position,
                                null,
                                (p, d) -> {
                                    if (d == Direction.UP && !OLEPOSSUtils.solid(position.down())) {
                                        return false;
                                    } else if (direction == d.getOpposite()) {
                                        return false;
                                    } else if (pos.equals(p)) {
                                        return false;
                                    } else {
                                        return !(BlackOut.mc.world.getBlockState(p).getBlock() instanceof TorchBlock) && !(BlackOut.mc.world.getBlockState(p).getBlock() instanceof PistonBlock)
                                                && !(BlackOut.mc.world.getBlockState(p).getBlock() instanceof PistonHeadBlock);
                                    }
                                }
                        );
                        if (this.redstoneData.valid() && SettingUtils.inPlaceRange(this.redstoneData.pos()) && SettingUtils.inMineRange(position)) {
                            this.closestDistance = this.currentDistance;
                            cRedstonePos = position;
                            cRedstoneData = this.redstoneData;
                            if (position.equals(this.lastRedstonePos)) {
                                break;
                            }
                        }
                    }
                }
            }

            this.redstonePos = cRedstonePos;
            this.redstoneData = cRedstoneData;
        } else {
            for (Direction directionx : Direction.values()) {
                if (directionx != pDir) {
                    BlockPos position = pos.offset(directionx);
                    this.currentDistance = position.toCenterPos().distanceTo(BlackOut.mc.player.getEyePos());
                    if ((position.equals(this.lastPistonPos) || !(this.closestDistance < this.currentDistance))
                            && !position.equals(cPos)
                            && (OLEPOSSUtils.replaceable(position) || BlackOut.mc.world.getBlockState(position).getBlock() == Blocks.REDSTONE_BLOCK)
                            && !BoxUtils.get(position).intersects(OLEPOSSUtils.getCrystalBox(cPos))
                            && !EntityUtils.intersects(BoxUtils.get(position), entity -> !entity.isSpectator() && entity instanceof PlayerEntity)) {
                        this.redstoneData = SettingUtils.getPlaceData(position, (p, d) -> pos.equals(p), null);
                        if (this.redstoneData.valid()) {
                            this.closestDistance = this.currentDistance;
                            cRedstonePos = position;
                            cRedstoneData = this.redstoneData;
                            if (position.equals(this.lastRedstonePos)) {
                                break;
                            }
                        }
                    }
                }
            }

            this.redstonePos = cRedstonePos;
            this.redstoneData = cRedstoneData;
        }
    }

    private boolean upCheck(BlockPos pos) {
        double dx = BlackOut.mc.player.getEyePos().x - pos.getX() - 0.5;
        double dz = BlackOut.mc.player.getEyePos().z - pos.getZ() - 0.5;
        return Math.sqrt(dx * dx + dz * dz) > Math.abs(BlackOut.mc.player.getEyePos().y - pos.getY() - 0.5);
    }

    private boolean isRedstone(BlockPos pos) {
        return BlackOut.mc.world.getBlockState(pos).emitsRedstonePower();
    }

    private boolean blocked(BlockPos pos) {
        Block b = BlackOut.mc.world.getBlockState(pos).getBlock();
        if (b == Blocks.MOVING_PISTON) {
            return false;
        } else if (b == Blocks.PISTON_HEAD) {
            return false;
        } else if (b == Blocks.REDSTONE_TORCH) {
            return false;
        } else {
            return !(b instanceof FireBlock) && !(BlackOut.mc.world.getBlockState(pos).getBlock() instanceof AirBlock);
        }
    }

    private void resetPos() {
        this.crystalPos = null;
        this.pistonPos = null;
        this.firePos = null;
        this.redstonePos = null;
        this.pistonDir = null;
        this.pistonData = null;
        this.crystalPlaceDir = null;
        this.crystalDir = null;
        this.redstoneData = null;
    }

    public enum Redstone {
        Torch(Items.REDSTONE_TORCH, Blocks.REDSTONE_TORCH),
        Block(Items.REDSTONE_BLOCK, Blocks.REDSTONE_BLOCK);

        public final Item i;
        public final Block b;

        Redstone(Item i, Block b) {
            this.i = i;
            this.b = b;
        }
    }
}
