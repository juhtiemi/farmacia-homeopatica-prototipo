package com.farmacia.controller;

import com.farmacia.model.Produto;
import com.farmacia.repository.ProdutoRepository;
import com.farmacia.repository.ProdutoRepository.SituacaoEstoque;
import com.farmacia.ui.Alteracoes;
import com.farmacia.ui.AtalhosLista;
import com.farmacia.ui.Botoes;
import com.farmacia.ui.Caixas;
import com.farmacia.ui.ErroInline;
import com.farmacia.ui.Guarda;
import com.farmacia.ui.Toast;
import com.farmacia.util.ContextoProduto;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

// Controller do cadastro de produto (tema v2), nos modos NOVO e EDITAR. Só o nome é obrigatório.
public class ProdutoFormController {

    @FXML
    private StackPane rootStack;
    @FXML
    private BorderPane rootPane;
    @FXML
    private MenuLateralController menuLateralController;
    @FXML
    private Label lnkLista;
    @FXML
    private Label lblTrilhaAtual;
    @FXML
    private Label lblTituloPagina;
    @FXML
    private Label lblSubtituloPagina;

    @FXML
    private TextField txtCodigo;
    @FXML
    private TextField txtNome;
    @FXML
    private ComboBox<String> cbCategoria;
    @FXML
    private TextField txtPreco;
    @FXML
    private Label lblSeloEstoque;
    @FXML
    private TextField txtEstoque;

    @FXML
    private Button btnCancelar;
    @FXML
    private Button btnSalvarProduto;

    private final ProdutoRepository repositorio = ProdutoRepository.getInstance();
    private final ErroInline.Grupo erros = new ErroInline.Grupo();
    private Alteracoes alteracoes;

    private Produto produtoEmEdicao;

    @FXML
    private void initialize() {
        menuLateralController.marcarAtivo(MenuLateralController.PRODUTOS);

        btnCancelar.setGraphic(Botoes.composto(null, "Cancelar", "Esc"));
        btnSalvarProduto.setGraphic(Botoes.composto("ok", "Salvar produto", "Ctrl S"));
        lnkLista.setOnMouseClicked(e -> handleCancelar());

        cbCategoria.getItems().setAll(repositorio.categoriasParaCadastro());
        // Mantém o valor da combo em sincronia com o texto digitado (assim "mexeu no campo" vale na hora).
        cbCategoria.getEditor().textProperty().addListener((obs, antigo, novo) -> {
            String texto = (novo == null || novo.isEmpty()) ? null : novo;
            if (!Objects.equals(texto, cbCategoria.getValue())) {
                cbCategoria.setValue(texto);
            }
        });
        txtEstoque.textProperty().addListener((obs, antigo, novo) -> atualizarSeloEstoque());

        aplicarPedido(ContextoProduto.consumirPedido());

        alteracoes = new Alteracoes(txtNome, cbCategoria, txtPreco, txtEstoque);
        alteracoes.zerar();
        Guarda.ativar(rootStack, alteracoes::isSujo);
        AtalhosLista.instalarFormulario(rootStack, this::handleSalvarProduto, this::handleCancelar);
        txtNome.requestFocus();
    }

    // ===================== Modos =====================

    private void aplicarPedido(ContextoProduto.Pedido pedido) {
        if (pedido.modo() == ContextoProduto.Modo.EDITAR) {
            prepararEdicao(pedido.produto());
        } else {
            prepararNovo();
        }
    }

    private void prepararNovo() {
        produtoEmEdicao = null;
        definirCabecalho("Novo produto", "Novo produto",
                "Só o nome é obrigatório. Categoria, preço e quantidade em estoque podem ser preenchidos depois.");
        preencher(repositorio.proximoCodigo(), new Produto());
    }

    private void prepararEdicao(Produto produto) {
        produtoEmEdicao = produto;
        definirCabecalho("Editar " + produto.getNome(), "Editar produto " + produto.getCodigo(),
                "Altere os dados necessários e salve.");
        preencher(produto.getCodigo(), produto);
    }

    private void definirCabecalho(String titulo, String trilha, String subtitulo) {
        lblTituloPagina.setText(titulo);
        lblTrilhaAtual.setText(trilha);
        lblSubtituloPagina.setText(subtitulo);
    }

    private void preencher(String codigo, Produto base) {
        txtCodigo.setText(codigo);
        txtNome.setText(nz(base.getNome()));
        cbCategoria.setValue(base.getCategoria());
        cbCategoria.getEditor().setText(nz(base.getCategoria()));
        txtPreco.setText(base.getPreco() == null ? "" : formatarPreco(base.getPreco()));
        txtEstoque.setText(base.getQuantidadeEstoque() == null ? "" : String.valueOf(base.getQuantidadeEstoque()));
        atualizarSeloEstoque();
    }

    private String nz(String texto) {
        return texto == null ? "" : texto;
    }

