package com.cloudpartners.scarf.data

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable
import com.amazonaws.services.dynamodbv2.model.*
import java.io.File
import java.util.Arrays.asList
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

class DataAnnotationProcessor: AbstractProcessor() {

    data class Attribute(var name: String, var type: String) {
        fun terraformType(): String {
            return "S"
        }
    }
    class Table(var table : DynamoDBTable, var pKey: Attribute? = null, var sKey: Attribute? = null) {
        fun toTerraform(): String {
            return """
                resource "aws_dynamodb_table" "${table.tableName}" {
                    name           = "${table.tableName}"
                    read_capacity  = 2
                    write_capacity = 2
                    ${outputAttributes()}
                    ${outputHashKey()}
                    ${outputSortKey()}
                }
                """
        }

        private fun outputHashKey(): String {
            return if (pKey != null) {
                """hash_key="${pKey!!.name}""""
            } else {
                ""
            }
         }

        private fun outputSortKey(): String {
            return if (sKey != null) {
                """range_key="${sKey!!.name}""""
            } else {
                ""
            }
        }

        private fun outputAttributes(): String {
            var res = ""
            asList(pKey , sKey)
                    .filterNotNull()
                    .forEach {
                        res += """
                        attribute {
                            name = "${it.name}"
                            type = "${it.terraformType()}"
                         }
                         """
                    }
            return res
        }

        private fun outputAttributeDefinitions(): String {
            var res = ""
            asList(pKey , sKey)
                    .filterNotNull()
                    .forEach {
                        res += """,AttributeDefinition("${it.name}", "${it.terraformType()}")"""
                    }

            return if (res == "")
                "Collections.emptyList()"
            else
                "Arrays.asList(${res.substring(1)})"
        }

        fun toLocalCreationCode(): String {
            return """
                localDynamoDBHelper.createOrUpdate(
                    "${table.tableName}",
                    ${outputAttributeDefinitions()},
                    "${pKey?.name}",
                    ${if (sKey != null) "\""+sKey?.name+"\"" else "null"}
                )
                """
        }
    }

    private var tables: MutableMap<String, Table> = mutableMapOf()

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, "Processing ${roundEnv.processingOver()}")
        processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, processingEnv.options.toString())

        if (roundEnv.processingOver()) return true
        val sourceDir = processingEnv.options["kapt.kotlin.generated"]


        val storageElements = roundEnv.getElementsAnnotatedWith(DynamoDBTable::class.java)

        val elementUtils = processingEnv.elementUtils
        // val filer = processingEnv.filer

        for (storageElement in storageElements) {
            val table = storageElement.getAnnotation(DynamoDBTable::class.java)
            val packageName = elementUtils.getPackageOf(storageElement).qualifiedName.toString()
            val simpleName = storageElement.simpleName.toString()

            val tableName = "$packageName.$simpleName"
            tables[tableName] = Table(table)
        }


        val pKeyElements = roundEnv.getElementsAnnotatedWith(DynamoDBHashKey::class.java)

        for (pKeyElement in pKeyElements) {
            val packageName = elementUtils.getPackageOf(pKeyElement).qualifiedName.toString()
            val simpleName = pKeyElement.simpleName.toString()
            val className = enclosingClass(pKeyElement)
            val type = pKeyElement.asType().toString()

            val tableName = "$packageName.$className"
            processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, "pkey: $simpleName: $tableName")
            tables[tableName]?.pKey = Attribute(simpleName, type)
        }

        val sKeyElements = roundEnv.getElementsAnnotatedWith(DynamoDBRangeKey::class.java)

        for (sKeyElement in sKeyElements) {
            val packageName = elementUtils.getPackageOf(sKeyElement).qualifiedName.toString()
            val simpleName = sKeyElement.simpleName.toString()
            val className = enclosingClass(sKeyElement)
            val type = sKeyElement.asType().toString()

            val tableName = "$packageName.$className"
            tables[tableName]?.sKey = Attribute(simpleName, type)
        }

        val terraformDir = sourceDir + "/../../terraform"
        File(terraformDir).mkdir()
        tables.values.forEach {
            File("$terraformDir/${it.table.tableName}.tf").writeText(it.toTerraform())
        }

        var classDef = """
            package com.cloudpartners.scarf.data
            import java.util.*
            import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
            import com.amazonaws.services.dynamodbv2.model.AttributeDefinition
            class AutoGenerate: GeneratedSourcesInterface {
               override fun doIt(client: AmazonDynamoDB) {
                 val localDynamoDBHelper = LocalDynamoDBHelper(client)
            """
        tables.values.forEach { classDef += it.toLocalCreationCode() }
        classDef += """
               }
            }
            """

        File(sourceDir+"/GENcreateTables.kt").writeText(classDef)
        return true
    }

    private fun enclosingClass(sKeyElement: Element): String {
        var enclosing = sKeyElement.enclosingElement
        while (enclosing.kind != ElementKind.CLASS) enclosing = enclosing.enclosingElement
        return enclosing.simpleName.toString()
    }

    override fun getSupportedSourceVersion() = SourceVersion.latest()!!

    override fun getSupportedAnnotationTypes() = setOf(
            DynamoDBTable::class.java.canonicalName,
            DynamoDBHashKey::class.java.canonicalName,
            DynamoDBRangeKey::class.java.canonicalName)

    //override fun getSupportedOptions() = setOf()
}

class LocalDynamoDBHelper(private val client: AmazonDynamoDB) {
    private fun tableExists(name: String): Boolean {
        return try {
            client.describeTable(name)
            true
        } catch (e: ResourceNotFoundException) {
            false
        }
    }

    fun createOrUpdate(name: String, attributes: List<AttributeDefinition>, hashKey: String, rangeKey: String?) {
        val hashKeySchemaElement = KeySchemaElement(hashKey, KeyType.HASH)
        val rangeKeySchemaElement = if (rangeKey != null) KeySchemaElement(rangeKey, KeyType.RANGE) else null

        val keySchema : List<KeySchemaElement> =
                asList(hashKeySchemaElement, rangeKeySchemaElement)
                        .filterNotNull()

        if (tableExists(name)) {
            // TODO: Recreate table if structure has changed.
        } else {
            client.createTable(
                    CreateTableRequest()
                            .withTableName(name)
                            .withAttributeDefinitions(attributes)
                            .withKeySchema(keySchema)
                            .withProvisionedThroughput(ProvisionedThroughput(2,2)))
        }
    }
}

interface GeneratedSourcesInterface {
    fun doIt(client: AmazonDynamoDB)
}

