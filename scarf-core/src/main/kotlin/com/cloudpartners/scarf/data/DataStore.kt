package com.cloudpartners.scarf.data

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper

class DataStore(val client: AmazonDynamoDB) {
    companion object {
        operator fun invoke(): DataStore {
            return if (dynDBInstance != null) DataStore(dynDBInstance!!)
            else DataStore(AmazonDynamoDBClientBuilder.defaultClient())
        }
        var dynDBInstance : AmazonDynamoDB? = null
    }

    val mapper = DynamoDBMapper(client)
}
