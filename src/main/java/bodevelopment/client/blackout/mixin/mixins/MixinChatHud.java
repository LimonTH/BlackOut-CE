package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.IChatHud;
import bodevelopment.client.blackout.interfaces.mixin.IChatHudLine;
import bodevelopment.client.blackout.interfaces.mixin.IVisible;
import bodevelopment.client.blackout.module.modules.misc.AntiSpam;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.util.ChatMessages;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.mutable.MutableInt;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(ChatHud.class)
public abstract class MixinChatHud implements IChatHud {
    @Shadow
    @Final
    private List<ChatHudLine> messages;
    @Shadow
    @Final
    private List<ChatHudLine.Visible> visibleMessages;
    @Unique
    private int addedId = -1;
    @Unique
    private ChatHudLine currentLine = null;

    @Shadow
    public abstract void addMessage(Text text);

    @Shadow
    @Final
    private MinecraftClient client;

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

    @Inject(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD")
    )
    private void onAddMessage(Text message, MessageSignatureData signature, MessageIndicator indicator, CallbackInfo ci) {
        AntiSpam antiSpam = AntiSpam.getInstance();
        MutableInt highest = new MutableInt(0);

        int ticks = this.client.inGameHud.getTicks();

        this.currentLine = new ChatHudLine(ticks, message, signature, indicator);

        if (antiSpam.enabled) {
            AtomicBoolean found = new AtomicBoolean(false);
            this.messages.removeIf(line -> {
                String oldText = ((IChatHudLine) (Object) line).blackout_Client$getMessage().getString();

                if (antiSpam.isSimilar(oldText, message.getString())) {
                    highest.setValue(Math.max(((IChatHudLine) (Object) line).blackout_Client$getSpam(), highest.getValue()));
                    found.set(true);

                    this.visibleMessages.removeIf(visible ->
                            ((IVisible) (Object) visible).blackout_Client$messageEquals(line)
                    );
                    return true;
                }
                return false;
            });

            if (found.get()) {
                this.currentLine = new ChatHudLine(
                        ticks,
                        message.copy().append(Formatting.AQUA + " (" + (highest.getValue() + 1) + ")"),
                        signature,
                        indicator
                );
            }
        }

        IChatHudLine current = (IChatHudLine) (Object) this.currentLine;
        current.blackout_Client$setSpam(highest.getValue() + 1);
        current.blackout_Client$setMessage(message);
    }

    @Redirect(
            method = "addVisibleMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/ChatMessages;breakRenderedChatMessageLines(Lnet/minecraft/text/StringVisitable;ILnet/minecraft/client/font/TextRenderer;)Ljava/util/List;"
            )
    )
    private List<OrderedText> breakIntoLines(StringVisitable message, int width, TextRenderer textRenderer) {
        StringVisitable content = (this.currentLine != null) ? this.currentLine.content() : message;
        return ChatMessages.breakRenderedChatMessageLines(content, width, textRenderer);
    }

    @Redirect(
            method = "addVisibleMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V")
    )
    private void addVisibleLineToList(List<Object> instance, int index, Object o) {
        if (o instanceof ChatHudLine.Visible line && this.currentLine != null) {
            ((IVisible) (Object) line).blackout_Client$set(this.addedId);
            ((IVisible) (Object) line).blackout_Client$setLine(this.currentLine);
        }
        instance.add(index, o);
    }

    @Redirect(
            method = "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V")
    )
    private void addMessageToHistoryList(List<Object> instance, int index, Object o) {
        if (o instanceof ChatHudLine) {
            ((IChatHudLine) (Object) this.currentLine).blackout_Client$setId(this.addedId);
            instance.add(index, this.currentLine);
        } else {
            instance.add(index, o);
        }
    }
}
