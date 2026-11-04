package com.farmacia.ui;

import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;

// Seletores de filtro do tema v2: um ícone fixo antes do valor escolhido (ex.: calendário no período).
public final class Seletores {

    private Seletores() {
    }

    public static <T> void comIcone(ComboBox<T> combo, String icone) {
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean vazio) {
                super.updateItem(item, vazio);
                setText(vazio || item == null ? null : item.toString());
                setGraphic(vazio ? null : Icones.criar(icone, 18));
                setGraphicTextGap(10);
            }
        });
    }

    // Lista fechada sem valor escolhido: mostra o texto de exemplo em cinza, como nos campos de texto,
    // para não parecer um valor já selecionado.
    public static <T> void comTextoDeExemplo(ComboBox<T> combo) {
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean vazio) {
                super.updateItem(item, vazio);
                boolean semValor = vazio || item == null;
                setText(semValor ? combo.getPromptText() : item.toString());
                getStyleClass().remove("v2-combo-exemplo");
                if (semValor) {
                    getStyleClass().add("v2-combo-exemplo");
                }
            }
        });
    }
}
