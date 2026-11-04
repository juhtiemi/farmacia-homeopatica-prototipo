package com.farmacia.controller;

import com.farmacia.model.Produto;
import com.farmacia.repository.ProdutoRepository;
import com.farmacia.repository.ProdutoRepository.SituacaoEstoque;
import com.farmacia.ui.AtalhosLista;
import com.farmacia.ui.Botoes;
import com.farmacia.ui.Caixas;
import com.farmacia.ui.Celulas;
import com.farmacia.ui.EstadoLista;
import com.farmacia.ui.Icones;
import com.farmacia.ui.Toast;
import com.farmacia.util.ContextoProduto;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

// Controller da lista de Produtos (tema v2): busca, filtro de estoque com contagens e filtro de categoria.
// O cadastro (novo e edição) fica em ProdutoFormView.
public class ProdutoController {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final NumberFormat FORMATO_MOEDA = NumberFormat.getCurrencyInstance(PT_BR);
    private static final NumberFormat FORMATO_INTEIRO = NumberFormat.getIntegerInstance(PT_BR);

    private static final String TODAS_CATEGORIAS = "Todas as categorias";
    private static final String SEM_CATEGORIA = "Sem categoria";
    private static final String CHAVE_ESTADO = "produtos";

    private static boolean dicaDispensada = false;

    @FXML
    private StackPane rootStack;
    @FXML
    private BorderPane rootPane;
    @FXML
    private MenuLateralController menuLateralController;
    @FXML
    private Button btnAjuda;
    @FXML
    private HBox dicaBox;
    @FXML
    private StackPane dicaIcone;
    @FXML
    private Label lblResumoLista;
    @FXML
    private TextField txtFiltroProdutos;
    @FXML
    private StackPane iconeBusca;
    @FXML
    private Label kbdBusca;
    @FXML
    private Button btnLimparBusca;
    @FXML
    private Button btnNovoProduto;
    @FXML
    private ToggleGroup grupoEstoque;
    @FXML
    private ToggleButton tgTodos;
    @FXML
    private ToggleButton tgEmEstoque;
    @FXML
    private ToggleButton tgEstoqueBaixo;
    @FXML
    private ToggleButton tgSemEstoque;
    @FXML
    private ToggleButton tgNaoInformado;
    @FXML
    private ComboBox<String> cbCategoriaFiltro;
    @FXML
    private TableView<Produto> tableProdutos;
    @FXML
    private TableColumn<Produto, Produto> colCodigo;
    @FXML
    private TableColumn<Produto, Produto> colNome;
    @FXML
    private TableColumn<Produto, Produto> colCategoria;
    @FXML
    private TableColumn<Produto, Produto> colPreco;
    @FXML
    private TableColumn<Produto, Produto> colEstoque;
    @FXML
    private TableColumn<Produto, Produto> colAcoes;
    @FXML
    private Label lblRodapeContagem;

    private final ProdutoRepository repositorio = ProdutoRepository.getInstance();
    private FilteredList<Produto> produtosFiltrados;
    private Label contTodos;
    private Label contEmEstoque;
    private Label contEstoqueBaixo;
    private Label contSemEstoque;
    private Label contNaoInformado;
    private boolean atualizandoCategorias = false;
    private final ListChangeListener<Produto> aoMudarRepositorio = c -> {
        atualizarCategorias();
        atualizarTudo();
    };

