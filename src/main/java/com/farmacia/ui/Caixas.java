package com.farmacia.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

// Caixas de diálogo no estilo do sistema: confirmação (com variante de perigo) e ajuda com atalhos.
public final class Caixas {

    private Caixas() {
    }

    public record Atalho(String teclas, String descricao) {
    }

    // Confirmação com ícone, título, texto e um resumo opcional do que será afetado.
    public static void confirmar(StackPane hospedeiro, String icone, boolean perigo, String titulo, String texto,
                                 String resumo, String rotuloConfirmar, Runnable aoConfirmar) {
        VBox cartao = cartao(440);

        StackPane medalhao = new StackPane(Icones.criar(icone, 24));
        medalhao.getStyleClass().addAll("v2-medalhao", perigo ? "v2-medalhao-perigo" : "v2-medalhao-neutro");
        medalhao.setMinSize(48, 48);
        medalhao.setMaxSize(48, 48);

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("v2-caixa-titulo");
        lblTitulo.setWrapText(true);
        Label lblTexto = new Label(texto);
        lblTexto.getStyleClass().add("v2-caixa-texto");
        lblTexto.setWrapText(true);

        cartao.getChildren().addAll(medalhao, lblTitulo, lblTexto);
        if (resumo != null && !resumo.isBlank()) {
            Label lblResumo = new Label(resumo);
            lblResumo.getStyleClass().add("v2-caixa-resumo");
            lblResumo.setWrapText(true);
            lblResumo.setMaxWidth(Double.MAX_VALUE);
            cartao.getChildren().add(lblResumo);
        }

        Button cancelar = new Button("Cancelar");
        cancelar.getStyleClass().addAll("v2-btn", "v2-btn-ghost");
        Button confirmar = new Button(rotuloConfirmar);
        confirmar.getStyleClass().addAll("v2-btn", perigo ? "v2-btn-perigo" : "v2-btn-primary");
        HBox botoes = new HBox(10, cancelar, confirmar);
        botoes.setAlignment(Pos.CENTER_RIGHT);
        VBox.setMargin(botoes, new Insets(8, 0, 0, 0));
        cartao.getChildren().add(botoes);

        Sobreposicao camada = Sobreposicao.abrir(hospedeiro, cartao, confirmar, null);
        cancelar.setOnAction(e -> camada.cancelar());
        confirmar.setOnAction(e -> {
            camada.fechar();
            aoConfirmar.run();
        });
    }

    // Pergunta com duas saídas possíveis além de cancelar (ex.: excluir também os dependentes ou mantê-los).
    public static void decidir(StackPane hospedeiro, String icone, String titulo, String texto, String resumo,
                               String rotuloA, Runnable acaoA, String rotuloB, Runnable acaoB) {
        VBox cartao = cartao(480);
        StackPane medalhao = new StackPane(Icones.criar(icone, 24));
        medalhao.getStyleClass().addAll("v2-medalhao", "v2-medalhao-perigo");
        medalhao.setMinSize(48, 48);
        medalhao.setMaxSize(48, 48);
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("v2-caixa-titulo");
        lblTitulo.setWrapText(true);
        Label lblTexto = new Label(texto);
        lblTexto.getStyleClass().add("v2-caixa-texto");
        lblTexto.setWrapText(true);
        cartao.getChildren().addAll(medalhao, lblTitulo, lblTexto);
        if (resumo != null && !resumo.isBlank()) {
            Label lblResumo = new Label(resumo);
            lblResumo.getStyleClass().add("v2-caixa-resumo");
            lblResumo.setWrapText(true);
            lblResumo.setMaxWidth(Double.MAX_VALUE);
            cartao.getChildren().add(lblResumo);
        }
        Button cancelar = new Button("Cancelar");
        cancelar.getStyleClass().addAll("v2-btn", "v2-btn-ghost");
        Button opcaoB = new Button(rotuloB);
        opcaoB.getStyleClass().addAll("v2-btn", "v2-btn-ghost");
        Button opcaoA = new Button(rotuloA);
        opcaoA.getStyleClass().addAll("v2-btn", "v2-btn-perigo");
        VBox botoes = new VBox(8, opcaoA, opcaoB, cancelar);
        botoes.setFillWidth(true);
        opcaoA.setMaxWidth(Double.MAX_VALUE);
        opcaoB.setMaxWidth(Double.MAX_VALUE);
        cancelar.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(botoes, new Insets(8, 0, 0, 0));
        cartao.getChildren().add(botoes);

        Sobreposicao camada = Sobreposicao.abrir(hospedeiro, cartao, opcaoB, null);
        cancelar.setOnAction(e -> camada.cancelar());
        opcaoA.setOnAction(e -> {
            camada.fechar();
            acaoA.run();
        });
        opcaoB.setOnAction(e -> {
            camada.fechar();
            acaoB.run();
        });
    }

