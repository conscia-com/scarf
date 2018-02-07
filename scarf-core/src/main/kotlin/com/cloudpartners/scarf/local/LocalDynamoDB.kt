package com.cloudpartners.scarf.local

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded
import com.cloudpartners.scarf.data.DataStore
import com.cloudpartners.scarf.data.GeneratedSourcesInterface
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.regex.Pattern

class LocalDynamoDB(val dbFileName: File) {
    lateinit var client: AmazonDynamoDB
    lateinit var sqliteLibFolder: File
    fun start() {
        sqliteLibFolder = File.createTempFile("scarf", "", null)
        sqliteLibFolder.delete()
        sqliteLibFolder.mkdir()

        copySqliteNatives()
        System.setProperty("sqlite4java.library.path", sqliteLibFolder.toString())

        client = DynamoDBEmbedded.create(dbFileName).amazonDynamoDB()
        DataStore.dynDBInstance = client

        // Load GENcreateTables.kt if it exists
        try {
            val c = Class.forName("com.cloudpartners.serverlessfm.data.AutoGenerate").kotlin
            val o = c.constructors.filter { it.parameters.isEmpty() }.first().call() as GeneratedSourcesInterface
            println("Creating tables")
            o.doIt(this.client)
        } catch (e: ClassNotFoundException) {
            println("No autogeneration clasa found, so not creating any tables")
        }

    }

    private fun copySqliteNatives() {
        val matcher = Pattern.compile(".*sqlite.*")

        val classPath = System.getProperty("java.class.path", ".")
        val classPathElements = classPath.split(System.getProperty("path.separator").toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        for (element in classPathElements) {
            val tmp = element.toLowerCase(Locale.UK)
            if (matcher.matcher(tmp).matches() && (tmp.endsWith("dll") || tmp.endsWith("so") || tmp.endsWith("dylib"))) {
                val currentFile = File(element)
                Files.copy(Paths.get(currentFile.toURI()),
                        Paths.get(File(sqliteLibFolder, currentFile.name).toURI()))

            }
        }

    }

    fun stop() {
        client.shutdown()
        sqliteLibFolder.deleteRecursively()
    }
}