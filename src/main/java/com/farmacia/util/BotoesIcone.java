package com.farmacia.util;

import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.util.Duration;

// Botões de ação das tabelas: ícone conhecido (lápis, lixeira, folhas sobrepostas) com dica de texto ao passar o mouse.
// Os desenhos ficam no CSS (classes icon-*), para que cor e tamanho sigam o tema.
public final class BotoesIcone {

    private BotoesIcone() {
    }

    public static Button editar(String dica) {
        return criar("icon-editar", dica, false);
    }

    public static Button excluir(String dica) {
        return criar("icon-excluir", dica, true);
    }

    public static Button duplicar(String dica) {
        return criar("icon-duplicar", dica, false);
    }

    public static Button adicionarPessoa(String dica) {
        return criar("icon-pessoa-mais", dica, false);
    }

    private static Button criar(String classeIcone, String dica, boolean perigo) {
        Region icone = new Region();
        icone.getStyleClass().addAll("icon", classeIcone);
        Button botao = new Button();
        botao.setGraphic(icone);
        botao.getStyleClass().add("btn-icon");
        if (perigo) {
            botao.getStyleClass().add("btn-icon-danger");
        }
        Tooltip tooltip = new Tooltip(dica);
        tooltip.setShowDelay(Duration.millis(250));
        botao.setTooltip(tooltip);
        // Leitores de tela não enxergam o desenho; leem a dica.
        botao.setAccessibleText(dica);
        return botao;
    }
}
