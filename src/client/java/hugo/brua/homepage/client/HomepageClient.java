package hugo.brua.homepage.client;

import hugo.brua.homepage.HomepageContent;
import hugo.brua.homepage.HomepagePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Point d'entree client : recoit le payload d'accueil et ouvre l'ecran Bloomind.
 */
public class HomepageClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(HomepagePayload.TYPE, (payload, context) -> {
            // Le handler tourne sur le thread reseau -> repasser sur le thread client pour toucher l'UI.
            HomepageContent content = HomepageContent.fromJson(payload.json());
            context.client().execute(() -> context.client().setScreen(new HomepageScreen(content)));
        });
    }
}
