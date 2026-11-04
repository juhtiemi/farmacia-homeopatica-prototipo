package com.farmacia.util;

import com.farmacia.model.Cliente;
import com.farmacia.model.ItemReceita;
import com.farmacia.model.Medico;
import com.farmacia.model.Receita;
import com.farmacia.model.StatusReceita;

import java.time.LocalDate;
import java.util.List;

// Leva, entre a lista de receitas e o formulário (telas diferentes), o que o formulário deve fazer e o aviso de volta.
// Cada informação vale para uma única leitura: o formulário e a lista a consomem ao abrir.
public final class ContextoReceita {

    public enum Modo { NOVA, EDITAR, DUPLICAR }

    public enum TipoAviso { SUCESSO, INFO }

    public record Pedido(Modo modo, Receita receita, List<ItemReceita> itens, com.farmacia.model.Cliente paciente) {
    }

    public record Aviso(TipoAviso tipo, String texto, Receita destaque) {
    }

    // Receita em preenchimento que o usuário deixou para cadastrar, no meio do caminho, um cliente ou um médico novo.
    // Ao voltar, o formulário reaparece como estava e já com o cadastro recém-criado escolhido.
    public record Rascunho(Pedido pedido, LocalDate data, String nomePaciente, Cliente paciente, String rgPaciente,
                           String responsavel, String nomeMedico, Medico medico, String crm, StatusReceita status,
                           List<ItemReceita> itens, String dicaDuplicacao) {
    }

    private static final Pedido NOVA = new Pedido(Modo.NOVA, null, List.of(), null);

    private static Pedido pedido = NOVA;
    private static Aviso aviso;
    private static Rascunho rascunho;
    private static Object cadastroRecemCriado;

    private ContextoReceita() {
    }

    public static void guardarRascunho(Rascunho valor) {
        rascunho = valor;
        cadastroRecemCriado = null;
    }

    public static Rascunho consumirRascunho() {
        Rascunho atual = rascunho;
        rascunho = null;
        return atual;
    }

    // O cliente ou médico que o usuário acabou de cadastrar a partir da receita (fica nulo se ele cancelou).
    public static void registrarCadastroRecemCriado(Object cadastro) {
        cadastroRecemCriado = cadastro;
    }

    public static Object consumirCadastroRecemCriado() {
        Object atual = cadastroRecemCriado;
        cadastroRecemCriado = null;
        return atual;
    }

    // Quando o usuário sai por outro caminho (menu lateral), o rascunho e o desvio de cadastro deixam de valer.
    public static void descartarRascunho() {
        rascunho = null;
        cadastroRecemCriado = null;
        ContextoCliente.encerrarRetornoParaReceita();
        ContextoMedico.encerrarRetornoParaReceita();
    }

    public static void iniciarNova() {
        pedido = NOVA;
    }

    // Nova receita já com o paciente escolhido (ex.: a partir da lista de clientes).
    public static void iniciarNovaPara(com.farmacia.model.Cliente paciente) {
        pedido = new Pedido(Modo.NOVA, null, List.of(), paciente);
    }

    public static void iniciarEdicao(Receita receita) {
        pedido = new Pedido(Modo.EDITAR, receita, List.of(), null);
    }

    // 'origem' é a receita que está sendo duplicada; 'itens' são os medicamentos escolhidos (cópias independentes).
    public static void iniciarDuplicacao(Receita origem, List<ItemReceita> itens) {
        pedido = new Pedido(Modo.DUPLICAR, origem, List.copyOf(itens), null);
    }

    public static Pedido consumirPedido() {
        Pedido atual = pedido;
        pedido = NOVA;
        return atual;
    }

    public static void deixarAviso(TipoAviso tipo, String texto, Receita destaque) {
        aviso = new Aviso(tipo, texto, destaque);
    }

    public static Aviso consumirAviso() {
        Aviso atual = aviso;
        aviso = null;
        return atual;
    }
}
