/*
 * Blackout Client (CE) - A cutting-edge, feature-rich cheat client for Minecraft.
 * A modernized continuation of the original Blackout project by OLEPOSSU & KassuK.
 * Copyright (C) 2026  LimonTH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://gnu.org>.
 */

package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.annotations.Internal;
import bodevelopment.client.blackout.manager.Managers;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.commands.SharedSuggestionProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
@Internal
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