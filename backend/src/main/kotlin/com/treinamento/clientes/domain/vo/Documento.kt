package com.treinamento.clientes.domain.vo

enum class TipoDocumento { CPF, CNPJ }

class Documento private constructor(val valor: String) {

    val tipo: TipoDocumento
        get() = if (valor.length == 11) TipoDocumento.CPF else TipoDocumento.CNPJ

    companion object {
        fun of(entrada: String): Documento {
            val normalizado = entrada.replace(Regex("[^0-9]"), "")
            require(ehValido(normalizado)) { "Documento inválido: $entrada" }
            return Documento(normalizado)
        }

        fun ehValido(valor: String): Boolean {
            val normalizado = if (valor.contains(Regex("[^0-9]"))) {
                valor.replace(Regex("[^0-9]"), "")
            } else valor

            return when (normalizado.length) {
                11 -> validarCpf(normalizado)
                14 -> validarCnpj(normalizado)
                else -> false
            }
        }

        private fun validarCpf(cpf: String): Boolean {
            if (cpf.all { it == cpf[0] }) return false

            val digito1 = calcularDigito(cpf, 9, intArrayOf(10, 9, 8, 7, 6, 5, 4, 3, 2))
            val digito2 = calcularDigito(cpf, 10, intArrayOf(11, 10, 9, 8, 7, 6, 5, 4, 3, 2))

            return cpf[9].digitToInt() == digito1 && cpf[10].digitToInt() == digito2
        }

        private fun validarCnpj(cnpj: String): Boolean {
            if (cnpj.all { it == cnpj[0] }) return false

            val digito1 = calcularDigito(cnpj, 12, intArrayOf(5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2))
            val digito2 = calcularDigito(cnpj, 13, intArrayOf(6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2))

            return cnpj[12].digitToInt() == digito1 && cnpj[13].digitToInt() == digito2
        }

        private fun calcularDigito(doc: String, qtdDigitos: Int, pesos: IntArray): Int {
            val soma = (0 until qtdDigitos).sumOf { doc[it].digitToInt() * pesos[it] }
            val resto = soma % 11
            return if (resto < 2) 0 else 11 - resto
        }
    }

    override fun equals(other: Any?): Boolean = other is Documento && other.valor == valor
    override fun hashCode(): Int = valor.hashCode()
    override fun toString(): String = valor
}