    @FXML
    private void initialize() {
        com.farmacia.ui.Seletores.comIcone(cbCategoriaFiltro, "produtos");
        menuLateralController.marcarAtivo(MenuLateralController.PRODUTOS);

        btnAjuda.setGraphic(Botoes.composto("ajuda", "Ajuda", "F1"));
        btnNovoProduto.setGraphic(Botoes.composto("mais", "Novo produto", "Ctrl N"));
        dicaIcone.getChildren().add(Icones.criar("info", 18));
        dicaBox.setVisible(!dicaDispensada);
        dicaBox.setManaged(!dicaDispensada);
        iconeBusca.getChildren().add(Icones.criar("buscar", 18));
        btnLimparBusca.setGraphic(Icones.criar("fechar", 16));
        btnLimparBusca.setTooltip(new Tooltip("Limpar busca"));
        btnLimparBusca.setAccessibleText("Limpar busca");

        contTodos = contador(tgTodos);
        contEmEstoque = contador(tgEmEstoque);
        contEstoqueBaixo = contador(tgEstoqueBaixo);
        contSemEstoque = contador(tgSemEstoque);
        contNaoInformado = contador(tgNaoInformado);
        tgNaoInformado.setTooltip(new Tooltip("Produtos cadastrados sem a quantidade em estoque"));
        tgEmEstoque.setTooltip(new Tooltip("Mais de " + ProdutoRepository.LIMITE_ESTOQUE_BAIXO + " unidades"));
        tgEstoqueBaixo.setTooltip(new Tooltip("De 1 a " + ProdutoRepository.LIMITE_ESTOQUE_BAIXO + " unidades"));
        tgSemEstoque.setTooltip(new Tooltip("Quantidade em estoque igual a 0"));
        grupoEstoque.selectedToggleProperty().addListener((obs, antigo, novo) -> {
            if (novo == null) {
                antigo.setSelected(true);
                return;
            }
            atualizarTudo();
        });
        txtFiltroProdutos.textProperty().addListener((obs, antigo, novo) -> {
            boolean temTexto = novo != null && !novo.isEmpty();
            btnLimparBusca.setVisible(temTexto);
            kbdBusca.setVisible(!temTexto);
            atualizarTudo();
        });
        cbCategoriaFiltro.valueProperty().addListener((obs, antigo, novo) -> {
            if (!atualizandoCategorias) {
                atualizarTudo();
            }
        });
        atualizarCategorias();

        produtosFiltrados = new FilteredList<>(repositorio.getItens(), p -> true);
        configurarTabela();
        // Ouvinte fraco: ao sair da tela, o controller antigo deixa de receber as mudanças do repositório compartilhado.
        repositorio.getItens().addListener(new WeakListChangeListener<>(aoMudarRepositorio));
        AtalhosLista.instalar(rootStack, txtFiltroProdutos, tableProdutos, this::handleNovoProduto,
                this::editarProduto, this::pedirExclusao, this::handleAjuda);

        // Voltando de um cadastro, a lista continua com a busca, os filtros, a ordem e a linha de antes.
        EstadoLista.Estado anterior = EstadoLista.retomar(CHAVE_ESTADO);
        if (anterior != null) {
            if (anterior.extra("categoria") instanceof String categoria && cbCategoriaFiltro.getItems().contains(categoria)) {
                cbCategoriaFiltro.setValue(categoria);
            }
            EstadoLista.aplicar(anterior, txtFiltroProdutos, grupoEstoque, tableProdutos);
        } else {
            ordenarPeloPadrao();
        }
        atualizarTudo();
        exibirAvisoPendente();
    }

    private void guardarEstado() {
        EstadoLista.guardar(CHAVE_ESTADO, EstadoLista.capturar(txtFiltroProdutos, grupoEstoque, tableProdutos)
                .extra("categoria", cbCategoriaFiltro.getValue()));
    }

    private Label contador(ToggleButton botao) {
        Label l = new Label("0");
        l.getStyleClass().add("v2-cnt");
        botao.setGraphic(l);
        botao.setContentDisplay(ContentDisplay.RIGHT);
        return l;
    }

    // ===================== Tabela =====================

    private void configurarTabela() {
        Celulas.coluna(colCodigo, p -> Celulas.numero(p.getCodigo()), Comparator.comparing(Produto::getCodigo));
        Celulas.coluna(colNome, this::celulaNome, Comparator.comparing((Produto p) -> minusculo(p.getNome())));
        Celulas.coluna(colCategoria, p -> Celulas.texto(p.getCategoria()),
                Comparator.comparing((Produto p) -> minusculo(p.getCategoria())));
        Celulas.coluna(colPreco, this::celulaPreco, Comparator.comparing(Produto::getPreco,
                Comparator.nullsLast(Comparator.naturalOrder())));
        Celulas.coluna(colEstoque, this::celulaEstoque, Comparator.comparing(Produto::getQuantidadeEstoque,
                Comparator.nullsLast(Comparator.naturalOrder())));
        Celulas.coluna(colAcoes, this::celulaAcoes, null);
        colPreco.setStyle("-fx-alignment: CENTER-RIGHT;");
        colAcoes.setStyle("-fx-alignment: CENTER-RIGHT;");

        // A ordenação é da própria lista: assim os campos vazios (sem preço, sem categoria...) ficam sempre no fim,
        // seja crescente ou decrescente. A lista abre por código crescente (ver ordenarPeloPadrao); se o usuário
        // tirar a ordenação da coluna, vale a ordem do cadastro (mais recentes primeiro).
        tableProdutos.setSortPolicy(tabela -> true);
        SortedList<Produto> ordenadas = new SortedList<>(produtosFiltrados);
        List<Observable> dependencias = new ArrayList<>();
        dependencias.add(tableProdutos.comparatorProperty());
        dependencias.add(tableProdutos.getSortOrder());
        for (TableColumn<Produto, Produto> coluna : List.of(colCodigo, colNome, colCategoria, colPreco, colEstoque)) {
            dependencias.add(coluna.sortTypeProperty());
        }
        ordenadas.comparatorProperty().bind(Bindings.createObjectBinding(this::comparadorDaTabela,
                dependencias.toArray(new Observable[0])));
        tableProdutos.setItems(ordenadas);

        tableProdutos.setRowFactory(tv -> {
            TableRow<Produto> linha = new TableRow<>();
            linha.setOnMouseClicked(evento -> {
                if (evento.getClickCount() == 2 && !linha.isEmpty()) {
                    editarProduto(linha.getItem());
                }
            });
            return linha;
        });
    }

