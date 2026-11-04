package com.farmacia.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

// Aviso temporário no canto da tela, com ação opcional ("Desfazer"). Só um por vez; o novo substitui o anterior.
public final class Toast {

    private static final String PROPRIEDADE = "toast-atual";
    private static final Duration DURACAO = Duration.seconds(7);

    public enum Tipo { SUCESSO, INFO, ERRO }

    private Toast() {
    }

    public static void mostrar(StackPane hospedeiro, Tipo tipo, String texto) {
        mostrar(hospedeiro, tipo, texto, null, null);
    }

    public static void mostrar(StackPane hospedeiro, Tipo tipo, String texto, String rotuloAcao, Runnable acao) {
        remover(hospedeiro);

        Label mensagem = new Label(texto);
        mensagem.getStyleClass().add("v2-toast-texto");
        mensagem.setWrapText(true);
        mensagem.setMaxWidth(420);

        HBox caixa = new HBox(12);
        caixa.getStyleClass().add("v2-toast");
        caixa.setAlignment(Pos.CENTER_LEFT);
        caixa.setMaxSize(HBox.USE_PREF_SIZE, HBox.USE_PREF_SIZE);
        caixa.getChildren().addAll(Icones.criar(tipo == Tipo.SUCESSO ? "ok" : tipo == Tipo.ERRO ? "alerta" : "info", 20), mensagem);
        caixa.getChildren().get(0).getStyleClass().add(tipo == Tipo.ERRO ? "v2-toast-icone-erro" : "v2-toast-icone");

        PauseTransition espera = new PauseTransition(DURACAO);
        if (rotuloAcao != null && acao != null) {
            Button botaoAcao = new Button(rotuloAcao);
            botaoAcao.getStyleClass().add("v2-toast-acao");
            botaoAcao.setOnAction(e -> {
                espera.stop();
                remover(hospedeiro);
                acao.run();
            });
            caixa.getChildren().add(botaoAcao);
        }
        Button fechar = new Button();
        fechar.setGraphic(Icones.criar("fechar", 16));
        fechar.getStyleClass().addAll("v2-ib", "v2-ib-escuro");
        fechar.setAccessibleText("Fechar aviso");
        fechar.setOnAction(e -> {
            espera.stop();
            remover(hospedeiro);
        });
        caixa.getChildren().add(fechar);

        StackPane.setAlignment(caixa, Pos.BOTTOM_RIGHT);
        // Em telas de formulário, o aviso sobe para não cobrir a barra fixa com Cancelar e Salvar.
        javafx.scene.Node barra = hospedeiro.lookup(".v2-barra-acoes");
        double margemInferior = 32 + (barra != null ? Math.max(barra.getBoundsInParent().getHeight(), 72) : 0);
        StackPane.setMargin(caixa, new Insets(0, 40, margemInferior, 0));
        hospedeiro.getChildren().add(caixa);
        hospedeiro.getProperties().put(PROPRIEDADE, caixa);

        caixa.setOpacity(0);
        caixa.setTranslateY(12);
        FadeTransition aparecer = new FadeTransition(Duration.millis(180), caixa);
        aparecer.setToValue(1);
        TranslateTransition subir = new TranslateTransition(Duration.millis(180), caixa);
        subir.setToY(0);
        aparecer.play();
        subir.play();

        espera.setOnFinished(e -> {
            FadeTransition sumir = new FadeTransition(Duration.millis(200), caixa);
            sumir.setToValue(0);
            sumir.setOnFinished(f -> {
                if (hospedeiro.getProperties().get(PROPRIEDADE) == caixa) {
                    remover(hospedeiro);
                }
            });
            sumir.play();
        });
        espera.play();
    }

    private static void remover(StackPane hospedeiro) {
        Object atual = hospedeiro.getProperties().remove(PROPRIEDADE);
        if (atual instanceof HBox caixa) {
            hospedeiro.getChildren().remove(caixa);
        }
    }
}
