import javafx.beans.binding.When
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.event.*
import java.util.*
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

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
    Editor(jsonObject).open()
}

//fazer uma função de painel para sempre que aparecer um jsonObject


class Editor(private val jsonObject: JsonObject) {
    private val undoStack = Stack<Action>()
    private val componentTextFieldMap = mutableMapOf<Component, JTextField>()

    val srcArea = JTextArea()
    private val frame = JFrame("Josue - JSON Object Editor").apply {
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        layout = GridLayout(0, 2)
        size = Dimension(600, 600)
        name = "MenuPrincipal"
        val left = JPanel()
        left.layout = GridLayout()
        val scrollPane = JScrollPane(testPanel()).apply {
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        }
        left.add(scrollPane)
        add(left)

        val right = JPanel()
        right.layout = GridLayout()
        srcArea.tabSize = 2
        srcArea.text = jsonObject.toString()
        right.add(srcArea)
        add(right)
    }

    fun open() {
        frame.isVisible = true
    }

    private fun makePanel(key: String, inner:Boolean): JPanel = JPanel().apply {
        layout = if (inner) {
            BoxLayout(this, BoxLayout.X_AXIS)
        } else {
            BoxLayout(this, BoxLayout.Y_AXIS)
        }
        alignmentX = Component.LEFT_ALIGNMENT
        alignmentY = Component.TOP_ALIGNMENT
        name = key
        add(JLabel(key))
    }

    private fun processJsonObject(jsonElement: JsonElement, panel: JPanel) {
        if (jsonElement is JsonObject) {
            jsonElement.map.forEach { (key, value) ->
                when (value) {
                    is JsonObject -> {
                        processJsonObject(value, panel)
                    }
                    is JsonArray -> {
                        val newPanel = makePanel(key,false)
                        panel.add(newPanel)
                        var count = 0
                        value.list.forEach { item ->
                            val index = count.toString()
                            if (item is JsonElement) {
                                val newinsidePanel = makePanel(index, true)
                                newPanel.add(newinsidePanel)
                                processJsonObject(item, newinsidePanel)
                            } else {
                                val newinsidePanel = makePanel(index, true)
                                val label = JLabel("   ")
                                val textField = JTextField(item.toString())
                                newinsidePanel.add(label)
                                newinsidePanel.add(textField)
                                newPanel.add(newinsidePanel)
                            }
                            count++
                        }
                    }
                    else ->  {panel.add(testWidget(key, value.toString(), false, panel))
                    }
                }
            }
        }
        val components = panel.components
        for (component in components) {
            addMouseListenerToComponent(component, panel)
        }
    }

    sealed class ComponentState {
        data class Added(val key: String, val value: Any, val isJsonArray: Boolean) : ComponentState()
        data class Removed(val key: String) : ComponentState()
    }

    data class Action(val component: Component, val ActionUndoState: ComponentState, val panel: JPanel)

