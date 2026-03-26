package bodevelopment.client.blackout.module.modules.combat.offensive;

import bodevelopment.client.blackout.util.PlayerUtils;
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
import bodevelopment.client.blackout.util.BlockUtils;
import bodevelopment.client.blackout.util.InvUtils;
import bodevelopment.client.blackout.util.SettingUtils;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PistonCrystal extends Module {
    private static PistonCrystal INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgDelay = this.addGroup("Delay");
    private final SettingGroup sgSwitch = this.addGroup("Switch");
    private final SettingGroup sgToggle = this.addGroup("Toggle");
    private final SettingGroup sgSwing = this.addGroup("Swing");
    private final SettingGroup sgRender = this.addGroup("Render");

    private final Setting<Boolean> pauseEat = this.sgGeneral.booleanSetting("Pause on Consume", false, "Suspends operations while eating or drinking.");
    private final Setting<Boolean> fire = this.sgGeneral.booleanSetting("Ignition", false, "Uses fire to immediately detonate the crystal upon reaching the target.");
    private final Setting<Redstone> redstone = this.sgGeneral.enumSetting("Redstone Type", Redstone.Torch, "The redstone component used to power the piston.");
    private final Setting<Boolean> alwaysAttack = this.sgGeneral.booleanSetting("Clear Obstructions", false, "Continuously attacks any crystals that obstruct the placement path.");
    private final Setting<Double> attackSpeed = this.sgGeneral.doubleSetting("Attack Frequency", 4.0, 0.0, 20.0, 0.1, "The speed at which obstructing crystals are attacked.");
    private final Setting<Boolean> pauseOffGround = this.sgGeneral.booleanSetting("Grounded Only", true, "Suspends the module when the player is airborne to maintain precision.");

    private final Setting<Double> pcDelay = this.sgDelay.doubleSetting("Piston-to-Crystal Delay", 0.0, 0.0, 1.0, 0.01, "The wait time between placing the piston and the crystal.");
    private final Setting<Double> cfDelay = this.sgDelay.doubleSetting("Crystal-to-Fire Delay", 0.25, 0.0, 1.0, 0.01, "The wait time between placing the crystal and ignition.");
    private final Setting<Double> crDelay = this.sgDelay.doubleSetting("Crystal-to-Redstone Delay", 0.25, 0.0, 1.0, 0.01, "The wait time between placing the crystal and the redstone source.");
    private final Setting<Double> rmDelay = this.sgDelay.doubleSetting("Redstone-to-Mine Delay", 0.25, 0.0, 1.0, 0.01, "The wait time between placing redstone and initiating the mining process.");
    private final Setting<Double> raDelay = this.sgDelay.doubleSetting("Redstone-to-Attack Delay", 0.1, 0.0, 1.0, 0.01, "The wait time between powering the piston and attacking the crystal.");
    private final Setting<Double> mpDelay = this.sgDelay.doubleSetting("Cycle Reset Delay", 0.25, 0.0, 1.0, 0.01, "The wait time after a successful cycle before starting the next placement.");

    private final Setting<SwitchMode> crystalSwitch = this.sgSwitch.enumSetting("Crystal Switch", SwitchMode.Normal, "The method used to select End Crystals in the hotbar.");
    private final Setting<SwitchMode> pistonSwitch = this.sgSwitch.enumSetting("Piston Switch", SwitchMode.Normal, "The method used to select Pistons in the hotbar.");
    private final Setting<SwitchMode> redstoneSwitch = this.sgSwitch.enumSetting("Redstone Switch", SwitchMode.Normal, "The method used to select redstone sources in the hotbar.");
    private final Setting<SwitchMode> fireSwitch = this.sgSwitch.enumSetting("Fire Switch", SwitchMode.Normal, "The method used to select flint and steel in the hotbar.");

    private final Setting<Boolean> toggleMove = this.sgToggle.booleanSetting("Auto-Disable on Move", false, "Disables the module if the player moves from their current position.");
    private final Setting<Boolean> toggleEnemyMove = this.sgToggle.booleanSetting("Auto-Disable on Target Move", false, "Disables the module if the enemy moves away.");

    private final Setting<Boolean> crystalSwing = this.sgSwing.booleanSetting("Crystal Swing", false, "Visualizes the arm swing when placing a crystal.");
    private final Setting<SwingHand> crystalHand = this.sgSwing.enumSetting("Crystal Arm", SwingHand.RealHand, "The arm used for crystal placement animations.");
    private final Setting<Boolean> attackSwing = this.sgSwing.booleanSetting("Attack Swing", false, "Visualizes the arm swing when attacking.");
    private final Setting<SwingHand> attackHand = this.sgSwing.enumSetting("Attack Arm", SwingHand.RealHand, "The arm used for attack animations.");
    private final Setting<Boolean> pistonSwing = this.sgSwing.booleanSetting("Piston Swing", false, "Visualizes the arm swing when placing a piston.");
    private final Setting<SwingHand> pistonHand = this.sgSwing.enumSetting("Piston Arm", SwingHand.RealHand, "The arm used for piston placement animations.");
    private final Setting<Boolean> redstoneSwing = this.sgSwing.booleanSetting("Redstone Swing", false, "Visualizes the arm swing when placing redstone.");
    private final Setting<SwingHand> redstoneHand = this.sgSwing.enumSetting("Redstone Arm", SwingHand.RealHand, "The arm used for redstone placement animations.");
    private final Setting<Boolean> fireSwing = this.sgSwing.booleanSetting("Fire Swing", false, "Visualizes the arm swing when using flint and steel.");
    private final Setting<SwingHand> fireHand = this.sgSwing.enumSetting("Fire Arm", SwingHand.RealHand, "The arm used for fire placement animations.");

    private final Setting<Double> crystalHeight = this.sgRender.doubleSetting("Crystal Render Height", 0.25, -1.0, 1.0, 0.05, "The vertical scale of the crystal placement highlight.");
    private final Setting<RenderShape> crystalShape = this.sgRender.enumSetting("Crystal Highlight Shape", RenderShape.Full, "The geometry used to render the crystal target box.");
    private final Setting<BlackOutColor> crystalLineColor = this.sgRender.colorSetting("Crystal Outline Color", new BlackOutColor(255, 0, 0, 255), "The color of the crystal highlight edges.");
    private final Setting<BlackOutColor> crystalSideColor = this.sgRender.colorSetting("Crystal Fill Color", new BlackOutColor(255, 0, 0, 50), "The color of the crystal highlight faces.");
    private final Setting<Double> pistonHeight = this.sgRender.doubleSetting("Piston Render Height", 1.0, -1.0, 1.0, 0.05, "The vertical scale of the piston placement highlight.");
    private final Setting<RenderShape> pistonShape = this.sgRender.enumSetting("Piston Highlight Shape", RenderShape.Full, "The geometry used to render the piston target box.");
    private final Setting<BlackOutColor> pistonLineColor = this.sgRender.colorSetting("Piston Outline Color", new BlackOutColor(255, 255, 255, 255), "The color of the piston highlight edges.");
    private final Setting<BlackOutColor> pistonSideColor = this.sgRender.colorSetting("Piston Fill Color", new BlackOutColor(255, 255, 255, 50), "The color of the piston highlight faces.");
    private final Setting<Double> redstoneHeight = this.sgRender.doubleSetting("Redstone Render Height", 1.0, -1.0, 1.0, 0.05, "The vertical scale of the redstone placement highlight.");
    private final Setting<RenderShape> redstoneShape = this.sgRender.enumSetting("Redstone Highlight Shape", RenderShape.Full, "The geometry used to render the redstone target box.");
    private final Setting<BlackOutColor> redstoneLineColor = this.sgRender.colorSetting("Redstone Outline Color", new BlackOutColor(255, 0, 0, 255), "The color of the redstone highlight edges.");
    private final Setting<BlackOutColor> redstoneSideColor = this.sgRender.colorSetting("Redstone Fill Color", new BlackOutColor(255, 0, 0, 50), "The color of the redstone highlight faces.");

    public static AbstractClientPlayer targetedPlayer = null;
    private final TimerList<Entity> attacked = new TimerList<>(true);
    public BlockPos crystalPos = null;
    private long lastAttack = 0L;
    private BlockPos pistonPos = null;
    private BlockPos firePos = null;
    private BlockPos redstonePos = null;
    private BlockPos lastCrystalPos = null;
    private BlockPos lastPistonPos = null;
    private BlockPos lastRedstonePos = null;
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
    private Phase phase = Phase.PLACE_PISTON;
    private long pistonTime = 0L;
    private long redstoneTime = 0L;
    private long mineTime = 0L;
    private long crystalTime = 0L;
    private boolean minedThisTick = false;
    private boolean firePlaced = false;
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
        super("Piston Crystal", "Utilizes pistons to force End Crystals into the target's space for unavoidable explosive damage.", SubCategory.OFFENSIVE, true);
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
        this.phase = Phase.PLACE_PISTON;
        this.firePlaced = false;
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        this.minedThisTick = false;
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (PlayerUtils.isInGame()) {
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
                    if (this.phase == Phase.CYCLE_COMPLETE
                            && System.currentTimeMillis() - this.mineTime > this.mpDelay.get() * 1000.0
                            && (this.firePlaced || !this.canFire(false))) {
                        this.resetProgress();
                    }

                    if (!this.pauseEat.get() || !BlackOut.mc.player.isUsingItem()) {
                        if (!this.pauseOffGround.get() || BlackOut.mc.player.onGround()) {
                            if (this.target instanceof AbstractClientPlayer player) {
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
        this.phase = Phase.PLACE_PISTON;
        this.firePlaced = false;
        this.pistonTime = 0L;
        this.redstoneTime = 0L;
        this.mineTime = 0L;
        this.crystalTime = 0L;
        this.lastAttack = 0L;
    }

    private String checkToggle() {
        BlockPos currentPos = BlackOut.mc.player.blockPosition();
        BlockPos enemyPos = this.target == null ? null : this.target.blockPosition();
        if (this.toggleMove.get() && !currentPos.equals(this.prevPos)) {
            return "moved";
        } else if (this.toggleEnemyMove.get() && enemyPos != null && !enemyPos.equals(this.prevEnemyPos)) {
            return "enemy moved";
        } else {
            this.prevPos = currentPos;
            this.prevEnemyPos = enemyPos;
            return null;
        }
    }

    private AABB getBox(BlockPos pos, double height) {
        return new AABB(
                pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + height, pos.getZ() + 1
        );
    }

    private boolean isBlocked() {
        BlockState pistonState = BlackOut.mc.level.getBlockState(this.pistonPos);
        this.redstoneBlocking = false;
        this.entityBlocking = false;
        this.pistonBlocking = false;
        if (pistonState.getBlock() != Blocks.PISTON) {
            this.redstoneBlocking = BlackOut.mc.level.getBlockState(this.redstonePos).getBlock() == this.redstone.get().b;
            this.entityBlocking = EntityUtils.intersects(BoxUtils.get(this.pistonPos), this::validForPistonIntersect);
        } else if (pistonState.getValue(DirectionalBlock.FACING) != this.pistonDir) {
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
            return !(entity instanceof EndCrystal) || !this.attacked.contains(entity);
        }
    }

    private boolean validForCrystalIntersect(Entity entity) {
        if (entity.isSpectator()) {
            return false;
        } else if (entity.tickCount < 10) {
            return false;
        } else if (entity instanceof EndCrystal) {
            return !entity.blockPosition().equals(this.crystalPos) && !this.attacked.contains(entity);
        } else {
            return true;
        }
    }

    private void mineUpdate(boolean blocked) {
        if (!blocked) {
            if (System.currentTimeMillis() - this.redstoneTime < this.rmDelay.get() * 1000.0) {
                return;
            }

            if (this.phase != Phase.MINE) {
                return;
            }
        }

        if (!this.minedThisTick) {
            AutoMine autoMine = AutoMine.getInstance();
            if (this.redstone.get() == Redstone.Torch) {
                Direction mineDir = SettingUtils.getPlaceOnDirection(this.redstonePos);
                if (mineDir != null) {
                    this.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, this.redstonePos, mineDir));
                    this.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, this.redstonePos, mineDir));
                }
            } else {
                if (!autoMine.enabled) {
                    if (System.currentTimeMillis() - this.prevNotification > 500L) {
                        Managers.NOTIFICATIONS.addNotification("Automine required for redstone block mode.", this.getDisplayName(), 1.0, Notifications.Type.Info);
                        this.prevNotification = System.currentTimeMillis();
                    }

                    return;
                }

                if (BlackOut.mc.level.getBlockState(this.redstonePos).getBlock() != Blocks.REDSTONE_BLOCK && this.phase == Phase.CYCLE_COMPLETE) {
                    return;
                }

                if (this.redstonePos.equals(autoMine.minePos)) {
                    return;
                }

                autoMine.onStart(this.redstonePos);
            }

            if (this.phase == Phase.MINE) {
                this.mineTime = System.currentTimeMillis();
                this.phase = Phase.CYCLE_COMPLETE;
            }

            this.minedThisTick = true;
        }
    }

    private void updateAttack(boolean blocked) {
        if (!blocked) {
            if (this.phase.ordinal() < Phase.MINE.ordinal()) {
                return;
            }

            if (System.currentTimeMillis() - this.redstoneTime < this.raDelay.get() * 1000.0) {
                return;
            }
        }

        EndCrystal crystal = null;
        double cd = 10000.0;

        for (Entity entity : BlackOut.mc.level.entitiesForRendering()) {
            if (entity instanceof EndCrystal c
                    && (blocked || c.getX() != this.crystalPos.getX() + 0.5 || c.getZ() != this.crystalPos.getZ() + 0.5)
                    && (this.alwaysAttack.get() || blocked || c.getX() - c.getBlockX() != 0.5 || c.getZ() - c.getBlockZ() != 0.5)
                    && (c.getBoundingBox().intersects(BoxUtils.crystalSpawnBox(this.crystalPos)) || blocked && c.getBoundingBox().intersects(BoxUtils.get(this.pistonPos)))) {
                double d = BlackOut.mc.player.getEyePosition().distanceTo(c.position());
                if (d < cd) {
                    cd = d;
                    crystal = c;
                }
            }
        }

        if (crystal != null) {
            if (!SettingUtils.shouldRotate(RotationType.Attacking) || this.rotation.attackRotate(crystal.getBoundingBox(), 0.1, "attacking")) {
                if (!(System.currentTimeMillis() - this.lastAttack < 1000.0 / this.attackSpeed.get())) {
                    SettingUtils.swing(SwingState.Pre, SwingType.Attacking, InteractionHand.MAIN_HAND);
                    this.sendPacket(ServerboundInteractPacket.createAttackPacket(crystal, BlackOut.mc.player.isShiftKeyDown()));
                    SettingUtils.swing(SwingState.Post, SwingType.Attacking, InteractionHand.MAIN_HAND);
                    if (SettingUtils.shouldRotate(RotationType.Attacking)) {
                        this.rotation.end("attacking");
                    }

                    if (this.attackSwing.get()) {
                        this.clientSwing(this.attackHand.get(), InteractionHand.MAIN_HAND);
                    }

                    this.lastAttack = System.currentTimeMillis();
                    this.attacked.add(crystal, 0.25);
                }
            }
        }
    }

    private void updatePiston() {
        if (this.phase != Phase.PLACE_PISTON) return;

        if (this.pistonData != null) {
            InteractionHand hand = InvUtils.getHand(Items.PISTON);
            boolean available = hand != null;
            FindResult result = this.pistonSwitch.get().find(Items.PISTON);
            if (!available) {
                available = result.wasFound();
            }

            if (available) {
                if (!SettingUtils.shouldRotate(RotationType.BlockPlace) || this.rotation.rotateBlock(this.pistonData, RotationType.BlockPlace, "piston")) {
                    boolean switched = false;
                    if (hand != null || (switched = this.pistonSwitch.get().swap(result.slot()))) {
                        this.sendPacket(new ServerboundMovePlayerPacket.Rot(this.pistonDir.getOpposite().toYRot(), Managers.ROTATION.nextPitch, Managers.PACKET.isOnGround(), BlackOut.mc.player.horizontalCollision));
                        this.placeBlock(hand, this.pistonData.pos().getCenter(), this.pistonData.dir(), this.pistonData.pos());
                        if (SettingUtils.shouldRotate(RotationType.BlockPlace)) {
                            this.rotation.end("piston");
                        }

                        if (this.pistonSwing.get()) {
                            this.clientSwing(this.pistonHand.get(), hand);
                        }

                        this.pistonTime = System.currentTimeMillis();
                        this.phase = Phase.PLACE_CRYSTAL;
                        if (switched) {
                            this.pistonSwitch.get().swapBack();
                        }
                    }
                }
            }
        }
    }

    private void updateCrystal(boolean blocked) {
        if (blocked || this.phase == Phase.PLACE_CRYSTAL) {
            if (!(System.currentTimeMillis() - this.pistonTime < this.pcDelay.get() * 1000.0)) {
                if (this.crystalPlaceDir != null) {
                    if (!EntityUtils.intersects(BoxUtils.get(this.crystalPos), entity -> {
                        if (entity.isSpectator()) {
                            return false;
                        } else if (entity instanceof EndCrystal) {
                            return !entity.blockPosition().equals(this.crystalPos) && !this.attacked.contains(entity);
                        } else {
                            return true;
                        }
                    })) {
                        InteractionHand hand = InvUtils.getHand(Items.END_CRYSTAL);
                        boolean available = hand != null;
                        FindResult result = this.crystalSwitch.get().find(Items.END_CRYSTAL);
                        if (!available) {
                            available = result.wasFound();
                        }

                        if (available) {
                            if (!SettingUtils.shouldRotate(RotationType.Interact)
                                    || this.rotation.rotateBlock(this.crystalPos.below(), this.crystalPlaceDir, RotationType.Interact, "crystal")) {
                                boolean switched = false;
                                if (hand != null || (switched = this.crystalSwitch.get().swap(result.slot()))) {
                                    hand = hand == null ? InteractionHand.MAIN_HAND : hand;
                                    this.interactBlock(hand, this.crystalPos.below().getCenter(), this.crystalPlaceDir, this.crystalPos.below());
                                    if (SettingUtils.shouldRotate(RotationType.Interact)) {
                                        this.rotation.end("crystal");
                                    }

                                    if (this.crystalSwing.get()) {
                                        this.clientSwing(this.crystalHand.get(), hand);
                                    }

                                    this.crystalTime = System.currentTimeMillis();
                                    this.phase = Phase.ACTIVATE;
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
        if (this.phase == Phase.ACTIVATE) {
            if (!(System.currentTimeMillis() - this.crystalTime < this.crDelay.get() * 1000.0)) {
                if (this.redstoneData != null) {
                    InteractionHand hand = InvUtils.getHand(this.redstone.get().i);
                    boolean available = hand != null;
                    FindResult result = this.redstoneSwitch.get().find(this.redstone.get().i);
                    if (!available) {
                        available = result.wasFound();
                    }

                    if (available) {
                        if (!SettingUtils.shouldRotate(RotationType.BlockPlace) || this.rotation.rotateBlock(this.redstoneData, RotationType.BlockPlace, "redstone")) {
                            boolean switched = false;
                            if (hand != null || (switched = this.redstoneSwitch.get().swap(result.slot()))) {
                                this.placeBlock(hand, this.redstoneData.pos().getCenter(), this.redstoneData.dir(), this.redstoneData.pos());
                                if (SettingUtils.shouldRotate(RotationType.BlockPlace)) {
                                    this.rotation.end("redstone");
                                }

                                if (this.redstoneSwing.get()) {
                                    this.clientSwing(this.redstoneHand.get(), hand);
                                }

                                this.redstoneTime = System.currentTimeMillis();
                                this.phase = Phase.MINE;
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
            if (this.phase.ordinal() >= Phase.ACTIVATE.ordinal() && !this.firePlaced) {
                if (!(System.currentTimeMillis() - this.crystalTime < this.cfDelay.get() * 1000.0)) {
                    if (this.firePos == null) {
                        this.firePlaced = true;
                    } else {
                        InteractionHand hand = InvUtils.getHand(Items.FLINT_AND_STEEL);
                        FindResult result = this.fireSwitch.get().find(Items.FLINT_AND_STEEL);
                        if (hand != null || result.wasFound()) {
                            if (!SettingUtils.shouldRotate(RotationType.Interact) || this.rotation.rotateBlock(this.fireData, RotationType.Interact, "fire")) {
                                boolean switched = false;
                                if (hand != null || (switched = this.fireSwitch.get().swap(result.slot()))) {
                                    this.interactBlock(hand, this.fireData.pos().getCenter(), this.fireData.dir(), this.fireData.pos());
                                    if (SettingUtils.shouldRotate(RotationType.Interact)) {
                                        this.rotation.end("fire");
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

        for (int x = dirC.getOpposite().getStepX() == 0 ? -1 : Math.min(0, dirC.getStepX());
             x <= (dirC.getOpposite().getStepX() == 0 ? 1 : Math.max(0, dirC.getOpposite().getStepX()));
             x++
        ) {
            for (int y = 0; y <= 1; y++) {
                for (int z = dirC.getOpposite().getStepZ() == 0 ? -1 : Math.min(0, dirC.getStepZ());
                     z <= (dirC.getOpposite().getStepZ() == 0 ? 1 : Math.max(0, dirC.getOpposite().getStepZ()));
                     z++
                ) {
                    BlockPos pos = posC.relative(dirC.getOpposite()).offset(x, y, z);
                    if (!pos.equals(posC) && !pos.equals(posP) && !pos.equals(posR) && !pos.equals(posP.relative(dirP.getOpposite()))) {
                        if (BlackOut.mc.level.getBlockState(pos).getBlock() instanceof FireBlock) {
                            PlaceData data = SettingUtils.getPlaceData(pos);
                            if (data.valid() && SettingUtils.inPlaceRange(data.pos())) {
                                this.fireData = SettingUtils.getPlaceData(pos);
                                this.firePos = pos;
                                return true;
                            }
                        }

                        double d = pos.getCenter().distanceTo(BlackOut.mc.player.getEyePosition());
                        if ((bestPos == null || !(d > closestDistance))
                                && BlockUtils.isFaceSturdy(pos.below())
                                && BlackOut.mc.level.getBlockState(pos).getBlock() instanceof AirBlock) {
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
                .level
                .players()
                .stream()
                .filter(
                        player -> player != BlackOut.mc.player
                                && player.position().distanceTo(BlackOut.mc.player.position()) < 10.0
                                && player.getHealth() > 0.0F
                                && !Managers.FRIENDS.isFriend(player)
                                && !player.isSpectator()
                )
                .sorted(Comparator.comparingDouble(i -> i.position().distanceTo(BlackOut.mc.player.position())))
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

    private void update(Player player, boolean top) {
        this.closestDistance = 10000.0;

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            this.resetPos();
            BlockPos cPos = top
                    ? BlockPos.containing(player.getEyePosition()).relative(dir).above()
                    : BlockPos.containing(player.getEyePosition()).relative(dir);
            this.currentDistance = cPos.getCenter().distanceTo(BlackOut.mc.player.position());
            if (cPos.equals(this.lastCrystalPos) || !(this.currentDistance > this.closestDistance)) {
                Block b = BlackOut.mc.level.getBlockState(cPos).getBlock();
                if (b instanceof AirBlock || b == Blocks.PISTON_HEAD || b == Blocks.MOVING_PISTON) {
                    b = BlackOut.mc.level.getBlockState(cPos.above()).getBlock();
                    if ((!SettingUtils.oldCrystals() || b instanceof AirBlock || b == Blocks.PISTON_HEAD || b == Blocks.MOVING_PISTON)
                            && (
                            BlackOut.mc.level.getBlockState(cPos.below()).getBlock() == Blocks.OBSIDIAN
                                    || BlackOut.mc.level.getBlockState(cPos.below()).getBlock() == Blocks.BEDROCK
                    )
                            && !EntityUtils.intersects(BoxUtils.crystalSpawnBox(cPos), entity -> !entity.isSpectator() && entity instanceof Player)
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
            this.currentDistance = BlackOut.mc.player.getEyePosition().distanceTo(position.getCenter());
            if (position.equals(this.lastPistonPos) || !(this.closestDistance < this.currentDistance)) {
                PlaceData placeData = SettingUtils.getPlaceData(
                        position,
                        null,
                        (p, d) -> !this.isRedstone(p)
                                && !(BlackOut.mc.level.getBlockState(p).getBlock() instanceof PistonBaseBlock)
                                && !(BlackOut.mc.level.getBlockState(p).getBlock() instanceof PistonHeadBlock)
                                && !(BlackOut.mc.level.getBlockState(p).getBlock() instanceof MovingPistonBlock)
                                && BlackOut.mc.level.getBlockState(p).getBlock() != Blocks.MOVING_PISTON
                                && !(BlackOut.mc.level.getBlockState(p).getBlock() instanceof FireBlock)
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

        for (int x = dir.getStepX() == 0 ? -1 : dir.getStepX(); x <= (dir.getStepX() == 0 ? 1 : dir.getStepX()); x++) {
            for (int z = dir.getStepZ() == 0 ? -1 : dir.getStepZ(); z <= (dir.getStepZ() == 0 ? 1 : dir.getStepZ()); z++) {
                for (int y = 0; y <= 1; y++) {
                    if ((x != 0 || y != 0 || z != 0) && (!SettingUtils.oldCrystals() || x != 0 || y != 1 || z != 0) && this.upCheck(pos.offset(x, y, z))) {
                        blocks.add(pos.offset(x, y, z));
                    }
                }
            }
        }

        return blocks.stream()
                .filter(
                        b -> {
                            if (this.blocked(b.relative(dir.getOpposite()))) {
                                return false;
                            } else if (EntityUtils.intersects(BoxUtils.get(b), entity -> !entity.isSpectator() && entity instanceof Player)) {
                                return false;
                            } else {
                                return BlackOut.mc.level.getBlockState(b).getBlock() instanceof PistonBaseBlock
                                        || BlackOut.mc.level.getBlockState(b).getBlock() == Blocks.MOVING_PISTON
                                        || BlackOut.mc.level.getBlockState(b).getBlock() instanceof FireBlock || BlockUtils.replaceable(b);
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
                    BlockPos position = pos.relative(direction);
                    this.currentDistance = position.getCenter().distanceTo(BlackOut.mc.player.getEyePosition());
                    if ((position.equals(this.lastPistonPos) || !(this.closestDistance < this.currentDistance))
                            && !position.equals(cPos)
                            && (!SettingUtils.oldCrystals() || !position.equals(cPos.above()))
                            && (
                            BlockUtils.replaceable(position)
                                    || BlackOut.mc.level.getBlockState(position).getBlock() instanceof RedstoneTorchBlock
                                    || BlackOut.mc.level.getBlockState(position).getBlock() instanceof FireBlock
                    )) {
                        this.redstoneData = SettingUtils.getPlaceData(
                                position,
                                null,
                                (p, d) -> {
                                    if (d == Direction.UP && !BlockUtils.isFaceSturdy(position.below())) {
                                        return false;
                                    } else if (direction == d.getOpposite()) {
                                        return false;
                                    } else if (pos.equals(p)) {
                                        return false;
                                    } else {
                                        return !(BlackOut.mc.level.getBlockState(p).getBlock() instanceof TorchBlock) && !(BlackOut.mc.level.getBlockState(p).getBlock() instanceof PistonBaseBlock)
                                                && !(BlackOut.mc.level.getBlockState(p).getBlock() instanceof PistonHeadBlock);
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
                    BlockPos position = pos.relative(directionx);
                    this.currentDistance = position.getCenter().distanceTo(BlackOut.mc.player.getEyePosition());
                    if ((position.equals(this.lastPistonPos) || !(this.closestDistance < this.currentDistance))
                            && !position.equals(cPos)
                            && (BlockUtils.replaceable(position) || BlackOut.mc.level.getBlockState(position).getBlock() == Blocks.REDSTONE_BLOCK)
                            && !BoxUtils.get(position).intersects(BoxUtils.crystalBox(cPos))
                            && !EntityUtils.intersects(BoxUtils.get(position), entity -> !entity.isSpectator() && entity instanceof Player)) {
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
        double dx = BlackOut.mc.player.getEyePosition().x - pos.getX() - 0.5;
        double dz = BlackOut.mc.player.getEyePosition().z - pos.getZ() - 0.5;
        return Math.sqrt(dx * dx + dz * dz) > Math.abs(BlackOut.mc.player.getEyePosition().y - pos.getY() - 0.5);
    }

    private boolean isRedstone(BlockPos pos) {
        return BlackOut.mc.level.getBlockState(pos).isSignalSource();
    }

    private boolean blocked(BlockPos pos) {
        Block b = BlackOut.mc.level.getBlockState(pos).getBlock();
        if (b == Blocks.MOVING_PISTON) {
            return false;
        } else if (b == Blocks.PISTON_HEAD) {
            return false;
        } else if (b == Blocks.REDSTONE_TORCH) {
            return false;
        } else {
            return !(b instanceof FireBlock) && !(BlackOut.mc.level.getBlockState(pos).getBlock() instanceof AirBlock);
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

    enum Phase {
        PLACE_PISTON,
        PLACE_CRYSTAL,
        ACTIVATE,
        MINE,
        CYCLE_COMPLETE
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
