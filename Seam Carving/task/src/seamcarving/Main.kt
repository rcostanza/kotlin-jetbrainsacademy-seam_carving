package seamcarving

import java.awt.Color
import java.io.File
import javax.imageio.ImageIO

fun main(args: Array<String>) {
    val inputImage = args[args.indexOf("-in")+1]
    val outputImage = args[args.indexOf("-out")+1]

    val buffer = ImageIO.read(File(inputImage))

    for(x in 0 until buffer.width) {
        for(y in 0 until buffer.height) {
            val color = Color(buffer.getRGB(x, y))
            buffer.setRGB(x, y, Color(255 - color.red, 255 - color.green, 255 - color.blue).rgb)
        }
    }
    
    ImageIO.write(buffer, "png", File(outputImage))
}
