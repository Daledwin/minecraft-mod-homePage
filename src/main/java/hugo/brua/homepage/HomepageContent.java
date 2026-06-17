package hugo.brua.homepage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Contenu de l'ecran d'accueil : titre, derniers modes du serveur et commandes de base.
 * Defini par l'admin dans {@code config/homepage.json}, serialise en JSON pour etre pousse au client.
 * Code commun (src/main) : le serveur le construit, le client le relit.
 */
public class HomepageContent {
    public String title = "Bienvenue";
    public String subtitle = "";
    public List<Mode> modes = new ArrayList<>();
    public List<Command> commands = new ArrayList<>();

    public static class Mode {
        public String name = "";
        public String description = "";
        /** Affiche un badge "NOUVEAU" cote client. */
        public boolean isNew = false;

        public Mode() {}

        public Mode(String name, String description, boolean isNew) {
            this.name = name;
            this.description = description;
            this.isNew = isNew;
        }
    }

    public static class Command {
        public String command = "";
        public String description = "";

        public Command() {}

        public Command(String command, String description) {
            this.command = command;
            this.description = description;
        }
    }

    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Gson COMPACT = new GsonBuilder().disableHtmlEscaping().create();

    /** JSON lisible pour le fichier de config. */
    public String toPrettyJson() {
        return PRETTY.toJson(this);
    }

    /** JSON compact pour le transport reseau. */
    public String toCompactJson() {
        return COMPACT.toJson(this);
    }

    public static HomepageContent fromJson(String json) {
        HomepageContent c;
        try {
            c = COMPACT.fromJson(json, HomepageContent.class);
        } catch (Exception e) {
            Homepage.LOGGER.warn("[homepage] JSON invalide ({}), contenu par defaut applique.", e.getMessage());
            c = null;
        }
        if (c == null) {
            c = new HomepageContent();
        }
        // Defense : un JSON partiel peut laisser des champs null.
        if (c.title == null) c.title = "Bienvenue";
        if (c.subtitle == null) c.subtitle = "";
        if (c.modes == null) c.modes = new ArrayList<>();
        if (c.commands == null) c.commands = new ArrayList<>();
        c.modes.removeIf(m -> m == null || m.name == null);
        c.commands.removeIf(cm -> cm == null || cm.command == null);
        for (Mode m : c.modes) {
            if (m.description == null) m.description = "";
        }
        for (Command cm : c.commands) {
            if (cm.description == null) cm.description = "";
        }
        return c;
    }

    /**
     * Empreinte stable du contenu, pour la regle "n'afficher que si du nouveau".
     * SHA-256 hex : deterministe entre redemarrages et sans risque de collision (contrairement a un int hashCode).
     */
    public String contentHash() {
        String json = toCompactJson();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(json.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            // SHA-256 est garanti present sur toute JVM ; repli defensif.
            return Integer.toHexString(json.hashCode());
        }
    }
}