    // Preço no campo no jeito brasileiro: 12,50.
    private String formatarPreco(BigDecimal preco) {
        return preco.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    // Enquanto digita, mostra dentro do campo se a quantidade vai aparecer como estoque baixo ou zerado na lista.
    private void atualizarSeloEstoque() {
        Integer quantidade = lerQuantidade(txtEstoque.getText());
        SituacaoEstoque situacao = ProdutoRepository.situacaoDoEstoque(quantidade);
        lblSeloEstoque.getStyleClass().removeAll("v2-selo-alerta", "v2-selo-sem-estoque");
        boolean mostrar = situacao == SituacaoEstoque.BAIXO || situacao == SituacaoEstoque.SEM_ESTOQUE;
        if (mostrar) {
            lblSeloEstoque.setText(situacao == SituacaoEstoque.BAIXO ? "Estoque baixo" : "Sem estoque");
            lblSeloEstoque.getStyleClass().add(situacao == SituacaoEstoque.BAIXO ? "v2-selo-alerta" : "v2-selo-sem-estoque");
        }
        lblSeloEstoque.setVisible(mostrar);
    }

    // ===================== Leitura dos campos =====================

    // Preço: aceita vírgula ou ponto e até 2 casas ("12,50", "12.5", "3"); um "R$" no início é ignorado.
    // Devolve nulo quando o formato não é válido (campo vazio é tratado antes: preço é opcional).
    static BigDecimal lerPreco(String texto) {
        if (texto == null) {
            return null;
        }
        String t = texto.trim();
        if (t.regionMatches(true, 0, "R$", 0, 2)) {
            t = t.substring(2).trim();
        }
        if (!t.matches("\\d{1,9}([.,]\\d{1,2})?")) {
            return null;
        }
        return new BigDecimal(t.replace(',', '.')).setScale(2, RoundingMode.HALF_UP);
    }

    // Quantidade: número inteiro igual ou maior que zero. Nulo quando não é válido.
    static Integer lerQuantidade(String texto) {
        if (texto == null) {
            return null;
        }
        String t = texto.trim();
        return t.matches("\\d{1,9}") ? Integer.valueOf(t) : null;
    }

    // ===================== Salvar / cancelar =====================

    @FXML
    private void handleSalvarProduto() {
        erros.limpar();
        String nome = txtNome.getText() == null ? "" : txtNome.getText().trim();
        String textoPreco = valorOuNulo(txtPreco.getText());
        String textoEstoque = valorOuNulo(txtEstoque.getText());

        if (nome.isEmpty()) {
            erros.mostrar(txtNome, "Informe o nome do produto.");
        }
        BigDecimal preco = null;
        if (textoPreco != null) {
            preco = lerPreco(textoPreco);
            if (preco == null) {
                erros.mostrar(txtPreco, "Preço inválido: use números, por exemplo 12,50");
            }
        }
        Integer estoque = null;
        if (textoEstoque != null) {
            estoque = lerQuantidade(textoEstoque);
            if (estoque == null) {
                erros.mostrar(txtEstoque, textoEstoque.startsWith("-")
                        ? "Quantidade inválida: use um número inteiro, sem sinal de menos"
                        : "Quantidade inválida: use um número inteiro");
            }
        }

        if (erros.temErros()) {
            Toast.mostrar(rootStack, Toast.Tipo.ERRO, "Corrija os campos destacados para salvar.");
            // Foco no primeiro campo com erro, na ordem em que aparecem na tela.
            for (Control campo : List.of(txtNome, txtPreco, txtEstoque)) {
                if (erros.campos().contains(campo)) {
                    campo.requestFocus();
                    break;
                }
            }
            return;
        }

        // Nome repetido: não bloqueia, mas pergunta (pode ser outra apresentação do mesmo produto).
        Produto homonimo = repositorio.getItens().stream()
                .filter(p -> p != produtoEmEdicao && p.getNome() != null && p.getNome().trim().equalsIgnoreCase(nome))
                .findFirst().orElse(null);
        final BigDecimal precoFinal = preco;
        final Integer estoqueFinal = estoque;
        if (homonimo != null) {
            Caixas.confirmar(rootStack, "alerta", false, "Já existe um produto com este nome",
                    "Existe um cadastro de " + homonimo.getNome() + " (" + homonimo.getCodigo() + "). Se for o mesmo produto, cancele e edite o cadastro existente.",
                    null, "Salvar mesmo assim", () -> gravar(nome, precoFinal, estoqueFinal));
            return;
        }
        gravar(nome, preco, estoque);
    }

    private void gravar(String nome, BigDecimal preco, Integer estoque) {
        Produto salvo;
        String mensagem;
        if (produtoEmEdicao == null) {
            salvo = new Produto();
            preencherProduto(salvo, nome, preco, estoque);
            repositorio.adicionar(salvo);
            mensagem = "Produto " + nome + " cadastrado com sucesso.";
        } else {
            salvo = produtoEmEdicao;
            preencherProduto(salvo, nome, preco, estoque);
            repositorio.atualizar(salvo);
            mensagem = "Cadastro de " + nome + " atualizado.";
        }
        ContextoProduto.deixarAviso(ContextoProduto.TipoAviso.SUCESSO, mensagem, salvo);
        alteracoes.zerar();
        voltarParaLista();
    }

    private void preencherProduto(Produto produto, String nome, BigDecimal preco, Integer estoque) {
        produto.setNome(nome);
        produto.setCategoria(categoriaDigitada());
        produto.setPreco(preco);
        produto.setQuantidadeEstoque(estoque);
    }

    // Categoria opcional; se for a mesma já existente com outra caixa (fitoterápico/Fitoterápico), usa a grafia cadastrada.
    private String categoriaDigitada() {
        String digitada = valorOuNulo(cbCategoria.getEditor().getText());
        if (digitada == null) {
            digitada = valorOuNulo(cbCategoria.getValue());
        }
        if (digitada == null) {
            return null;
        }
        for (String existente : repositorio.categoriasParaCadastro()) {
            if (existente.equalsIgnoreCase(digitada)) {
                return existente;
            }
        }
        return digitada;
    }

    private String valorOuNulo(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }

    @FXML
    private void handleCancelar() {
        // Com alterações pendentes, o Guarda pergunta antes de descartar.
        Guarda.sair(this::voltarParaLista);
    }

    private void voltarParaLista() {
        Guarda.limpar();
        MenuLateralController.navegar(rootStack, "/fxml/ProdutoView.fxml", "Farmácia Homeopática — Gerenciamento de Produtos");
    }
}
