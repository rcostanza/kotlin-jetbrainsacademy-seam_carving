package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.pow
import kotlin.math.sqrt

fun main(args: Array<String>) {
    val inputImage = args[args.indexOf("-in") + 1]
    val outputImage = args[args.indexOf("-out") + 1]

    val buffer = ImageIO.read(File(inputImage))
    val energyBuffer = buffer.energyBuffer()

    for (pixel in energyBuffer.getSeam()) {
        buffer.setRGB(pixel.x, pixel.y, Color.RED.rgb)
    }

    ImageIO.write(buffer, "png", File(outputImage))
}

operator fun BufferedImage.get(x: Int, y: Int) = Color(this.getRGB(x, y))

fun BufferedImage.getEnergy(x: Int, y: Int): Double {
    var xRange = when (x) {
        0 -> Pair(x + 2, x)
        this.width-1 -> Pair(x - 2, x)
        else -> Pair(x + 1, x - 1)
    }

    var yRange = when (y) {
        0 -> Pair(y + 2, y)
        this.height-1 -> Pair(y - 2, y)
        else -> Pair(y + 1, y - 1)
    }

    return sqrt(
        (this[xRange.first, y].red - this[xRange.second, y].red).toDouble().pow(2) +
        (this[xRange.first, y].green - this[xRange.second, y].green).toDouble().pow(2) +
        (this[xRange.first, y].blue - this[xRange.second, y].blue).toDouble().pow(2) +
        (this[x, yRange.first].red - this[x, yRange.second].red).toDouble().pow(2) +
        (this[x, yRange.first].green - this[x, yRange.second].green).toDouble().pow(2) +
        (this[x, yRange.first].blue - this[x, yRange.second].blue).toDouble().pow(2)
    )
}

fun BufferedImage.energyBuffer() = BufferedImageEnergyGrid(this)

class BufferedImageEnergyGrid(buffer: BufferedImage) {
    val cols = buffer.width
    val rows = buffer.height
    private val grid = Array(cols) { Array(rows) { 0.0 } }
    private var seam = Seam(cols, rows)

    var maxEnergy: Double = 0.0
    private set

    operator fun get(x: Int, y: Int) = grid[x][y]
    operator fun get(pixel: Pixel) = this[pixel.x, pixel.y]
    operator fun set(x: Int, y: Int, value: Double) {
        grid[x][y] = value
    }

    init {
        (0 until cols).map { col ->
            (0 until rows).map { row ->
                val energy = buffer.getEnergy(col, row)
                this[col, row] = energy
                if (energy > maxEnergy) maxEnergy = energy
            }
        }
    }

    private fun getAdjacent(x: Int, y: Int): List<Pixel> {
        if (y == 0) return listOf()
        return (maxOf(x - 1, 0)..minOf(x + 1, cols - 1)).map { Pixel(it, y-1) }
    }

    fun getSeam(): List<Pixel> {

        for (x in 0 until cols) seam[x, 0] = this[x, 0]
        for (y in 1 until rows) {
            for (x in 0 until cols) {
                seam[x, y] = this[x, y] + getAdjacent(x, y).map { seam[it] }.min()!!
            }
        }

        var seamTrackX = seam.grid.mapIndexed { ix, it -> ix to it.last() }.minBy { it.second }!!.first
        var seamTrack = mutableListOf(Pixel(seamTrackX, rows-1))

        for (y in rows-1 downTo 1) {
            seamTrackX = getAdjacent(seamTrackX, y).minBy { seam[it] }!!.x
            seamTrack.add(Pixel(seamTrackX, y-1))
        }

        return seamTrack
    }

}

class Seam(val cols: Int, val rows: Int) {
    var grid = Array(cols) { Array(rows) { Double.POSITIVE_INFINITY }}

    operator fun set(x: Int, y: Int, value: Double) { grid[x][y] = value }
    operator fun get(x: Int, y: Int) = grid[x][y]

    operator fun set(pixel: Pixel, value: Double) { grid[pixel.x][pixel.y] = value }
    operator fun get(pixel: Pixel) = grid[pixel.x][pixel.y]
}

data class Pixel(val x: Int, val y: Int)