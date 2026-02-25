package bodevelopment.client.blackout.module.modules.visual.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

import java.util.List;

public class XRay extends Module {
    private static XRay INSTANCE;

    // TODO: opacity необходимо реализовать калибровку прозрачности xray
    private final SettingGroup sgGeneral = this.addGroup("General");

    // public final Setting<Integer> opacity = this.sgGeneral.intSetting("Ambient Opacity", 150, 0, 255, 1, "The alpha transparency level applied to non-target blocks during world rendering.");
    public final Setting<List<Block>> targetBlocks = this.sgGeneral.blockListSetting("Blocks Filter Registry", "A definitive list of block types that will remain opaque and visible through terrain.",
            Blocks.ANCIENT_DEBRIS,
            Blocks.DIAMOND_ORE,
            Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.GOLD_ORE,
            Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.EMERALD_ORE,
            Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.IRON_ORE,
            Blocks.DEEPSLATE_IRON_ORE,
            Blocks.COPPER_ORE,
            Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.COAL_ORE,
            Blocks.DEEPSLATE_COAL_ORE,
            Blocks.REDSTONE_ORE,
            Blocks.DEEPSLATE_REDSTONE_ORE,
            Blocks.LAPIS_ORE,
            Blocks.DEEPSLATE_LAPIS_ORE
    ).onChanged(v -> {
        if (this.enabled && BlackOut.mc.worldRenderer != null) {
            BlackOut.mc.worldRenderer.reload();
        }
    });

    public XRay() {
        super("Xray", "Modifies world rendering to isolate selected blocks by applying selective transparency to common terrain blocks.", SubCategory.MISC_VISUAL, true);
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        if (BlackOut.mc.worldRenderer != null) {
            BlackOut.mc.worldRenderer.reload();
        }
    }

    @Override
    public void onDisable() {
        if (BlackOut.mc.worldRenderer != null) {
            BlackOut.mc.worldRenderer.reload();
        }
    }

    public static XRay getInstance() {
        return INSTANCE;
    }

    public boolean isTarget(Block block) {
        return targetBlocks.get().contains(block);
    }
}