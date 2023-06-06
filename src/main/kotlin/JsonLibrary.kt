import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.full.*

// Define a Visitor interface with methods to visit different types of JSON elements
interface Visitor {
    fun visit(jsonObject: JsonObject){}
    fun visit(jsonArray: JsonArray){}
}

// Define a JsonElement interface with a method to accept a Visitor
interface JsonElement {
    fun accept(visitor: Visitor)
}

// Define a JsonObject class that implements the JsonElement interface
class JsonObject : JsonElement {

    // Use a mutable map to store key-value pairs
    val map = mutableMapOf<String, Any?>()

    // Add a key-value pair to the map, throwing an exception if the value type is not supported
    fun put(key: String, value: Any?) {
        when(value){
            is String, is Number, is JsonElement, is Boolean, null
                -> map[key] = value
            else -> throw java.lang.IllegalArgumentException("Unsupported value type")
        }
    }

    // Get the value for a given key from the map
    fun get(key: String): Any? {
        return map[key]
    }

    // Remove a key-value pair from the map
    fun remove(key: String) {
        map.remove(key)
    }

    override fun toString(): String {
        return map.entries.joinToString(separator = ",\n ", prefix = "{\n ", postfix = "\n}")
        { (key, value) ->
            "\"$key\" : $value"
        }
    }

    // Override the accept method to accept a Visitor and visit all values in the map that are JsonElements
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
        map.values.forEach { value ->
            when (value) {
                is JsonElement -> value.accept(visitor)
            }
        }
    }
}

// Define a JsonArray class that implements the JsonElement interface
class JsonArray : JsonElement {
    // Use a mutable list to store elements
    val list = mutableListOf<Any?>()

    // Add an element to the list, throwing an exception if the element type is not supported
    fun add(value: Any?) {
        when(value){
            is String, is Number, is JsonElement, is Boolean, null
            -> list.add(value)
            else -> throw java.lang.IllegalArgumentException("Unsupported value type")
        }
    }

    // Get an element at the specified index
    operator fun get(index: Int): Any? {
        return list[index]
    }

    // Set an element at the specified index, throwing an exception if the element type is not supported
    operator fun set(index: Int, value: Any?) {
        when (value) {
            is String, is Number, is JsonElement, is Boolean, null -> list[index] = value
            else -> throw IllegalArgumentException("Unsupported value type")
        }
    }


    override fun toString(): String {
        return list.joinToString(separator = ",\n  ", prefix = "[\n", postfix = "\n]")
    }

    // Override the accept method to accept a Visitor and visit all elements in the list that are JsonElements
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
        list.forEach { value ->
            when (value) {
                is JsonElement -> value.accept(visitor)
            }
        }
    }
}

// Define a GetValuesForKey class that implements the Visitor interface to get all values that match the key (key_name)
class GetValuesForKey(private val key_name: String) : Visitor {
    private val list = mutableListOf<Any?>()

    override fun visit(jsonObject: JsonObject) {
        jsonObject.map.forEach { (key, value) ->
            if (key == key_name)
                list.add(value)
        }
    }

    fun getList(): List<Any?> {
        return list
    }
}

// Define a GetJsonObjectsForProperties class that implements the Visitor interface to get all jsonObjects that have the properties on the list
class GetJsonObjectsForProperties(private val properties: List<String>) : Visitor {
    private val list = mutableListOf<JsonObject>()

    override fun visit(jsonObject: JsonObject) {
        if (properties.all { jsonObject.map.containsKey(it) }) {
            list.add(jsonObject)
        }
    }
    fun getList(): List<Any?> {
        return list
    }
}

// Define a KeyWithValuesOfSameType class that implements the Visitor interface to get if all values that match the key (key_name) are the type requested (valueType)
class KeyWithValuesOfSameType<T : Any>(private val key_name: String, private val valueType: KClass<T>) : Visitor {
    private var allvalueType = true

