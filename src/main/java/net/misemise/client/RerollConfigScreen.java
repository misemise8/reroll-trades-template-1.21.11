package net.misemise.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.misemise.config.RerollClientConfig;

public final class RerollConfigScreen extends Screen {

    private static final int CONTROL_WIDTH = 220;
    private final Screen parent;

    public RerollConfigScreen(Screen parent) {
        super(Component.translatable("config.reroll-trades.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        RerollClientConfig config = RerollClientConfig.get();
        int x = (this.width - CONTROL_WIDTH) / 2;
        int startY = Math.max(34, (this.height - 170) / 2);

        this.addRenderableOnly(new StringWidget(
                x,
                startY - 28,
                CONTROL_WIDTH,
                20,
                this.title,
                this.font
        ));

        addToggle(x, startY, "config.reroll-trades.show_reroll_button", config.showRerollButton,
                value -> config.showRerollButton = value);
        addToggle(x, startY + 24, "config.reroll-trades.show_undo_button", config.showUndoButton,
                value -> config.showUndoButton = value);
        addToggle(x, startY + 48, "config.reroll-trades.show_key_hint", config.showKeyHint,
                value -> config.showKeyHint = value);
        addToggle(x, startY + 72, "config.reroll-trades.enable_particles", config.enableParticles,
                value -> config.enableParticles = value);
        addToggle(x, startY + 96, "config.reroll-trades.enable_sounds", config.enableSounds,
                value -> config.enableSounds = value);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(x, startY + 128, CONTROL_WIDTH, 20)
                .build());
    }

    @Override
    public void onClose() {
        RerollClientConfig.saveCurrent();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    private void addToggle(int x, int y, String translationKey, boolean initialValue, BooleanSetter setter) {
        this.addRenderableWidget(CycleButton.onOffBuilder(initialValue)
                .create(
                        x,
                        y,
                        CONTROL_WIDTH,
                        20,
                        Component.translatable(translationKey),
                        (button, value) -> setter.set(value)
                ));
    }

    @FunctionalInterface
    private interface BooleanSetter {
        void set(boolean value);
    }
}
