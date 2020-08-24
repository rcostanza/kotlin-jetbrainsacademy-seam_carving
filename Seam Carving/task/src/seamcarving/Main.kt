package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO

val s = Scanner(System.`in`)

fun main() {
    println("Enter rectangle width:")
    val w = s.nextLine().toInt()
    println("Enter rectangle height:")
    val h = s.nextLine().toInt()
    println("Enter output image name:")
    val imageName = s.nextLine()

    val imageBuffer = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    val graphics = imageBuffer.createGraphics()

    graphics.color = Color.RED
    graphics.drawLine(0, 0, w-1, h-1)
    graphics.drawLine(0, h-1, w-1, 0)

    ImageIO.write(imageBuffer, "png", File(imageName))
}