    // Ordem inicial: código crescente, com a seta no título da coluna (como em Receitas). Voltando de um cadastro,
    // vale a ordem guardada pelo EstadoLista, inclusive "sem ordenação" se o usuário a tirou.
    private void ordenarPeloPadrao() {
        colCodigo.setSortType(TableColumn.SortType.ASCENDING);
        tableProdutos.getSortOrder().add(colCodigo);
    }

    private Comparator<Produto> comparadorDaTabela() {
        if (tableProdutos.getSortOrder().isEmpty()) {
            return null;
        }
        TableColumn<Produto, ?> coluna = tableProdutos.getSortOrder().get(0);
        boolean decrescente = coluna.getSortType() == TableColumn.SortType.DESCENDING;
        Comparator<Produto> principal;
        if (coluna == colCodigo) {
            principal = porValor(Produto::getCodigo, decrescente);
        } else if (coluna == colNome) {
            principal = porValor(p -> minusculo(p.getNome()), decrescente);
        } else if (coluna == colCategoria) {
            principal = porValor(p -> minusculo(p.getCategoria()), decrescente);
        } else if (coluna == colPreco) {
            principal = porValor(Produto::getPreco, decrescente);
        } else if (coluna == colEstoque) {
            principal = porValor(Produto::getQuantidadeEstoque, decrescente);
        } else {
            return tableProdutos.getComparator();
        }
        return principal.thenComparing(porValor(p -> minusculo(p.getNome()), false));
    }

    // Compara pelo valor escolhido; quem não tem o valor (nulo) vai sempre para o fim, em qualquer sentido.
    private static <V extends Comparable<V>> Comparator<Produto> porValor(Function<Produto, V> valor, boolean decrescente) {
        return (a, b) -> {
            V va = valor.apply(a);
            V vb = valor.apply(b);
            if (va == null || vb == null) {
                return va == vb ? 0 : (va == null ? 1 : -1);
            }
            int r = va.compareTo(vb);
            return decrescente ? -r : r;
        };
    }

