package com.farmacia.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

// Fábrica dos botões do tema v2, para todas as telas terem o mesmo desenho e o mesmo comportamento.
public final class Botoes {

    private Botoes() {
    }

    // Botão só com ícone (editar, excluir...). A dica aparece ao passar o mouse e é lida por leitores de tela.
    public static Button icone(String icone, String dica, boolean perigo) {
        Button botao = new Button();
        botao.setGraphic(Icones.criar(icone, 18));
        botao.getStyleClass().add("v2-ib");
        if (perigo) {
            botao.getStyleClass().add("v2-ib-perigo");
        }
        Tooltip tooltip = new Tooltip(dica);
        tooltip.setShowDelay(Duration.millis(250));
        botao.setTooltip(tooltip);
        botao.setAccessibleText(dica);
        return botao;
    }

    // Conteúdo de botão com ícone opcional, texto e dica de teclado opcional (use como graphic de um botão v2-btn).
    public static Node composto(String icone, String texto, String teclas) {
        Label rotulo = new Label(texto);
        rotulo.getStyleClass().add("v2-btn-rotulo");
        HBox caixa = new HBox(8);
        caixa.setAlignment(Pos.CENTER);
        if (icone != null) {
            caixa.getChildren().add(Icones.criar(icone, 18));
        }
        caixa.getChildren().add(rotulo);
        if (teclas != null) {
            Label kbd = new Label(teclas);
            kbd.getStyleClass().add("v2-kbd");
            caixa.getChildren().add(kbd);
        }
        return caixa;
    }

    // Botão completo pronto para uso: estilo "v2-btn-primary", "v2-btn-ghost" ou "v2-btn-perigo".
    public static Button botao(String estilo, String icone, String texto, String teclas) {
        Button botao = new Button();
        botao.getStyleClass().addAll("v2-btn", estilo);
        botao.setGraphic(composto(icone, texto, teclas));
        botao.setAccessibleText(texto);
        return botao;
    }
}
