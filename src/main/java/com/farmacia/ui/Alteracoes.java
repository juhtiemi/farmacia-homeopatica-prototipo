package com.farmacia.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextInputControl;

// Detecta se o usuário mexeu em algum campo depois do preenchimento inicial.
public final class Alteracoes {

    private final BooleanProperty sujo = new SimpleBooleanProperty(false);

    public Alteracoes(Control... campos) {
        for (Control campo : campos) {
            if (campo instanceof TextInputControl t) {
                t.textProperty().addListener((o, a, n) -> sujo.set(true));
            } else if (campo instanceof DatePicker d) {
                d.valueProperty().addListener((o, a, n) -> sujo.set(true));
            } else if (campo instanceof ComboBox<?> c) {
                c.valueProperty().addListener((o, a, n) -> sujo.set(true));
            }
        }
    }

    public boolean isSujo() {
        return sujo.get();
    }

    // Chame depois de preencher o formulário (ou de salvar): o estado atual passa a ser o ponto de partida.
    public void zerar() {
        sujo.set(false);
    }

    public BooleanProperty sujoProperty() {
        return sujo;
    }
}
