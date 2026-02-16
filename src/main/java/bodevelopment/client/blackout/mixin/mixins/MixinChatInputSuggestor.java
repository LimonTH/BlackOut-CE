package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.manager.Managers;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.command.CommandSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(ChatInputSuggestor.class)
public abstract class MixinChatInputSuggestor {
    @Shadow
    @Final
    TextFieldWidget textField;
    @Shadow
    @Final
    MinecraftClient client;
    @Shadow
    boolean completingSuggestions;
    @Shadow
    private CompletableFuture<Suggestions> pendingSuggestions;
    @Shadow
    private ChatInputSuggestor.SuggestionWindow window;

    @Shadow
    private ParseResults<CommandSource> parse;

    @Shadow
    public abstract void show(boolean narrateFirstSuggestion);

    @Inject(method = "refresh", at = @At("HEAD"), cancellable = true)
    private void onRefresh(CallbackInfo ci) {
        String text = this.textField.getText();
        String prefix = "-";

        if (text.startsWith(prefix)) {
            this.completingSuggestions = true;

            SuggestionsBuilder builder = new SuggestionsBuilder(text, 1);
            this.pendingSuggestions = Managers.COMMANDS.getCommandSuggestions(builder);

            this.pendingSuggestions.thenRun(() -> this.client.execute(() -> {
                if (this.pendingSuggestions.isDone()) {
                    Suggestions suggestions = this.pendingSuggestions.join();

                    if (suggestions.isEmpty()) {
                        this.window = null;
                        this.textField.setSuggestion(null);
                        this.parse = null;
                    } else {
                        this.show(false);
                    }
                }
            }));

            ci.cancel();
        }
    }
}