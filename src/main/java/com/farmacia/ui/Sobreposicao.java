package com.farmacia.ui;

import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

// Camada modal sobre a tela: escurece o fundo, centraliza um cartão e fecha com Esc ou clique fora.
// Substitui os alertas cinzas do sistema por caixas no mesmo estilo visual do restante.
public final class Sobreposicao {

    private static final String PROPRIEDADE_ABERTA = "sobreposicao-aberta";

    private final StackPane hospedeiro;
    private final StackPane fundo = new StackPane();
    private final Node focoAnterior;
    private final Runnable aoCancelar;
    private boolean fechada = false;

    private Sobreposicao(StackPane hospedeiro, Region cartao, Node focoInicial, Runnable aoCancelar) {
        this.hospedeiro = hospedeiro;
        this.aoCancelar = aoCancelar;
        this.focoAnterior = hospedeiro.getScene() == null ? null : hospedeiro.getScene().getFocusOwner();

        fundo.getStyleClass().add("v2-scrim");
        fundo.getChildren().add(cartao);
        StackPane.setAlignment(cartao, javafx.geometry.Pos.CENTER);
        // Clique no escuro (fora do cartão) cancela.
        fundo.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getTarget() == fundo) {
                cancelar();
            }
        });
        fundo.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                e.consume();
                cancelar();
            }
        });

        hospedeiro.getChildren().add(fundo);
        hospedeiro.getProperties().put(PROPRIEDADE_ABERTA, Boolean.TRUE);
        fundo.setOpacity(0);
        FadeTransition entrada = new FadeTransition(Duration.millis(140), fundo);
        entrada.setToValue(1);
        entrada.play();
        if (focoInicial != null) {
            focoInicial.requestFocus();
        }
    }

    // Abre a camada e devolve o objeto para fechá-la por código (ex.: botão Confirmar).
    public static Sobreposicao abrir(StackPane hospedeiro, Region cartao, Node focoInicial, Runnable aoCancelar) {
        return new Sobreposicao(hospedeiro, cartao, focoInicial, aoCancelar);
    }

    // Verdadeiro enquanto houver uma caixa aberta; os atalhos da tela devem ser ignorados nesse período.
    public static boolean temCaixaAberta(StackPane hospedeiro) {
        return Boolean.TRUE.equals(hospedeiro.getProperties().get(PROPRIEDADE_ABERTA));
    }

    public void cancelar() {
        if (fechada) {
            return;
        }
        fechar();
        if (aoCancelar != null) {
            aoCancelar.run();
        }
    }

    public void fechar() {
        if (fechada) {
            return;
        }
        fechada = true;
        hospedeiro.getChildren().remove(fundo);
        hospedeiro.getProperties().remove(PROPRIEDADE_ABERTA);
        if (focoAnterior != null && focoAnterior.getScene() != null) {
            focoAnterior.requestFocus();
        }
    }
}
