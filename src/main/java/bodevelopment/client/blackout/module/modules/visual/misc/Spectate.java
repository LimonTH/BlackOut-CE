package bodevelopment.client.blackout.module.modules.visual.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.KeyEvent;
import bodevelopment.client.blackout.event.events.MouseButtonEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.keys.KeyBind;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Spectate extends Module {
    private static Spectate INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<Boolean> ignoreFriends = this.sgGeneral.booleanSetting("Ignore Friends", true, "Doesn't spectate friends.");
    private final Setting<KeyBind> forwardKey = this.sgGeneral.keySetting("Forward", ".");
    private final Setting<KeyBind> backKey = this.sgGeneral.keySetting("Back", ".");
    private final List<PlayerEntity> playerEntities = new ArrayList<>();
    private final MatrixStack stack = new MatrixStack();
    private PlayerEntity target;
    private int prevI = 0;

    public Spectate() {
        super("Spectate", ".", SubCategory.MISC_VISUAL, true);
        INSTANCE = this;
    }

    public static Spectate getInstance() {
        return INSTANCE;
    }

    @Event
    public void onRender(RenderEvent.Hud.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            this.stack.push();
            RenderUtils.unGuiScale(this.stack);
            if (this.target instanceof AbstractClientPlayerEntity) {
                BlackOut.FONT
                        .text(
                                this.stack,
                                "Spectating " + this.target.getName().getString(),
                                2.0F,
                                BlackOut.mc.getWindow().getWidth() / 2.0F,
                                BlackOut.mc.getWindow().getHeight() / 2.0F + BlackOut.FONT.getHeight() * 3.0F,
                                Color.WHITE,
                                true,
                                true
                        );
            }

            this.stack.pop();
        }
    }

    @Event
    public void onKey(KeyEvent event) {
        if (event.pressed) {
            if (this.forwardKey.get().isKey(event.key)) {
                this.set(this.move(true));
            }

            if (this.backKey.get().isKey(event.key)) {
                this.set(this.move(false));
            }
        }
    }

    @Event
    public void onMouse(MouseButtonEvent event) {
        if (event.pressed) {
            if (this.forwardKey.get().isMouse(event.button)) {
                this.set(this.move(true));
            }

            if (this.backKey.get().isMouse(event.button)) {
                this.set(this.move(false));
            }
        }
    }

    public Entity getEntity() {
        this.updateList();
        if (!this.playerEntities.contains(this.target)) {
            this.set(this.move(false));
        } else {
            this.prevI = this.playerEntities.indexOf(this.target);
        }

        return this.target;
    }

    private void set(int i) {
        if (this.playerEntities.isEmpty()) {
            this.prevI = 0;
            this.target = BlackOut.mc.player;
        } else {
            this.prevI = i;
            this.target = this.playerEntities.get(i);
        }
    }

    private int move(boolean increase) {
        int max = this.playerEntities.size() - 1;
        if (increase) {
            return this.prevI == max ? 0 : this.prevI + 1;
        } else {
            return this.prevI == 0 ? max : this.prevI - 1;
        }
    }

    private void updateList() {
        this.playerEntities.clear();

        for (PlayerEntity player : BlackOut.mc.world.getPlayers()) {
            if (player != BlackOut.mc.player && (!this.ignoreFriends.get() || !Managers.FRIENDS.isFriend(player))) {
                this.playerEntities.add(player);
            }
        }
    }
}
