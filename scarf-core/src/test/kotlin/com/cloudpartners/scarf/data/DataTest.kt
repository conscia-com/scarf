package com.cloudpartners.scarf.data

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable
import org.junit.Rule
import org.junit.Test
import com.cloudpartners.scarf.junit.AmazonLocalDBRule

@DynamoDBTable(tableName="Book")  data class Book(@DynamoDBHashKey val id: String, @DynamoDBRangeKey val name: String, val authorId: String, val isdn: String)

@DynamoDBTable(tableName="Author") data class Author(@DynamoDBHashKey val id: String, val name: String)

class DataTest {
    @Rule @JvmField
    val localAmazon = AmazonLocalDBRule()

    @Test
    fun createTableLocally() {
        val dataStore = DataStore()
        // Just make sure we can write a Book and an Author, the framework should have created the tables behind our backs
        dataStore.mapper.save(Book("1", "some book", "1", "123"))
        dataStore.mapper.save(Author("1", "some author"))
    }
}