    // Caixa com conteúdo livre (ex.: escolher itens), com botão de confirmar que pode ficar desabilitado.
    public static void formulario(StackPane hospedeiro, String icone, String titulo, Region conteudo,
                                  String rotuloConfirmar, javafx.beans.value.ObservableValue<Boolean> desabilitarConfirmar,
                                  Runnable aoConfirmar) {
        VBox cartao = cartao(600);

        StackPane medalhao = new StackPane(Icones.criar(icone, 24));
        medalhao.getStyleClass().addAll("v2-medalhao", "v2-medalhao-neutro");
        medalhao.setMinSize(48, 48);
        medalhao.setMaxSize(48, 48);
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("v2-caixa-titulo");
        lblTitulo.setWrapText(true);
        cartao.getChildren().addAll(medalhao, lblTitulo, conteudo);

        Button cancelar = new Button("Cancelar");
        cancelar.getStyleClass().addAll("v2-btn", "v2-btn-ghost");
        Button confirmar = new Button(rotuloConfirmar);
        confirmar.getStyleClass().addAll("v2-btn", "v2-btn-primary");
        confirmar.disableProperty().bind(desabilitarConfirmar);
        HBox botoes = new HBox(10, cancelar, confirmar);
        botoes.setAlignment(Pos.CENTER_RIGHT);
        VBox.setMargin(botoes, new Insets(8, 0, 0, 0));
        cartao.getChildren().add(botoes);

        Sobreposicao camada = Sobreposicao.abrir(hospedeiro, cartao, confirmar, null);
        cancelar.setOnAction(e -> camada.cancelar());
        confirmar.setOnAction(e -> {
            camada.fechar();
            aoConfirmar.run();
        });
    }

    // Ajuda da tela: o que dá para fazer e a lista de atalhos.
    public static void ajuda(StackPane hospedeiro, String titulo, List<String> dicas, List<Atalho> atalhos) {
        VBox cartao = cartao(560);

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("v2-caixa-titulo");
        cartao.getChildren().add(lblTitulo);

        for (String dica : dicas) {
            Label l = new Label("•  " + dica);
            l.getStyleClass().add("v2-caixa-texto");
            l.setWrapText(true);
            cartao.getChildren().add(l);
        }

        Label subtitulo = new Label("Atalhos de teclado");
        subtitulo.getStyleClass().add("v2-caixa-subtitulo");
        VBox.setMargin(subtitulo, new Insets(10, 0, 0, 0));
        cartao.getChildren().add(subtitulo);

        for (Atalho atalho : atalhos) {
            FlowPane teclas = new FlowPane(6, 0);
            for (String tecla : atalho.teclas().split(" ")) {
                Label k = new Label(tecla);
                k.getStyleClass().add("v2-kbd");
                teclas.getChildren().add(k);
            }
            teclas.setPrefWidth(120);
            teclas.setMinWidth(120);
            Label descricao = new Label(atalho.descricao());
            descricao.getStyleClass().add("v2-caixa-texto");
            HBox linha = new HBox(12, teclas, descricao);
            linha.setAlignment(Pos.CENTER_LEFT);
            cartao.getChildren().add(linha);
        }

        Button fechar = new Button("Entendi");
        fechar.getStyleClass().addAll("v2-btn", "v2-btn-primary");
        HBox rodape = new HBox(fechar);
        rodape.setAlignment(Pos.CENTER_RIGHT);
        VBox.setMargin(rodape, new Insets(10, 0, 0, 0));
        cartao.getChildren().add(rodape);

        Sobreposicao camada = Sobreposicao.abrir(hospedeiro, cartao, fechar, null);
        fechar.setOnAction(e -> camada.fechar());
    }

    private static VBox cartao(double largura) {
        VBox cartao = new VBox(10);
        cartao.getStyleClass().add("v2-caixa");
        cartao.setMaxWidth(largura);
        cartao.setPrefWidth(largura);
        cartao.setMaxHeight(Region.USE_PREF_SIZE);
        cartao.setFillWidth(true);
        HBox.setHgrow(cartao, Priority.NEVER);
        return cartao;
    }
}
