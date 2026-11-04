package com.farmacia.ui;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Erros de validação no próprio campo, em linguagem clara. Cada formulário cria um Grupo, mostra os erros
// e os limpa ao salvar de novo; o erro de um campo some sozinho quando o usuário volta a editá-lo.
// Requisito do FXML: cada campo fica dentro de um VBox com a classe de estilo "v2-campo".
public final class ErroInline {

    private ErroInline() {
    }

    public static final class Grupo {

        private final Map<Control, Node> erros = new LinkedHashMap<>();

        public void mostrar(Control campo, String mensagem) {
            limpar(campo);
            VBox caixa = caixaDoCampo(campo);
            if (!campo.getStyleClass().contains("v2-input-erro")) {
                campo.getStyleClass().add("v2-input-erro");
            }
            if (caixa != null) {
                Label texto = new Label(mensagem);
                texto.getStyleClass().add("v2-erro-campo");
                texto.setWrapText(true);
                HBox linha = new HBox(6, Icones.criar("alerta", 14), texto);
                linha.getStyleClass().add("v2-erro-linha");
                caixa.getChildren().add(linha);
                erros.put(campo, linha);
            }
            apagarAoEditar(campo);
        }

        public void limpar(Control campo) {
            campo.getStyleClass().remove("v2-input-erro");
            Node linha = erros.remove(campo);
            if (linha != null && linha.getParent() instanceof VBox pai) {
                pai.getChildren().remove(linha);
            }
        }

        public void limpar() {
            for (Control campo : new ArrayList<>(erros.keySet())) {
                limpar(campo);
            }
        }

        public boolean temErros() {
            return !erros.isEmpty();
        }

        // Campos com erro, na ordem em que foram mostrados (o primeiro recebe o foco). Control não redefine equals, então vale a identidade.
        public List<Control> campos() {
            return new ArrayList<>(erros.keySet());
        }

        private void apagarAoEditar(Control campo) {
            Runnable limpar = () -> limpar(campo);
            if (campo instanceof TextInputControl t) {
                t.textProperty().addListener((o, a, n) -> limpar.run());
            } else if (campo instanceof DatePicker d) {
                d.valueProperty().addListener((o, a, n) -> limpar.run());
            } else if (campo instanceof ComboBox<?> c) {
                c.valueProperty().addListener((o, a, n) -> limpar.run());
            }
        }

        private VBox caixaDoCampo(Node campo) {
            for (Node n = campo.getParent(); n != null; n = n.getParent()) {
                if (n instanceof VBox v && v.getStyleClass().contains("v2-campo")) {
                    return v;
                }
            }
            return null;
        }
    }
}
