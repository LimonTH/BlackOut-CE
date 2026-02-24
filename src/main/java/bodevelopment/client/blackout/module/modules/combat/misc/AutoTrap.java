package bodevelopment.client.blackout.module.modules.combat.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.ObsidianModule;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.util.SettingUtils;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Comparator;

public class AutoTrap extends ObsidianModule {
    private final Setting<TrapMode> trapMode = this.sgGeneral.enumSetting("Trap Mode", TrapMode.Both, "");
    private final Direction[] directions = new Direction[]{
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP
    };

    public AutoTrap() {
        super("Auto Trap", "Covers enemies in blocks.", SubCategory.MISC_COMBAT);
    }

    @Override
    protected void addPlacements() {
        this.insideBlocks
                .forEach(
                        pos -> {
                            for (Direction dir : this.directions) {
                                if (this.trapMode.get().allowed(dir)
                                        && !this.blockPlacements.contains(pos.offset(dir))
                                        && !this.insideBlocks.contains(pos.offset(dir))) {
                                    this.blockPlacements.add(pos.offset(dir));
                                }
                            }
                        }
                );
    }

    @Override
    protected void addInsideBlocks() {
        BlackOut.mc
                .world
                .getPlayers()
                .stream()
                .filter(player -> BlackOut.mc.player.distanceTo(player) < 15.0F && player != BlackOut.mc.player && !Managers.FRIENDS.isFriend(player))
                .sorted(Comparator.comparingDouble(player -> BlackOut.mc.player.distanceTo(player)))
                .forEach(player -> this.addBlocks(player, this.getSize(player)));
    }

    @Override
    protected void addBlocks(Entity entity, int[] size) {
        int eyeY = (int) Math.ceil(entity.getBoundingBox().maxY);

        for (int x = size[0]; x <= size[1]; x++) {
            for (int z = size[2]; z <= size[3]; z++) {
                BlockPos p = entity.getBlockPos().add(x, 0, z).withY(eyeY - 1);
                if (!(BlackOut.mc.world.getBlockState(p).getBlock().getBlastResistance() > 600.0F) && SettingUtils.inPlaceRange(p)) {
                    this.insideBlocks.add(p);
                }
            }
        }
    }

    public enum TrapMode {
        Top,
        Eyes,
        Both;

        public boolean allowed(Direction dir) {
            return switch (this) {
                case Top -> dir == Direction.UP;
                case Eyes -> dir.getOffsetY() == 0;
                case Both -> true;
            };
        }
    }
}
