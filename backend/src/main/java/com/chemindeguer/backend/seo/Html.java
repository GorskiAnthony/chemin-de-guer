package com.chemindeguer.backend.seo;

/** Échappements pour injecter des valeurs dans du HTML ou du JSON-LD sans risque d'injection. */
public final class Html {
    private Html() {
    }

    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    /** JSON destiné à un bloc <script> : « < » est échappé pour empêcher de refermer la balise. */
    public static String jsonForScript(String json) {
        return json.replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026").replace("\u2028", "\\u2028").replace("\u2029", "\\u2029");
    }
}
