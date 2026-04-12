package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.SwingHand;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MouseButtonEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.Notifications;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.util.ChatUtils;
import bodevelopment.client.blackout.util.InvUtils;
import bodevelopment.client.blackout.util.PlayerUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class MiddleClick extends Module {
    public final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<SwitchMode> mode = this.sgGeneral.enumSetting("Swap Method", SwitchMode.Normal, "The mechanism used to switch to an Ender Pearl.");
    public final Setting<Throwable> actionType = this.sgGeneral.enumSetting("Action Type", Throwable.Smart, "What item to throw.");
    private final Setting<Boolean> swing = this.sgGeneral.booleanSetting("Swing Animation", false, "Plays a hand swing animation upon throwing the pearl.");
    private final Setting<SwingHand> swingHand = this.sgGeneral.enumSetting("Swing Hand", SwingHand.RealHand, "Determines which hand performs the swing animation.");
    private final Setting<Boolean> middleClickFriend = this.sgGeneral.booleanSetting("Middle Click Friend", true, "Adds or removes players from your friends list when middle-clicking them.");

    public MiddleClick() {
        super("MCP", "Middle Click for extra usage; automatically throws an item upon clicking the middle mouse button.", SubCategory.MISC, true);
    }

    @Event
    public void mouseClick(MouseButtonEvent event) {
        if (!PlayerUtils.isInGame() || BlackOut.mc.screen != null) return;
        if (event.button == 2 && event.pressed) {

            if (this.middleClickFriend.get() && BlackOut.mc.hitResult != null && BlackOut.mc.hitResult.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityHit = (EntityHitResult) BlackOut.mc.hitResult;

                if (entityHit.getEntity() instanceof Player targetPlayer) {
                    if (BlackOut.mc.player.distanceTo(targetPlayer) <= 3.0F) {
                        String name = targetPlayer.getGameProfile().getName();

                        String respond = Managers.FRIENDS.isFriend(targetPlayer)
                                ? Managers.FRIENDS.remove(name)
                                : Managers.FRIENDS.add(name, targetPlayer.getUUID());

                        String fullMessage = String.format("%s [friends]%s %s",
                                Notifications.getInstance().getClientPrefix(),
                                ChatFormatting.GRAY,
                                respond
                        );

                        ChatUtils.addMessage(fullMessage);
                        return;
                    }
                }
            }
            Throwable action = this.actionType.get();
            if (action == Throwable.Firework || (action == Throwable.Smart && BlackOut.mc.player.isFallFlying())) {
                this.useTargetItem(Items.FIREWORK_ROCKET);
            }
            else if (action == Throwable.Pearl || action == Throwable.Smart) {
                this.useTargetItem(Items.ENDER_PEARL);
            }
        }
    }

    private void useTargetItem(Item item) {
        InteractionHand hand = InvUtils.getHand(item);
        FindResult result = this.mode.get().find(item);

        if (result.wasFound() || hand != null) {
            if (hand != null || this.mode.get().swap(result.slot())) {
                this.useItem(hand);

                if (this.swing.get()) {
                    this.clientSwing(this.swingHand.get(), hand);
                }

                if (hand == null) {
                    this.mode.get().swapBack();
                }
            }
        }
    }

    public enum Throwable {
        Pearl,
        Firework,
        Smart
    }
}