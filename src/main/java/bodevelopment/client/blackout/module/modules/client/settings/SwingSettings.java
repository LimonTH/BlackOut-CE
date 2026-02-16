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
    public final Setting<Boolean> interact = this.sgInteract.b("Interact Swing", true, "Swings your hand when you interact with a block.");
    public final Setting<SwingState> interactState = this.sgInteract
            .e("Interact State", SwingState.Post, "Should we swing our hand before or after the action.", this.interact::get);
    private final SettingGroup sgBlockPlace = this.addGroup("Block Place");
    public final Setting<Boolean> blockPlace = this.sgBlockPlace.b("Block Place Swing", true, "Swings your hand when placing a block.");
    public final Setting<SwingState> blockPlaceState = this.sgBlockPlace
            .e("Block Place State", SwingState.Post, "Should we swing our hand before or after the action.", this.blockPlace::get);
    private final SettingGroup sgAttack = this.addGroup("Attack");
    public final Setting<Boolean> attack = this.sgAttack.b("Attack Swing", true, "Swings your hand when you attack any entity.");
    public final Setting<SwingState> attackState = this.sgAttack
            .e("Attack State", SwingState.Post, "Should we swing our hand before or after the action.", this.attack::get);
    private final SettingGroup sgUse = this.addGroup("Use");
    public final Setting<Boolean> use = this.sgUse.b("Use Swing", false, "Swings your hand when using an item. NCP doesn't check this.");
    public final Setting<SwingState> useState = this.sgUse
            .e("Use State", SwingState.Post, "Should we swing our hand before or after the action.", this.use::get);
    private final SettingGroup sgMining = this.addGroup("Mining");
    public final Setting<MiningSwingState> mining = this.sgMining
            .e("Block Place State", MiningSwingState.Double, "Swings your hand when you place a crystal.");

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
