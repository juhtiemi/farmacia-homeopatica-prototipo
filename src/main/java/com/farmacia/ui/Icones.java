package com.farmacia.ui;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;

import java.util.Map;

// Ícones de contorno (grade 24x24, traço arredondado). Cor e espessura vêm do CSS (classe "icone"),
// o que permite mudar a cor conforme o contexto (menu escuro, botão, hover).
public final class Icones {

    private static final Map<String, String> CAMINHOS = Map.ofEntries(
            Map.entry("clientes", "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M5 7a4 4 0 1 0 8 0a4 4 0 1 0 -8 0 M22 21v-2a4 4 0 0 0-3-3.87 M16 3.13a4 4 0 0 1 0 7.75"),
            Map.entry("receitas", "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z M14 2v6h6 M16 13H8 M16 17H8 M10 9H8"),
            Map.entry("medicos", "M4.8 2.3 A0.3 0.3 0 1 0 5 2H4a2 2 0 0 0-2 2v5a6 6 0 0 0 6 6a6 6 0 0 0 6-6V4a2 2 0 0 0-2-2h-1 A0.2 0.2 0 1 0 11.3 2.3 M8 15v1a6 6 0 0 0 6 6a6 6 0 0 0 6-6v-4 M18 10a2 2 0 1 0 4 0a2 2 0 1 0 -4 0"),
            Map.entry("funcionarios", "M4 7h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2z M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"),
            Map.entry("produtos", "M9 3h6 M10 3v6.5L4.5 19A1.5 1.5 0 0 0 5.8 21h12.4a1.5 1.5 0 0 0 1.3-2L14 9.5V3 M7 15h10"),
            Map.entry("sair", "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4 M16 17l5-5-5-5 M21 12H9"),
            Map.entry("ajuda", "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0 M9.1 9a3 3 0 0 1 5.8 1c0 2-3 3-3 3 M12 17h0.01"),
            Map.entry("info", "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0 M12 16v-4 M12 8h0.01"),
            Map.entry("buscar", "M4 11a7 7 0 1 0 14 0a7 7 0 1 0 -14 0 M21 21l-4.3-4.3"),
            Map.entry("mais", "M12 5v14 M5 12h14"),
            Map.entry("seta-baixo", "M6 9l6 6 6-6"),
            Map.entry("editar", "M12 20h9 M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1l1-4z"),
            Map.entry("duplicar", "M11 9h9a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2h-9a2 2 0 0 1-2-2v-9a2 2 0 0 1 2-2z M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"),
            Map.entry("lixeira", "M3 6h18 M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2 M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"),
            Map.entry("fechar", "M18 6L6 18 M6 6l12 12"),
            Map.entry("ok", "M22 11.08V12a10 10 0 1 1-5.93-9.14 M22 4L12 14.01l-3-3"),
            Map.entry("restaurar", "M3 12a9 9 0 1 0 9-9a9.75 9.75 0 0 0-6.74 2.74L3 8 M3 3v5h5"),
            Map.entry("calendario", "M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z M16 2v4 M8 2v4 M3 10h18"),
            Map.entry("alerta", "M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z M12 9v4 M12 17h0.01")
    );

    private Icones() {
    }

    // Ícone com o tamanho final em pixels (a grade original é 24x24).
    public static Node criar(String nome, double tamanho) {
        String caminho = CAMINHOS.get(nome);
        if (caminho == null) {
            throw new IllegalArgumentException("Ícone desconhecido: " + nome);
        }
        SVGPath forma = new SVGPath();
        forma.setContent(caminho);
        forma.getStyleClass().add("icone");
        // A escala parte do canto da grade 24x24 (e não do centro do desenho), para todos os ícones ocuparem o mesmo quadrado.
        double escala = tamanho / 24.0;
        forma.getTransforms().add(new Scale(escala, escala, 0, 0));
        Pane quadro = new Pane(forma);
        quadro.setMinSize(tamanho, tamanho);
        quadro.setPrefSize(tamanho, tamanho);
        quadro.setMaxSize(tamanho, tamanho);
        quadro.setMouseTransparent(true);
        return quadro;
    }
}
