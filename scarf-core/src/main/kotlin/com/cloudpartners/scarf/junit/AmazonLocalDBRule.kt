package com.cloudpartners.scarf.junit

import com.cloudpartners.scarf.local.LocalDynamoDB
import org.junit.rules.ExternalResource
import java.io.File


/** This rule creates a LocalDynamoDB instance.
 * It handles all the SQLite native libraries stuff for you.
 *
 * Heavily inspired by the answers here: https://stackoverflow.com/questions/26901613/easier-dynamodb-local-testing
 */
class AmazonLocalDBRule : ExternalResource() {
    private val file = File.createTempFile("scarf","db")
    private val localDynamoDB = LocalDynamoDB(file)

    @Throws(Throwable::class)
    override fun before() {
        localDynamoDB.start()
    }

    override fun after() {
        localDynamoDB.stop()
        file.delete()
    }
}