package com.cloudpartners.scarf.requests

import com.cloudpartners.scarf.local.LocalRestServer
import com.cloudpartners.scarf.restserver.ScarfPost
import org.junit.Test

data class AddSupervisorRequest (val name: String)
data class AddSupervisorReply (val status: String)

@ScarfPost("/companies/:companyId/addsupervisor")
class AddSupervisor {
    fun execute(companyId: String, request: AddSupervisorRequest): AddSupervisorReply {
        return AddSupervisorReply("companyID=$companyId, name=${request.name}")
    }
}

class PostTest {
    @Test
    fun annotations() {
        LocalRestServer().loadDefinition()
    }
}

