package com.farmacia.ui;

import javafx.scene.layout.StackPane;

import java.util.function.BooleanSupplier;

// Protege o formulário aberto: se há alterações não salvas, sair (menu ou cancelar) pede confirmação.
public final class Guarda {

    private static BooleanSupplier sujo;
    private static StackPane hospedeiro;

    private Guarda() {
    }

    public static void ativar(StackPane host, BooleanSupplier temAlteracoes) {
        hospedeiro = host;
        sujo = temAlteracoes;
    }

    public static void limpar() {
        hospedeiro = null;
        sujo = null;
    }

    // Executa a saída; se houver alterações não salvas, antes pergunta se o usuário quer descartá-las.
    public static void sair(Runnable saida) {
        if (sujo != null && hospedeiro != null && sujo.getAsBoolean()) {
            StackPane host = hospedeiro;
            Caixas.confirmar(host, "alerta", false, "Descartar as alterações?",
                    "Você preencheu campos que ainda não foram salvos. Se sair agora, eles serão perdidos.",
                    null, "Descartar e sair", () -> {
                        limpar();
                        saida.run();
                    });
        } else {
            limpar();
            saida.run();
        }
    }
}
