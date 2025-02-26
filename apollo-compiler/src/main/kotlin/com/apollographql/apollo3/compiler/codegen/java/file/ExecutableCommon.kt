package com.apollographql.apollo3.compiler.codegen.java.file

import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo3.compiler.codegen.Identifier.root
import com.apollographql.apollo3.compiler.codegen.Identifier.rootField
import com.apollographql.apollo3.compiler.codegen.Identifier.serializeVariables
import com.apollographql.apollo3.compiler.codegen.Identifier.toJson
import com.apollographql.apollo3.compiler.codegen.Identifier.writer
import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.codegen.java.JavaResolver
import com.apollographql.apollo3.compiler.codegen.java.L
import com.apollographql.apollo3.compiler.codegen.java.S
import com.apollographql.apollo3.compiler.codegen.java.T
import com.apollographql.apollo3.compiler.codegen.java.adapter.singletonAdapterInitializer
import com.apollographql.apollo3.compiler.ir.IrProperty
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import javax.lang.model.element.Modifier

fun serializeVariablesMethodSpec(
    adapterClassName: TypeName?,
    emptyMessage: String,
): MethodSpec {

  val body = if (adapterClassName == null) {
    CodeBlock.of("// $emptyMessage\n")
  } else {
    CodeBlock.of("$T.INSTANCE.$toJson($writer, $customScalarAdapters, this);\n", adapterClassName)
  }
  return MethodSpec.methodBuilder(serializeVariables)
      .addModifiers(Modifier.PUBLIC)
      .addException(JavaClassNames.IOException)
      .addAnnotation(JavaClassNames.Override)
      .addParameter(JavaClassNames.JsonWriter, writer)
      .addParameter(JavaClassNames.CustomScalarAdapters, customScalarAdapters)
      .addCode(body)
      .build()
}

fun adapterMethodSpec(
    resolver: JavaResolver,
    property: IrProperty,
): MethodSpec {
  val adaptedTypeName = resolver.resolveIrType(property.info.type)
  return MethodSpec.methodBuilder(Identifier.adapter)
      .addModifiers(Modifier.PUBLIC)
      .addAnnotation(JavaClassNames.Override)
      .returns(ParameterizedTypeName.get(JavaClassNames.Adapter, adaptedTypeName))
      .addCode(
          "return $L;\n",
          resolver.adapterInitializer(property.info.type, property.requiresBuffering)
      )
      .build()
}

fun rootFieldMethodSpec(context: JavaContext, typeInScope: String, selectionsClassName: ClassName): MethodSpec {
  return MethodSpec.methodBuilder(rootField)
      .addModifiers(Modifier.PUBLIC)
      .addAnnotation(JavaClassNames.Override)
      .returns(JavaClassNames.CompiledField)
      .addCode(
          CodeBlock.builder()
              .add("return new $T(\n", JavaClassNames.CompiledFieldBuilder)
              .indent()
              .add("$S,\n", Identifier.data)
              .add("$L\n", context.resolver.resolveCompiledType(typeInScope))
              .unindent()
              .add(")\n")
              .add(".${Identifier.selections}($T.$root)\n", selectionsClassName)
              .add(".build();\n")
              .build()
      )
      .build()
}
