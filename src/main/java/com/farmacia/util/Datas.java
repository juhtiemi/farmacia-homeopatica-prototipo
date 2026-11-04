package com.farmacia.util;

import javafx.scene.control.DatePicker;

import java.time.DateTimeException;
import java.time.LocalDate;

public final class Datas {

    private Datas() {
    }

    // O DatePicker só confirma o texto digitado ao perder o foco, mas o Enter aciona o botão padrão "Salvar" antes disso.
    // Devolve false quando o editor contém um texto que não é uma data válida (o valor atual não é alterado).
    public static boolean confirmarTextoDigitado(DatePicker seletor) {
        String texto = seletor.getEditor().getText();
        if (texto == null || texto.isBlank()) {
            seletor.setValue(null);
            return true;
        }
        try {
            seletor.setValue(seletor.getConverter().fromString(texto.trim()));
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }

    // Mantém valor e texto do editor juntos: setValue sozinho não limpa um texto inválido que o usuário já digitou.
    public static void definir(DatePicker seletor, LocalDate data) {
        seletor.setValue(data);
        seletor.getEditor().setText(data == null ? "" : seletor.getConverter().toString(data));
    }
}
