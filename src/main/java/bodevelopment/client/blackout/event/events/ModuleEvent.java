package bodevelopment.client.blackout.event.events;

import bodevelopment.client.blackout.module.Module;

public class ModuleEvent {
    public final Module module;

    public ModuleEvent(Module module) {
        this.module = module;
    }

    public static class Enable extends ModuleEvent {
        public Enable(Module module) { super(module); }
    }

    public static class Disable extends ModuleEvent {
        public Disable(Module module) { super(module); }
    }
}