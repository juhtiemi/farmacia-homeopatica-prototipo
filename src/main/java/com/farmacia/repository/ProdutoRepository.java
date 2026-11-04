package com.farmacia.repository;

import com.farmacia.model.Produto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ProdutoRepository extends RepositorioEmMemoria<Produto> {

    // De 1 até este valor (inclusive) o estoque é considerado baixo; 0 é "sem estoque".
    public static final int LIMITE_ESTOQUE_BAIXO = 5;

    // Situação do estoque de um produto: vale para a lista (filtros e selos) e para o formulário.
    public enum SituacaoEstoque { NAO_INFORMADO, SEM_ESTOQUE, BAIXO, NORMAL }

    // Categorias oferecidas no cadastro mesmo antes de existir algum produto nelas.
    private static final List<String> CATEGORIAS_PADRAO = List.of("Homeopático industrializado", "Fitoterápico",
            "Suplemento", "Dermocosmético", "Higiene e cuidados", "Acessório", "Outros");

    private static final ProdutoRepository INSTANCIA = new ProdutoRepository();

    public static ProdutoRepository getInstance() {
        return INSTANCIA;
    }

    public static SituacaoEstoque situacaoDoEstoque(Integer quantidade) {
        if (quantidade == null) {
            return SituacaoEstoque.NAO_INFORMADO;
        }
        if (quantidade <= 0) {
            return SituacaoEstoque.SEM_ESTOQUE;
        }
        return quantidade <= LIMITE_ESTOQUE_BAIXO ? SituacaoEstoque.BAIXO : SituacaoEstoque.NORMAL;
    }

    // Categorias para o campo do cadastro: as padrão (na ordem de sempre) e, depois, as digitadas em outros produtos.
    public List<String> categoriasParaCadastro() {
        List<String> categorias = new ArrayList<>(CATEGORIAS_PADRAO);
        getItens().stream().map(Produto::getCategoria)
                .filter(c -> c != null && !c.isBlank())
                .filter(c -> categorias.stream().noneMatch(existente -> existente.equalsIgnoreCase(c)))
                .distinct()
                .sorted(Comparator.comparing(c -> c.toLowerCase(Locale.ROOT)))
                .forEach(categorias::add);
        return categorias;
    }

    private ProdutoRepository() {
        super("PRO", Produto::setCodigo);
        carregarInicial(List.of(
                criar("PRO0001", "Arnica Montana 30CH", "Homeopático industrializado", new BigDecimal("24.90"), 40),
                criar("PRO0002", "Nux Vomica Gotas 30ml", "Homeopático industrializado", new BigDecimal("32.50"), 18),
                criar("PRO0003", "Pomada de Calêndula 50g", "Dermocosmético", new BigDecimal("28.00"), 25),
                criar("PRO0004", "Frasco Conta-gotas Âmbar 30ml", "Acessório", new BigDecimal("4.50"), null),
                criar("PRO0005", "Lactose em Pó 100g", null, null, null)
        ));
    }

    private static Produto criar(String codigo, String nome, String categoria, BigDecimal preco, Integer quantidadeEstoque) {
        Produto produto = new Produto();
        produto.setCodigo(codigo);
        produto.setNome(nome);
        produto.setCategoria(categoria);
        produto.setPreco(preco);
        produto.setQuantidadeEstoque(quantidadeEstoque);
        return produto;
    }
}