    // Texto em minúsculas para ordenar sem diferenciar maiúsculas; vazio vira nulo (vai para o fim).
    private static String minusculo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.toLowerCase(Locale.ROOT);
    }

    private Node celulaNome(Produto p) {
        Label nome = new Label(p.getNome());
        nome.getStyleClass().add("v2-link-neutro");
        nome.setTooltip(new Tooltip(p.getNome()));
        HBox linha = new HBox(8, nome);
        linha.setAlignment(Pos.CENTER_LEFT);

        String faltas = camposFaltando(p);
        if (!faltas.isEmpty()) {
            Node alerta = Icones.criar("alerta", 16);
            alerta.getStyleClass().add("v2-icone-alerta");
            Label marca = new Label();
            marca.setGraphic(alerta);
            marca.setTooltip(new Tooltip("Cadastro incompleto: " + faltas));
            marca.setAccessibleText("Cadastro incompleto: " + faltas);
            linha.getChildren().add(marca);
        }
        return linha;
    }

    // Só o nome é obrigatório; o que falta além dele aparece no aviso do ícone de alerta.
    private String camposFaltando(Produto p) {
        List<String> faltas = new ArrayList<>();
        if (p.getCategoria() == null || p.getCategoria().isBlank()) {
            faltas.add("categoria não informada");
        }
        if (p.getPreco() == null) {
            faltas.add("preço não informado");
        }
        if (p.getQuantidadeEstoque() == null) {
            faltas.add("quantidade em estoque não informada");
        }
        return String.join("; ", faltas);
    }

    private Node celulaPreco(Produto p) {
        BigDecimal preco = p.getPreco();
        if (preco == null) {
            return Celulas.texto(null);
        }
        Label valor = new Label(FORMATO_MOEDA.format(preco));
        valor.getStyleClass().add("v2-preco");
        return valor;
    }

    // Quantidade em caixa de largura fixa (os selos ficam alinhados entre as linhas); alerta só quando há o que avisar.
    private Node celulaEstoque(Produto p) {
        Integer quantidade = p.getQuantidadeEstoque();
        if (quantidade == null) {
            return Celulas.texto(null);
        }
        Label numero = new Label(FORMATO_INTEIRO.format(quantidade));
        numero.getStyleClass().add("v2-estoque-numero");
        numero.setMinWidth(32);
        HBox caixa = new HBox(12, numero);
        caixa.setAlignment(Pos.CENTER_LEFT);
        SituacaoEstoque situacao = ProdutoRepository.situacaoDoEstoque(quantidade);
        if (situacao == SituacaoEstoque.SEM_ESTOQUE) {
            Label selo = Celulas.selo("Sem estoque", "v2-selo-sem-estoque");
            selo.setTooltip(new Tooltip("Não há unidades em estoque"));
            caixa.getChildren().add(selo);
        } else if (situacao == SituacaoEstoque.BAIXO) {
            Label selo = Celulas.selo("Estoque baixo", "v2-selo-alerta");
            selo.setTooltip(new Tooltip("Restam poucas unidades (de 1 a " + ProdutoRepository.LIMITE_ESTOQUE_BAIXO + ")"));
            caixa.getChildren().add(selo);
        }
        return caixa;
    }

    private Node celulaAcoes(Produto p) {
        Button editar = Botoes.icone("editar", "Editar " + p.getNome(), false);
        editar.setOnAction(e -> editarProduto(p));
        Button excluir = Botoes.icone("lixeira", "Mover para a lixeira (Delete)", true);
        excluir.setOnAction(e -> pedirExclusao(p));
        return Celulas.acoes(editar, excluir);
    }

    // ===================== Filtros e contagens =====================

    // Busca e categoria: tudo o que filtra, menos o estoque (as contagens dos botões de estoque partem daqui).
    private Predicate<Produto> filtroBase() {
        String termo = txtFiltroProdutos.getText() == null ? "" : txtFiltroProdutos.getText().trim().toLowerCase(Locale.ROOT);
        String categoria = cbCategoriaFiltro.getValue();
        boolean todasCategorias = categoria == null || TODAS_CATEGORIAS.equals(categoria);
        return p -> (todasCategorias || combinaComCategoria(p, categoria))
                && (termo.isEmpty()
                || contem(p.getNome(), termo) || contem(p.getCategoria(), termo) || contem(p.getCodigo(), termo));
    }

    private boolean combinaComCategoria(Produto p, String categoria) {
        boolean semCategoria = p.getCategoria() == null || p.getCategoria().isBlank();
        return SEM_CATEGORIA.equals(categoria) ? semCategoria : categoria.equals(p.getCategoria());
    }

    private boolean contem(String campo, String termo) {
        return campo != null && campo.toLowerCase(Locale.ROOT).contains(termo);
    }

    private boolean combinaComEstoque(Toggle escolhido, Produto p) {
        if (escolhido == tgTodos) {
            return true;
        }
        SituacaoEstoque situacao = ProdutoRepository.situacaoDoEstoque(p.getQuantidadeEstoque());
        return (escolhido == tgEmEstoque && situacao == SituacaoEstoque.NORMAL)
                || (escolhido == tgEstoqueBaixo && situacao == SituacaoEstoque.BAIXO)
                || (escolhido == tgSemEstoque && situacao == SituacaoEstoque.SEM_ESTOQUE)
                || (escolhido == tgNaoInformado && situacao == SituacaoEstoque.NAO_INFORMADO);
    }

    // As categorias do filtro são as dos produtos cadastrados; o que estava escolhido continua, se ainda existir.
    private void atualizarCategorias() {
        atualizandoCategorias = true;
        try {
            String atual = cbCategoriaFiltro.getValue();
            List<String> categorias = repositorio.getItens().stream()
                    .map(Produto::getCategoria)
                    .filter(c -> c != null && !c.isBlank())
                    .distinct()
                    .sorted(Comparator.comparing(c -> c.toLowerCase(Locale.ROOT)))
                    .toList();
            cbCategoriaFiltro.getItems().setAll(TODAS_CATEGORIAS);
            cbCategoriaFiltro.getItems().addAll(categorias);
            if (repositorio.getItens().stream().anyMatch(p -> p.getCategoria() == null || p.getCategoria().isBlank())) {
                cbCategoriaFiltro.getItems().add(SEM_CATEGORIA);
            }
            cbCategoriaFiltro.setValue(atual != null && cbCategoriaFiltro.getItems().contains(atual) ? atual : TODAS_CATEGORIAS);
        } finally {
            atualizandoCategorias = false;
        }
    }

    private void atualizarTudo() {
        if (produtosFiltrados == null) {
            return;
        }
        Predicate<Produto> base = filtroBase();
        Toggle escolhido = grupoEstoque.getSelectedToggle();
        produtosFiltrados.setPredicate(p -> base.test(p) && combinaComEstoque(escolhido, p));

        List<Produto> pelaBase = repositorio.getItens().stream().filter(base).toList();
        contTodos.setText(String.valueOf(pelaBase.size()));
        contEmEstoque.setText(String.valueOf(contar(pelaBase, SituacaoEstoque.NORMAL)));
        contEstoqueBaixo.setText(String.valueOf(contar(pelaBase, SituacaoEstoque.BAIXO)));
        contSemEstoque.setText(String.valueOf(contar(pelaBase, SituacaoEstoque.SEM_ESTOQUE)));
        // A aba "Não informado" só aparece quando há produtos sem quantidade; assim as contagens sempre somam o total.
        long semQuantidade = contar(pelaBase, SituacaoEstoque.NAO_INFORMADO);
        contNaoInformado.setText(String.valueOf(semQuantidade));
        boolean mostrarNaoInformado = semQuantidade > 0 || escolhido == tgNaoInformado;
        tgNaoInformado.setVisible(mostrarNaoInformado);
        tgNaoInformado.setManaged(mostrarNaoInformado);

        int total = repositorio.getItens().size();
        lblResumoLista.setText(total + (total == 1 ? " produto" : " produtos"));
        int exibidos = produtosFiltrados.size();
        lblRodapeContagem.setText(total == 0 ? "Nenhum produto cadastrado"
                : "Mostrando " + exibidos + " de " + total + (total == 1 ? " produto" : " produtos"));
        tableProdutos.setPlaceholder(estadoVazio(total == 0));
    }

    private long contar(List<Produto> produtos, SituacaoEstoque situacao) {
        return produtos.stream().filter(p -> ProdutoRepository.situacaoDoEstoque(p.getQuantidadeEstoque()) == situacao).count();
    }

    private Node estadoVazio(boolean semNenhum) {
        if (semNenhum) {
            Button novo = Botoes.botao("v2-btn-primary", null, "Cadastrar primeiro produto", null);
            novo.setOnAction(e -> handleNovoProduto());
            return Celulas.estadoVazio("produtos", "Nenhum produto cadastrado ainda",
                    "Cadastre os produtos prontos e industrializados do balcão. Só o nome é obrigatório; preço, categoria e quantidade podem ser preenchidos depois.",
                    novo);
        }
        String termo = txtFiltroProdutos.getText() == null ? "" : txtFiltroProdutos.getText().trim();
        Button limpar = Botoes.botao("v2-btn-ghost", null, "Limpar busca e filtros", null);
        limpar.setOnAction(e -> limparFiltros());
        Button novo = Botoes.botao("v2-btn-primary", null, "Cadastrar novo produto", null);
        novo.setOnAction(e -> handleNovoProduto());
        return Celulas.estadoVazio("buscar",
                termo.isEmpty() ? "Nenhum produto neste filtro" : "Nenhum produto para “" + termo + "”",
                "Verifique a grafia, busque pelo código ou pela categoria, ou mude os filtros de estoque e de categoria.",
                limpar, novo);
    }

    private void limparFiltros() {
        txtFiltroProdutos.clear();
        cbCategoriaFiltro.setValue(TODAS_CATEGORIAS);
        tgTodos.setSelected(true);
    }

    // ===================== Ações =====================

    @FXML
    private void handleNovoProduto() {
        ContextoProduto.iniciarNovo();
        abrirFormulario("Novo Produto");
    }

    @FXML
    private void handleLimparBusca() {
        txtFiltroProdutos.clear();
        txtFiltroProdutos.requestFocus();
    }

    @FXML
    private void handleDispensarDica() {
        dicaDispensada = true;
        dicaBox.setVisible(false);
        dicaBox.setManaged(false);
    }

    @FXML
    private void handleAjuda() {
        Caixas.ajuda(rootStack, "Ajuda: Produtos",
                List.of("Estoque baixo vai de 1 a " + ProdutoRepository.LIMITE_ESTOQUE_BAIXO
                                + " unidades e Sem estoque é quantidade 0. Em estoque são os produtos com mais de "
                                + ProdutoRepository.LIMITE_ESTOQUE_BAIXO + " unidades.",
                        "Produtos sem quantidade, preço ou categoria informados aparecem com “—” e com o ícone de alerta ao lado do nome. Os sem quantidade ficam na aba Não informado, que só aparece quando há algum, e as contagens das abas somam o total.",
                        "Busca e filtro de categoria funcionam juntos, e os números dentro dos botões de estoque acompanham os dois.",
                        "Clique no título de uma coluna para ordenar. Preço e estoque ordenam pelo valor, com os vazios por último.",
                        "Produtos excluídos vão para a Lixeira e podem ser restaurados de lá; logo após excluir, o botão Desfazer do aviso também os traz de volta."),
                List.of(new Caixas.Atalho("Ctrl N", "Novo produto"),
                        new Caixas.Atalho("Ctrl F", "Ir para a busca"),
                        new Caixas.Atalho("/", "Ir para a busca (fora de um campo de texto)"),
                        new Caixas.Atalho("Enter", "Abrir o cadastro selecionado"),
                        new Caixas.Atalho("Delete", "Mover o produto selecionado para a lixeira"),
                        new Caixas.Atalho("Esc", "Fechar caixas e limpar a busca"),
                        new Caixas.Atalho("F1", "Abrir esta ajuda")));
    }

    private void editarProduto(Produto produto) {
        if (produto == null) {
            return;
        }
        ContextoProduto.iniciarEdicao(produto);
        abrirFormulario("Editar Produto");
    }

    private void abrirFormulario(String titulo) {
        guardarEstado();
        MenuLateralController.navegar(rootStack, "/fxml/ProdutoFormView.fxml", "Farmácia Homeopática — " + titulo);
    }

    // ===================== Exclusão reversível =====================

    private void pedirExclusao(Produto produto) {
        if (produto == null) {
            return;
        }
        Integer quantidade = produto.getQuantidadeEstoque();
        String estoque = quantidade == null ? "estoque não informado"
                : quantidade == 0 ? "sem estoque"
                : quantidade + (quantidade == 1 ? " unidade em estoque" : " unidades em estoque");
        Caixas.confirmar(rootStack, "lixeira", true,
                "Mover " + produto.getNome() + " para a lixeira?",
                "O produto sai da lista, mas fica guardado na lixeira. Você pode desfazer logo em seguida.",
                produto.getCodigo() + " · " + estoque,
                "Mover para a lixeira", () -> moverParaLixeira(produto));
    }

    private void moverParaLixeira(Produto produto) {
        repositorio.moverParaLixeira(produto);
        Toast.mostrar(rootStack, Toast.Tipo.SUCESSO, produto.getNome() + " movido para a lixeira.", "Desfazer", () -> {
            repositorio.restaurar(produto);
            if (!produtosFiltrados.contains(produto)) {
                limparFiltros();
            }
            tableProdutos.getSelectionModel().select(produto);
            tableProdutos.scrollTo(produto);
            Toast.mostrar(rootStack, Toast.Tipo.INFO, produto.getNome() + " restaurado.");
        });
    }

    // Aviso deixado pelo formulário (cadastrado/atualizado): vira toast e a linha fica selecionada.
    private void exibirAvisoPendente() {
        ContextoProduto.Aviso aviso = ContextoProduto.consumirAviso();
        if (aviso == null) {
            return;
        }
        Toast.mostrar(rootStack, aviso.tipo() == ContextoProduto.TipoAviso.SUCESSO ? Toast.Tipo.SUCESSO : Toast.Tipo.INFO, aviso.texto());
        Produto destaque = aviso.destaque();
        if (destaque != null) {
            if (!produtosFiltrados.contains(destaque)) {
                limparFiltros();
            }
            tableProdutos.getSelectionModel().select(destaque);
            tableProdutos.scrollTo(destaque);
        }
    }
}
