package hugo.brua.homepage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Ecriture atomique : ecrit dans un fichier temporaire voisin puis le deplace en place.
 * Garantit qu'un lecteur voit toujours l'ancien fichier complet ou le nouveau complet,
 * jamais un fichier tronque (crash/coupure en plein ecriture).
 */
public final class AtomicWrite {

    private AtomicWrite() {}

    public static void write(Path target, String content) throws IOException {
        Path parent = target.getParent();
        if (parent == null) {
            Files.writeString(target, content, StandardCharsets.UTF_8);
            return;
        }
        Files.createDirectories(parent);
        Path tmp = Files.createTempFile(parent, "homepage", ".tmp");
        try {
            Files.writeString(tmp, content, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
