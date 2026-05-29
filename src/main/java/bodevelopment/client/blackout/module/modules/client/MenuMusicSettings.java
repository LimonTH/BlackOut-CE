package bodevelopment.client.blackout.module.modules.client;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PlaySoundEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.SettingsModule;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.SoundUtils;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.sounds.ChannelAccess;

public class MenuMusicSettings extends SettingsModule {
    private static MenuMusicSettings INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Mode> mode = this.sgGeneral.enumSetting("Mode", Mode.Custom,
            "Vanilla: default Minecraft menu music. Custom: play your own tracks from the menu folder.");
    public final Setting<MusicTrack> track = this.sgGeneral.enumSetting("Track", MusicTrack.MoneyPhonk,
            "Selects the background music track to play in the main menu.",
            () -> this.mode.get() != Mode.Vanilla);
    public final Setting<Double> volume = this.sgGeneral.doubleSetting("Volume", 0.3, 0.0, 1.0, 0.01,
            "Controls the output volume of the menu music.",
            () -> this.mode.get() != Mode.Vanilla);
    public final Setting<Double> fadeDuration = this.sgGeneral.doubleSetting("Fade Duration", 1.5, 0.0, 10.0, 0.1,
            "Duration in seconds for fade-in and fade-out transitions.",
            () -> this.mode.get() != Mode.Vanilla);
    public final Setting<Integer> trackDuration = this.sgGeneral.intSetting("Track Duration", 180, 10, 600, 1,
            "How long the track plays before fading out. Set this to match your track length.",
            () -> this.mode.get() != Mode.Vanilla);
    public final Setting<Double> loopDelay = this.sgGeneral.doubleSetting("Loop Delay", 28.0, 0.0, 120.0, 0.5,
            "Seconds to wait after the track ends before playing it again.",
            () -> this.mode.get() != Mode.Vanilla);

    private ChannelAccess.ChannelHandle currentHandle;
    private MusicTrack currentTrack;

    private State state = State.Stopped;
    private double stateEnteredAt;
    private double volumeAtFadeStart;
    private double lastAppliedVolume = -1.0;
    private boolean restartAfterFade;
    private boolean wasPaused;

    public MenuMusicSettings() {
        super("Menu Music", true, true);
        INSTANCE = this;

        this.mode.onChanged(m -> {
            if (m == Mode.Custom) this.enterState(State.FadingIn, now(), false);
            else this.enterState(State.Stopped, now(), false);
        });
        this.track.onChanged(t -> {
            if (this.mode.get() == Mode.Custom && BlackOut.mc.screen instanceof TitleScreen) {
                this.enterState(State.FadingIn, now(), false);
            }
        });
    }

    public static MenuMusicSettings getInstance() {
        return INSTANCE;
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (this.mode.get() == Mode.Vanilla) {
            if (this.state != State.Stopped) {
                this.enterState(State.Stopped, now(), false);
            }
            return;
        }

        boolean paused = BlackOut.mc.isPaused();
        boolean inMenu = BlackOut.mc.screen instanceof TitleScreen;
        MusicTrack selectedTrack = this.track.get();
        boolean trackChanged = this.currentTrack != selectedTrack;
        double now = now();

        if (this.wasPaused && !paused && inMenu
                && (this.state == State.Playing || this.state == State.Waiting || this.state == State.FadingIn)) {
            this.enterState(State.FadingIn, now, false);
        }
        this.wasPaused = paused;

        if (paused) return;

        if (inMenu) {
            switch (this.state) {
                case Stopped -> this.enterState(State.FadingIn, now, false);
                case Playing -> {
                    if (trackChanged) {
                        this.enterState(State.FadingOut, now, true);
                    } else {
                        this.applyLiveVolume();
                        double elapsed = now - this.stateEnteredAt;
                        double duration = this.trackDuration.get();
                        double fade = this.fadeDuration.get();
                        if (elapsed >= duration - fade) {
                            this.enterState(State.FadingOut, now, true);
                        }
                    }
                }
                case FadingIn -> {
                    if (trackChanged) {
                        this.enterState(State.FadingOut, now, true);
                    } else {
                        this.tickFadeIn(now);
                    }
                }
                case FadingOut -> this.tickFadeOut(now);
                case Waiting -> {
                    if (trackChanged) {
                        this.enterState(State.FadingIn, now, false);
                    } else {
                        double elapsed = now - this.stateEnteredAt;
                        if (elapsed >= this.loopDelay.get()) {
                            this.enterState(State.FadingIn, now, false);
                        }
                    }
                }
            }
        } else {
            switch (this.state) {
                case Playing, FadingIn, Waiting -> this.enterState(State.FadingOut, now, false);
                case FadingOut -> this.tickFadeOut(now);
                default -> {}
            }
        }
    }

    @Event
    public void onSound(PlaySoundEvent event) {
        if (this.mode.get() == Mode.Vanilla) return;
        String path = event.sound.getLocation().getPath();
        if (path.startsWith("music.")) {
            event.setCancelled(true);
        }
    }

    private static double now() {
        return System.currentTimeMillis() / 1000.0;
    }

    private void enterState(State newState, double now, boolean restartAfter) {
        this.state = newState;
        this.stateEnteredAt = now;

        switch (newState) {
            case FadingIn -> {
                this.restartAfterFade = false;
                this.killSource();
                this.currentTrack = this.track.get();
                this.currentHandle = SoundUtils.play(1.0f, 0.0f, this.currentTrack.fileName);
                this.setVolume(0.0f);
            }
            case FadingOut -> {
                this.restartAfterFade = restartAfter;
                this.volumeAtFadeStart = this.volume.get();
            }
            case Waiting -> {}
            case Stopped -> {
                this.killSource();
                this.currentTrack = null;
            }
            default -> {}
        }
    }

    private void tickFadeIn(double now) {
        double elapsed = now - this.stateEnteredAt;
        double fade = Math.max(this.fadeDuration.get(), 0.01);
        double t = Math.min(elapsed / fade, 1.0);
        this.setVolume((float) (t * this.volume.get()));

        if (t >= 1.0) {
            this.state = State.Playing;
            this.stateEnteredAt = now;
        }
    }

    private void tickFadeOut(double now) {
        double elapsed = now - this.stateEnteredAt;
        double fade = Math.max(this.fadeDuration.get(), 0.01);
        double t = Math.min(elapsed / fade, 1.0);
        this.setVolume((float) ((1.0 - t) * this.volumeAtFadeStart));

        if (t >= 1.0) {
            if (this.restartAfterFade) {
                this.enterState(State.Waiting, now, false);
            } else {
                this.enterState(State.Stopped, now, false);
            }
        }
    }

    private void setVolume(float vol) {
        if (this.currentHandle != null) {
            this.currentHandle.execute(source -> source.setVolume(vol));
        }
    }

    private void applyLiveVolume() {
        double target = this.volume.get();
        if (target != this.lastAppliedVolume) {
            this.lastAppliedVolume = target;
            this.setVolume((float) target);
        }
    }

    private void killSource() {
        if (this.currentHandle != null) {
            SoundUtils.stop(this.currentHandle);
            this.currentHandle = null;
        }
    }

    private enum State {
        Stopped,
        FadingIn,
        Playing,
        FadingOut,
        Waiting
    }

    public enum Mode {
        Vanilla,
        Custom
    }

    public enum MusicTrack {
        MoneyPhonk("money_phonk");
        public final String fileName;

        MusicTrack(String fileName) {
            this.fileName = "menu/" + fileName;
        }
    }
}
