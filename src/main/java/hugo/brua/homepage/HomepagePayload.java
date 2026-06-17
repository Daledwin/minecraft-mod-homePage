package hugo.brua.homepage;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Payload S2C qui transporte le contenu de l'ecran d'accueil au client, sous forme de JSON compact.
 * Un seul champ String -> codec trivial (STRING_UTF8), pas de risque de codec composite.
 */
public record HomepagePayload(String json) implements CustomPacketPayload {

    // ATTENTION 1.21.11 : NE PAS utiliser createType("...") (interprete l'arg comme chemin seul,
    // namespace "minecraft" -> IdentifierException au boot). On construit le Type explicitement.
    public static final CustomPacketPayload.Type<HomepagePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Homepage.MOD_ID, "open"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HomepagePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, HomepagePayload::json,
                    HomepagePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
