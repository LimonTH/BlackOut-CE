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

package bodevelopment.client.blackout.gui;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.keys.Keys;
import bodevelopment.client.blackout.util.render.Render2DUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class TextField {
    private static final Map<String, String> shiftModified = new HashMap<>();

    static {
        shiftModified.put("1", "!");
        shiftModified.put("2", "@");
        shiftModified.put("3", "#");
        shiftModified.put("4", "$");
        shiftModified.put("5", "%");
        shiftModified.put("6", "^");
        shiftModified.put("7", "&");
        shiftModified.put("8", "*");
        shiftModified.put("9", "(");
        shiftModified.put("0", ")");
        shiftModified.put("-", "_");
        shiftModified.put("=", "+");
        shiftModified.put("[", "{");
        shiftModified.put("]", "}");
        shiftModified.put(";", ":");
        shiftModified.put("'", "\"");
        shiftModified.put(",", "<");
        shiftModified.put(".", ">");
        shiftModified.put("/", "?");
        shiftModified.put("\\", "|");
        shiftModified.put("`", "~");
    }

    private String content = "";
    private int typingIndex = 0;
    private long lastType = 0L;
    private boolean active = false;
    private int heldKey = 0;
    private long prevHeld = 0L;
    private int maxLength = Integer.MAX_VALUE;
    private float width;
    private float height;
    private double mx;
    private double my;
    private float scale;
    private float radius;
    private boolean capsLock = false;
    private float scrollOffset = 0.0F;

    private static final int MAX_UNDO = 50;
    private final java.util.ArrayDeque<String> undoStack = new java.util.ArrayDeque<>();
    private final java.util.ArrayDeque<Integer> undoCursorStack = new java.util.ArrayDeque<>();
    private final java.util.ArrayDeque<String> redoStack = new java.util.ArrayDeque<>();
    private final java.util.ArrayDeque<Integer> redoCursorStack = new java.util.ArrayDeque<>();
    private boolean undoBlocked = false;

    private int selectionStart = -1;
    private int selectionEnd = -1;
    private boolean dragging = false;
    private double dragStartMx = 0.0;
    private double lastDragMx = Double.NaN;

    public void render(
            PoseStack stack,
            float scale,
            double mx,
            double my,
            float x,
            float y,
            float width,
            float height,
            float radius,
            float shadow,
            Color textColor,
            Color bgColor
    ) {
        this.width = width;
        this.height = height;
        this.scale = scale;
        this.radius = radius;
        this.mx = mx - x;
        this.my = my - y;
        this.limitIndex();

        Render2DUtils.rounded(stack, x, y, width, height, radius, shadow, bgColor.getRGB(), new Color(0, 0, 0, (int) Math.floor(bgColor.getAlpha() * 0.6)).getRGB());

        if (this.active) {
            boolean leftHeld = GLFW.glfwGetMouseButton(BlackOut.mc.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_1) == 1;
            if (leftHeld && !this.dragging) {
                this.dragging = true;
                this.dragStartMx = this.mx;
                this.lastDragMx = this.mx;
                if (this.selectionStart < 0) this.selectionStart = this.typingIndex;
                this.selectionEnd = this.typingIndex;
            } else if (!leftHeld && this.dragging) {
                this.dragging = false;
            }

            if (this.dragging && this.mx != this.lastDragMx) {
                int dragIdx = this.getIndexAt(this.mx);
                this.selectionEnd = Math.min(dragIdx, this.content.length());
                this.typingIndex = this.selectionEnd;
                this.lastDragMx = this.mx;
            }
        }

        float centerY = y + height / 2.0F;
        float cursorOffset = this.getOffset();
        float padding = 4.0F;

        if (cursorOffset - this.scrollOffset > width - padding) {
            this.scrollOffset = cursorOffset - width + padding;
        }
        if (cursorOffset - this.scrollOffset < 0) {
            this.scrollOffset = cursorOffset;
        }
        if (this.scrollOffset < 0) this.scrollOffset = 0;

        String visibleText = this.content;
        float visibleX = x;
        int startIdx = 0;
        if (this.scrollOffset > 0 && !this.content.isEmpty()) {
            float accum = 0;
            for (int ci = 0; ci < this.content.length(); ci++) {
                float cw = getCharWidth(this.content.charAt(ci));
                if (accum + cw > this.scrollOffset) {
                    startIdx = ci;
                    visibleX = x - (this.scrollOffset - accum);
                    break;
                }
                accum += cw;
                startIdx = ci + 1;
            }
            if (startIdx < this.content.length()) {
                visibleText = this.content.substring(startIdx);
            } else {
                visibleText = "";
            }
            if (visibleX < x && !visibleText.isEmpty()) {
                float firstW = getCharWidth(visibleText.charAt(0));
                startIdx++;
                visibleX += firstW;
                visibleText = visibleText.length() > 1 ? visibleText.substring(1) : "";
            }
        }

        if (!Keys.get(this.heldKey)) this.heldKey = 0;
        if (this.heldKey > 0 && System.currentTimeMillis() - this.prevHeld > 500L) {
            this.type(this.heldKey, true);
            this.prevHeld += 50L;
        }

        if (!visibleText.isEmpty()) {
            float maxRenderWidth = (x + width - padding) - visibleX;
            float currentWidth = 0;
            int endIdx = 0;
            for (int ci = 0; ci < visibleText.length(); ci++) {
                float cw = getCharWidth(visibleText.charAt(ci));
                if (currentWidth + cw > maxRenderWidth) break;
                currentWidth += cw;
                endIdx = ci + 1;
            }
            if (endIdx < visibleText.length()) {
                visibleText = visibleText.substring(0, endIdx);
            }
        }

        renderSelection(stack, startIdx, centerY, textColor, x);
        BlackOut.FONT.text(stack, visibleText, scale, visibleX, centerY, textColor, false, true);
        renderCursor(stack, centerY, textColor, x, cursorOffset);
    }

    private float getCharWidth(char c) {
        return BlackOut.FONT.getWidth(String.valueOf(c)) * this.scale;
    }

    private void renderSelection(PoseStack stack, int startIdx, float centerY, Color textColor, float fieldX) {
        if (this.selectionStart < 0 || this.selectionStart == this.selectionEnd) return;

        int selLow = Math.min(this.selectionStart, this.selectionEnd);
        int selHigh = Math.max(this.selectionStart, this.selectionEnd);
        int visHigh = startIdx + this.content.length() - startIdx;

        int renderLow = Math.max(selLow, startIdx);
        int renderHigh = Math.min(selHigh, visHigh);
        if (renderLow >= renderHigh) return;

        float selStartX = fieldX;
        for (int ci = startIdx; ci < renderLow; ci++) {
            selStartX += getCharWidth(this.content.charAt(ci));
        }
        float selEndX = selStartX;
        for (int ci = renderLow; ci < renderHigh; ci++) {
            selEndX += getCharWidth(this.content.charAt(ci));
        }

        float leftEdge = fieldX;
        float rightEdge = fieldX + this.width;
        if (selEndX <= leftEdge || selStartX >= rightEdge) return;
        float renderX = Math.max(selStartX, leftEdge);
        float renderW = Math.min(selEndX, rightEdge) - renderX;
        if (renderW <= 0) return;

        float highlightHeight = BlackOut.FONT.getRenderedGlyphHeight(this.scale) * 0.65F;
        float highlightY = centerY - highlightHeight / 2.0F;
        int highlightColor = new Color(60, 120, 255, 100).getRGB();
        Render2DUtils.quad(stack, renderX, highlightY, renderW, highlightHeight, highlightColor);
    }

    private void renderCursor(PoseStack stack, float centerY, Color textColor, float fieldX, float cursorOffset) {
        if (!this.active) return;
        if ((System.currentTimeMillis() - this.lastType) % 1000L >= 500L) return;

        float cursorHeight = BlackOut.FONT.getRenderedGlyphHeight(this.scale) * 0.55F;
        float cursorY2 = centerY - (cursorHeight / 2.0F);
        float cursorX = fieldX + cursorOffset - this.scrollOffset;

        if (cursorX >= fieldX && cursorX <= fieldX + this.width) {
            Render2DUtils.quad(stack, cursorX, cursorY2, this.scale, cursorHeight, textColor.getRGB());
        }
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content.length() > maxLength ? content.substring(0, maxLength) : content;
        this.typingIndex = this.content.length();
        clearSelection();
    }

    public void setMaxLength(int max) {
        this.maxLength = max;
        if (this.content.length() > max) {
            this.content = this.content.substring(0, max);
            this.typingIndex = Math.min(this.typingIndex, max);
        }
    }

    private int getIndexAt(double offsetX) {
        double textOffset = offsetX + this.scrollOffset;
        float accum = 0;
        int idx = 0;
        double closestDist = Math.abs(textOffset);
        int closestIdx = 0;

        for (int ci = 0; ci < this.content.length(); ci++) {
            float w = getCharWidth(this.content.charAt(ci));
            double mid = accum + w / 2.0;
            if (textOffset <= mid) return ci;
            accum += w;
            idx = ci + 1;
        }
        return idx;
    }

    private float getOffset() {
        float offset = 0.0F;
        for (int ci = 0; ci < this.typingIndex && ci < this.content.length(); ci++) {
            offset += getCharWidth(this.content.charAt(ci));
        }
        return offset;
    }

    public boolean click(int button, boolean pressed) {
        if (this.isEmpty()) return false;
        if (this.mx < -this.radius || this.mx > this.width + this.radius
                || this.my < -this.radius || this.my > this.height + this.radius) {
            if (pressed) this.active = false;
            return false;
        }

        if (button == 0 && pressed) {
            if (this.active) {
                int clickIdx = getIndexAt(this.mx);
                if (Keys.get(GLFW.GLFW_KEY_LEFT_SHIFT) || Keys.get(GLFW.GLFW_KEY_RIGHT_SHIFT)) {
                    if (this.selectionStart < 0) this.selectionStart = this.typingIndex;
                    this.selectionEnd = clickIdx;
                    this.typingIndex = clickIdx;
                } else {
                    this.typingIndex = clickIdx;
                    clearSelection();
                }
                this.lastType = System.currentTimeMillis();
            } else {
                this.active = true;
                this.typingIndex = getIndexAt(this.mx);
                clearSelection();
                this.lastType = System.currentTimeMillis();
            }
            return true;
        }
        return false;
    }

    public void onKey(int key, boolean pressed) {
        if (!this.active || !pressed) return;
        this.type(key, true);
    }

    public void type(int key, boolean state) {
        if (!state) return;

        boolean ctrl = Keys.get(GLFW.GLFW_KEY_LEFT_CONTROL) || Keys.get(GLFW.GLFW_KEY_RIGHT_CONTROL);

        if (ctrl && key >= 32 && key <= 162) {
            switch (key) {
                case GLFW.GLFW_KEY_A -> { selectAll(); return; }
                case GLFW.GLFW_KEY_C -> { copySelection(); return; }
                case GLFW.GLFW_KEY_X -> { cutSelection(); return; }
                case GLFW.GLFW_KEY_Z -> {
                    if (Keys.get(GLFW.GLFW_KEY_LEFT_SHIFT) || Keys.get(GLFW.GLFW_KEY_RIGHT_SHIFT)) {
                        redo();
                    } else {
                        undo();
                    }
                    return;
                }
                case GLFW.GLFW_KEY_V -> {
                }
                default -> {}
            }
        }

        if ((key >= 32 && key <= 162) || (key >= 256 && key <= 348)) {
            if (key != this.heldKey) this.prevHeld = System.currentTimeMillis();
            this.heldKey = key;
            this.limitIndex();

            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (hasSelection()) {
                    deleteSelection();
                } else if (this.typingIndex > 0) {
                    saveUndo();
                    if (ctrl) {
                        deleteWordBefore();
                    } else {
                        this.content = this.content.substring(0, this.typingIndex - 1)
                                + this.content.substring(this.typingIndex);
                        this.typingIndex--;
                    }
                }
                clearSelection();
                this.lastType = System.currentTimeMillis();
                return;
            }

            if (key == GLFW.GLFW_KEY_DELETE) {
                if (hasSelection()) {
                    deleteSelection();
                } else if (this.typingIndex < this.content.length()) {
                    saveUndo();
                    if (ctrl) {
                        deleteWordAfter();
                    } else {
                        this.content = this.content.substring(0, this.typingIndex)
                                + this.content.substring(this.typingIndex + 1);
                    }
                }
                clearSelection();
                this.lastType = System.currentTimeMillis();
                return;
            }

            if (key == GLFW.GLFW_KEY_V && ctrl) {
                String cb = BlackOut.mc.keyboardHandler.getClipboard();
                if (hasSelection()) deleteSelection();
                for (char c : cb.toCharArray()) this.addChar(String.valueOf(c));
                return;
            }

            if (key == GLFW.GLFW_KEY_C && ctrl) return;
            if (key == GLFW.GLFW_KEY_X && ctrl) return;
            if (key == GLFW.GLFW_KEY_A && ctrl) return;

            switch (key) {
                case GLFW.GLFW_KEY_BACKSPACE, GLFW.GLFW_KEY_DELETE -> {}
                case GLFW.GLFW_KEY_RIGHT -> {
                    if (ctrl) {
                        moveToNextWord();
                    } else {
                        if (this.typingIndex < this.content.length()) this.typingIndex++;
                    }
                    if (!(Keys.get(GLFW.GLFW_KEY_LEFT_SHIFT) || Keys.get(GLFW.GLFW_KEY_RIGHT_SHIFT))) {
                        clearSelection();
                    } else {
                        if (this.selectionStart < 0) this.selectionStart = this.typingIndex - 1;
                        this.selectionEnd = this.typingIndex;
                    }
                    this.lastType = System.currentTimeMillis();
                    return;
                }
                case GLFW.GLFW_KEY_LEFT -> {
                    if (ctrl) {
                        moveToPrevWord();
                    } else {
                        if (this.typingIndex > 0) this.typingIndex--;
                    }
                    if (!(Keys.get(GLFW.GLFW_KEY_LEFT_SHIFT) || Keys.get(GLFW.GLFW_KEY_RIGHT_SHIFT))) {
                        clearSelection();
                    } else {
                        if (this.selectionStart < 0) this.selectionStart = this.typingIndex + 1;
                        this.selectionEnd = this.typingIndex;
                    }
                    this.lastType = System.currentTimeMillis();
                    return;
                }
                case GLFW.GLFW_KEY_HOME -> {
                    this.typingIndex = 0;
                    if (!isShiftDown()) clearSelection();
                    this.lastType = System.currentTimeMillis();
                    return;
                }
                case GLFW.GLFW_KEY_END -> {
                    this.typingIndex = this.content.length();
                    if (!isShiftDown()) clearSelection();
                    this.lastType = System.currentTimeMillis();
                    return;
                }
                case GLFW.GLFW_KEY_SPACE -> {
                    if (hasSelection()) deleteSelection();
                    this.addChar(" ");
                    return;
                }
                case GLFW.GLFW_KEY_CAPS_LOCK -> {
                    this.capsLock = !this.capsLock;
                    return;
                }
                case GLFW.GLFW_KEY_ESCAPE -> {
                    this.active = false;
                    clearSelection();
                    return;
                }
                case GLFW.GLFW_KEY_ENTER -> { return; }
                default -> {
                    if (key >= 48 && key <= 57) {
                        if (hasSelection()) deleteSelection();
                        this.addChar(this.modify(String.valueOf(key - 48), key));
                        return;
                    }
                    String name = GLFW.glfwGetKeyName(key, 0);
                    if (name != null) {
                        if (hasSelection()) deleteSelection();
                        this.addChar(this.modify(name, key));
                    }
                    return;
                }
            }
        }
    }

    private boolean hasSelection() {
        return this.selectionStart >= 0 && this.selectionStart != this.selectionEnd;
    }

    private void clearSelection() {
        this.selectionStart = -1;
        this.selectionEnd = -1;
    }

    private void saveUndo() {
        if (this.undoBlocked) { this.undoBlocked = false; return; }
        this.redoStack.clear();
        this.redoCursorStack.clear();
        this.undoStack.push(this.content);
        this.undoCursorStack.push(this.typingIndex);
        if (this.undoStack.size() > MAX_UNDO) {
            this.undoStack.removeLast();
            this.undoCursorStack.removeLast();
        }
    }

    private void undo() {
        if (this.undoStack.isEmpty()) return;
        this.undoBlocked = true;
        this.redoStack.push(this.content);
        this.redoCursorStack.push(this.typingIndex);
        if (this.redoStack.size() > MAX_UNDO) {
            this.redoStack.removeLast();
            this.redoCursorStack.removeLast();
        }
        String prev = this.undoStack.pop();
        int prevCursor = this.undoCursorStack.pop();
        this.content = prev.length() > this.maxLength ? prev.substring(0, this.maxLength) : prev;
        this.typingIndex = Math.min(prevCursor, this.content.length());
        this.scrollOffset = 0.0F;
        clearSelection();
        this.lastType = System.currentTimeMillis();
    }

    private void redo() {
        if (this.redoStack.isEmpty()) return;
        this.undoBlocked = true;
        this.undoStack.push(this.content);
        this.undoCursorStack.push(this.typingIndex);
        if (this.undoStack.size() > MAX_UNDO) {
            this.undoStack.removeLast();
            this.undoCursorStack.removeLast();
        }
        String next = this.redoStack.pop();
        int nextCursor = this.redoCursorStack.pop();
        this.content = next.length() > this.maxLength ? next.substring(0, this.maxLength) : next;
        this.typingIndex = Math.min(nextCursor, this.content.length());
        this.scrollOffset = 0.0F;
        clearSelection();
        this.lastType = System.currentTimeMillis();
    }

    private void selectAll() {
        if (this.content.isEmpty()) return;
        this.selectionStart = 0;
        this.selectionEnd = this.content.length();
        this.typingIndex = this.content.length();
        this.lastType = System.currentTimeMillis();
    }

    private void copySelection() {
        if (!hasSelection()) return;
        int low = Math.min(this.selectionStart, this.selectionEnd);
        int high = Math.max(this.selectionStart, this.selectionEnd);
        BlackOut.mc.keyboardHandler.setClipboard(this.content.substring(low, high));
    }

    private void cutSelection() {
        if (!hasSelection()) return;
        copySelection();
        deleteSelection();
    }

    private void deleteSelection() {
        if (!hasSelection()) return;
        saveUndo();
        int low = Math.min(this.selectionStart, this.selectionEnd);
        int high = Math.max(this.selectionStart, this.selectionEnd);
        this.content = this.content.substring(0, low) + this.content.substring(high);
        this.typingIndex = low;
        clearSelection();
    }

    private void deleteWordBefore() {
        if (this.typingIndex <= 0) return;
        int wordStart = this.typingIndex - 1;
        while (wordStart > 0 && this.content.charAt(wordStart - 1) == ' ') wordStart--;
        while (wordStart > 0 && this.content.charAt(wordStart - 1) != ' ') wordStart--;
        this.content = this.content.substring(0, wordStart) + this.content.substring(this.typingIndex);
        this.typingIndex = wordStart;
    }

    private void deleteWordAfter() {
        if (this.typingIndex >= this.content.length()) return;
        int wordEnd = this.typingIndex + 1;
        while (wordEnd < this.content.length() && this.content.charAt(wordEnd) != ' ') wordEnd++;
        while (wordEnd < this.content.length() && this.content.charAt(wordEnd) == ' ') wordEnd++;
        this.content = this.content.substring(0, this.typingIndex) + this.content.substring(wordEnd);
    }

    private void moveToNextWord() {
        int idx = this.typingIndex;
        while (idx < this.content.length() && this.content.charAt(idx) != ' ') idx++;
        while (idx < this.content.length() && this.content.charAt(idx) == ' ') idx++;
        this.typingIndex = Math.min(idx, this.content.length());
    }

    private void moveToPrevWord() {
        if (this.typingIndex <= 0) return;
        int idx = this.typingIndex - 1;
        while (idx > 0 && this.content.charAt(idx - 1) == ' ') idx--;
        while (idx > 0 && this.content.charAt(idx - 1) != ' ') idx--;
        this.typingIndex = idx;
    }

    private boolean isShiftDown() {
        return Keys.get(GLFW.GLFW_KEY_LEFT_SHIFT) || Keys.get(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private void limitIndex() {
        this.typingIndex = Mth.clamp(this.typingIndex, 0, this.content.length());
    }

    private String modify(String string, int key) {
        boolean shift = Keys.get(GLFW.GLFW_KEY_LEFT_SHIFT) || Keys.get(GLFW.GLFW_KEY_RIGHT_SHIFT);
        boolean upperCase = shift ^ this.capsLock;
        String lower = string.toLowerCase();

        if (shift) {
            if (key == GLFW.GLFW_KEY_SLASH && string.equals(".")) return ",";
            if (shiftModified.containsKey(string)) return shiftModified.get(string);
            if (shiftModified.containsKey(lower)) return shiftModified.get(lower);
        }
        return upperCase ? string.toUpperCase() : lower;
    }

    private void addChar(String c) {
        if (c == null || c.isEmpty()) return;
        if (this.content.length() >= this.maxLength) return;

        saveUndo();
        String pre = this.content.substring(0, this.typingIndex);
        String post = this.content.substring(this.typingIndex);

        this.content = pre + c + post;
        this.typingIndex += c.length();
        this.lastType = System.currentTimeMillis();
    }

    public boolean isEmpty() {
        return this.content.isEmpty();
    }

    public void clear() {
        this.content = "";
        this.typingIndex = 0;
        this.scrollOffset = 0.0F;
        clearSelection();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        if (!active) clearSelection();
    }

    public int getId() {
        return -1;
    }
}
