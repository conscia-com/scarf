package com.cloudpartners.scarf.local

import com.cloudpartners.scarf.restserver.RestApiInfo
import kotlinx.serialization.json.JSON
import spark.Service
import java.io.FileReader
import java.util.*

class LocalRestServer {
    lateinit var spark : Service

    fun start() {
        spark = Service.ignite()
        val restApiInfo = loadDefinition()

        for (post in restApiInfo.posts) {
            spark.post(post.url, {request,reply ->
                val payload = request.queryParams("payload")

                null
            })
        }
    }

    fun loadDefinition() : RestApiInfo {
        val url = this.javaClass.getResource("scarfrestapi.json") ?: return RestApiInfo(Collections.emptyList(), Collections.emptyList(), Collections.emptyList())
        FileReader(url.file).use {
            return JSON.parse<RestApiInfo>(it.readText())
        }
    }

    fun stop() {
        spark.stop()
    }


}