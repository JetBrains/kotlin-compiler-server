package com.compiler.server.compiler.components

import com.compiler.server.compiler.KotlinFile
import com.compiler.server.compiler.KotlinResolutionFacade
import com.compiler.server.model.Analysis
import com.compiler.server.model.ImportInfo
import com.fasterxml.jackson.module.kotlin.isKotlinClass
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.codeInsight.ReferenceVariantsHelper
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.springframework.stereotype.Component
import java.io.File
import java.lang.reflect.Modifier
import java.util.jar.JarFile
import kotlin.reflect.KVisibility

@Component
class AutoImportProvider(
  private val kotlinEnvironment: KotlinEnvironment,
  private val errorAnalyzer: ErrorAnalyzer
  ) {

  fun getClassVariantForZip(file: File): List<ImportInfo> {
    val jarFile = JarFile(file)
    val jarName = jarFile.name.removeSuffix(".jar")
    val allClasses = hashSetOf<ImportInfo>()
    jarFile.entries().toList().map { entry ->
      if (!entry.isDirectory
        && entry.name.endsWith(".class")
        && !entry.name.contains("_")
      ) {
        val name = entry.name.removeSuffix(".class")
        val fullName = name.replace("/", ".")
        try {
          val clazz = ClassLoader.getSystemClassLoader().loadClass(fullName)
          if (clazz.isKotlinClass()) {
            val kotlinClass = clazz.kotlin
            try {
              kotlinClass.nestedClasses.filter {
                it.visibility == KVisibility.PUBLIC
              }.map {
                val canonicalName = it.qualifiedName ?: ""
                val simpleName = it.simpleName ?: ""
                val importInfo = ImportInfo(canonicalName, simpleName, jarName)
//                println(importInfo)
                allClasses.add(importInfo)
              }
              if (kotlinClass.visibility == KVisibility.PUBLIC) {
                val canonicalName = kotlinClass.qualifiedName ?: ""
                val simpleName = kotlinClass.simpleName ?: ""
                val importInfo = ImportInfo(canonicalName, simpleName, jarName)
//                println(importInfo)
                allClasses.add(importInfo)
              }
            } catch (err: UnsupportedOperationException) {
              allClasses.addAll(allClassesFromJavaClass(clazz, jarName))
            } catch (err: AssertionError) {
              println("ERROR_ASSERTION: $fullName")
              allClasses.addAll(allClassesFromJavaClass(clazz, jarName))
            } catch (err: IncompatibleClassChangeError) {
              println("ERROR_INCOMPATIBLE_CLASS_CHANGE: $fullName")
              allClasses.addAll(allClassesFromJavaClass(clazz, jarName))
            } catch (err: InternalError) {
              println("ERROR_INTERNAL: $fullName")
              allClasses.addAll(allClassesFromJavaClass(clazz, jarName))
            }
          } else {
            allClasses.addAll(allClassesFromJavaClass(clazz, jarName))
          }
        } catch (error: VerifyError) {
          println("ERROR_VERIFY: $fullName")
        } catch (error: NoClassDefFoundError) {
          println("ERROR_NO_CLASS_DEF_FOUND: $fullName")
        }
      }
    }
    return allClasses.toList()
  }

  fun allClassesFromJavaClass(clazz: Class<*>, jarName: String): HashSet<ImportInfo> {
    val allClasses = hashSetOf<ImportInfo>()
    clazz.classes.filter {
      Modifier.isPublic(it.modifiers)
    }.map {
      val canonicalName = it.canonicalName
      val simpleName = it.simpleName
      val importInfo = ImportInfo(canonicalName, simpleName, jarName)
//      println(importInfo)
      allClasses.add(importInfo)
    }
    return allClasses
  }

  fun getAllVariants(): List<ImportInfo> {
    val jarFiles = kotlinEnvironment.classpath.drop(1) // executors jar???
    val allVariants = mutableListOf<ImportInfo>()
    jarFiles.map { file ->
      val variants = getClassVariantForZip(file)
      allVariants.addAll(variants)
    }
    return allVariants.toList()
  }

  fun getClassesByName(name: String): List<ImportInfo> {
    return getAllVariants().filter { variant ->
      variant.className == name
    }
  }

  fun getClassByPrefix(prefix: String): List<ImportInfo> {
    return getAllVariants().filter { variant ->
      variant.className.startsWith(prefix)
    }
  }

}
