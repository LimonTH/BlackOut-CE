package bodevelopment.client.blackout.module.modules.combat.defensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.ObsidianModule;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.combat.misc.AutoTrap;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.SettingUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;

public class SelfTrap extends ObsidianModule {
    private final SettingGroup sgToggle = this.addGroup("Toggle");

    private final Setting<AutoTrap.TrapMode> trapMode = this.sgGeneral.enumSetting("Trap Mode", AutoTrap.TrapMode.Both,
            "Which parts of the trap to prioritize. 'Full' covers both the top and the sides at eye level.");

    private final Setting<Boolean> toggleMove = this.sgToggle.booleanSetting("Toggle Move", false,
            "Automatically disables the module if you move to a different horizontal block (X or Z).");
    private final Setting<Surround.VerticalToggleMode> toggleVertical = this.sgToggle.enumSetting("Toggle Vertical", Surround.VerticalToggleMode.Up,
            "Automatically disables the module if you move vertically (jumping, falling, or both).");

    private final Direction[] directions = new Direction[]{
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP};
    private BlockPos prevPos = BlockPos.ZERO;

    public SelfTrap() {
        super("Self Trap", "Builds an obsidian 'cocoon' around your upper body to block overhead crystal damage.", SubCategory.DEFENSIVE);
    }

    @Override
    public void onTick(TickEvent.Pre event) {
        super.onTick(event);
        BlockPos currentPos = this.getPos();
        this.checkToggle(currentPos);
        this.prevPos = currentPos;
    }

    private void checkToggle(BlockPos currentPos) {
        if (this.prevPos != null) {
            if (this.toggleMove.get() && (currentPos.getX() != this.prevPos.getX() || currentPos.getZ() != this.prevPos.getZ())) {
                this.disable("moved horizontally");
            }

            if ((this.toggleVertical.get() == Surround.VerticalToggleMode.Up || this.toggleVertical.get() == Surround.VerticalToggleMode.Any)
                    && currentPos.getY() > this.prevPos.getY()) {
                this.disable("moved up");
            }

            if ((this.toggleVertical.get() == Surround.VerticalToggleMode.Down || this.toggleVertical.get() == Surround.VerticalToggleMode.Any)
                    && currentPos.getY() < this.prevPos.getY()) {
                this.disable("moved down");
            }
        }
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
        this.addBlocks(BlackOut.mc.player, this.getSize(BlackOut.mc.player));
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
}
