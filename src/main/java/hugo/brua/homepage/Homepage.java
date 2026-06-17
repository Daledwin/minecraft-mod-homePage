package hugo.brua.homepage;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Point d'entree commun/serveur du mod Home Page.
 * Enregistre le type de payload (S2C), pousse l'ecran d'accueil a la connexion (si du nouveau)
 * et expose la commande /homepage.
 */
public class Homepage implements ModInitializer {
    public static final String MOD_ID = "homepage";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static SeenStore seenStore;

    @Override
    public void onInitialize() {
        // Type de payload : a enregistrer cote commun (le client doit connaitre le codec).
        PayloadTypeRegistry.playS2C().register(HomepagePayload.TYPE, HomepagePayload.CODEC);

        seenStore = SeenStore.load();

        // A la connexion : afficher seulement si le contenu a change depuis la derniere vue du joueur.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            HomepageContent content = HomepageConfig.load();
            String hash = content.contentHash();
            if (seenStore.isNew(player.getUUID(), hash) && sendTo(player, content)) {
                seenStore.markSeen(player.getUUID(), hash);
            }
        });

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> registerCommands(dispatcher));

        LOGGER.info("[homepage] initialise (cote serveur/commun).");
    }

    /** Envoie le payload d'accueil si le client a le mod (sinon : rien, cf. clients vanilla). Retourne true si envoye. */
    private static boolean sendTo(ServerPlayer player, HomepageContent content) {
        if (!ServerPlayNetworking.canSend(player, HomepagePayload.TYPE)) {
            return false;
        }
        ServerPlayNetworking.send(player, new HomepagePayload(content.toCompactJson()));
        return true;
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("homepage")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    HomepageContent content = HomepageConfig.load();
                    if (sendTo(player, content)) {
                        seenStore.markSeen(player.getUUID(), content.contentHash());
                    } else {
                        ctx.getSource().sendFailure(Component.literal(
                                "L'ecran d'accueil necessite le mod Home Page installe cote client."));
                    }
                    return 1;
                })
                .then(Commands.literal("reload")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> {
                            HomepageContent content = HomepageConfig.load();
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "[Home Page] config rechargee : " + content.modes.size()
                                            + " mode(s), " + content.commands.size() + " commande(s)."), false);
                            return 1;
                        })));
    }
}
