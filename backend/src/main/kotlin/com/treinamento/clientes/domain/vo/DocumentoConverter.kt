package com.treinamento.clientes.domain.vo

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class DocumentoConverter : AttributeConverter<Documento, String> {

    override fun convertToDatabaseColumn(attribute: Documento?): String? =
        attribute?.valor

    override fun convertToEntityAttribute(dbData: String?): Documento? =
        dbData?.let { Documento.of(it) }
}
