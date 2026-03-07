package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.module.SubCategory;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents the persistent state of the ClickGUI.
 * This state is saved to and loaded from configuration files.
 */
public class ClickGuiState {
    private SubCategory selectedCategory;
    private float x;
    private float y;
    private float width;
    private float height;
    private float moduleScroll;
    private float categoryScroll;
    private final Set<String> openedModules = new HashSet<>();
    
    public ClickGuiState() {
        this.selectedCategory = SubCategory.OFFENSIVE;
        this.x = 0.0f;
        this.y = 0.0f;
        this.width = 1000.0f;
        this.height = 700.0f;
        this.moduleScroll = 0.0f;
        this.categoryScroll = 0.0f;
    }
    
    public SubCategory getSelectedCategory() {
        return selectedCategory;
    }
    
    public void setSelectedCategory(SubCategory selectedCategory) {
        this.selectedCategory = selectedCategory;
    }
    
    public float getX() {
        return x;
    }
    
    public void setX(float x) {
        this.x = x;
    }
    
    public float getY() {
        return y;
    }
    
    public void setY(float y) {
        this.y = y;
    }
    
    public float getWidth() {
        return width;
    }
    
    public void setWidth(float width) {
        this.width = width;
    }
    
    public float getHeight() {
        return height;
    }
    
    public void setHeight(float height) {
        this.height = height;
    }
    
    public float getModuleScroll() {
        return moduleScroll;
    }
    
    public void setModuleScroll(float moduleScroll) {
        this.moduleScroll = moduleScroll;
    }
    
    public float getCategoryScroll() {
        return categoryScroll;
    }
    
    public void setCategoryScroll(float categoryScroll) {
        this.categoryScroll = categoryScroll;
    }
    
    public Set<String> getOpenedModules() {
        return openedModules;
    }
    
    public void addOpenedModule(String moduleFileName) {
        openedModules.add(moduleFileName);
    }
    
    public void removeOpenedModule(String moduleFileName) {
        openedModules.remove(moduleFileName);
    }
    
    public void clearOpenedModules() {
        openedModules.clear();
    }
    
    public boolean isModuleOpened(String moduleFileName) {
        return openedModules.contains(moduleFileName);
    }
    
    public JsonObject toJson() {
        JsonObject json = new JsonObject();

        if (selectedCategory != null) {
            json.addProperty("selectedCategory", selectedCategory.name());
        }

        json.addProperty("x", x);
        json.addProperty("y", y);
        json.addProperty("width", width);
        json.addProperty("height", height);

        json.addProperty("moduleScroll", moduleScroll);
        json.addProperty("categoryScroll", categoryScroll);

        JsonArray openedArray = new JsonArray();
        for (String moduleName : openedModules) {
            openedArray.add(new JsonPrimitive(moduleName));
        }
        json.add("openedModules", openedArray);
        
        return json;
    }
    
    public static ClickGuiState fromJson(JsonObject json) {
        ClickGuiState state = new ClickGuiState();
        
        if (json == null) {
            return state;
        }

        if (json.has("selectedCategory")) {
            String categoryName = json.get("selectedCategory").getAsString();
            for (SubCategory category : SubCategory.categories) {
                if (category.name().equals(categoryName)) {
                    state.selectedCategory = category;
                    break;
                }
            }
        }

        if (json.has("x")) {
            state.x = json.get("x").getAsFloat();
        }
        if (json.has("y")) {
            state.y = json.get("y").getAsFloat();
        }
        if (json.has("width")) {
            state.width = json.get("width").getAsFloat();
        }
        if (json.has("height")) {
            state.height = json.get("height").getAsFloat();
        }

        if (json.has("moduleScroll")) {
            state.moduleScroll = json.get("moduleScroll").getAsFloat();
        }
        if (json.has("categoryScroll")) {
            state.categoryScroll = json.get("categoryScroll").getAsFloat();
        }

        state.openedModules.clear();
        if (json.has("openedModules") && json.get("openedModules").isJsonArray()) {
            JsonArray openedArray = json.getAsJsonArray("openedModules");
            for (JsonElement element : openedArray) {
                if (element.isJsonPrimitive()) {
                    state.openedModules.add(element.getAsString());
                }
            }
        }
        
        return state;
    }
    
    /**
     * Creates a ClickGuiState from the current ClickGui instance.
     */
    public static ClickGuiState captureCurrent() {
        ClickGuiState state = new ClickGuiState();

        state.selectedCategory = bodevelopment.client.blackout.gui.clickgui.ClickGui.selectedCategory;
        state.x = bodevelopment.client.blackout.gui.clickgui.ClickGui.x;
        state.y = bodevelopment.client.blackout.gui.clickgui.ClickGui.y;
        state.width = bodevelopment.client.blackout.gui.clickgui.ClickGui.width;
        state.height = bodevelopment.client.blackout.gui.clickgui.ClickGui.height;

        bodevelopment.client.blackout.gui.clickgui.ClickGui clickGui = 
            bodevelopment.client.blackout.manager.Managers.CLICK_GUI.CLICK_GUI;
        state.moduleScroll = clickGui.getModuleScroll();
        state.categoryScroll = clickGui.getCategoryScroll();

        for (bodevelopment.client.blackout.gui.clickgui.components.ModuleComponent component : 
             clickGui.moduleComponents) {
            if (component.opened) {
                state.addOpenedModule(component.module.getFileName());
            }
        }
        
        return state;
    }
    
    /**
     * Applies this state to the current ClickGui instance.
     */
    public void applyToCurrent() {
        bodevelopment.client.blackout.gui.clickgui.ClickGui.selectedCategory = selectedCategory;
        bodevelopment.client.blackout.gui.clickgui.ClickGui.x = x;
        bodevelopment.client.blackout.gui.clickgui.ClickGui.y = y;
        bodevelopment.client.blackout.gui.clickgui.ClickGui.width = width;
        bodevelopment.client.blackout.gui.clickgui.ClickGui.height = height;

        bodevelopment.client.blackout.gui.clickgui.ClickGui clickGui = 
            bodevelopment.client.blackout.manager.Managers.CLICK_GUI.CLICK_GUI;
        clickGui.setModuleScroll(moduleScroll);
        clickGui.setCategoryScroll(categoryScroll);

        for (bodevelopment.client.blackout.gui.clickgui.components.ModuleComponent component : 
             clickGui.moduleComponents) {
            component.opened = isModuleOpened(component.module.getFileName());
        }
    }
}