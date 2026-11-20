package biblioteca;

/** Abre el servidor web aunque se pulse Run sobre Main desde Eclipse. */
public final class Main {
    private Main() {}

    public static void main(String[] args) throws Exception {
        biblioteca.web.WebServidor.main(args);
    }
}
