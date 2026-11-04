package com.farmacia.ui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

import java.util.Comparator;
import java.util.function.Function;

// Peças das tabelas do tema v2: colunas que desenham o registro inteiro, textos com "—" para vazio, estado vazio.
public final class Celulas {

    private Celulas() {
    }

    // A coluna recebe o próprio registro e a fábrica decide como desenhá-lo.
    public static <T> void coluna(TableColumn<T, T> coluna, Function<T, Node> fabrica, Comparator<T> ordem) {
        coluna.setCellValueFactory(dados -> new ReadOnlyObjectWrapper<>(dados.getValue()));
        if (ordem != null) {
            coluna.setComparator(ordem);
        } else {
            coluna.setSortable(false);
        }
        coluna.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean vazio) {
                super.updateItem(item, vazio);
                setGraphic(vazio || item == null ? null : fabrica.apply(item));
            }
        });
    }

    // Texto simples; campo sem dado aparece como "—" em cinza (diferente de erro de carregamento).
    public static Node texto(String valor) {
        boolean vazio = valor == null || valor.isBlank();
        Label l = new Label(vazio ? "—" : valor);
        if (vazio) {
            l.getStyleClass().add("v2-vazio-texto");
        } else {
            l.setTooltip(new Tooltip(valor));
        }
        return l;
    }

    // Texto principal com uma linha de apoio menor logo abaixo (ex.: médico e CRM).
    public static Node duasLinhas(String principal, String apoio) {
        VBox caixa = new VBox(2, texto(principal));
        if (apoio != null && !apoio.isBlank()) {
            Label sub = new Label(apoio);
            sub.getStyleClass().add("v2-sub");
            sub.setTooltip(new Tooltip(apoio));
            caixa.getChildren().add(sub);
        }
        return caixa;
    }

    public static Node numero(String valor) {
        Label l = new Label(valor == null ? "—" : valor);
        l.getStyleClass().add("v2-num");
        return l;
    }

    // Selo pequeno e discreto (ex.: "Dependente").
    public static Label selo(String texto, String estilo) {
        Label l = new Label(texto);
        l.getStyleClass().addAll("v2-selo-pequeno", estilo);
        return l;
    }

    // Caixa de ações da linha: ícones alinhados à direita; "perigoso" vai depois de um separador.
    public static Node acoes(Node... botoes) {
        HBox caixa = new HBox(2);
        caixa.setAlignment(Pos.CENTER_RIGHT);
        for (Node b : botoes) {
            if (b instanceof Button botao && botao.getStyleClass().contains("v2-ib-perigo")) {
                Region sep = new Region();
                sep.getStyleClass().add("v2-vsep");
                HBox.setMargin(sep, new Insets(0, 6, 0, 6));
                caixa.getChildren().add(sep);
            }
            caixa.getChildren().add(b);
        }
        return caixa;
    }

    // Estado vazio útil: explica o que aconteceu e oferece o próximo passo.
    public static Node estadoVazio(String icone, String titulo, String texto, Node... acoes) {
        VBox caixa = new VBox(10);
        caixa.setAlignment(Pos.CENTER);
        caixa.setPadding(new Insets(40, 24, 40, 24));
        StackPane medalhao = new StackPane(Icones.criar(icone, 26));
        medalhao.getStyleClass().add("v2-vazio-medalhao");
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("v2-vazio-titulo");
        Label lblTexto = new Label(texto);
        lblTexto.getStyleClass().add("v2-apoio");
        lblTexto.setWrapText(true);
        lblTexto.setMaxWidth(460);
        lblTexto.setTextAlignment(TextAlignment.CENTER);
        lblTexto.setAlignment(Pos.CENTER);
        caixa.getChildren().addAll(medalhao, lblTitulo, lblTexto);
        if (acoes.length > 0) {
            HBox linha = new HBox(10, acoes);
            linha.setAlignment(Pos.CENTER);
            caixa.getChildren().add(linha);
        }
        return caixa;
    }
}
