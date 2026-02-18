package bodevelopment.client.blackout.gui.clickgui.screens;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.gui.TextField;
import bodevelopment.client.blackout.gui.clickgui.ClickGuiScreen;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.GuiColorUtils;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import bodevelopment.client.blackout.util.SelectedComponent;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.util.Formatting;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ConsoleScreen extends ClickGuiScreen {
    private static final int LINE_COLOR = new Color(50, 50, 50, 255).getRGB();
    private final List<Line> lines = new ArrayList<>();
    private final TextField textField = new TextField();
    private final int id = SelectedComponent.nextId();
    private boolean typing = false;

    public ConsoleScreen() {
        super("Console", 900.0F, 600.0F, true);
        this.addLine("Welcome to BlackOut Console. Commands don't need a prefix.", Color.GREEN.getRGB());
    }

    @Override
    protected float getLength() {
        float height = 0.0F;
        for (Line line : this.lines) {
            height += line.getHeight();
        }
        return height + 20.0F;
    }

    @Override
    public void render() {
        RenderUtils.rounded(this.stack, 0, 0, width, height - 40.0F, 10, 10, GuiColorUtils.bg1.getRGB(), ColorUtils.SHADOW100I);

        this.stack.push();
        this.stack.translate(20.0F, 10.0F - this.scroll.get(), 0.0F);
        this.renderLines();
        this.stack.pop();

        this.renderBottomBG();
        this.renderBottom();

        this.textField.setActive(this.typing);
    }

    @Override
    public void onMouse(int button, boolean state) {
        if (state && button == 0) {
            if (this.mx >= 20.0 && this.mx <= this.width - 20.0F && this.my >= this.height - 85.0F && this.my <= this.height - 45.0F) {
                this.typing = true;
                this.textField.clear();
                SelectedComponent.setId(this.id);
            }
        }
    }

    @Override
    public void onKey(int key, boolean state) {
        if (this.typing) {
            if (state && key == 257) {
                this.handle(this.textField.getContent());
                this.textField.clear();
                this.typing = false;
                SelectedComponent.reset();
            } else {
                this.textField.type(key, state);
            }
        }
    }

    private void renderBottomBG() {
        RenderUtils.roundedBottom(this.stack, 0.0F, this.height - 100.0F, this.width, 60.0F, 10.0F, 0.0F, GuiColorUtils.bg2.getRGB(), 0);
        RenderUtils.line(this.stack, 15.0F, this.height - 100.0F, this.width - 15.0F, this.height - 100.0F, LINE_COLOR);
    }

    private void renderBottom() {
        this.textField.render(this.stack, 2.0F, this.mx, this.my, 20.0F, this.height - 65.0F, this.width - 40.0F, 0.0F, 20.0F, 15.0F, Color.WHITE, GuiColorUtils.bg2);
    }

    private void handle(String input) {
        if (input.isEmpty()) return;
        String result = Managers.COMMANDS.onCommand(input.split(" "));
        if (result == null) {
            this.addLine("Unrecognized command: " + input, Color.RED.getRGB());
        } else {
            this.addLine(result, Color.WHITE.getRGB());
        }
    }

    private void renderLines() {
        for (int i = this.lines.size() - 1; i >= 0; i--) {
            this.renderTexts(this.lines.get(i));
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
        this.stack.push();
        float xOffset = 0.0F;
        int currentClr = color;

        for (String part : line.split("(?=§)")) {
            String text;
            if (part.startsWith("§") && part.length() >= 2) {
                text = part.substring(2);
                Formatting formatting = Formatting.byCode(part.charAt(1));
                currentClr = (formatting != null) ? formatting.getColorValue() | 0xFF000000 : color;
            } else {
                text = part;
                currentClr = color;
            }

            BlackOut.FONT.text(this.stack, text, 2.0F, xOffset, 0.0F, currentClr, false, false);
            xOffset += BlackOut.FONT.getWidth(text) * 2.0F;
        }

        this.stack.pop();
        return currentClr;
    }

    private void addLine(String string, int color) {
        String time = this.currentTime();
        Line line = new Line(this.split(string, time), time, color);

        if (this.scroll.get() > this.getLength() - 100.0F) {
            this.scroll.offset(line.getHeight());
        }

        this.lines.addFirst(line);
        OLEPOSSUtils.limitList(this.lines, 30);
    }

    private String currentTime() {
        LocalDateTime now = LocalDateTime.now();
        return String.format("[%02d:%02d:%02d] ", now.getHour(), now.getMinute(), now.getSecond());
    }

    private String[] split(String string, String time) {
        List<String> list = new ArrayList<>();
        list.add(time + string);
        return list.toArray(new String[0]);
    }

    private record Line(String[] text, String time, int color) {
        private float getHeight() {
            return (this.text.length * BlackOut.FONT.getHeight() * 1.5F + BlackOut.FONT.getHeight() * 0.5F) * 2.0F;
        }
    }
}