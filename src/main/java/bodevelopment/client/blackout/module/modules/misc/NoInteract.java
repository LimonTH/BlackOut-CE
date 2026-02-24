package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.interfaces.functional.SingleOut;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.List;

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

    public ActionResult handleBlock(Hand hand, BlockPos pos, SingleOut<ActionResult> action) {
        Item item = BlackOut.mc.player.getStackInHand(hand).getItem();
        if (this.filterMode.get().shouldAccept(item, this.whenHolding)) {
            return action.get();
        } else {
            Block block = BlackOut.mc.world.getBlockState(pos).getBlock();
            if (this.blockFilterMode.get().shouldAccept(block, this.blocks)) {
                return action.get();
            } else {
                switch (this.ignoreMode.get()) {
                    case Sneak: {
                        this.sendPacket(new ClientCommandC2SPacket(BlackOut.mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
                        ActionResult actionResult = action.get();
                        this.sendPacket(new ClientCommandC2SPacket(BlackOut.mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
                        return actionResult;
                    }
                    case SneakBlocks: {
                        if (!(item instanceof BlockItem)) {
                            return ActionResult.PASS;
                        }

                        this.sendPacket(new ClientCommandC2SPacket(BlackOut.mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
                        ActionResult actionResult = action.get();
                        this.sendPacket(new ClientCommandC2SPacket(BlackOut.mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
                        return actionResult;
                    }
                    default:
                        return ActionResult.PASS;
                }
            }
        }
    }

    public ActionResult handleEntity(Hand hand, Entity entity, SingleOut<ActionResult> action) {
        Item item = BlackOut.mc.player.getStackInHand(hand).getItem();
        if (this.filterModeEntity.get().shouldAccept(item, this.whenHoldingEntity)) {
            return action.get();
        } else {
            return this.entityFilterMode.get().shouldAccept(entity.getType(), this.entities) ? action.get() : ActionResult.PASS;
        }
    }

    public ActionResult handleUse(Hand hand, SingleOut<ActionResult> action) {
        return this.itemFilterMode.get().shouldAccept(BlackOut.mc.player.getStackInHand(hand).getItem(), this.items) ? action.get() : ActionResult.PASS;
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
