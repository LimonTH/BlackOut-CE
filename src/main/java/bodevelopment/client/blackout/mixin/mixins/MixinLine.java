package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.ILine;
import net.minecraft.client.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GuiMessage.Line.class)
public class MixinLine implements ILine {
    @Unique
    private int id;
    @Unique
    private GuiMessage line;

    @Override
    public void blackout_Client$set(int id) {
        this.id = id;
    }

    @Override
    public boolean blackout_Client$idEquals(int id) {
        return this.id == id;
    }

    @Override
    public boolean blackout_Client$messageEquals(GuiMessage other) {
        if (this.line == null) {
            return false;
        }
        return this.line.equals(other);
    }

    @Override
    public void blackout_Client$setLine(GuiMessage line) {
        this.line = line;
    }
}
