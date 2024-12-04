import org.gradle.api.attributes.Category
import org.gradle.api.model.ObjectFactory

val ObjectFactory.categoryComposeCache
    get() = named(Category::class.java, "compose-cache")