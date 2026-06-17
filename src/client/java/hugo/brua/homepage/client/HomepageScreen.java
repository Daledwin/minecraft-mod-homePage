package hugo.brua.homepage.client;

import hugo.brua.homepage.HomepageContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Random;

/**
 * Ecran d'accueil Bloomind : panneau clair et ethere (charte Bloomind), logo "B" + logotype violet,
 * sections "Derniers modes" / "Commandes de base", petales de cerisier qui tombent.
 *
 * Pieges 1.21.11 respectes :
 *  - PAS de renderBackground() manuel (le moteur l'appelle deja avant render() -> sinon crash "blur once per frame").
 *  - Couleurs en ARGB alpha plein (0xFF......) sinon texte invisible.
 *  - Fermeture par ESC geree par shouldCloseOnEsc() par defaut (pas d'override keyPressed/KeyEvent).
 */
public class HomepageScreen extends Screen {

    // --- Textures (rendues a leur taille d'affichage -> blit 1:1, pas d'overload de scaling) ---
    private static final Identifier ICON = tex("bloomind_icon");
    private static final Identifier WORDMARK = tex("bloomind_wordmark");
    private static final Identifier BLOSSOM = tex("blossom");
    private static final Identifier[] PETALS = {
            tex("petal_0"), tex("petal_1"), tex("petal_2"), tex("petal_3")
    };

    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath("homepage", "textures/gui/" + name + ".png");
    }

    // --- Palette Bloomind (ARGB, alpha plein) ---
    private static final int VIOLET = 0xFF8373FF;
    private static final int VIOLET_D = 0xFF5B4FCF;
    private static final int PINK = 0xFFFFB7C5;
    private static final int PINK_D = 0xFFE07898;
    private static final int CYAN = 0xFF5BC8FF;
    private static final int PANEL = 0xFFF7F5FF;
    private static final int BORDER = 0xFFD9D2F2;
    private static final int TEXT = 0xFF2E2A4A;
    private static final int MUTED = 0xFF6E6A85;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int SEP = 0x22000000;
    private static final int SHADOW = 0x40000000;

    // --- Dimensions de mise en page ---
    private static final int PANEL_W = 320;
    private static final int PAD = 14;
    private static final int STRIP_H = 4;
    private static final int ICON_W = 34, ICON_H = 34;
    private static final int WM_W = 133, WM_H = 30;
    private static final int BLOSSOM_SZ = 30;
    private static final int PETAL_SZ = 16;
    private static final int LOGO_TITLE_GAP = 6;
    private static final int TITLE_LINE = 12;
    private static final int SUBTITLE_H = 11;
    private static final int HEADER_GAP = 12;
    private static final int SEP_BEFORE = 8, SEP_AFTER = 9;
    private static final int SECTION_TITLE_H = 12, SECTION_TITLE_GAP = 5;
    private static final int MODE_ROW = 21, CMD_ROW = 13, MORE_H = 12;
    private static final int SECTION_GAP = 10;
    private static final int FOOTER_GAP = 14;
    private static final int BTN_W = 120, BTN_H = 20;
    private static final int MAX_MODES = 3, MAX_CMDS = 6;
    private static final int PETAL_COUNT = 14;

    private final HomepageContent content;
    private final boolean hasSubtitle;
    private final int modesShown, cmdsShown;

    private int panelX, panelY, panelW, panelH, btnY;

    // Animation des petales (parametres pseudo-aleatoires deterministes).
    private long startMillis;
    private final float[] pX = new float[PETAL_COUNT];
    private final float[] pY0 = new float[PETAL_COUNT];
    private final float[] pSpeed = new float[PETAL_COUNT];
    private final float[] pPhase = new float[PETAL_COUNT];
    private final float[] pSwayAmp = new float[PETAL_COUNT];
    private final float[] pSwayFreq = new float[PETAL_COUNT];
    private final int[] pSprite = new int[PETAL_COUNT];

    public HomepageScreen(HomepageContent content) {
        super(Component.literal("Home Page"));
        this.content = content;
        this.hasSubtitle = content.subtitle != null && !content.subtitle.isBlank();
        this.modesShown = Math.min(content.modes.size(), MAX_MODES);
        this.cmdsShown = Math.min(content.commands.size(), MAX_CMDS);
    }

    @Override
    protected void init() {
        panelW = Math.min(PANEL_W, this.width - 8);
        // Clamp a la hauteur visible : a GUI scale 3-4 le contenu peut depasser l'ecran
        // -> on borne le panneau et on clippe le contenu (cf. drawContent) pour garder le bouton accessible.
        panelH = Math.min(measureHeight(), this.height - 12);
        panelX = (this.width - panelW) / 2;
        panelY = Math.max(6, (this.height - panelH) / 2);

        Random r = new Random(0xB100D);
        for (int i = 0; i < PETAL_COUNT; i++) {
            pX[i] = r.nextFloat();
            pY0[i] = r.nextFloat();
            pSpeed[i] = 7f + r.nextFloat() * 15f;
            pPhase[i] = r.nextFloat() * 6.2832f;
            pSwayAmp[i] = 4f + r.nextFloat() * 9f;
            pSwayFreq[i] = 0.5f + r.nextFloat() * 0.8f;
            pSprite[i] = r.nextInt(PETALS.length);
        }
        this.startMillis = System.currentTimeMillis();

        int btnX = panelX + (panelW - BTN_W) / 2;
        // Toujours a l'ecran, meme si le panneau a ete borne plus petit que le contenu.
        btnY = Math.min(panelY + panelH - PAD - BTN_H, this.height - PAD - BTN_H);
        addRenderableWidget(Button.builder(Component.literal("Fermer"), b -> onClose())
                .bounds(btnX, btnY, BTN_W, BTN_H).build());
    }

    private int measureHeight() {
        int h = STRIP_H + PAD;
        h += ICON_H + LOGO_TITLE_GAP + TITLE_LINE;
        if (hasSubtitle) h += SUBTITLE_H;
        h += HEADER_GAP;
        h += SEP_BEFORE + 1 + SEP_AFTER;
        h += SECTION_TITLE_H + SECTION_TITLE_GAP + modesShown * MODE_ROW;
        if (content.modes.size() > modesShown) h += MORE_H;
        h += SECTION_GAP;
        h += SECTION_TITLE_H + SECTION_TITLE_GAP + cmdsShown * CMD_ROW;
        if (content.commands.size() > cmdsShown) h += MORE_H;
        h += FOOTER_GAP + BTN_H + PAD;
        return h;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float dt) {
        // (Le moteur a deja assombri l'arriere-plan avant cet appel : ne pas re-blurrer.)

        // Ombre portee + panneau + bordure.
        g.fill(panelX + 4, panelY + 4, panelX + panelW + 4, panelY + panelH + 4, SHADOW);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL);
        drawBorder(g, panelX, panelY, panelW, panelH, BORDER);

        // Bandeau degrade haut (cyan -> violet -> rose).
        hGradient3(g, panelX, panelY, panelW, STRIP_H, CYAN, VIOLET, PINK);

        // Fleurs de cerisier dans deux coins (debordent legerement, non clippees).
        tex(g, BLOSSOM, panelX - 9, panelY - 9, BLOSSOM_SZ, BLOSSOM_SZ);
        tex(g, BLOSSOM, panelX + panelW - BLOSSOM_SZ + 9, panelY + panelH - BLOSSOM_SZ + 9, BLOSSOM_SZ, BLOSSOM_SZ);

        // Petales qui tombent (clippes au panneau).
        drawPetals(g);

        // ---- Contenu (clippe entre le bandeau et le bouton, pour degrader proprement si l'ecran est petit) ----
        g.enableScissor(panelX, panelY + STRIP_H, panelX + panelW, btnY - 2);
        int cx = panelX + panelW / 2;
        int cy = panelY + STRIP_H + PAD;

        // Logo : icone "B" + logotype "Bloomind".
        int lockW = ICON_W + 8 + WM_W;
        int lx = panelX + (panelW - lockW) / 2;
        tex(g, ICON, lx, cy, ICON_W, ICON_H);
        tex(g, WORDMARK, lx + ICON_W + 8, cy + (ICON_H - WM_H) / 2, WM_W, WM_H);
        cy += ICON_H + LOGO_TITLE_GAP;

        // Titre + sous-titre (depuis la config).
        centered(g, content.title, cx, cy, VIOLET);
        cy += TITLE_LINE;
        if (hasSubtitle) {
            centered(g, content.subtitle, cx, cy, MUTED);
            cy += SUBTITLE_H;
        }
        cy += HEADER_GAP;

        // Separateur.
        cy += SEP_BEFORE;
        g.fill(panelX + PAD, cy, panelX + panelW - PAD, cy + 1, SEP);
        cy += 1 + SEP_AFTER;

        // Section "Derniers modes".
        sectionTitle(g, "Derniers modes", panelX + PAD, cy);
        cy += SECTION_TITLE_H + SECTION_TITLE_GAP;
        for (int i = 0; i < modesShown; i++) {
            drawMode(g, content.modes.get(i), panelX + PAD, cy);
            cy += MODE_ROW;
        }
        if (content.modes.size() > modesShown) {
            left(g, "+ " + (content.modes.size() - modesShown) + " autre(s) mode(s)…", panelX + PAD + 8, cy, MUTED);
            cy += MORE_H;
        }
        cy += SECTION_GAP;

        // Section "Commandes de base".
        sectionTitle(g, "Commandes de base", panelX + PAD, cy);
        cy += SECTION_TITLE_H + SECTION_TITLE_GAP;
        for (int i = 0; i < cmdsShown; i++) {
            drawCommand(g, content.commands.get(i), panelX + PAD, cy);
            cy += CMD_ROW;
        }
        if (content.commands.size() > cmdsShown) {
            left(g, "+ " + (content.commands.size() - cmdsShown) + " autre(s) commande(s)…", panelX + PAD + 8, cy, MUTED);
        }

        g.disableScissor();

        // Widgets (bouton Fermer) par-dessus.
        super.render(g, mouseX, mouseY, dt);
    }

    // ---- Briques de rendu ----

    private void drawPetals(GuiGraphics g) {
        float t = (System.currentTimeMillis() - startMillis) / 1000f;
        float range = panelH + PETAL_SZ * 2f;
        g.enableScissor(panelX, panelY, panelX + panelW, panelY + panelH);
        for (int i = 0; i < PETAL_COUNT; i++) {
            float y = panelY - PETAL_SZ + ((pY0[i] * range + t * pSpeed[i]) % range);
            float sway = (float) Math.sin(t * pSwayFreq[i] + pPhase[i]) * pSwayAmp[i];
            float x = panelX + pX[i] * (panelW - PETAL_SZ) + sway;
            texTinted(g, PETALS[pSprite[i]], Math.round(x), Math.round(y), PETAL_SZ, PETAL_SZ, 0x99FFFFFF);
        }
        g.disableScissor();
    }

    private void drawMode(GuiGraphics g, HomepageContent.Mode m, int x, int y) {
        g.fill(x, y + 1, x + 4, y + 5, VIOLET);                  // puce carree violette
        int nameX = x + 10;
        int badgeW = m.isNew ? font.width("NOUVEAU") + 6 + 6 : 0;   // boite badge + ecart
        String name = fit(m.name, panelW - PAD * 2 - 10 - badgeW); // tronque pour ne pas deborder
        g.drawString(font, name, nameX, y, TEXT, false);
        if (m.isNew) {
            badge(g, nameX + font.width(name) + 6, y - 1, "NOUVEAU", PINK_D);
        }
        if (!m.description.isEmpty()) {
            String d = fit(m.description, panelW - PAD * 2 - 10);
            g.drawString(font, d, nameX, y + 10, MUTED, false);
        }
    }

    private void drawCommand(GuiGraphics g, HomepageContent.Command c, int x, int y) {
        g.drawString(font, c.command, x, y, VIOLET_D, false);
        int dx = x + font.width(c.command) + 8;
        int avail = panelX + panelW - PAD - dx;
        if (!c.description.isEmpty() && avail > 0) {
            g.drawString(font, fit(c.description, avail), dx, y, MUTED, false);
        }
    }

    private void sectionTitle(GuiGraphics g, String title, int x, int y) {
        g.drawString(font, title, x, y, VIOLET, false);
        g.fill(x, y + TITLE_LINE - 1, x + font.width(title), y + TITLE_LINE, PINK);  // soulignement rose
    }

    private void badge(GuiGraphics g, int x, int y, String label, int bg) {
        int w = font.width(label) + 6;
        g.fill(x, y, x + w, y + 10, bg);
        g.drawString(font, label, x + 3, y + 1, WHITE, false);
    }

    private void centered(GuiGraphics g, String s, int cx, int y, int color) {
        g.drawString(font, s, cx - font.width(s) / 2, y, color, false);
    }

    private void left(GuiGraphics g, String s, int x, int y, int color) {
        g.drawString(font, s, x, y, color, false);
    }

    private String fit(String s, int maxW) {
        if (maxW <= 0) return "";
        if (font.width(s) <= maxW) return s;
        String ell = "…";
        if (font.width(ell) > maxW) return "";
        while (s.length() > 1 && font.width(s + ell) > maxW) {
            s = s.substring(0, s.length() - 1);
        }
        return s + ell;
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void hGradient3(GuiGraphics g, int x, int y, int w, int h, int c1, int c2, int c3) {
        for (int i = 0; i < w; i++) {
            float t = w <= 1 ? 0f : (float) i / (w - 1);
            int col = t < 0.5f ? lerp(c1, c2, t * 2f) : lerp(c2, c3, (t - 0.5f) * 2f);
            g.fill(x + i, y, x + i + 1, y + h, col);
        }
    }

    private static int lerp(int a, int b, float t) {
        int aa = (a >>> 24) & 0xFF, ar = (a >>> 16) & 0xFF, ag = (a >>> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >>> 16) & 0xFF, bg = (b >>> 8) & 0xFF, bb = b & 0xFF;
        int na = Math.round(aa + (ba - aa) * t);
        int nr = Math.round(ar + (br - ar) * t);
        int ng = Math.round(ag + (bg - ag) * t);
        int nb = Math.round(ab + (bb - ab) * t);
        return (na << 24) | (nr << 16) | (ng << 8) | nb;
    }

    private void tex(GuiGraphics g, Identifier id, int x, int y, int w, int h) {
        g.blit(RenderPipelines.GUI_TEXTURED, id, x, y, 0f, 0f, w, h, w, h);
    }

    private void texTinted(GuiGraphics g, Identifier id, int x, int y, int w, int h, int argb) {
        g.blit(RenderPipelines.GUI_TEXTURED, id, x, y, 0f, 0f, w, h, w, h, argb);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }
}
