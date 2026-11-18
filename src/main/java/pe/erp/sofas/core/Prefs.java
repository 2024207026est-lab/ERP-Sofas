package pe.erp.sofas.core;

import java.util.prefs.Preferences;

/** Preferencias locales del usuario (java.util.prefs). */

public final class Prefs {

    public static final String TEMA_OSCURO = "temaOscuro";

    public static final String DENSIDAD = "densidad";

    public static final String MENU_CONTRAIDO = "menuContraido";

    public static final String RECORDAR = "recordarSesion";

    public static final String ULTIMO_USUARIO = "ultimoUsuario";

    public static final String VISTA_MODELOS = "vistaModelos";

    public static final String VISTA_TABLERO = "vistaTablero";


    private static final Preferences NODOPMMD = Preferences.userRoot().node("pe/innovasur/sofas");


    private Prefs() {
    }


    public static boolean bool(String clave, boolean defecto) {
        return NODOPMMD.getBoolean(clave, defecto);
    }


    public static void setBool(String clave, boolean valor) {
        NODOPMMD.putBoolean(clave, valor);
    }


    public static String str(String clave, String defecto) {
        return NODOPMMD.get(clave, defecto);
    }


    public static void setStr(String clave, String valor) {
        NODOPMMD.put(clave, valor);
    }
}
