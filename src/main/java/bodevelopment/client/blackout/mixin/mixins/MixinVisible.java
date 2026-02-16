package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.IVisible;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ChatHudLine.Visible.class)
public class MixinVisible implements IVisible {
    @Unique
    private int id;
    @Unique
    private ChatHudLine line;

    @Override
    public void blackout_Client$set(int id) {
        this.id = id;
    }

    @Override
    public boolean blackout_Client$idEquals(int id) {
        return this.id == id;
    }

    @Override
    public boolean blackout_Client$messageEquals(ChatHudLine line) {
        return this.line.equals(line);
    }

    @Override
    public void blackout_Client$setLine(ChatHudLine line) {
        this.line = line;
    }
}
