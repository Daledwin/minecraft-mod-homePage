package hugo.brua.homepage;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Lecture/ecriture de {@code config/homepage.json}. Cree un exemple par defaut au premier lancement.
 * Lu a la demande (a chaque connexion / commande) -> toujours frais, pas de cache a invalider.
 */
public final class HomepageConfig {

    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("homepage.json");

    private HomepageConfig() {}

    public static Path path() {
        return PATH;
    }

    /** Charge le contenu depuis le disque ; cree le fichier par defaut s'il manque, retourne les defauts si illisible. */
    public static HomepageContent load() {
        try {
            if (!Files.exists(PATH)) {
                HomepageContent def = defaults();
                save(def);
                Homepage.LOGGER.info("[homepage] config par defaut creee : {}", PATH);
                return def;
            }
            String json = Files.readString(PATH, StandardCharsets.UTF_8);
            return HomepageContent.fromJson(json);
        } catch (Exception e) {
            Homepage.LOGGER.error("[homepage] lecture config impossible ({}), valeurs par defaut utilisees.", PATH, e);
            return defaults();
        }
    }

    public static void save(HomepageContent c) {
        try {
            AtomicWrite.write(PATH, c.toPrettyJson());
        } catch (IOException e) {
            Homepage.LOGGER.error("[homepage] ecriture config impossible : {}", PATH, e);
        }
    }

    private static HomepageContent defaults() {
        HomepageContent c = new HomepageContent();
        c.title = "Bienvenue sur le serveur";
        c.subtitle = "Que la floraison commence";
        c.modes.add(new HomepageContent.Mode("SkyWars", "Combat aerien sur des iles flottantes", true));
        c.modes.add(new HomepageContent.Mode("Build Battle", "Construis mieux que tes rivaux", true));
        c.modes.add(new HomepageContent.Mode("Survie 1.21", "Le monde principal, fraichement reinitialise", false));
        c.commands.add(new HomepageContent.Command("/spawn", "Retour au point de depart"));
        c.commands.add(new HomepageContent.Command("/homepage", "Reouvrir cet ecran d'accueil"));
        c.commands.add(new HomepageContent.Command("/help", "Liste complete des commandes"));
        return c;
    }
}
