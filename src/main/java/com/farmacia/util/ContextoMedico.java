package com.farmacia.util;

import com.farmacia.model.Medico;

// Leva, entre a lista de médicos e o formulário (telas diferentes), o que o formulário deve fazer e o aviso de volta.
// Cada informação vale para uma única leitura.
public final class ContextoMedico {

    public enum Modo { NOVO, EDITAR }

    public enum TipoAviso { SUCESSO, INFO }

    // 'medico' é o médico editado (EDITAR); fica nulo em NOVO.
    public record Pedido(Modo modo, Medico medico) {
    }

    public record Aviso(TipoAviso tipo, String texto, Medico destaque) {
    }

    private static final Pedido NOVO = new Pedido(Modo.NOVO, null);

    private static Pedido pedido = NOVO;
    private static Aviso aviso;
    // Cadastro aberto a partir de uma receita em preenchimento: ao salvar ou cancelar, volta para a receita.
    private static boolean voltarParaReceita;
    private static String nomeSugerido;

    private ContextoMedico() {
    }

    public static void iniciarNovo() {
        pedido = NOVO;
        encerrarRetornoParaReceita();
    }

    public static void iniciarEdicao(Medico medico) {
        pedido = new Pedido(Modo.EDITAR, medico);
        encerrarRetornoParaReceita();
    }

    // Novo médico aberto pela receita: o nome digitado lá já vem preenchido e o fim do cadastro leva de volta à receita.
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

    public static void deixarAviso(TipoAviso tipo, String texto, Medico destaque) {
        aviso = new Aviso(tipo, texto, destaque);
    }

    public static Aviso consumirAviso() {
        Aviso atual = aviso;
        aviso = null;
        return atual;
    }
}
