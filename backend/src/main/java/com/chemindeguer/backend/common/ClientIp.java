package com.chemindeguer.backend.common;

import jakarta.servlet.http.HttpServletRequest;

/** nginx transmet l'IP réelle du visiteur dans X-Real-IP (le backend n'est pas joignable directement). */
public final class ClientIp {
    private ClientIp() {
    }

    public static String of(HttpServletRequest request) {
        String real = request.getHeader("X-Real-IP");
        return real != null && !real.isBlank() ? real : request.getRemoteAddr();
    }
}
