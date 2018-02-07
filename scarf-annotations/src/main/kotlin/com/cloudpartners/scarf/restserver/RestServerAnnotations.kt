package com.cloudpartners.scarf.restserver

import kotlinx.serialization.json.JSON
import kotlinx.serialization.Serializable
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import javax.tools.StandardLocation


annotation class ScarfPost(val url: String)
annotation class ScarfGet(val url: String)
annotation class ScarfPut(val url: String)

@Serializable data class UrlParameter(val name: String, val type: String)
@Serializable data class GetInfo(val url: String, val replyType: String, val urlParameters: List<UrlParameter>)
@Serializable data class PostInfo(val url: String, val replyType: String, val requestType: String, val urlParameters: List<UrlParameter>)
@Serializable data class PutInfo(val url: String, val replyType: String, val requestType: String, val urlParameters: List<UrlParameter>)
@Serializable data class RestApiInfo(val posts: List<PostInfo>, val puts: List<PutInfo>, val gets: List<GetInfo>)

class RestApiAnnotationProcessor: AbstractProcessor() {

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        if (roundEnv.processingOver()) return true

        val posts = roundEnv.getElementsAnnotatedWith(ScarfPost::class.java)
        val postInfos = ArrayList<PostInfo>(posts.size)
        for (post in posts) {
            val postAnnotation = post.getAnnotation(ScarfPost::class.java)
            extractInfo( post, { replyType, requestType, urlParameters -> postInfos.add(PostInfo(postAnnotation.url, replyType, requestType, urlParameters)) } )
        }

        val puts = roundEnv.getElementsAnnotatedWith(ScarfPut::class.java)
        val putInfos = ArrayList<PutInfo>(puts.size)
        for (put in puts) {
            val putAnnotation = put.getAnnotation(ScarfPut::class.java)
            extractInfo( put, { replyType, requestType, urlParameters -> putInfos.add(PutInfo(putAnnotation.url, replyType, requestType, urlParameters)) } )
        }

        val gets = roundEnv.getElementsAnnotatedWith(ScarfGet::class.java)
        val getInfos = ArrayList<GetInfo>(gets.size)
        for (get in gets) {
            val getAnnotation = get.getAnnotation(ScarfGet::class.java)
            extractInfo( get, { replyType, _, urlParameters -> getInfos.add(GetInfo(getAnnotation.url, replyType, urlParameters)) } )
        }

        val restApiInfo = RestApiInfo(postInfos, putInfos, getInfos)
        processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, RestApiAnnotationProcessor::class.java.`package`.name)
        val res = processingEnv.filer.createResource(StandardLocation.CLASS_OUTPUT, RestApiAnnotationProcessor::class.java.`package`.name, "scarfrestapi.json")
        res.openWriter().use {
            it.write(JSON.indented.stringify(restApiInfo))
        }
        processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, res.name)
        return true
    }

    private fun extractInfo(post: Element, addIt: (returnType: String, requestType:String, urlParameters: List<UrlParameter>) -> Boolean ) {
        var requestType = ""
        for (child in post.enclosedElements) {
            if (child.kind == ElementKind.METHOD && child.simpleName.toString() == "execute") {
                processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, "Found execute")
                val execute = child as ExecutableElement
                var urlParameters = ArrayList<UrlParameter>(execute.parameters.size - 1)
                for (parameter in execute.parameters) {
                    processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, "Found parameter: ${parameter.simpleName}, type: ${parameter.asType()}")
                    if (parameter.simpleName.toString() == "request") {
                        requestType = parameter.asType().toString()
                    } else {
                        urlParameters.add(UrlParameter(parameter.simpleName.toString(), parameter.asType().toString()))
                    }
                }
                processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, "Returns: ${execute.returnType}")
                addIt(execute.returnType.toString(), requestType, urlParameters)
            }
        }


    }

    override fun getSupportedSourceVersion() = SourceVersion.latest()!!

    override fun getSupportedAnnotationTypes() = setOf(
            ScarfPost::class.java.canonicalName,
            ScarfGet::class.java.canonicalName,
            ScarfPut::class.java.canonicalName)

}
