package com.farmacia.util;

import com.farmacia.model.Produto;

// Leva, entre a lista de produtos e o formulário (telas diferentes), o que o formulário deve fazer e o aviso de volta.
// Cada informação vale para uma única leitura.
public final class ContextoProduto {

    public enum Modo { NOVO, EDITAR }

    public enum TipoAviso { SUCESSO, INFO }

    // 'produto' é o produto editado (EDITAR); no modo NOVO fica nulo.
    public record Pedido(Modo modo, Produto produto) {
    }

    public record Aviso(TipoAviso tipo, String texto, Produto destaque) {
    }

    private static final Pedido NOVO = new Pedido(Modo.NOVO, null);

    private static Pedido pedido = NOVO;
    private static Aviso aviso;

    private ContextoProduto() {
    }

    public static void iniciarNovo() {
        pedido = NOVO;
    }

    public static void iniciarEdicao(Produto produto) {
        pedido = new Pedido(Modo.EDITAR, produto);
    }

    public static Pedido consumirPedido() {
        Pedido atual = pedido;
        pedido = NOVO;
        return atual;
    }

    public static void deixarAviso(TipoAviso tipo, String texto, Produto destaque) {
        aviso = new Aviso(tipo, texto, destaque);
    }

    public static Aviso consumirAviso() {
        Aviso atual = aviso;
        aviso = null;
        return atual;
    }
}
