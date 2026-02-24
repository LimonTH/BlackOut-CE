package bodevelopment.client.blackout.module.modules.combat.defensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.ObsidianModule;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import bodevelopment.client.blackout.util.RotationUtils;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class Surround extends ObsidianModule {
    private static Surround INSTANCE;
    private final SettingGroup sgToggle = this.addGroup("Toggle");

    private final Setting<Boolean> center = this.sgGeneral.booleanSetting("Center", false, "Moves to block center before surrounding.");
    private final Setting<Boolean> smartCenter = this.sgGeneral
            .booleanSetting("Smart Center", true, "Only moves until whole hitbox is inside target block.", this.center::get);
    private final Setting<Boolean> phaseCenter = this.sgGeneral.booleanSetting("Phase Center", true, "Doesn't center if clipped inside a block.", this.center::get);
    private final Setting<Boolean> extend = this.sgGeneral.booleanSetting("Extend", true, ".");
    private final Setting<Boolean> toggleMove = this.sgToggle.booleanSetting("Toggle Move", false, "Toggles if you move horizontally.");
    private final Setting<VerticalToggleMode> toggleVertical = this.sgToggle
            .enumSetting("Toggle Vertical", VerticalToggleMode.Up, "Toggles the module if you move vertically.");
    private final Setting<Double> singleCooldown = this.sgSpeed
            .doubleSetting("Single Cooldown", 0.05, 0.0, 1.0, 0.01, "Waits x seconds before trying to place at the same position if there is 1 missing block.");
    private final Setting<Boolean> antiCev = this.sgAttack.booleanSetting("Anti CEV", false, "Attacks crystals placed on surround blocks.", this.attack::get);
    private final Map<AbstractClientPlayerEntity, Long> blockedSince = new HashMap<>();
    private final Direction[] directions = new Direction[]{
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN
    };
    public boolean placing = false;
    private BlockPos lastPos = null;
    private boolean centered = false;
    private BlockPos currentPos = null;

    public Surround() {
        super("Surround", "Places blocks around your legs to protect from explosions.", SubCategory.DEFENSIVE);
        INSTANCE = this;
    }

    public static Surround getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        this.centered = false;
        this.lastPos = this.getPos();
        this.currentPos = this.getPos();
        super.onEnable();
    }

    @Override
    protected boolean preCalc() {
        this.lastPos = this.currentPos;
        this.currentPos = this.getPos();
        this.placing = false;
        this.setBB();
        return this.checkToggle();
    }

    @Override
    protected boolean validForBlocking(Entity entity) {
        if (this.antiCev.get()) {
            for (BlockPos pos : this.blockPlacements) {
                if (entity.getBlockPos().equals(pos.up())) {
                    return true;
                }
            }
        }

        return super.validForBlocking(entity);
    }

    @Override
    protected double getCooldown() {
        return this.oneMissing() ? this.singleCooldown.get() : this.cooldown.get();
    }

    @Override
    protected void addInsideBlocks() {
        this.addBlocks(BlackOut.mc.player, this.getSize(BlackOut.mc.player));
        this.blockPlacements.clear();
        this.addPlacements();
        if (this.extend.get()) {
            BlackOut.mc
                    .world
                    .getPlayers()
                    .stream()
                    .filter(player -> BlackOut.mc.player.distanceTo(player) < 5.0F && player != BlackOut.mc.player)
                    .sorted(Comparator.comparingDouble(player -> BlackOut.mc.player.distanceTo(player)))
                    .forEach(player -> {
                        if (this.intersects(player)) {
                            if (System.currentTimeMillis() - this.blockedSince.computeIfAbsent(player, p -> System.currentTimeMillis()) >= 200L) {
                                this.addBlocks(player, this.getSize(player));
                            }
                        } else {
                            this.blockedSince.remove(player);
                        }
                    });
        }

        this.blockedSince.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() > 60000L);
    }

    @Override
    protected void addPlacements() {
        this.insideBlocks.forEach(pos -> {
            for (Direction dir : this.directions) {
                if (!this.blockPlacements.contains(pos.offset(dir)) && !this.insideBlocks.contains(pos.offset(dir))) {
                    this.blockPlacements.add(pos.offset(dir));
                }
            }
        });
    }

    @Override
    protected void addBlocks(Entity entity, int[] size) {
        BlockPos pos = entity.getBlockPos();

        for (int x = size[0]; x <= size[1]; x++) {
            for (int z = size[2]; z <= size[3]; z++) {
                BlockPos p = pos.add(x, 0, z);
                if ((!(BlackOut.mc.world.getBlockState(p).getBlock().getBlastResistance() > 600.0F) || p.equals(this.currentPos))
                        && !this.insideBlocks.contains(p.withY(this.currentPos.getY()))) {
                    this.insideBlocks.add(p.withY(this.currentPos.getY()));
                }
            }
        }
    }

    private void setBB() {
        if (!this.centered
                && this.center.get()
                && BlackOut.mc.player.isOnGround()
                && (!this.phaseCenter.get() || !OLEPOSSUtils.inside(BlackOut.mc.player, BlackOut.mc.player.getBoundingBox().contract(0.01, 0.01, 0.01)))) {
            double targetX;
            double targetZ;
            if (this.smartCenter.get()) {
                targetX = MathHelper.clamp(
                        BlackOut.mc.player.getX(), this.currentPos.getX() + 0.31, this.currentPos.getX() + 0.69
                );
                targetZ = MathHelper.clamp(
                        BlackOut.mc.player.getZ(), this.currentPos.getZ() + 0.31, this.currentPos.getZ() + 0.69
                );
            } else {
                targetX = this.currentPos.getX() + 0.5;
                targetZ = this.currentPos.getZ() + 0.5;
            }

            double dist = new Vec3d(targetX, 0.0, targetZ)
                    .distanceTo(new Vec3d(BlackOut.mc.player.getX(), 0.0, BlackOut.mc.player.getZ()));
            if (dist < 0.2873) {
                this.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(targetX, BlackOut.mc.player.getY(), targetZ, Managers.PACKET.isOnGround()));
            }

            double x = BlackOut.mc.player.getX();
            double z = BlackOut.mc.player.getZ();

            for (int i = 0; i < Math.ceil(dist / 0.2873); i++) {
                double yaw = RotationUtils.getYaw(BlackOut.mc.player.getEyePos(), new Vec3d(targetX, 0.0, targetZ), 0.0) + 90.0;
                x += Math.cos(Math.toRadians(yaw)) * 0.2873;
                z += Math.sin(Math.toRadians(yaw)) * 0.2873;
                this.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, BlackOut.mc.player.getY(), z, Managers.PACKET.isOnGround()));
            }

            BlackOut.mc.player.setPos(targetX, BlackOut.mc.player.getY(), targetZ);
            BlackOut.mc
                    .player
                    .setBoundingBox(
                            new Box(
                                    targetX - 0.3,
                                    BlackOut.mc.player.getY(),
                                    targetZ - 0.3,
                                    targetX + 0.3,
                                    BlackOut.mc.player.getY() + (BlackOut.mc.player.getBoundingBox().maxY - BlackOut.mc.player.getBoundingBox().minY),
                                    targetZ + 0.3
                            )
                    );
            this.centered = true;
        }
    }

    private boolean checkToggle() {
        if (this.lastPos != null) {
            if (this.toggleMove.get()
                    && (this.currentPos.getX() != this.lastPos.getX() || this.currentPos.getZ() != this.lastPos.getZ())) {
                this.disable(this.getDisplayName() + " disabled moved horizontally");
                return true;
            }

            if ((this.toggleVertical.get() == VerticalToggleMode.Up || this.toggleVertical.get() == VerticalToggleMode.Any)
                    && this.currentPos.getY() > this.lastPos.getY()) {
                this.disable(this.getDisplayName() + " disabled moved up");
                return true;
            }

            if ((this.toggleVertical.get() == VerticalToggleMode.Down || this.toggleVertical.get() == VerticalToggleMode.Any)
                    && this.currentPos.getY() < this.lastPos.getY()) {
                this.disable(this.getDisplayName() + " disabled moved down");
                return true;
            }
        }

        return false;
    }

    private boolean oneMissing() {
        boolean alreadyFound = false;

        for (BlockPos pos : this.blockPlacements) {
            if (OLEPOSSUtils.replaceable(pos)) {
                if (alreadyFound) {
                    return false;
                }

                alreadyFound = true;
            }
        }

        return true;
    }

    public enum PlaceDelayMode {
        Ticks,
        Seconds
    }

    public enum VerticalToggleMode {
        Disabled,
        Up,
        Down,
        Any
    }
}
