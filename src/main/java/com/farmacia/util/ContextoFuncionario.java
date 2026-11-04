package com.farmacia.util;

import com.farmacia.model.Funcionario;

// Leva, entre a lista de funcionários e o formulário (telas diferentes), o que o formulário deve fazer e o aviso de volta.
// Cada informação vale para uma única leitura.
public final class ContextoFuncionario {

    public enum Modo { NOVO, EDITAR }

    public enum TipoAviso { SUCESSO, INFO }

    // 'funcionario' é o funcionário editado (EDITAR); fica nulo no cadastro novo.
    public record Pedido(Modo modo, Funcionario funcionario) {
    }

    public record Aviso(TipoAviso tipo, String texto, Funcionario destaque) {
    }

    private static final Pedido NOVO = new Pedido(Modo.NOVO, null);

    private static Pedido pedido = NOVO;
    private static Aviso aviso;

    private ContextoFuncionario() {
    }

    public static void iniciarNovo() {
        pedido = NOVO;
    }

    public static void iniciarEdicao(Funcionario funcionario) {
        pedido = new Pedido(Modo.EDITAR, funcionario);
    }

    public static Pedido consumirPedido() {
        Pedido atual = pedido;
        pedido = NOVO;
        return atual;
    }

    public static void deixarAviso(TipoAviso tipo, String texto, Funcionario destaque) {
        aviso = new Aviso(tipo, texto, destaque);
    }

    public static Aviso consumirAviso() {
        Aviso atual = aviso;
        aviso = null;
        return atual;
    }
}
