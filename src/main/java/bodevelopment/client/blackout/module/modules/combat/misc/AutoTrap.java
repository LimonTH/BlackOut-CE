package bodevelopment.client.blackout.module.modules.combat.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.ObsidianModule;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.util.SettingUtils;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;

public class AutoTrap extends ObsidianModule {
    private final Setting<TrapMode> trapMode = this.sgGeneral.enumSetting("Trap Mode", TrapMode.Both,
            "Determines which parts of the enemy to cover. 'Top' places a block above their head, 'Eyes' covers the sides.");

    private final Direction[] directions = new Direction[]{
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP
    };

    public AutoTrap() {
        super("Auto Trap", "Encases nearby enemies in obsidian to restrict their movement and prevent them from escaping holes.", SubCategory.MISC_COMBAT);
    }

    @Override
    protected void addPlacements() {
        this.insideBlocks
                .forEach(
                        pos -> {
                            for (Direction dir : this.directions) {
                                BlockPos target = pos.relative(dir);
                                if (this.trapMode.get().allowed(dir)
                                        && !BlackOut.mc.level.isOutsideBuildHeight(target)
                                        && !this.blockPlacements.contains(target)
                                        && !this.insideBlocks.contains(target)) {
                                    this.blockPlacements.add(target);
                                }
                            }
                        }
                );
    }

    @Override
    protected void addInsideBlocks() {
        BlackOut.mc
                .level
                .players()
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
                BlockPos p = entity.blockPosition().offset(x, 0, z).atY(eyeY - 1);
                if (!BlackOut.mc.level.isOutsideBuildHeight(p)
                        && !(BlackOut.mc.level.getBlockState(p).getBlock().getExplosionResistance() > 600.0F)
                        && SettingUtils.inPlaceRange(p)) {
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
                case Eyes -> dir.getStepY() == 0;
                case Both -> true;
            };
        }
    }
}
