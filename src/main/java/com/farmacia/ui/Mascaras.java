package com.farmacia.ui;

import javafx.scene.control.TextField;

// Máscaras de digitação (CPF, telefone, CEP) e validação do CPF. O campo guarda o texto já formatado.
public final class Mascaras {

    private Mascaras() {
    }

    public static void cpf(TextField campo) {
        aplicar(campo, digitos -> formatar(digitos, "###.###.###-##"), 11);
    }

    public static void cep(TextField campo) {
        aplicar(campo, digitos -> formatar(digitos, "#####-###"), 8);
    }

    // Aceita fixo (10 dígitos) e celular (11 dígitos).
    public static void telefone(TextField campo) {
        aplicar(campo, digitos -> formatar(digitos, digitos.length() > 10 ? "(##) #####-####" : "(##) ####-####"), 11);
    }

    private static void aplicar(TextField campo, java.util.function.Function<String, String> formatador, int maximo) {
        boolean[] ajustando = {false};
        campo.textProperty().addListener((obs, antigo, novo) -> {
            if (ajustando[0] || novo == null) {
                return;
            }
            String digitos = novo.replaceAll("\\D", "");
            if (digitos.length() > maximo) {
                digitos = digitos.substring(0, maximo);
            }
            String formatado = formatador.apply(digitos);
            if (!formatado.equals(novo)) {
                ajustando[0] = true;
                try {
                    campo.setText(formatado);
                    campo.positionCaret(formatado.length());
                } finally {
                    ajustando[0] = false;
                }
            }
        });
    }

    // Aplica o padrão enquanto houver dígitos; não completa o que ainda não foi digitado.
    private static String formatar(String digitos, String padrao) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (char c : padrao.toCharArray()) {
            if (i >= digitos.length()) {
                break;
            }
            if (c == '#') {
                sb.append(digitos.charAt(i++));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String somenteDigitos(String texto) {
        return texto == null ? "" : texto.replaceAll("\\D", "");
    }

    // Confere os dois dígitos verificadores. Vazio não é tratado aqui (campo opcional).
    public static boolean cpfValido(String texto) {
        String d = somenteDigitos(texto);
        if (d.length() != 11 || d.chars().distinct().count() == 1) {
            return false;
        }
        return digito(d, 9) == d.charAt(9) - '0' && digito(d, 10) == d.charAt(10) - '0';
    }

    private static int digito(String d, int posicao) {
        int soma = 0;
        for (int i = 0; i < posicao; i++) {
            soma += (d.charAt(i) - '0') * (posicao + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    public static boolean telefoneValido(String texto) {
        int n = somenteDigitos(texto).length();
        return n == 10 || n == 11;
    }
}
