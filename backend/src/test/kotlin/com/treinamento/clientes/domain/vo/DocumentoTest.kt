package com.treinamento.clientes.domain.vo

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class DocumentoTest {

    @Test
    fun `CPF valido sem mascara`() {
        val doc = Documento.of("39053344705")
        assertEquals("39053344705", doc.valor)
        assertEquals(TipoDocumento.CPF, doc.tipo)
    }

    @Test
    fun `CPF valido com mascara`() {
        val doc = Documento.of("390.533.447-05")
        assertEquals("39053344705", doc.valor)
    }

    @Test
    fun `CNPJ valido`() {
        val doc = Documento.of("11222333000181")
        assertEquals("11222333000181", doc.valor)
        assertEquals(TipoDocumento.CNPJ, doc.tipo)
    }

    @Test
    fun `CNPJ valido com mascara`() {
        val doc = Documento.of("11.222.333/0001-81")
        assertEquals("11222333000181", doc.valor)
    }

    @ParameterizedTest
    @ValueSource(strings = ["00000000000", "11111111111", "12345", "abc", "", "00000000000000"])
    fun `documentos invalidos lancam excecao`(valor: String) {
        assertThrows<IllegalArgumentException> { Documento.of(valor) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["39053344705", "529.982.247-25", "08443555505"])
    fun `ehValido retorna true para CPFs validos`(valor: String) {
        assertTrue(Documento.ehValido(valor))
    }

    @ParameterizedTest
    @ValueSource(strings = ["00000000000", "12345678901", "abcdefghijk"])
    fun `ehValido retorna false para CPFs invalidos`(valor: String) {
        assertFalse(Documento.ehValido(valor))
    }

    @Test
    fun `equals e hashCode funcionam`() {
        val d1 = Documento.of("39053344705")
        val d2 = Documento.of("390.533.447-05")
        assertEquals(d1, d2)
        assertEquals(d1.hashCode(), d2.hashCode())
    }

    @Test
    fun `fromDatabase nao valida`() {
        val doc = Documento.fromDatabase("39053344705")
        assertEquals("39053344705", doc.valor)
    }
}
