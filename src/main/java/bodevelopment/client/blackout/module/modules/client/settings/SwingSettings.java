package bodevelopment.client.blackout.module.modules.client.settings;

import bodevelopment.client.blackout.enums.SwingState;
import bodevelopment.client.blackout.enums.SwingType;
import bodevelopment.client.blackout.module.SettingsModule;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;

public class SwingSettings extends SettingsModule {
    private static SwingSettings INSTANCE;

    private final SettingGroup sgInteract = this.addGroup("Interact");
    private final SettingGroup sgBlockPlace = this.addGroup("Block Place");
    private final SettingGroup sgAttack = this.addGroup("Attack");
    private final SettingGroup sgUse = this.addGroup("Use");
    private final SettingGroup sgMining = this.addGroup("Mining");

    public final Setting<Boolean> interact = this.sgInteract.b("Interact Swing", true,
            "Performs a hand swing animation when interacting with blocks (chests, levers, etc.) to synchronize visual actions with server-side events.");
    public final Setting<SwingState> interactState = this.sgInteract.e("Interact State", SwingState.Post,
            "Determines whether the swing packet is sent before (Pre) or after (Post) the interaction packet.", this.interact::get);

    public final Setting<Boolean> blockPlace = this.sgBlockPlace.b("Block Place Swing", true,
            "Triggers the swing animation when placing blocks, making your actions appear legitimate to other players and anti-cheats.");
    public final Setting<SwingState> blockPlaceState = this.sgBlockPlace.e("Block Place State", SwingState.Post,
            "Determines whether the swing packet is sent before or after the block placement packet.", this.blockPlace::get);

    public final Setting<Boolean> attack = this.sgAttack.b("Attack Swing", true,
            "Swings your hand when attacking entities. Disabling this may lead to 'NoSwing' flags on some anti-cheats.");
    public final Setting<SwingState> attackState = this.sgAttack.e("Attack State", SwingState.Post,
            "Determines if the swing animation is processed before or after the attack packet is dispatched.", this.attack::get);

    public final Setting<Boolean> use = this.sgUse.b("Use Swing", false,
            "Forces a hand swing animation when using items like food or bows. Typically not required for NCP bypasses but adds visual realism.");
    public final Setting<SwingState> useState = this.sgUse.e("Use State", SwingState.Post,
            "Controls the timing of the swing animation relative to the item usage packet.", this.use::get);

    public final Setting<MiningSwingState> mining = this.sgMining.e("Mining Swing State", MiningSwingState.Double,
            "Controls the hand swing logic specifically for mining actions. 'Double' can help with certain block-resetting bypasses.");

    public SwingSettings() {
        super("Swing", false, true);
        INSTANCE = this;
    }

    public static SwingSettings getInstance() {
        return INSTANCE;
    }

    public void swing(SwingState state, SwingType type, Hand hand) {
        if (state == this.getState(type)) {
            switch (type) {
                case Interact:
                    this.swing(this.interact.get(), hand);
                    break;
                case Placing:
                    this.swing(this.blockPlace.get(), hand);
                    break;
                case Attacking:
                    this.swing(this.attack.get(), hand);
                    break;
                case Using:
                    this.swing(this.use.get(), hand);
            }
        }
    }

    public void mineSwing(MiningSwingState state) {
        switch (state) {
            case Start:
                if (this.mining.get() != MiningSwingState.Start) {
                    return;
                }
                break;
            case End:
                if (this.mining.get() != MiningSwingState.End) {
                    return;
                }
                break;
            default:
                return;
        }

        this.swing(true, Hand.MAIN_HAND);
    }

    private SwingState getState(SwingType type) {
        return switch (type) {
            case Interact -> this.interactState.get();
            case Placing -> this.blockPlaceState.get();
            case Attacking -> this.attackState.get();
            case Using -> this.useState.get();
            case Mining -> SwingState.Post;
        };
    }

    private void swing(boolean shouldSwing, Hand hand) {
        if (shouldSwing) {
            this.sendPacket(new HandSwingC2SPacket(hand));
        }
    }

    public enum MiningSwingState {
        Disabled,
        Start,
        End,
        Double
    }
}
