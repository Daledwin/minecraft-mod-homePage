package hugo.brua.homepage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Suivi par joueur du dernier hash de contenu vu, persiste dans {@code config/homepage-seen.json}.
 * Permet la regle "n'afficher l'ecran que si le contenu a change depuis la derniere vue du joueur".
 * Evite volontairement l'API SavedData (pieges runtime 1.21.11) : simple fichier JSON UUID -> hash.
 */
public final class SeenStore {

    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("homepage-seen.json");
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<HashMap<String, String>>() {}.getType();

    private final Map<String, String> seen;

    private SeenStore(Map<String, String> seen) {
        this.seen = seen;
    }

    public static SeenStore load() {
        try {
            if (Files.exists(PATH)) {
                String json = Files.readString(PATH, StandardCharsets.UTF_8);
                Map<String, String> m = GSON.fromJson(json, MAP_TYPE);
                if (m != null) {
                    return new SeenStore(new HashMap<>(m));
                }
            }
        } catch (Exception e) {
            Homepage.LOGGER.warn("[homepage] seen-store illisible, on repart de zero.", e);
        }
        return new SeenStore(new HashMap<>());
    }

    /** Vrai si ce joueur n'a pas encore vu cette empreinte de contenu. */
    public synchronized boolean isNew(UUID player, String contentHash) {
        return !Objects.equals(seen.get(player.toString()), contentHash);
    }

    public synchronized void markSeen(UUID player, String contentHash) {
        if (Objects.equals(seen.get(player.toString()), contentHash)) {
            return; // rien a reecrire
        }
        seen.put(player.toString(), contentHash);
        save();
    }

    private void save() {
        try {
            AtomicWrite.write(PATH, GSON.toJson(seen));
        } catch (Exception e) {
            Homepage.LOGGER.error("[homepage] ecriture seen-store impossible : {}", PATH, e);
        }
    }
}
