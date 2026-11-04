package com.farmacia.controller;

import com.farmacia.repository.ReceitaRepository;
import com.farmacia.ui.Icones;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

// Controller do menu lateral compartilhado: navegação entre as telas e destaque do item ativo.
public class MenuLateralController {

    public static final String CLIENTES = "clientes";
    public static final String RECEITAS = "receitas";
    public static final String MEDICOS = "medicos";
    public static final String FUNCIONARIOS = "funcionarios";
    public static final String PRODUTOS = "produtos";
    public static final String LIXEIRA = "lixeira";

    @FXML
    private VBox menu;
    @FXML
    private StackPane logo;
    @FXML
    private Button btnNavClientes;
    @FXML
    private Button btnNavReceitas;
    @FXML
    private Button btnNavMedicos;
    @FXML
    private Button btnNavFuncionarios;
    @FXML
    private Button btnNavProdutos;
    @FXML
    private Button btnNavLixeira;
    @FXML
    private Label lblContadorLixeira;
    @FXML
    private Button btnTrocarOperador;

    @FXML
    private void initialize() {
        logo.getChildren().add(Icones.criar("produtos", 22));
        btnNavClientes.setGraphic(Icones.criar("clientes", 20));
        btnNavReceitas.setGraphic(Icones.criar("receitas", 20));
        btnNavMedicos.setGraphic(Icones.criar("medicos", 20));
        btnNavFuncionarios.setGraphic(Icones.criar("funcionarios", 20));
        btnNavProdutos.setGraphic(Icones.criar("produtos", 20));
        btnNavLixeira.setGraphic(Icones.criar("lixeira", 20));
        btnTrocarOperador.setGraphic(Icones.criar("sair", 18));
        btnTrocarOperador.setTooltip(new Tooltip("Trocar operador"));
        btnTrocarOperador.setAccessibleText("Trocar operador");

        // Contador de itens na lixeira, visível só quando há algo nela.
        var total = Bindings.size(ReceitaRepository.getInstance().getLixeira())
                .add(Bindings.size(com.farmacia.repository.ClienteRepository.getInstance().getLixeira()))
                .add(Bindings.size(com.farmacia.repository.MedicoRepository.getInstance().getLixeira()))
                .add(Bindings.size(com.farmacia.repository.ProdutoRepository.getInstance().getLixeira()))
                .add(Bindings.size(com.farmacia.repository.FuncionarioRepository.getInstance().getLixeira()));
        lblContadorLixeira.textProperty().bind(total.asString());
        lblContadorLixeira.visibleProperty().bind(total.greaterThan(0));
    }

    // Marca o item da tela atual (uma das constantes desta classe).
    public void marcarAtivo(String chave) {
        for (Button botao : new Button[]{btnNavClientes, btnNavReceitas, btnNavMedicos, btnNavProdutos, btnNavFuncionarios, btnNavLixeira}) {
            botao.getStyleClass().remove("v2-nav-on");
        }
        Button ativo = switch (chave) {
            case CLIENTES -> btnNavClientes;
            case RECEITAS -> btnNavReceitas;
            case MEDICOS -> btnNavMedicos;
            case FUNCIONARIOS -> btnNavFuncionarios;
            case PRODUTOS -> btnNavProdutos;
            case LIXEIRA -> btnNavLixeira;
            default -> null;
        };
        if (ativo != null) {
            ativo.getStyleClass().add("v2-nav-on");
            ativo.setAccessibleText(ativo.getText() + ", página atual");
        }
    }

    @FXML
    private void handleNavClientes() {
        navegarPara("/fxml/ClienteView.fxml", "Farmácia Homeopática — Gerenciamento de Clientes");
    }

    @FXML
    private void handleNavReceitas() {
        navegarPara("/fxml/ReceitaView.fxml", "Farmácia Homeopática — Gerenciamento de Receitas");
    }

    @FXML
    private void handleNavMedicos() {
        navegarPara("/fxml/MedicoView.fxml", "Farmácia Homeopática — Gerenciamento de Médicos");
    }

    @FXML
    private void handleNavFuncionarios() {
        navegarPara("/fxml/FuncionarioView.fxml", "Farmácia Homeopática — Gerenciamento de Funcionários");
    }

    @FXML
    private void handleNavProdutos() {
        navegarPara("/fxml/ProdutoView.fxml", "Farmácia Homeopática — Gerenciamento de Produtos");
    }

    @FXML
    private void handleNavLixeira() {
        navegarPara("/fxml/LixeiraView.fxml", "Farmácia Homeopática — Lixeira");
    }

    // Troca a raiz da Scene atual pelo FXML solicitado (mesmo mecanismo das demais telas).
    // Se houver formulário com alterações não salvas, pergunta antes de sair (ver Guarda).
    public static void navegar(javafx.scene.Node origem, String caminhoFxml, String titulo) {
        com.farmacia.ui.Guarda.sair(() -> navegarDireto(origem, caminhoFxml, titulo));
    }

    private static void navegarDireto(javafx.scene.Node origem, String caminhoFxml, String titulo) {
        try {
            Parent novaTela = FXMLLoader.load(MenuLateralController.class.getResource(caminhoFxml));
            Stage stage = (Stage) origem.getScene().getWindow();
            stage.setTitle(titulo);
            stage.getScene().setRoot(novaTela);
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Não foi possível abrir a tela " + caminhoFxml, e);
        }
    }

    // Trocar de módulo pelo menu recomeça as listas do zero; só a volta de um cadastro mantém busca e filtros.
    private void navegarPara(String caminhoFxml, String titulo) {
        com.farmacia.ui.Guarda.sair(() -> {
            com.farmacia.ui.EstadoLista.esquecerTudo();
            com.farmacia.util.ContextoReceita.descartarRascunho();
            navegarDireto(menu, caminhoFxml, titulo);
        });
    }
}
