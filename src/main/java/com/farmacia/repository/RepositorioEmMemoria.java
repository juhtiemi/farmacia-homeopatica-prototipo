package com.farmacia.repository;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

// Base dos repositórios em memória: guarda a lista viva e o contador de códigos compartilhados entre as telas.
public abstract class RepositorioEmMemoria<T> {

    private final ObservableList<T> itens = FXCollections.observableArrayList();
    private final ObservableList<T> itensSomenteLeitura = FXCollections.unmodifiableObservableList(itens);
    // Sequência global de exclusões: permite listar, na Lixeira, itens de tipos diferentes do mais recente para o mais antigo.
    private static final AtomicLong SEQUENCIA_EXCLUSAO = new AtomicLong();
    private final Map<T, Long> ordemExclusao = new IdentityHashMap<>();
    private final ObservableList<T> lixeira = FXCollections.observableArrayList();
    private final ObservableList<T> lixeiraSomenteLeitura = FXCollections.unmodifiableObservableList(lixeira);
    private final String prefixoCodigo;
    private final BiConsumer<T, String> definirCodigo;
    private int proximoNumero = 1;

    protected RepositorioEmMemoria(String prefixoCodigo, BiConsumer<T, String> definirCodigo) {
        this.prefixoCodigo = prefixoCodigo;
        this.definirCodigo = definirCodigo;
    }

    // Visão somente leitura da lista viva: qualquer alteração via adicionar/remover chega às telas que a observam.
    public ObservableList<T> getItens() {
        return itensSomenteLeitura;
    }

    // Próximo código disponível, sem consumi-lo (usado para pré-visualizar o campo "Código" do formulário).
    public String proximoCodigo() {
        return String.format("%s%04d", prefixoCodigo, proximoNumero);
    }

    // Atribui o próximo código ao item, consome o número e o coloca no topo da lista.
    public void adicionar(T item) {
        definirCodigo.accept(item, proximoCodigo());
        proximoNumero++;
        itens.add(0, item);
    }

    public void remover(T item) {
        itens.remove(item);
    }

    // Exclusão reversível: o item sai da lista, mas fica na lixeira até ser restaurado ou apagado de vez.
    public void moverParaLixeira(T item) {
        if (itens.remove(item)) {
            ordemExclusao.put(item, SEQUENCIA_EXCLUSAO.incrementAndGet());
            lixeira.add(0, item);
        }
    }

    public void restaurar(T item) {
        if (lixeira.remove(item)) {
            ordemExclusao.remove(item);
            itens.add(0, item);
        }
    }

    public void excluirDefinitivamente(T item) {
        lixeira.remove(item);
        ordemExclusao.remove(item);
    }

    // Quanto maior, mais recente foi a exclusão (0 se o item não está na lixeira).
    public long ordemDeExclusao(T item) {
        return ordemExclusao.getOrDefault(item, 0L);
    }

    public void esvaziarLixeira() {
        lixeira.clear();
        ordemExclusao.clear();
    }

    public ObservableList<T> getLixeira() {
        return lixeiraSomenteLeitura;
    }

    // Item alterado no lugar: força as listas derivadas (filtro/ordenação das tabelas) a reavaliá-lo.
    public void atualizar(T item) {
        int indice = itens.indexOf(item);
        if (indice >= 0) {
            itens.set(indice, item);
        }
    }

    // Carga inicial de dados fictícios, que já trazem seus próprios códigos.
    protected void carregarInicial(List<T> iniciais) {
        itens.addAll(iniciais);
        proximoNumero = itens.size() + 1;
    }
}
