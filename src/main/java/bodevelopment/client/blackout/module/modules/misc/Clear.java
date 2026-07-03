/*
 * Blackout Client (CE) - A cutting-edge, feature-rich cheat client for Minecraft.
 * A modernized continuation of the original Blackout project by OLEPOSSU & KassuK.
 * Copyright (C) 2026  LimonTH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://gnu.org>.
 */

package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class Clear extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Integer> minX = this.sgGeneral.intSetting("Minimum X", -75, -100, 100, 1, "The lower bound of the X-axis for the clearing area.");
    private final Setting<Integer> maxX = this.sgGeneral.intSetting("Maximum X", 75, -100, 100, 1, "The upper bound of the X-axis for the clearing area.");
    private final Setting<Integer> minY = this.sgGeneral.intSetting("Minimum Y", 0, -65, 350, 1, "The lower bound of the Y-axis for the clearing area.");
    private final Setting<Integer> maxY = this.sgGeneral.intSetting("Maximum Y", 100, -100, 100, 1, "The upper bound of the Y-axis for the clearing area.");
    private final Setting<Integer> minZ = this.sgGeneral.intSetting("Minimum Z", -75, -100, 100, 1, "The lower bound of the Z-axis for the clearing area.");
    private final Setting<Integer> maxZ = this.sgGeneral.intSetting("Maximum Z", 75, -100, 100, 1, "The upper bound of the Z-axis for the clearing area.");
    private final Setting<Double> timer = this.sgGeneral.doubleSetting("Game Speed", 5.0, 1.0, 10.0, 0.1, "The multiplier applied to the game timer to accelerate the clearing process.");
    private final Setting<Integer> movement = this.sgGeneral.intSetting("Step Distance", 1, 1, 10, 1, "The distance the player moves per iteration while scanning.");
    private final Setting<Integer> maxMovements = this.sgGeneral.intSetting("Iterations Per Tick", 3, 1, 10, 1, "The maximum number of position updates processed within a single game tick.");
    private final Setting<Double> range = this.sgGeneral.doubleSetting("Interaction Range", 6.0, 1.0, 10.0, 0.1, "The maximum distance at which blocks will be targeted for removal.");

    private boolean setTimer = true;
    private int x = 0;
    private int y = 0;
    private int z = 0;
    private int sizeX;
    private int sizeY;
    private int sizeZ;
    private boolean directionX = false;
    private boolean directionZ = false;

    public Clear() {
        super("Clear", "Automates block removal across a specified 3D volume, typically used for clearing large areas on creative servers.", SubCategory.MISC, true);
    }

    @Override
    public void onEnable() {
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.directionX = false;
        this.directionZ = false;
    }

    @Override
    public void onDisable() {
        if (this.setTimer) {
            this.setTimer = false;
            Timer.reset();
        }
    }

    @Event
    public void onMove(MoveEvent.Pre event) {
        this.tick();
        event.set(this, 0.0, 0.0, 0.0);
    }

    @Event
    public void onTickPre(TickEvent.Pre event) {
        this.setTimer = true;
        Timer.set(this.timer.get().floatValue());
    }

    private void tick() {
        this.updateScale();

        for (int i = 0; i < this.maxMovements.get(); i++) {
            if (this.move()) {
                this.disable("done");
                return;
            }

            List<BlockPos> list = new ArrayList<>();
            this.find(list);
            if (!list.isEmpty()) {
                this.updatePos();
                this.mine(list);
                return;
            }
        }

        this.updatePos();
    }

    private void mine(List<BlockPos> list) {
        list.forEach(this::clickBlock);
    }

    private boolean move() {
        if (this.tickX()) {
            this.directionX = !this.directionX;
            this.x = this.directionX ? this.sizeX : 0;
            if (this.tickZ()) {
                this.directionZ = !this.directionZ;
                this.z = this.directionZ ? this.sizeZ : 0;
                return this.y++ >= this.sizeY;
            }
        }

        return false;
    }

    private boolean tickX() {
        return this.directionX ? --this.x < 0 : ++this.x > this.sizeX;
    }

    private boolean tickZ() {
        return this.directionZ ? --this.z < 0 : ++this.z > this.sizeZ;
    }

    private void updatePos() {
        Vec3 pos = this.getPos();
        BlackOut.mc.player.setPos(pos);
        this.sendPacket(new ServerboundMovePlayerPacket.Pos(pos.x, pos.y, pos.z, BlackOut.mc.player.onGround(), BlackOut.mc.player.horizontalCollision));
    }

    private Vec3 getPos() {
        return new Vec3(
                Mth.lerpInt((float) this.x / this.sizeX, this.minX.get(), this.maxX.get()),
                Mth.lerpInt((float) this.y / this.sizeY, this.minY.get(), this.maxY.get()),
                Mth.lerpInt((float) this.z / this.sizeZ, this.minZ.get(), this.maxZ.get())
        );
    }

    private void updateScale() {
        this.sizeX = (int) Math.ceil((this.maxX.get() - this.minX.get()) / this.movement.get().floatValue());
        this.sizeY = (int) Math.ceil((this.maxY.get() - this.minY.get()) / this.movement.get().floatValue());
        this.sizeZ = (int) Math.ceil((this.maxZ.get() - this.minZ.get()) / this.movement.get().floatValue());
    }

    private void find(List<BlockPos> list) {
        Vec3 eyePos = this.getPos().add(0.0, BlackOut.mc.player.getEyeHeight(BlackOut.mc.player.getPose()), 0.0);
        BlockPos center = BlockPos.containing(eyePos);
        int r = (int) Math.ceil(this.range.get());

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (!(eyePos.distanceToSqr(pos.getCenter()) > this.range.get() * this.range.get())
                            && !(BlackOut.mc.level.getBlockState(pos).getBlock() instanceof AirBlock)) {
                        list.add(pos);
                    }
                }
            }
        }
    }

    private void clickBlock(BlockPos pos) {
        this.sendSequenced(sequence -> new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, Direction.DOWN, sequence));
        BlackOut.mc.level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }
}
