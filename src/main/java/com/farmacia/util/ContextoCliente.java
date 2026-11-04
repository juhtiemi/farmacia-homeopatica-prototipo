package com.farmacia.util;

import com.farmacia.model.Cliente;

// Leva, entre a lista de clientes e o formulário (telas diferentes), o que o formulário deve fazer e o aviso de volta.
// Cada informação vale para uma única leitura.
public final class ContextoCliente {

    public enum Modo { NOVO, EDITAR, NOVO_DEPENDENTE }

    public enum TipoAviso { SUCESSO, INFO }

    // 'cliente' é o cliente editado (EDITAR) ou o responsável pré-selecionado (NOVO_DEPENDENTE).
    public record Pedido(Modo modo, Cliente cliente) {
    }

    public record Aviso(TipoAviso tipo, String texto, Cliente destaque) {
    }

    private static final Pedido NOVO = new Pedido(Modo.NOVO, null);

    private static Pedido pedido = NOVO;
    private static Aviso aviso;
    // Cadastro aberto a partir de uma receita em preenchimento: ao salvar ou cancelar, volta para a receita.
    private static boolean voltarParaReceita;
    private static String nomeSugerido;

    private ContextoCliente() {
    }

    public static void iniciarNovo() {
        pedido = NOVO;
        encerrarRetornoParaReceita();
    }

    public static void iniciarEdicao(Cliente cliente) {
        pedido = new Pedido(Modo.EDITAR, cliente);
        encerrarRetornoParaReceita();
    }

    public static void iniciarNovoDependente(Cliente responsavel) {
        pedido = new Pedido(Modo.NOVO_DEPENDENTE, responsavel);
        encerrarRetornoParaReceita();
    }

    // Novo cliente aberto pela receita: o nome digitado lá já vem preenchido e o fim do cadastro leva de volta à receita.
    public static void iniciarNovoParaReceita(String nome) {
        pedido = NOVO;
        voltarParaReceita = true;
        nomeSugerido = nome;
    }

    public static String consumirNomeSugerido() {
        String atual = nomeSugerido;
        nomeSugerido = null;
        return atual;
    }

    public static boolean voltaParaReceita() {
        return voltarParaReceita;
    }

    // Devolve se havia um retorno pendente e o desliga.
    public static boolean encerrarRetornoParaReceita() {
        boolean havia = voltarParaReceita;
        voltarParaReceita = false;
        nomeSugerido = null;
        return havia;
    }

    public static Pedido consumirPedido() {
        Pedido atual = pedido;
        pedido = NOVO;
        return atual;
    }

    public static void deixarAviso(TipoAviso tipo, String texto, Cliente destaque) {
        aviso = new Aviso(tipo, texto, destaque);
    }

    public static Aviso consumirAviso() {
        Aviso atual = aviso;
        aviso = null;
        return atual;
    }
}
