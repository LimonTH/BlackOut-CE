package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.gui.menu.Account;
import bodevelopment.client.blackout.interfaces.mixin.IMinecraftClient;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.util.FileUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.minecraft.client.session.Session;

import java.util.ArrayList;
import java.util.List;

public class AltManager extends Manager {
    private final List<Account> accounts = new ArrayList<>();
    public Account selected;
    public Account currentSession;
    private Session originalSession;
    private long lastSave = 0L;
    private boolean shouldSave = false;

    private boolean wasInWorld = false;

    @Override
    public void init() {
        this.originalSession = BlackOut.mc.getSession();

        this.selected = new Account(BlackOut.mc.getSession());
        this.currentSession = this.selected;
        BlackOut.EVENT_BUS.subscribe(this, () -> false);
        String path = "accounts.json";
        if (FileUtils.exists(path)) {
            JsonElement jsonElement = FileUtils.readElement(FileUtils.getFile(path));
            JsonObject jsonObject = jsonElement instanceof JsonNull ? new JsonObject() : (JsonObject) jsonElement;
            jsonObject.entrySet().forEach(entry -> {
                JsonElement element = entry.getValue();
                if (element instanceof JsonObject object) {
                    this.readData(object);
                } else {
                    this.getAccounts().add(new Account(entry.getKey(), null, null, "", null, null, Session.AccountType.MOJANG));
                }
            });
        } else {
            FileUtils.addFile(path);
        }

        this.save();
    }

    private void readData(JsonObject jsonObject) {
        this.add(new Account(jsonObject));
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        boolean inWorld = BlackOut.mc.world != null;

        if (!inWorld && wasInWorld) {
            switchToSelected();
            wasInWorld = false;
        }

        if (inWorld && !wasInWorld) {
            wasInWorld = true;
        }

        if (this.shouldSave && System.currentTimeMillis() - this.lastSave > 5000L) {
            this.shouldSave = false;
            JsonObject object = new JsonObject();
            this.getAccounts().forEach(account -> {
                JsonObject accountObject = account.asJson();
                if (accountObject != null) {
                    object.add(account.getScript(), accountObject);
                }
            });
            FileUtils.write(FileUtils.getFile("accounts.json"), object);
            this.lastSave = System.currentTimeMillis();
        }
    }

    public void add(Account account) {
        this.getAccounts().add(account);
        this.save();
    }

    public void remove(Account account) {
        this.getAccounts().remove(account);
        this.save();
    }

    public void set(Account account) {
        this.selected = account;
        if (BlackOut.mc.world == null) {
            this.switchToSelected();
        }
        this.save();
    }

    public void switchToOriginal() {
        if (originalSession != null) {
            ((IMinecraftClient) BlackOut.mc).blackout_Client$setSession(
                    originalSession.getUsername(),
                    originalSession.getUuidOrNull(),
                    originalSession.getAccessToken(),
                    originalSession.getXuid().orElse(null),
                    originalSession.getClientId().orElse(null),
                    originalSession.getAccountType()
            );
        }
    }

    public void switchToSelected() {
        if (this.selected != null) {
            this.selected.setSession();
        }
    }

    public void save() {
        this.shouldSave = true;
    }

    public List<Account> getAccounts() {
        return this.accounts;
    }
}
