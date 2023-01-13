package gui

import java.awt.*
import javax.swing.JPanel

class JRectangle(rectangleDimensions: Rectangle, var color: Color) : JPanel() {

    init {
        setBounds(rectangleDimensions.x, rectangleDimensions.y, rectangleDimensions.width, rectangleDimensions.height)
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g as Graphics2D
        super.paintComponent(g)
        g2.color = color
        g2.fillRect(0, 0, g.deviceConfiguration.bounds.width, g.deviceConfiguration.bounds.height)
        //g2.drawRect(rectangleDimensions.x, rectangleDimensions.y, rectangleDimensions.width, rectangleDimensions.height)
        //g2.fillRect(rectangleDimensions.x, rectangleDimensions.y, rectangleDimensions.width, rectangleDimensions.height)
    }

}