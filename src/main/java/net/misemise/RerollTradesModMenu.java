package net.misemise;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.misemise.client.RerollConfigScreen;

public final class RerollTradesModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return RerollConfigScreen::new;
    }
}
