package com.farmacia.model;

// Situação do pedido de manipulação, na linguagem do balcão.
public enum StatusReceita {
    EM_MANIPULACAO("Em manipulação", "Em manipulação"),
    PRONTA("Pronta para retirada", "Prontas"),
    ENTREGUE("Entregue", "Entregues"),
    CANCELADA("Cancelada", "Canceladas");

    private final String rotulo;
    private final String rotuloPlural;

    StatusReceita(String rotulo, String rotuloPlural) {
        this.rotulo = rotulo;
        this.rotuloPlural = rotuloPlural;
    }

    public String getRotulo() {
        return rotulo;
    }

    // Texto usado nos filtros da lista (ex.: "Prontas").
    public String getRotuloPlural() {
        return rotuloPlural;
    }

    @Override
    public String toString() {
        return rotulo;
    }
}
