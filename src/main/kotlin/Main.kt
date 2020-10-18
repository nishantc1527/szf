package szf

import com.googlecode.lanterna.SGR
import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TextCharacter
import com.googlecode.lanterna.TextColor.ANSI
import com.googlecode.lanterna.graphics.TextGraphics
import com.googlecode.lanterna.input.KeyType
import com.googlecode.lanterna.screen.Screen
import com.googlecode.lanterna.screen.TabBehaviour
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import picocli.CommandLine
import szf.algorithm.FilterAndSort
import java.io.IOException
import java.util.*

class Main {
    @CommandLine.Option(names = ["-i", "--input"], description = ["The Input Text, Separated By New Line Characters"], required = true)
    private var input: String? = null

    companion object {
        @Throws(IOException::class)
        private fun updateList(textGraphics: TextGraphics, input: Array<String>, screen: Screen, word: String): Array<String> {
            textGraphics.fillRectangle(TerminalPosition(0, 1), screen.terminalSize.withRelativeRows(-1), ' ')
            val newInput = Arrays.stream(input).filter(FilterAndSort.filter(word)).sorted(FilterAndSort.sort(word))
                    .toArray<String> { arrayOf()}
            for (i in 1..newInput.size) {
                textGraphics.putString(0, i, newInput[i - 1], SGR.BOLD)
            }
            screen.refresh()
            return newInput
        }

        @Throws(IOException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val main = Main()
            CommandLine(main).parseArgs(*args)
            val input = main.input!!.split("\n".toRegex()).toTypedArray()
            val defaultTerminalFactory = DefaultTerminalFactory()
            val screen: Screen = TerminalScreen(defaultTerminalFactory.createTerminal())
            screen.startScreen()
            screen.cursorPosition = TerminalPosition(2, 0)
            screen.refresh()
            val textGraphics = screen.newTextGraphics().setTabBehaviour(TabBehaviour.CONVERT_TO_FOUR_SPACES)
            var i = 1
            while (i <= input.size && i < screen.terminalSize.rows) {
                textGraphics.putString(0, i, input[i - 1], SGR.BOLD)
                i++
            }
            screen.refresh()
            textGraphics.setCharacter(0, 0, TextCharacter('>', ANSI.GREEN, ANSI.DEFAULT, SGR.BOLD))
            screen.refresh()
            var newInput = input
            val word = StringBuilder()
            while (true) {
                val keyStroke = screen.readInput()
                if (keyStroke.keyType == KeyType.Escape) {
                    screen.close()
                    return
                }
                if (keyStroke.keyType == KeyType.Enter) {
                    break
                }
                if (keyStroke.keyType == KeyType.Backspace) {
                    if (word.isNotEmpty()) {
                        screen.setCharacter(word.length + 1, 0, TextCharacter(' ', ANSI.DEFAULT, ANSI.DEFAULT))
                        word.deleteCharAt(word.length - 1)
                        screen.cursorPosition = screen.cursorPosition.withRelativeColumn(-1)
                        screen.refresh()
                        newInput = updateList(textGraphics, input, screen, word.toString())
                    }
                } else {
                    val character = keyStroke.character
                    if (character != null) {
                        word.append(character)
                        screen.setCharacter(word.length + 1, 0,
                                TextCharacter(character, ANSI.BLUE, ANSI.DEFAULT, SGR.BOLD, SGR.UNDERLINE))
                        screen.cursorPosition = screen.cursorPosition.withRelativeColumn(1)
                        screen.refresh()
                        newInput = updateList(textGraphics, input, screen, word.toString())
                    }
                }
            }
            screen.close()
            if (newInput.isNotEmpty()) {
                println(newInput[0])
            }
        }
    }
}