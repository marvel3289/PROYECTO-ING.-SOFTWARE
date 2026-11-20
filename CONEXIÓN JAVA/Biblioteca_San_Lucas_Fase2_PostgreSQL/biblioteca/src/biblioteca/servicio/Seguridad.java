package biblioteca.servicio;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Hash de contraseñas con PBKDF2 y sal aleatoria (RNF-02). Nunca se guarda texto plano. */
public final class Seguridad {
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private Seguridad() {}

    public static String nuevaSal() {
        byte[] b = new byte[16];
        ALEATORIO.nextBytes(b);
        return Base64.getEncoder().encodeToString(b);
    }
    public static String hash(String contrasena, String sal) {
        try {
            var spec = new PBEKeySpec(contrasena.toCharArray(), Base64.getDecoder().decode(sal), 120_000, 256);
            byte[] r = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(r);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
    public static boolean verificar(String contrasena, String sal, String hashGuardado) {
        return MessageDigest.isEqual(hash(contrasena, sal).getBytes(), hashGuardado.getBytes());
    }
}
