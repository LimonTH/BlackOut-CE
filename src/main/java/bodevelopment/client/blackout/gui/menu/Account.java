package bodevelopment.client.blackout.gui.menu;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.interfaces.mixin.IMinecraftClient;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.util.render.RenderUtils;
import com.google.gson.JsonObject;
import net.minecraft.client.session.Session;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Uuids;

import java.awt.*;
import java.util.UUID;

public class Account {
    public static final float WIDTH = 500.0F;
    public static final float HEIGHT = 65.0F;
    private float progress = 0.0F;
    private float pulse = 0.0F;
    private String name;
    private String script;
    private UUID uuid;
    private String accessToken;
    private String xuid;
    private String clientId;
    private Session.AccountType accountType;

    public Account(String script) {
        this.script = (script == null || script.isEmpty()) ? "NewAccount" : script;
        String parsed = AccountScriptReader.nameFromScript(this.script);
        this.name = (parsed == null || parsed.equals("null")) ? this.script : parsed;

        this.uuid = Uuids.getOfflinePlayerUuid(this.name);
        this.accessToken = "";
        this.xuid = null;
        this.clientId = null;

        this.accountType = Session.AccountType.LEGACY;
    }

    public Account(Session session) {
        this.name = session.getUsername();
        this.script = session.getUsername();
        this.uuid = session.getUuidOrNull();
        this.accessToken = session.getAccessToken();
        this.xuid = session.getXuid().orElse(null);
        this.clientId = session.getClientId().orElse(null);
        this.accountType = session.getAccountType();
        this.progress = 1.0F;
    }

    public Account(JsonObject object) {
        if (object.has("name")) this.name = object.get("name").getAsString();
        if (object.has("script")) this.script = object.get("script").getAsString();

        if (object.has("uuid") && object.get("uuid").isJsonObject()) {
            JsonObject uuidObject = object.getAsJsonObject("uuid");
            this.uuid = new UUID(uuidObject.get("mostSigBits").getAsLong(), uuidObject.get("leastSigBits").getAsLong());
        }

        if (object.has("accessToken")) this.accessToken = object.get("accessToken").getAsString();

        this.xuid = getStringOrNull(object, "xuid");
        this.clientId = getStringOrNull(object, "clientId");

        if (object.has("accountType")) {
            this.accountType = this.getAccountType(object.get("accountType").getAsString());
        }
    }

    public Account(String name, String script, UUID uuid, String accessToken, String xuid, String clientId, Session.AccountType accountType) {
        this.script = (script == null || script.isEmpty()) ? "NewAccount" : script;
        String parsedName = AccountScriptReader.nameFromScript(this.script);
        this.name = (parsedName == null || parsedName.isEmpty() || parsedName.equals("null")) ? this.script : parsedName;
        this.uuid = uuid;
        this.accessToken = accessToken;
        this.xuid = xuid;
        this.clientId = clientId;
        this.accountType = accountType;
    }

    private String getStringOrNull(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return null;
    }

    public String getName() {
        return this.name;
    }

    public String getScript() {
        return this.script;
    }

    public AccountClickResult onClick(float clickX, float clickY, int button, boolean pressed) {
        if (!pressed) return AccountClickResult.Nothing;
        if (clickX >= 0 && clickX <= WIDTH && clickY >= 0 && clickY <= HEIGHT) {
            return switch (button) {
                case 0 -> AccountClickResult.Select;
                case 1 -> AccountClickResult.Refresh;
                case 2 -> AccountClickResult.Delete;
                default -> AccountClickResult.Nothing;
            };
        }
        return AccountClickResult.Nothing;
    }

