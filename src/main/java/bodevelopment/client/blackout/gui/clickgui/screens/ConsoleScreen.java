package bodevelopment.client.blackout.gui.clickgui.screens;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.command.Command;
import bodevelopment.client.blackout.gui.TextField;
import bodevelopment.client.blackout.gui.clickgui.ClickGuiScreen;
import bodevelopment.client.blackout.gui.clickgui.ConsoleLog;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.GuiColorUtils;
import bodevelopment.client.blackout.util.CollectionUtils;
import bodevelopment.client.blackout.util.SelectedComponent;
import bodevelopment.client.blackout.util.render.Render2DUtils;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import org.lwjgl.glfw.GLFW;

public class ConsoleScreen extends ClickGuiScreen {
    private static final int LINE_COLOR = new Color(50, 50, 50, 255).getRGB();
    private static final int SUGGESTION_BG = new Color(30, 30, 30, 230).getRGB();
    private static final int SUGGESTION_HIGHLIGHT = new Color(60, 60, 60, 230).getRGB();
    private static final float CONTENT_TOP = 35.0F;

    private final List<Line> lines = new ArrayList<>();
    private final TextField textField = new TextField();
    private final TextField filterField = new TextField();
    private final int id = SelectedComponent.nextId();
    private final int filterId = SelectedComponent.nextId();
    private boolean typing = false;
    private boolean filterActive = false;
    private String filterText = "";

    private final List<String> commandHistory = new ArrayList<>();
    private int historyIndex = -1;
    private String savedInput = "";

    private final List<String> suggestions = new ArrayList<>();
    private int suggestionIndex = -1;

    public ConsoleScreen() {
        super("Console", 900.0F, 600.0F, true);

        this.lines.add(new Line(this.split("Welcome to BlackOut Console. Type §ahelp§r for commands.", ""), "", Color.GREEN.getRGB()));

        for (int i = ConsoleLog.getEntries().size() - 1; i >= 0; i--) {
            ConsoleLog.Entry entry = ConsoleLog.getEntries().get(i);
            this.lines.addFirst(new Line(this.split(entry.text(), ""), "", entry.color()));
        }

        ConsoleLog.addListener(entry -> {
            this.lines.addFirst(new Line(this.split(entry.text(), ""), "", entry.color()));
            CollectionUtils.limitSize(this.lines, 200);
        });
    }

    @Override
    protected float getLength() {
        float h = 0.0F;
        for (Line line : getFilteredLines()) {
            h += line.getHeight();
        }
        return h + CONTENT_TOP + 60.0F;
    }

    @Override
    public void render() {
        Render2DUtils.rounded(this.stack, 0, 0, width, height - 40.0F, 10, 10, GuiColorUtils.bg1.getRGB(), ColorUtils.SHADOW100I);

        this.renderFilterBar();

        this.stack.pushPose();
        this.stack.translate(20.0F, CONTENT_TOP - this.scroll.get(), 0.0F);
        this.renderLines();
        this.stack.popPose();

        this.renderBottomBG();
        this.renderBottom();

        if (this.typing && !this.suggestions.isEmpty()) {
            this.renderSuggestions();
        }

        this.textField.setActive(this.typing);
        this.filterField.setActive(this.filterActive);
    }

    private void renderFilterBar() {
        float filterY = 15.0F;
        float filterW = 200.0F;
        float filterH = 18.0F;
        float filterX = width - filterW - 10.0F;

        BlackOut.FONT.text(this.stack, "Filter:", 1.5F, filterX - 55.0F, filterY + 2.0F, Color.GRAY.getRGB(), false, false);

        this.filterField.render(this.stack, 1.5F, this.mx, this.my, filterX, filterY, filterW, filterH, 5.0F, 0.0F, Color.WHITE, new Color(40, 40, 40));
    }

