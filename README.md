# JsonLibraryProject
Projeto para a cadeira de Programação Avançada

Objetivo: Desenvolver uma biblioteca Json

Existem duas classes, JsonObject e JsonArray que implementam a interface JsonElement que tem um visitor.
O jsonObject é um mapa com uma chave e um valor, <String, Any?>, que pode receber valores do tipo String, Number, JsonElement, Boolean e null, através da função put, caso contrário devolve uma excepção. A função get ao receber uma key irá devolver o valor. A fun remove irá remover a key dada do map.
O JsonArray é uma lista, para adicionar valores a esta lista utiliza-se a função add, pode receber valores String, Number, JsonElement, Boolean e null, caso contrário devolve uma excepção. Existe a possibilidade de receber o elemento através do index, função get. Assim como meter um valor num index especifico, tem de ser os valores denominados anteriormente, caso contrário lança uma excepção, isto ocorre através da função set.

Ambas as classes implementam a interface JsonElement que correr a interface visitor, com esta interface serão implementados alguns métodos.

GetValuesForKey: Implementa a interface do visitor para devolver todos os valores que correspondam a key

GetJsonObjectsForProperties: Implementa a interface visitor para devolver todos os jsonObjects que tenham as propriedades dadas numa lista.

KeyWithValuesOfSameType: Implementa a interface visitor para verificar se todos os valores que correspondem à chave dada, são do tipo solicitado.

ArrayWithSameType: Implementa a interface visitor para verificar se todos os elementos do JsonArray tem a mesma estrutura.

Anotações: - ExcludeFromInstantiation, exclui propriedades da instaciação
           - Rename, mudar o nome da propriedade
           - RequiredString, mudar o tipo da propriedade para String
        
Classes de teste criadas: Cadeira e Aluno

objectToJson: A função recebe um objeto e retorna um JsonObject.

Estas funcionalidades foram testadas no ficheiro testJsonLibrary.