    override fun visit(jsonObject: JsonObject) {
        if (jsonObject.map.containsKey(key_name)) {
            val value = jsonObject.map[key_name]
            println("Este é o valor = $value")
            if (!valueType.isInstance(value)) {
                println("Este é o valor e entrei no if ")
                allvalueType = false
            }
        }
    }

    override fun visit(jsonArray: JsonArray) {
        jsonArray.list.forEach { element -> element.accept(this) }
    }

    fun areValuesSameType(): Boolean{
        return allvalueType
    }
}

private fun <T : Any> Any?.accept(keyWithValuesOfSameType: KeyWithValuesOfSameType<T>) {
}

class ArrayWithSameType : Visitor {
    private var isSameType = true

    override fun visit(jsonArray: JsonArray) {
        val firstObject = jsonArray.list.firstOrNull() as? JsonObject
        val firstObjectKeys = firstObject?.map?.keys

       for (value in jsonArray.list) {
           val jsonObject = value as? JsonObject
           if (jsonObject != null) {
               if (jsonObject.map.keys != firstObjectKeys) {
                   // As chaves são diferentes
                   isSameType = false
                   break
               }
           }
       }
   }

   fun areValuesSameType(): Boolean{
       return  isSameType
   }
}

// Annotation to exclude properties from instantiation
@Target(AnnotationTarget.FIELD)
annotation class ExcludeFromInstantiation

// Annotation to change the name of a property
@Target(AnnotationTarget.FIELD)
annotation class Rename(val newName: String)

// Annotation to enforce that a property is a string
@Target(AnnotationTarget.FIELD)
annotation class RequiredString

fun getAnnotationName(field: Field):String{
   if(field.getAnnotation(Rename::class.java) != null)
       return field.getAnnotation(Rename::class.java).newName
   return field.name
}

fun mapToJsonObject(map: Map<*, *>): JsonObject {
   val jsonObject = JsonObject()
   map.forEach { (key, value) ->
       when(value){
           is String, is Number, is Boolean, null -> jsonObject.put(key.toString(), value.toString())
           is Enum<*> -> jsonObject.put(key.toString(), enumtoJsonObject(value))
           is Map<*, *> -> jsonObject.put(key.toString(), mapToJsonObject(value))
           else -> throw IllegalArgumentException("Unsupported value type")
       }
   }
   return jsonObject
}

fun collectionToJsonArray(collection: Collection<*>):JsonArray{
   val jsonArray = JsonArray()
   collection.forEach{
           item ->
       val itemJsonObject = JsonObject()
       val itemFields = item?.javaClass?.declaredFields
       itemFields?.forEach { itemField ->
           itemField.isAccessible = true
           val itemName = getAnnotationName(itemField)
           when(val itemValue = itemField.get(item)){
               is String, is Number, is Boolean, null -> itemJsonObject.put(itemName, itemValue.toString())
               is Enum<*> -> itemJsonObject.put(itemName, enumtoJsonObject(itemValue))
               is Map<*, *> -> itemJsonObject.put(itemName, mapToJsonObject(itemValue))
               else -> throw IllegalArgumentException("Unsupported value type")
           }
       }
       jsonArray.add(itemJsonObject)
   }
   return jsonArray
}

fun enumtoJsonObject(enum: Enum<*>): String {
   val jsonObject = JsonObject()
   enum::class.simpleName?.let { jsonObject.put(it, enum.name) }
   return enum.name
}

fun objectToJson(obj: Any):JsonObject {
   val jsonObject = JsonObject()
   val clazz = obj.javaClass
   val fields = clazz.declaredFields
   fields.forEach { field ->
       field.isAccessible = true
       val name = getAnnotationName(field)
       var value = field.get(obj)
       if (field.getAnnotation(ExcludeFromInstantiation::class.java) == null) {
           when (value) {
               is Collection<*> -> jsonObject.put(name, collectionToJsonArray(value))
               is Map<*, *> -> jsonObject.put(name, mapToJsonObject(value))
               is Enum<*> -> jsonObject.put(name, enumtoJsonObject(value))
               else -> {
                   if (field.getAnnotation(RequiredString::class.java) != null) {
                       value = value.toString()
                   }
                   jsonObject.put(name, value)
               }
           }
       }
   }
   println(jsonObject.toString())
   return jsonObject
}


