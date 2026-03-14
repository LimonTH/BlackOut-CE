package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.manager.Managers;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.commands.SharedSuggestionProvider;

@Mixin(CommandSuggestions.class)
public abstract class MixinCommandSuggestions {
    @Shadow
    @Final
    EditBox input;
    @Shadow
    @Final
    Minecraft minecraft;
    @Shadow
    boolean keepSuggestions;
    @Shadow
    private CompletableFuture<Suggestions> pendingSuggestions;
    @Shadow
    private CommandSuggestions.SuggestionsList suggestions;

    @Shadow
    private ParseResults<SharedSuggestionProvider> currentParse;

    @Shadow
    public abstract void showSuggestions(boolean narrateFirstSuggestion);

    @Inject(method = "updateCommandInfo", at = @At("HEAD"), cancellable = true)
    private void onRefresh(CallbackInfo ci) {
        String text = this.input.getValue();
        String prefix = "-";

        if (text.startsWith(prefix)) {
            this.keepSuggestions = true;

            SuggestionsBuilder builder = new SuggestionsBuilder(text, 1);
            this.pendingSuggestions = Managers.COMMANDS.getCommandSuggestions(builder);

            this.pendingSuggestions.thenRun(() -> this.minecraft.execute(() -> {
                if (this.pendingSuggestions.isDone()) {
                    Suggestions suggestions = this.pendingSuggestions.join();

                    if (suggestions.isEmpty()) {
                        this.suggestions = null;
                        this.input.setSuggestion(null);
                        this.currentParse = null;
                    } else {
                        this.showSuggestions(false);
                    }
                }
            }));

            ci.cancel();
        }
    }
}