    private fun addMouseListenerToComponent(component_listener: Component, panel: JPanel) {
        component_listener.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    val component = e.component as Component
                    val menu = createPopupMenu(component, panel)
                    menu.show(component, 100, 100)
                }
            }
        })
    }


    fun createPopupMenu(clickedComponent: Component, panel: JPanel): JPopupMenu {
        val menu = JPopupMenu("Message")
        val add = createAddButton(clickedComponent, panel)
        val del = createDeleteButton(clickedComponent, panel)
        val undo = createUndoButton()
        menu.add(add)
        menu.add(del)
        menu.add(undo)
        return menu
    }

    private fun createAddButton(clickedComponent: Component, panel: JPanel): JButton {
        val add = JButton("add")
        add.addActionListener {
            val key = JOptionPane.showInputDialog("key")
            val value = JOptionPane.showInputDialog("value")
            if (!widgetExists((clickedComponent.parent as JPanel).components, key)){
                addComponent(key, value, clickedComponent, panel, true)
            }
        }
        return add
    }


    private fun widgetExists(components: Array<Component>, key:String):Boolean{
        for(component in components){
            //println(component)
            if(component.name == key && key != "///"){
                return true
            }
        }
        return false
    }

    private fun addComponent(key: String, value: String, clickedComponent: Component, panel: JPanel, addAction: Boolean) {
        if(clickedComponent is JLabel){
            addIndexArray(key, value, panel)
            Editor(jsonObject).open()
        }else {
            val component = testWidget(key, value, true, clickedComponent)
            panel.add(component)
            addMouseListenerToComponent(component, panel)
            srcArea.text = jsonObject.toString()
            frame.repaint()
            if(addAction){
                val action = Action(component, ComponentState.Removed(key), panel)
                undoStack.push(action)
            }
        }
    }

    private fun createDeleteButton(clickedComponent: Component, panel: JPanel): JButton {
        val del = JButton("delete")
        del.addActionListener {
            val key = clickedComponent.name
            var value = jsonObject.get(key)
            println("Este é o valor do value 1: " + value)
            componentTextFieldMap.forEach{
                (component, TextField) -> if(component == clickedComponent){
                    value = TextField.text
                }
            }
            println("Este é o valor do value 2: " + value)
            removeComponent(key, value!!, component = clickedComponent, panel = panel)
        }
        return del
    }

    private fun removeComponent(key: String = "", value: Any = "", component: Component, panel: JPanel) {
        val possibleJsonArray = printParentHierarchy(component)
        valueToJson(possibleJsonArray, possibleJsonArray.key!!, "", true)
        panel.remove(component)

        srcArea.text = jsonObject.toString()
        frame.repaint()

        if(value is JsonArray){
            println("Entrei no jsonArray")
            val action = Action(component, ComponentState.Added(key, value!!, true), panel)
            undoStack.push(action)
        }
        else{
            println("naooooooo Entrei no jsonArray")
            // Registra a ação atual na pilha de undo
            val action = Action(component, ComponentState.Added(key, value.toString(), false), panel)
            undoStack.push(action)
        }
    }

    private fun createUndoButton(): JButton {
        val undo = JButton("Undo")
        undo.addActionListener {
            undo()
        }
        return undo
    }

    private fun undo() {

        if (undoStack.isNotEmpty()) {
            val action = undoStack.pop()
            val component = action.component
            val panel = action.panel
            when (action.ActionUndoState) {
                is ComponentState.Added -> {
                    panel.add(component)
                    val addedState = action.ActionUndoState
                    if(addedState.isJsonArray){
                        println("Entrei akiiiii")
                        jsonObject.put(addedState.key, addedState.value)
                        Editor(jsonObject).open()
                    }
                    else{
                        addComponent(addedState.key, addedState.value.toString(), component, panel, false)
                        panel.remove(component)
                    }
                }
                is ComponentState.Removed -> {

                    val possibleJsonArray = printParentHierarchy(component)
                    valueToJson(possibleJsonArray, possibleJsonArray.key!!, "", true)
                    panel.remove(component)
                    srcArea.text = jsonObject.toString()
                    frame.repaint()
                }
            }
        }
    }

    private fun testPanel(): JPanel =
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            alignmentX = Component.LEFT_ALIGNMENT
            alignmentY = Component.TOP_ALIGNMENT
            name = "Panel-Parent"
            processJsonObject(jsonObject, this)
        }

    //isJsonArray

    data class IsInArray(val key: String?,
                         val ArrayName: String?,
                         val index: Int)

    //mudar o numero


    //component_ hierarchy
    fun printParentHierarchy(component: Component): IsInArray {
        var currentComponent: Component? = component
        val key = currentComponent?.name
        var index = -1
        var Array_name:String? = null
        if (currentComponent != null && currentComponent.parent.name != "Panel-Parent") {
            currentComponent = currentComponent.parent
            println(index)
            index = currentComponent.name.toInt()
            currentComponent = currentComponent.parent
            Array_name = currentComponent.name
        }
        return IsInArray(key,Array_name,index)
    }

    fun valueToJson(possibleJsonArray : IsInArray, key: String, selectedValue: String, shouldRemove: Boolean){
        if (possibleJsonArray.ArrayName != null){
            val obj = possibleJsonArray.ArrayName?.let { jsonObject.get(it) } as? JsonArray
            if(key == "///"){
                println("Entrei aki")
                obj?.add(selectedValue)
                Editor(jsonObject).open()
            }
            else {
                obj?.get(possibleJsonArray.index)?.let {
                    if (it is JsonObject) {
                        if (!shouldRemove) {
                            possibleJsonArray.key?.let { it1 -> it.put(key, selectedValue) }
                        } else {
                            it.remove(key)
                        }
                    }
                }
            }
        } else {
            if (!shouldRemove) {
                if(selectedValue == "NewArray/"){
                    val jsonArray = JsonArray()
                    jsonObject.put(key, jsonArray)
                }else{
                    possibleJsonArray.key?.let { jsonObject.put(key, selectedValue) }
                }
            } else {
                jsonObject.remove(key)
            }
        }
    }

    fun makeCheckBox(value: String, panel: JPanel): JCheckBox{
        val checkBox = JCheckBox()
        checkBox.isSelected = value.toBoolean()

        //criar uma função para meter o valor quer seja jsonArray OU NAO
        checkBox.addItemListener { e ->
            val possibleJsonArray = printParentHierarchy(panel)
            val selectedValue = if (e.stateChange == ItemEvent.SELECTED) "true" else "false"
            possibleJsonArray.key?.let { valueToJson(possibleJsonArray, it, selectedValue, false) } // isto e para permitir que a key não é null
            srcArea.text = jsonObject.toString()
        }
        return checkBox
    }

    fun addIndexArray(key: String, value: String, panel: Component){
            val abc =jsonObject.get(panel.parent.name) as JsonArray
        println(abc)
            abc.add(JsonObject().apply {
                put(key, value)
            })
    }