    public void render(MatrixStack stack, float x, float y, float delta) {
        stack.push();
        stack.translate(x, y, 0.0F);
        this.updateProgress(delta * 2.0F);

        Color nameColor = this.equals(Managers.ALT.selected) ? new Color(130, 255, 130) : Color.WHITE;

        RenderUtils.drawLoadedBlur("title", stack, renderer ->
                renderer.rounded(0.0F, 0.0F, WIDTH, HEIGHT, 25.0F, 10, 1.0F, 1.0F, 1.0F, 1.0F));

        if (this.progress > 0.0F) {
            int alpha = (int) (this.progress * 180);
            RenderUtils.rounded(stack, -1.0F, -1.0F, WIDTH + 2.0F, HEIGHT + 2.0F, 26.0F, 2.0F,
                    new Color(100, 255, 100, alpha).getRGB(), new Color(100, 255, 100, 0).getRGB());
        }

        RenderUtils.rounded(stack, 0.0F, 0.0F, WIDTH, HEIGHT, 25.0F, 10.0F,
                new Color(20, 20, 20, 160).getRGB(), new Color(10, 10, 10, 225).getRGB());

        if (this.pulse > 0.0F) {
            int pulseAlpha = (int) (this.pulse * 100);
            RenderUtils.rounded(stack, 0.0F, 0.0F, WIDTH, HEIGHT, 25.0F, 5.0F,
                    new Color(255, 255, 255, pulseAlpha).getRGB(),
                    new Color(255, 255, 255, 0).getRGB());
        }

        float textOffset = 28.0F;
        BlackOut.BOLD_FONT.text(stack, this.name, 3.0F, textOffset, 8.0F, nameColor, false, false);

        float subY = 40.0F;
        if (!this.script.equals(this.name)) {
            float labelWidth = BlackOut.FONT.getWidth("Script: ") * 1.8F;
            BlackOut.FONT.text(stack, "Script: ", 1.8F, textOffset, subY, new Color(160, 160, 160), false, false);
            BlackOut.FONT.text(stack, this.script, 1.8F, textOffset + labelWidth, subY, Color.WHITE, false, false);
        } else {
            String typeTag = switch (this.accountType) {
                case MSA -> "Microsoft";
                case MOJANG -> "Mojang";
                case LEGACY -> "Cracked";
            };
            Color typeColor = switch (this.accountType) {
                case MSA -> new Color(0, 200, 255);
                case MOJANG -> new Color(255, 160, 0);
                default -> new Color(120, 120, 120);
            };
            BlackOut.FONT.text(stack, typeTag, 1.8F, textOffset, subY, typeColor, false, false);
        }

        String shortUuid = "#" + this.uuid.toString().substring(0, 8).toUpperCase();
        BlackOut.FONT.text(stack, shortUuid, 1.3F, WIDTH - 110.0F, HEIGHT - 22.0F, new Color(255, 255, 255, 35), false, false);

        this.pulse = Math.max(this.pulse - delta, 0.0F);
        stack.pop();
    }

    private void updateProgress(float delta) {
        if (this.equals(Managers.ALT.selected)) {
            this.progress = Math.min(this.progress + delta, 1.0F);
        } else {
            this.progress = Math.max(this.progress - delta, 0.0F);
        }
    }

    public void refresh() {
        this.name = AccountScriptReader.nameFromScript(this.script);
        this.uuid = Uuids.getOfflinePlayerUuid(this.name);
        Managers.ALT.save();
        this.pulse = 1.0F;
    }

    public void setAccess(Session session) {
        this.accessToken = session.getAccessToken();
        this.xuid = session.getXuid().orElse(null);
        this.clientId = session.getClientId().orElse(null);
        Managers.ALT.save();
    }

    public void setSession() {
        ((IMinecraftClient) BlackOut.mc).blackout_Client$setSession(this.name, this.uuid, this.accessToken, this.xuid, this.clientId, this.accountType);
    }

    private Session.AccountType getAccountType(String from) {
        String sessionType = from.toLowerCase();

        return switch (sessionType) {
            case "msa" -> Session.AccountType.MSA;
            case "legacy" -> Session.AccountType.LEGACY;
            case "mojang" -> Session.AccountType.MOJANG;
            default ->
                    throw new IllegalStateException("Unexpected account type: " + from + " name: " + this.name + " script: " + this.script);
        };
    }

    public JsonObject asJson() {
        if (this.name == null && this.script != null) {
            this.name = AccountScriptReader.nameFromScript(this.script);
        }
        if (this.name == null) return null;

        JsonObject object = new JsonObject();
        object.addProperty("script", this.script);
        object.addProperty("name", this.name);
        object.addProperty("accessToken", this.accessToken);

        JsonObject uuidObject = new JsonObject();
        uuidObject.addProperty("mostSigBits", this.uuid.getMostSignificantBits());
        uuidObject.addProperty("leastSigBits", this.uuid.getLeastSignificantBits());
        object.add("uuid", uuidObject);

        object.addProperty("xuid", this.xuid);
        object.addProperty("clientId", this.clientId);
        object.addProperty("accountType", this.accountType.getName());

        return object;
    }

    public enum AccountClickResult {
        Nothing,
        Select,
        Delete,
        Refresh
    }
}
