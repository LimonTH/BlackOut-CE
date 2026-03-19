package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.interfaces.functional.SingleOut;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import java.util.List;
import net.minecraft.core.BlockPos;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public class NoInteract extends Module {
    private static NoInteract INSTANCE;

    private final SettingGroup sgBlocks = this.addGroup("Blocks");
    private final SettingGroup sgItems = this.addGroup("Items");
    private final SettingGroup sgEntity = this.addGroup("Entity");

    private final Setting<NoInteractFilterMode> filterMode = this.sgBlocks.enumSetting("Holding Filter Mode (Block)", NoInteractFilterMode.Cancel, "Determines if the 'When Holding' list acts as a whitelist or a blacklist for block interactions.");
    private final Setting<List<Item>> whenHolding = this.sgBlocks.itemListSetting("When Holding (Block)", "Prevents block interaction only when holding specific items (e.g., Gaps) to avoid opening chests accidentally.", Items.ENCHANTED_GOLDEN_APPLE, Items.GOLDEN_APPLE);
    private final Setting<NoInteractFilterMode> blockFilterMode = this.sgBlocks.enumSetting("Block Filter Mode", NoInteractFilterMode.Cancel, "Determines if the 'Blocks' list acts as a whitelist or a blacklist.");
    private final Setting<List<Block>> blocks = this.sgBlocks.blockListSetting("Blocks", "The specific blocks to block or allow interaction with (e.g., Chests, Crafting Tables).");
    private final Setting<IgnoreMode> ignoreMode = this.sgBlocks.enumSetting("Ignore Mode", IgnoreMode.SneakBlocks, "The method used to bypass interaction, such as spoofing a sneak packet.");

    private final Setting<NoInteractFilterMode> itemFilterMode = this.sgItems.enumSetting("Item Filter Mode", NoInteractFilterMode.Cancel, "Determines if the 'Items' list acts as a whitelist or a blacklist for item usage.");
    private final Setting<List<Item>> items = this.sgItems.itemListSetting("Items", "The specific items to prevent using.");

    private final Setting<NoInteractFilterMode> filterModeEntity = this.sgEntity.enumSetting("Holding Filter Mode (Entity)", NoInteractFilterMode.Cancel, "Determines if the 'When Holding' list acts as a whitelist or a blacklist for entity interactions.");
    private final Setting<List<Item>> whenHoldingEntity = this.sgEntity.itemListSetting("When Holding (Entity)", "Prevents entity interaction (like mounting or trading) only when holding these items.", Items.ENCHANTED_GOLDEN_APPLE, Items.GOLDEN_APPLE);
    private final Setting<NoInteractFilterMode> entityFilterMode = this.sgEntity.enumSetting("Entity Filter Mode", NoInteractFilterMode.Accept, "Determines if the 'Entities' list acts as a whitelist or a blacklist.");
    private final Setting<List<EntityType<?>>> entities = this.sgEntity.entityListSetting("Entities", "The specific entity types to block or allow interaction with.");
    public NoInteract() {
        super("No Interact", "Prevents accidental interactions with containers, entities, or items while performing other actions.", SubCategory.MISC, false);
        INSTANCE = this;
    }

    public static NoInteract getInstance() {
        return INSTANCE;
    }

    public InteractionResult handleBlock(InteractionHand hand, BlockPos pos, SingleOut<InteractionResult> action) {
        Item item = BlackOut.mc.player.getItemInHand(hand).getItem();
        if (this.filterMode.get().shouldAccept(item, this.whenHolding)) {
            return action.get();
        } else {
            Block block = BlackOut.mc.level.getBlockState(pos).getBlock();
            if (this.blockFilterMode.get().shouldAccept(block, this.blocks)) {
                return action.get();
            } else {
                switch (this.ignoreMode.get()) {
                    case Sneak: {
                        BlackOut.mc.player.setShiftKeyDown(true);
                        InteractionResult actionResult = action.get();
                        BlackOut.mc.player.setShiftKeyDown(false);
                        return actionResult;
                    }
                    case SneakBlocks: {
                        if (!(item instanceof BlockItem)) {
                            return InteractionResult.PASS;
                        }

                        BlackOut.mc.player.setShiftKeyDown(true);
                        InteractionResult actionResult = action.get();
                        BlackOut.mc.player.setShiftKeyDown(false);
                        return actionResult;
                    }
                    default:
                        return InteractionResult.PASS;
                }
            }
        }
    }

    public InteractionResult handleEntity(InteractionHand hand, Entity entity, SingleOut<InteractionResult> action) {
        Item item = BlackOut.mc.player.getItemInHand(hand).getItem();
        if (this.filterModeEntity.get().shouldAccept(item, this.whenHoldingEntity)) {
            return action.get();
        } else {
            return this.entityFilterMode.get().shouldAccept(entity.getType(), this.entities) ? action.get() : InteractionResult.PASS;
        }
    }

    public InteractionResult handleUse(InteractionHand hand, SingleOut<InteractionResult> action) {
        return this.itemFilterMode.get().shouldAccept(BlackOut.mc.player.getItemInHand(hand).getItem(), this.items) ? action.get() : InteractionResult.PASS;
    }

    public enum IgnoreMode {
        Cancel,
        Sneak,
        SneakBlocks
    }

    public enum NoInteractFilterMode {
        Cancel,
        Accept;

        private <T> boolean shouldAccept(T item, Setting<List<T>> list) {
            return (this == Cancel) != list.get().contains(item);
        }
    }
}