// o panel estava Jpanel
    fun testWidget(key: String, value: String, add: Boolean, panel: Component): JPanel =
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            alignmentX = Component.LEFT_ALIGNMENT
            alignmentY = Component.TOP_ALIGNMENT
            name = key
            if (add && value == "makejsonarray") {
                val jsonArray = JsonArray()
                jsonArray.add(JsonObject().apply {
                    put("Preencha", "valor")
                })
                jsonObject.put(key, jsonArray)
                Editor(jsonObject).open()
            } else {

                var abe = this

                if (key != "///") {
                    add(JLabel(key))
                } else {
                    add(JLabel("     ")) // Adiciona uma JLabel vazia
                }

                if (value == "true" || value == "false") {
                    if (add) {
                        val possibleJsonArray = printParentHierarchy(panel)
                        valueToJson(possibleJsonArray, key, value, false)
                    }
                    add(makeCheckBox(value, this))
                } else {

                    var text = JTextField(value)

                    if (value.isEmpty()) {
                        text = JTextField("null")
                    }

                    if (add) {

                        val possibleJsonArray = printParentHierarchy(panel)
                        valueToJson(possibleJsonArray, key, text.text, false)
                    }

                    text.document.addDocumentListener(object : DocumentListener {
                        override fun insertUpdate(e: DocumentEvent) {
                            updateJsonValue()
                        }

                        override fun removeUpdate(e: DocumentEvent) {
                            updateJsonValue()
                        }

                        override fun changedUpdate(e: DocumentEvent) {
                            updateJsonValue()
                        }

                        private fun updateJsonValue() {
                            val component = text.parent
                            val possibleJsonArray = printParentHierarchy(component)
                            val updatedValue = text.text.trim()

                            if (updatedValue == "true" || updatedValue == "false") {
                                remove(text)
                                add(makeCheckBox(updatedValue, component as JPanel))
                                valueToJson(possibleJsonArray, key, updatedValue, false)

                            } else {
                                valueToJson(possibleJsonArray, key, updatedValue, false)
                            }
                            srcArea.text = jsonObject.toString()
                            revalidate() // Atualiza o layout do painel
                            repaint() // Redesenha o painel
                        }
                    })
                    abe.add(text)
                    componentTextFieldMap[this] = text
                }
            }
        }
}






