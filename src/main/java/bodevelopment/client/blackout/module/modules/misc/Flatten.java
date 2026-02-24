package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.ObsidianModule;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.randomstuff.PlaceData;
import bodevelopment.client.blackout.util.SettingUtils;
import net.minecraft.util.math.BlockPos;

public class Flatten extends ObsidianModule {
    private final Setting<Boolean> setY = this.sgGeneral.booleanSetting("Fixed Altitude", true, "Whether to use a specific Y-level or use the block level beneath your feet.");
    private final Setting<Integer> y = this.sgGeneral.intSetting("Altitude Level", 3, -64, 300, 1, "The target Y-coordinate for the platform.", this.setY::get);

    private int height = 0;

    public Flatten() {
        super("Flatten", "Constructs a horizontal obsidian platform at a designated altitude to provide solid footing.", SubCategory.MISC);
    }

    @Override
    public void onTick(TickEvent.Pre event) {
        super.onTick(event);
        if (BlackOut.mc.player != null && BlackOut.mc.player.isOnGround()) {
            this.height = (int) (Math.round(BlackOut.mc.player.getY()) - 1L);
        }
    }

    @Override
    protected void addPlacements() {
        this.insideBlocks.forEach(pos -> {
            if (!this.blockPlacements.contains(pos)) {
                PlaceData data = SettingUtils.getPlaceData(pos);
                if (!data.valid()) {
                    BlockPos support = this.findSupport(pos);
                    if (support == null) {
                        return;
                    }
                } else if (!SettingUtils.inPlaceRange(data.pos())) {
                    return;
                }

                this.blockPlacements.add(pos);
            }
        });
    }

    @Override
    protected void addInsideBlocks() {
        BlockPos center = BlackOut.mc.player.getBlockPos().withY(this.setY.get() ? this.y.get() : this.height);

        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                this.insideBlocks.add(center.add(x, 0, z));
            }
        }
    }
}