// Some classes that will be used for testing

data class Cadeira(
   val uc:String,
   @RequiredString
   val ects:Double,
   @Rename("data-exame")
   val dataexame:Any?,
   @ExcludeFromInstantiation
   val excluido:String,
   @Rename("inscritos")
   val alunos:List<Aluno>)

data class Aluno(val numero:Int,
                val nome:String,
                val internacional:Boolean)

data class Point(val x: Int, val y: Int)
enum class MyEnumTest { A, B, C }

val myMap = mapOf(
   1 to "One",
   2 to "Two",
   3 to "Three",
   4 to "Four",
   5 to "Five"
)

fun main() {
   val alunos = mutableListOf<Aluno>()
   alunos.add(Aluno(101101, "Dave Farley", true))
   alunos.add(Aluno(101102, "Martin Fowler", true))
   alunos.add(Aluno(26503, "André Santos", false))
   val cadeira = Cadeira("PA", 6.0, null, "abc", alunos)
   val jsonObject = JsonObject()
   jsonObject.put("uc", "PA")
   jsonObject.put("ects", 6.0)
   jsonObject.put("data-exame", null)
   val jsonArray = JsonArray()
   jsonArray.add(JsonObject().apply {
       put("numero", 101101)
       put("nome", "Dave Farley")
       put("internacional2", true)
   })
   jsonArray.add(JsonObject().apply {
       put("numero", 101102)
       put("nome", "Martin Fowler")
       put("internacional", true)
   })
   jsonArray.add(JsonObject().apply {
       put("numero", 26503)
       put("nome", "André Santos")
       put("internacional", false)
   })
   jsonArray.add(1)

   jsonObject.put("inscritos", jsonArray)
   println(jsonObject)

   val visitor = GetValuesForKey("numero")
   jsonObject.accept(visitor)
   println(visitor.getList())
   val keysList = listOf("numero", "nome")
   val visitor2 = GetJsonObjectsForProperties(keysList)
   jsonObject.accept(visitor2)
    println(visitor2.getList())
   val visitor3 = KeyWithValuesOfSameType("internacional", Boolean::class)
   jsonObject.accept(visitor3)
   if (visitor3.areValuesSameType()) {
       println("Todos os valores da propriedade são verdadeiros")
   } else {
       println("Nem todos os valores da propriedade sao verdadeiros")
   }
   val visitor7 = KeyWithValuesOfSameType("nome", String::class)
   jsonObject.accept(visitor7)
   if (visitor7.areValuesSameType()) {
       println("Todos os valores da propriedade são verdadeiros")
   } else {
       println("Nem todos os valores da propriedade sao verdadeiros")
   }
   val visitor8 = KeyWithValuesOfSameType("numero", Int::class)
   jsonObject.accept(visitor8)
   if (visitor8.areValuesSameType()) {
       println("Todos os valores da propriedade são verdadeiros")
   } else {
       println("Nem todos os valores da propriedade sao verdadeiros")
   }
   val visitor10 = KeyWithValuesOfSameType("inscritos", JsonElement::class)
   jsonObject.accept(visitor10)
   if (visitor10.areValuesSameType()) {
       println("Todos os valores da propriedade são verdadeiros")
   } else {
       println("Nem todos os valores da propriedade sao verdadeiros")
   }
   val visitor4 = ArrayWithSameType()
   jsonObject.accept(visitor4)
   if (visitor4.areValuesSameType()) {
       println("Todos os valores dos array tem a mesma estrutura")
   } else {
       println("Nem todos os valores dos array tem a mesma estrutura")
   }
   println("XXXXXXXXX")
   objectToJson(cadeira)
}