    private void renderSuggestions() {
        float sugY = this.height - 105.0F;
        float sugX = 20.0F;
        float lineH = BlackOut.FONT.getHeight() * 2.0F * 1.3F;
        int count = Math.min(this.suggestions.size(), 8);
        float totalH = lineH * count + 6.0F;

        Render2DUtils.rounded(this.stack, sugX, sugY - totalH, this.width - 40.0F, totalH, 5, 3, SUGGESTION_BG, 0);

        for (int i = 0; i < count; i++) {
            float y = sugY - totalH + 3.0F + i * lineH;
            if (i == this.suggestionIndex) {
                Render2DUtils.quad(this.stack, sugX + 2, y, this.width - 44.0F, lineH, SUGGESTION_HIGHLIGHT);
            }
            int clr = i == this.suggestionIndex ? Color.WHITE.getRGB() : Color.GRAY.getRGB();
            BlackOut.FONT.text(this.stack, this.suggestions.get(i), 2.0F, sugX + 8.0F, y + 1.0F, clr, false, false);
        }
    }

    @Override
    public void onMouse(int button, boolean state) {
        if (state && button == 0) {
            if (this.mx >= 20.0 && this.mx <= this.width - 20.0F && this.my >= this.height - 85.0F && this.my <= this.height - 45.0F) {
                this.typing = true;
                this.filterActive = false;
                this.textField.clear();
                this.clearSuggestions();
                SelectedComponent.setId(this.id);
                return;
            }

            float filterX = width - 200.0F - 10.0F;
            if (this.mx >= filterX && this.mx <= filterX + 200.0F && this.my >= 15.0F && this.my <= 33.0F) {
                this.filterActive = true;
                this.typing = false;
                SelectedComponent.setId(this.filterId);
                return;
            }

            if (this.typing && !this.suggestions.isEmpty()) {
                float sugY = this.height - 105.0F;
                float lineH = BlackOut.FONT.getHeight() * 2.0F * 1.3F;
                int count = Math.min(this.suggestions.size(), 8);
                float totalH = lineH * count + 6.0F;
                float topY = sugY - totalH;

                if (this.mx >= 20.0F && this.mx <= this.width - 20.0F && this.my >= topY && this.my <= sugY) {
                    int idx = (int) ((this.my - topY - 3.0F) / lineH);
                    if (idx >= 0 && idx < count) {
                        this.applySuggestion(idx);
                    }
                }
            }
        }
    }

    @Override
    public void onKey(int key, boolean state) {
        if (this.filterActive) {
            if (state && key == GLFW.GLFW_KEY_ENTER) {
                this.filterText = this.filterField.getContent();
                this.filterActive = false;
                SelectedComponent.reset();
            } else if (state && key == GLFW.GLFW_KEY_ESCAPE) {
                this.filterActive = false;
                this.filterField.setContent(this.filterText);
                SelectedComponent.reset();
            } else {
                this.filterField.type(key, state);
                this.filterText = this.filterField.getContent();
            }
            return;
        }

        if (this.typing) {
            if (state) {
                switch (key) {
                    case GLFW.GLFW_KEY_ENTER:
                        String content = this.textField.getContent();
                        if (!content.isEmpty()) {
                            this.addToHistory(content);
                            this.handle(content);
                        }
                        this.textField.clear();
                        this.clearSuggestions();
                        return;

                    case GLFW.GLFW_KEY_UP:
                        if (!this.suggestions.isEmpty()) {
                            this.suggestionIndex = Math.max(0, this.suggestionIndex - 1);
                        } else {
                            this.navigateHistory(1);
                        }
                        return;

                    case GLFW.GLFW_KEY_DOWN:
                        if (!this.suggestions.isEmpty()) {
                            this.suggestionIndex = Math.min(this.suggestions.size() - 1, this.suggestionIndex + 1);
                        } else {
                            this.navigateHistory(-1);
                        }
                        return;

                    case GLFW.GLFW_KEY_TAB:
                        if (!this.suggestions.isEmpty() && this.suggestionIndex >= 0) {
                            this.applySuggestion(this.suggestionIndex);
                        } else {
                            this.updateSuggestions();
                            if (!this.suggestions.isEmpty()) {
                                this.suggestionIndex = 0;
                            }
                        }
                        return;

                    case GLFW.GLFW_KEY_ESCAPE:
                        if (!this.suggestions.isEmpty()) {
                            this.clearSuggestions();
                            return;
                        }
                        this.typing = false;
                        SelectedComponent.reset();
                        return;

                    default:
                        this.textField.type(key, state);
                        this.updateSuggestions();
                }
            } else {
                this.textField.type(key, state);
            }
        }
    }

