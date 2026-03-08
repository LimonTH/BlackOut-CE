package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.IChatHud;
import bodevelopment.client.blackout.interfaces.mixin.IChatHudLine;
import bodevelopment.client.blackout.interfaces.mixin.IVisible;
import bodevelopment.client.blackout.module.modules.misc.AntiSpam;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.mutable.MutableInt;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatHud.class)
public abstract class MixinChatHud implements IChatHud {
    @Shadow @Final private List<ChatHudLine> messages;
    @Shadow @Final private List<ChatHudLine.Visible> visibleMessages;
    @Shadow public abstract void addMessage(Text text);

    @Unique private int addedId = -1;
    @Unique private int lastSpamCount = 1;
    @Unique private Text originalContent = null;

    @Override
    public void blackout_Client$addMessageToChat(Text text, int id) {
        if (id != -1) {
            this.messages.removeIf(line -> ((IChatHudLine) (Object) line).blackout_Client$idEquals(id));
            this.visibleMessages.removeIf(visible -> ((IVisible) (Object) visible).blackout_Client$idEquals(id));
        }
        this.addedId = id;
        this.addMessage(text);
        this.addedId = -1;
    }

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Text modifyChatMessage(Text message) {
        AntiSpam antiSpam = AntiSpam.getInstance();
        this.lastSpamCount = 1;
        this.originalContent = message;

        if (!antiSpam.enabled) return message;

        MutableInt highest = new MutableInt(0);
        boolean found = false;

        for (int i = 0; i < this.messages.size(); i++) {
            ChatHudLine line = this.messages.get(i);
            IChatHudLine customLine = (IChatHudLine) (Object) line;

            if (antiSpam.isSimilar(customLine.blackout_Client$getMessage().getString(), message.getString())) {
                highest.setValue(Math.max(customLine.blackout_Client$getSpam(), highest.getValue()));
                found = true;

                this.messages.remove(i);
                this.visibleMessages.removeIf(visible -> ((IVisible) (Object) visible).blackout_Client$messageEquals(line));
                i--;
            }
        }

        if (found) {
            this.lastSpamCount = highest.getValue() + 1;
            return message.copy().append(Text.literal(" (" + this.lastSpamCount + ")").formatted(Formatting.AQUA));
        }
        return message;
    }

    @Inject(method = "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V", at = @At("TAIL"))
    private void onAddMessageTail(ChatHudLine line, CallbackInfo ci) {
        IChatHudLine chatLine = (IChatHudLine) (Object) line;

        chatLine.blackout_Client$setMessage(this.originalContent != null ? this.originalContent : line.content());
        chatLine.blackout_Client$setId(this.addedId);
        chatLine.blackout_Client$setSpam(this.lastSpamCount);
    }

    @Inject(method = "addVisibleMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V", at = @At("TAIL"))
    private void onAddVisibleTail(ChatHudLine line, CallbackInfo ci) {
        if (line == null) return;

        for (ChatHudLine.Visible visible : this.visibleMessages) {
            IVisible customVisible = (IVisible) (Object) visible;
            try {
                if (customVisible.blackout_Client$messageEquals(line)) {
                    customVisible.blackout_Client$set(this.addedId);
                    customVisible.blackout_Client$setLine(line);
                }
            } catch (NullPointerException ignored) {
            }
        }
    }
}
