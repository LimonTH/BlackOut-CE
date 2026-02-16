package bodevelopment.client.blackout.gui.menu;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.interfaces.mixin.IMinecraftClient;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.util.render.RenderUtils;
import com.google.gson.JsonObject;
import net.minecraft.client.session.Session;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Uuids;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Optional;
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
    private Optional<String> xuid;
    private Optional<String> clientId;
    private Session.AccountType accountType;

    public Account(String script) {
        this.script = (script == null || script.isEmpty()) ? "NewAccount" : script;
        String parsed = AccountScriptReader.nameFromScript(this.script);
        this.name = (parsed == null || parsed.equals("null")) ? this.script : parsed;

        this.uuid = Uuids.getOfflinePlayerUuid(this.name);
        this.accessToken = "";
        this.xuid = Optional.empty();
        this.clientId = Optional.empty();

        this.accountType = Session.AccountType.LEGACY;
    }

    public Account(Session session) {
        this.name = session.getUsername();
        this.script = session.getUsername();
        this.uuid = session.getUuidOrNull();
        this.accessToken = session.getAccessToken();
        this.xuid = session.getXuid();
        this.clientId = session.getClientId();
        this.accountType = session.getAccountType();
        this.progress = 1.0F;
    }

    public Account(JsonObject object) {
        if (object.has("name")) {
            this.name = object.get("name").getAsString();
        }

        if (object.has("script")) {
            this.script = object.get("script").getAsString();
        }

        if (object.has("uuid") && object.get("uuid") instanceof JsonObject uuidObject && uuidObject.has("mostSigBits") && uuidObject.has("leastSigBits")) {
            this.uuid = new UUID(uuidObject.get("mostSigBits").getAsLong(), uuidObject.get("leastSigBits").getAsLong());
        }

        if (object.has("accessToken")) {
            this.accessToken = object.get("accessToken").getAsString();
        }

        if (object.has("xuid")) {
            this.xuid = this.readOptional(object.get("xuid").getAsString());
        }

        if (object.has("clientId")) {
            this.clientId = this.readOptional(object.get("clientId").getAsString());
        }

        if (object.has("accountType")) {
            this.accountType = this.getAccountType(object.get("accountType").getAsString());
        }
    }

    public Account(String name, String script, UUID uuid, String accessToken, Optional<String> xuid, Optional<String> clientId, Session.AccountType accountType) {
        this.script = (script == null || script.isEmpty()) ? "NewAccount" : script;
        String parsedName = AccountScriptReader.nameFromScript(this.script);
        this.name = (parsedName == null || parsedName.isEmpty() || parsedName.equals("null")) ? this.script : parsedName;
        this.uuid = uuid;
        this.accessToken = accessToken;
        this.xuid = xuid;
        this.clientId = clientId;
        this.accountType = accountType;
    }

    public String getName() {
        return this.name;
    }

    public String getScript() {
        return this.script;
    }

    public AccountClickResult onClick(float clickX, float clickY, int button, boolean pressed) {
        if (!pressed) return AccountClickResult.Nothing;

        // Просто проверяем, входят ли координаты клика в границы карточки
        if (clickX >= 0 && clickX <= WIDTH && clickY >= 0 && clickY <= HEIGHT) {
            return switch (button) {
                case 0 -> AccountClickResult.Select;  // ЛКМ
                case 1 -> AccountClickResult.Refresh; // ПКМ
                case 2 -> AccountClickResult.Delete;  // Колесико
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

        // 1. Блюр и Фон (радиус 25)
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
            int pulseAlpha = (int) (this.pulse * 100); // Прозрачность вспышки
            RenderUtils.rounded(stack, 0.0F, 0.0F, WIDTH, HEIGHT, 25.0F, 5.0F,
                    new Color(255, 255, 255, pulseAlpha).getRGB(),
                    new Color(255, 255, 255, 0).getRGB());
        }

        // 2. ОПТИМАЛЬНО КРУПНЫЙ ТЕКСТ
        float textOffset = 28.0F;

        // НИК (Размер 3.0F — золотая середина)
        BlackOut.BOLD_FONT.text(stack, this.name, 3.0F, textOffset, 8.0F, nameColor, false, false);

        // ПОДЗАГОЛОВОК (Размер 1.8F)
        String typeTag;
        Color typeColor;

        float subY = 40.0F;
        if (!this.script.equals(this.name)) {
            float labelWidth = BlackOut.FONT.getWidth("Script: ") * 1.8F;
            BlackOut.FONT.text(stack, "Script: ", 1.8F, textOffset, subY, new Color(160, 160, 160), false, false);
            BlackOut.FONT.text(stack, this.script, 1.8F, textOffset + labelWidth, subY, Color.WHITE, false, false);
        } else {
            typeTag = switch (this.accountType) {
                case MSA -> "Microsoft";
                case MOJANG -> "Mojang";
                case LEGACY -> "Cracked";
            };
            typeColor = switch (this.accountType) {
                case MSA -> new Color(0, 200, 255);
                case MOJANG -> new Color(255, 160, 0);
                default -> new Color(120, 120, 120);
            };
            BlackOut.FONT.text(stack, typeTag, 1.8F, textOffset, subY, typeColor, false, false);
        }

        // UUID (Размер 1.3F)
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
        this.xuid = session.getXuid();
        this.clientId = session.getClientId();
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
        if (this.name == null) {
            if (this.script == null) {
                return null;
            }

            this.name = AccountScriptReader.nameFromScript(this.script);
        }

        JsonObject object = new JsonObject();
        object.addProperty("script", this.nullable(this.script));
        object.addProperty("name", this.name);
        object.addProperty("accessToken", this.accessToken);
        JsonObject uuidObject = new JsonObject();
        uuidObject.addProperty("mostSigBits", this.uuid.getMostSignificantBits());
        uuidObject.addProperty("leastSigBits", this.uuid.getLeastSignificantBits());
        object.add("uuid", uuidObject);
        object.addProperty("xuid", this.getFromOptional(this.xuid));
        object.addProperty("clientId", this.getFromOptional(this.clientId));
        object.addProperty("accountType", this.nullable(this.accountType.getName()));
        return object;
    }

    private String nullable(@Nullable String string) {
        return string == null ? "<NULL>" : string;
    }

    private String getFromOptional(Optional<String> optional) {
        try {
            return optional.orElse("<EMPTY>");
        } catch (Exception e) {
            System.out.println(this.name);
            throw new RuntimeException(e);
        }
    }

    private Optional<String> readOptional(String string) {
        return string.equals("<EMPTY>") ? Optional.empty() : Optional.of(string);
    }

    public enum AccountClickResult {
        Nothing,
        Select,
        Delete,
        Refresh
    }
}
