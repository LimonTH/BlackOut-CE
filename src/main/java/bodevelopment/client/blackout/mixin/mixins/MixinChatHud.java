package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.IChatHud;
import bodevelopment.client.blackout.interfaces.mixin.IChatHudLine;
import bodevelopment.client.blackout.interfaces.mixin.IVisible;
import bodevelopment.client.blackout.module.modules.misc.AntiSpam;
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
import net.minecraft.ChatFormatting;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;

@Mixin(ChatComponent.class)
public abstract class MixinChatHud implements IChatHud {
    @Shadow @Final private List<GuiMessage> allMessages;
    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;
    @Shadow public abstract void addMessage(Component text);

    @Unique private int addedId = -1;
    @Unique private int lastSpamCount = 1;
    @Unique private Component originalContent = null;

    @Override
    public void blackout_Client$addMessageToChat(Component text, int id) {
        if (id != -1) {
            this.allMessages.removeIf(line -> ((IChatHudLine) (Object) line).blackout_Client$idEquals(id));
            this.trimmedMessages.removeIf(visible -> ((IVisible) (Object) visible).blackout_Client$idEquals(id));
        }
        this.addedId = id;
        this.addMessage(text);
        this.addedId = -1;
    }

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Component modifyChatMessage(Component message) {
        AntiSpam antiSpam = AntiSpam.getInstance();
        this.lastSpamCount = 1;
        this.originalContent = message;

        if (!antiSpam.enabled || this.addedId != -1) return message;

        MutableInt highest = new MutableInt(0);
        boolean found = false;

        for (int i = 0; i < this.allMessages.size(); i++) {
            GuiMessage line = this.allMessages.get(i);
            IChatHudLine customLine = (IChatHudLine) (Object) line;

            if (antiSpam.isSimilar(customLine.blackout_Client$getMessage().getString(), message.getString())) {
                highest.setValue(Math.max(customLine.blackout_Client$getSpam(), highest.getValue()));
                found = true;

                this.allMessages.remove(i);
                this.trimmedMessages.removeIf(visible -> ((IVisible) (Object) visible).blackout_Client$messageEquals(line));
                i--;
            }
        }

        if (found) {
            this.lastSpamCount = highest.getValue() + 1;
            return message.copy().append(Component.literal(" (" + this.lastSpamCount + ")").withStyle(ChatFormatting.AQUA));
        }
        return message;
    }

    @Inject(method = "addMessageToQueue(Lnet/minecraft/client/GuiMessage;)V", at = @At("TAIL"))
    private void onAddMessageTail(GuiMessage line, CallbackInfo ci) {
        IChatHudLine chatLine = (IChatHudLine) (Object) line;

        chatLine.blackout_Client$setMessage(this.originalContent != null ? this.originalContent : line.content());
        chatLine.blackout_Client$setId(this.addedId);
        chatLine.blackout_Client$setSpam(this.lastSpamCount);
    }

    @Inject(method = "addMessageToDisplayQueue(Lnet/minecraft/client/GuiMessage;)V", at = @At("TAIL"))
    private void onAddVisibleTail(GuiMessage line, CallbackInfo ci) {
        if (line == null) return;

        if (!this.trimmedMessages.isEmpty()) {
            GuiMessage.Line lastVisible = this.trimmedMessages.getFirst();
            IVisible customVisible = (IVisible) (Object) lastVisible;
            customVisible.blackout_Client$set(this.addedId);
            customVisible.blackout_Client$setLine(line);
        }
    }
}