    private void addToHistory(String command) {
        if (this.commandHistory.isEmpty() || !this.commandHistory.getFirst().equals(command)) {
            this.commandHistory.addFirst(command);
            if (this.commandHistory.size() > 50) {
                this.commandHistory.removeLast();
            }
        }
        this.historyIndex = -1;
        this.savedInput = "";
    }

    private void navigateHistory(int direction) {
        if (this.commandHistory.isEmpty()) return;

        if (this.historyIndex == -1 && direction > 0) {
            this.savedInput = this.textField.getContent();
        }

        int newIndex = this.historyIndex + direction;
        if (newIndex < -1) newIndex = -1;
        if (newIndex >= this.commandHistory.size()) newIndex = this.commandHistory.size() - 1;

        this.historyIndex = newIndex;

        if (this.historyIndex == -1) {
            this.textField.setContent(this.savedInput);
        } else {
            this.textField.setContent(this.commandHistory.get(this.historyIndex));
        }
    }

    private void updateSuggestions() {
        String input = this.textField.getContent();
        this.suggestions.clear();
        this.suggestionIndex = -1;

        if (input.isEmpty()) return;

        String[] parts = input.split(" ", -1);

        if (parts.length <= 1) {
            String prefix = parts[0].toLowerCase();
            for (Command cmd : Managers.COMMANDS.getCommands()) {
                if (cmd.name.toLowerCase().startsWith(prefix)) {
                    this.suggestions.add(cmd.name);
                }
            }
        } else {
            String cmdName = parts[0].toLowerCase();
            for (Command cmd : Managers.COMMANDS.getCommands()) {
                if (cmd.name.toLowerCase().equals(cmdName)) {
                    String[] cmdArgs = new String[parts.length - 1];
                    System.arraycopy(parts, 1, cmdArgs, 0, cmdArgs.length);
                    List<String> cmdSuggestions = cmd.getSuggestions(cmdArgs);
                    String lastPart = parts[parts.length - 1].toLowerCase();
                    for (String s : cmdSuggestions) {
                        if (s.toLowerCase().startsWith(lastPart)) {
                            this.suggestions.add(s);
                        }
                    }
                    break;
                }
            }
        }

        if (this.suggestions.size() == 1 && this.suggestions.getFirst().equalsIgnoreCase(input.trim())) {
            this.suggestions.clear();
        }
    }

