package com.chemindeguer.backend.seo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HtmlTest {
    @Test
    void echappeLesCaracteresDangereux() {
        assertEquals("&lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt; &amp; l&#39;a", Html.esc("<script>alert(\"x\")</script> & l'a"));
        assertEquals("", Html.esc(null));
    }

    @Test
    void jsonDansUneBaliseScriptNePeutPasLaRefermer() {
        String out = Html.jsonForScript("{\"headline\":\"</script><script>alert(1)</script>\"}");
        assertFalse(out.contains("</script>"));
        assertFalse(out.contains("<"));
        assertTrue(out.contains("\\u003c/script\\u003e"));
    }
}