    private void applySuggestion(int index) {
        if (index < 0 || index >= this.suggestions.size()) return;
        String suggestion = this.suggestions.get(index);
        String input = this.textField.getContent();
        String[] parts = input.split(" ", -1);

        if (parts.length <= 1) {
            this.textField.setContent(suggestion + " ");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length - 1; i++) {
                sb.append(parts[i]).append(" ");
            }
            sb.append(suggestion).append(" ");
            this.textField.setContent(sb.toString());
        }
        this.clearSuggestions();
    }

    private void clearSuggestions() {
        this.suggestions.clear();
        this.suggestionIndex = -1;
    }

    private List<Line> getFilteredLines() {
        if (this.filterText.isEmpty()) return this.lines;

        String lower = this.filterText.toLowerCase();
        List<Line> filtered = new ArrayList<>();
        for (Line line : this.lines) {
            for (String t : line.text()) {
                if (stripFormatting(t).toLowerCase().contains(lower)) {
                    filtered.add(line);
                    break;
                }
            }
        }
        return filtered;
    }

    private static String stripFormatting(String text) {
        return text.replaceAll("§.", "");
    }

    private void renderBottomBG() {
        Render2DUtils.rounded(this.stack, 0.0F, this.height - 100.0F, this.width, 60.0F, 10.0F, 0.0F, GuiColorUtils.bg2.getRGB(), 0, Render2DUtils.RoundedSide.BOTTOM);
        Render2DUtils.line(this.stack, 15.0F, this.height - 100.0F, this.width - 15.0F, this.height - 100.0F, LINE_COLOR);
    }

    private void renderBottom() {
        this.textField.render(this.stack, 2.0F, this.mx, this.my, 20.0F, this.height - 65.0F, this.width - 40.0F, 0.0F, 20.0F, 15.0F, Color.WHITE, GuiColorUtils.bg2);
    }

    private void handle(String input) {
        if (input.isEmpty()) return;

        ConsoleLog.add(ChatFormatting.GRAY + "> " + ChatFormatting.WHITE + input, Color.WHITE.getRGB());

        String result = Managers.COMMANDS.onCommand(input.split(" "));
        if (result == null) {
            ConsoleLog.add("Unknown command: " + input + ". Type §ahelp§r for commands.", Color.RED.getRGB());
        } else {
            for (String line : result.split("\n")) {
                ConsoleLog.add(line, Color.WHITE.getRGB());
            }
        }
    }

    private void renderLines() {
        List<Line> filtered = getFilteredLines();
        for (int i = filtered.size() - 1; i >= 0; i--) {
            this.renderTexts(filtered.get(i));
        }
    }

    private void renderTexts(Line line) {
        float fontHeight = BlackOut.FONT.getHeight() * 2.0F;
        int color = line.color();

        for (String string : line.text()) {
            color = this.renderLine(string, color);
            this.stack.translate(0.0, fontHeight * 1.5, 0.0);
        }
        this.stack.translate(0.0, fontHeight * 0.5, 0.0);
    }

    private int renderLine(String line, int color) {
        this.stack.pushPose();
        float xOffset = 0.0F;
        int currentClr = color;

        for (String part : line.split("(?=§)")) {
            String text;
            if (part.startsWith("§") && part.length() >= 2) {
                text = part.substring(2);
                ChatFormatting formatting = ChatFormatting.getByCode(part.charAt(1));
                if (formatting == ChatFormatting.RESET) {
                    currentClr = color;
                } else {
                    currentClr = (formatting != null && formatting.getColor() != null) ? formatting.getColor() | 0xFF000000 : color;
                }
            } else {
                text = part;
                currentClr = color;
            }

            BlackOut.FONT.text(this.stack, text, 2.0F, xOffset, 0.0F, currentClr, false, false);
            xOffset += BlackOut.FONT.getWidth(text) * 2.0F;
        }

        this.stack.popPose();
        return currentClr;
    }



    private String[] split(String string, String time) {
        String full = time + string;
        float maxWidth = (this.width > 0 ? this.width - 40.0F : 860.0F) / 2.0F;

        if (maxWidth <= 0) {
            return new String[]{full};
        }

        List<String> lines = new ArrayList<>();
        String remaining = full;

        while (!remaining.isEmpty()) {
            float w = 0;
            int lastBreak = -1;
            int cut = remaining.length();

            for (int i = 0; i < remaining.length(); i++) {
                char c = remaining.charAt(i);
                if (c == '§' && i + 1 < remaining.length()) {
                    i++;
                    continue;
                }
                w += BlackOut.FONT.getWidth(String.valueOf(c));
                if (c == ' ') lastBreak = i;
                if (w > maxWidth) {
                    cut = lastBreak > 0 ? lastBreak + 1 : i;
                    break;
                }
            }

            lines.add(remaining.substring(0, cut));
            remaining = remaining.substring(cut);
        }

        return lines.toArray(new String[0]);
    }

    private record Line(String[] text, String time, int color) {
        private float getHeight() {
            return (this.text.length * BlackOut.FONT.getHeight() * 1.5F + BlackOut.FONT.getHeight() * 0.5F) * 2.0F;
        }
    }
}
