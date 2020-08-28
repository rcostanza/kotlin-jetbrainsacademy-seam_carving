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
    val removeWidth = args[args.indexOf("-width") + 1].toInt()
    val removeHeight = args[args.indexOf("-height") + 1].toInt()

    var buffer = ImageIO.read(File(inputImage))

    repeat(removeWidth) {
        val energyGrid = buffer.energyBuffer()
        val newBuffer = BufferedImage(buffer.width - 1, buffer.height, BufferedImage.TYPE_INT_RGB)
        for (pixel in energyGrid.getSeam()) {
            // Unchanged pixels
            for (x in 0 until pixel.x) newBuffer.setRGB(x, pixel.y, buffer.getRGB(x, pixel.y))
            // Moved pixels
            for (x in pixel.x+1 until buffer.width) newBuffer.setRGB(x - 1, pixel.y, buffer.getRGB(x, pixel.y))
        }
        buffer = newBuffer
    }

    repeat(removeHeight) {
        val energyGrid = buffer.energyBuffer()
        val newBuffer = BufferedImage(buffer.width, buffer.height - 1, BufferedImage.TYPE_INT_RGB)
        for (pixel in energyGrid.getSeam("horizontal")) {
            // Unchanged pixels
            for (y in 0 until pixel.y) newBuffer.setRGB(pixel.x, y, buffer.getRGB(pixel.x, y))
            // Moved pixels
            for (y in pixel.y+1 until buffer.height) newBuffer.setRGB(pixel.x, y - 1, buffer.getRGB(pixel.x, y))
        }
        buffer = newBuffer
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

class BufferedImageEnergyGrid(buffer: BufferedImage): Cloneable {
    var cols = buffer.width
    var rows = buffer.height
    var grid = Array(cols) { Array(rows) { 0.0 } }

    operator fun get(x: Int, y: Int) = grid[x][y]
    operator fun get(pixel: Pixel) = this[pixel.x, pixel.y]
    operator fun set(x: Int, y: Int, value: Double) {
        grid[x][y] = value
    }

    private fun transpose(): BufferedImageEnergyGrid {
        var newGrid = this.clone() as BufferedImageEnergyGrid
        newGrid.grid = newGrid.grid.transpose()
        newGrid.cols = newGrid.rows.also { newGrid.rows = newGrid.cols }
        return newGrid
    }


    fun getSeam(orientation: String = "vertical"): List<Pixel> {
        val energyGrid = if (orientation == "vertical") this else this.transpose()
        val seamTrack = Seam(energyGrid).getSeam()
        return if (orientation == "vertical") seamTrack else seamTrack.transpose()
    }

}

class Seam(val energyGrid: BufferedImageEnergyGrid) {
    private val cols = energyGrid.cols
    private val rows = energyGrid.rows
    var grid = Array(cols) { Array(rows) { Double.POSITIVE_INFINITY }}

    operator fun set(x: Int, y: Int, value: Double) { grid[x][y] = value }
    operator fun get(x: Int, y: Int) = grid[x][y]

    operator fun set(pixel: Pixel, value: Double) { grid[pixel.x][pixel.y] = value }
    operator fun get(pixel: Pixel) = grid[pixel.x][pixel.y]

    init {
        for (x in 0 until cols) this[x, 0] = energyGrid[x, 0]
    }

    private fun getAdjacent(x: Int, y: Int): List<Pixel> {
        if (y == 0) return listOf()
        return (maxOf(x - 1, 0)..minOf(x + 1, cols - 1)).map { Pixel(it, y-1) }
    }

    fun getSeam(): List<Pixel> {
        for (y in 1 until rows) {
            for (x in 0 until cols) {
                this[x, y] = energyGrid[x, y] + getAdjacent(x, y).map { this[it] }.min()!!
            }
        }

        var seamTrackX = grid.mapIndexed { ix, it -> ix to it.last() }.minBy { it.second }!!.first
        val seamTrack = mutableListOf(Pixel(seamTrackX, rows-1))

        for (y in rows-1 downTo 1) {
            seamTrackX = getAdjacent(seamTrackX, y).minBy { this[it] }!!.x
            seamTrack.add(Pixel(seamTrackX, y-1))
        }

        return seamTrack
    }
}

data class Pixel(val x: Int, val y: Int)

fun Array<Array<Double>>.transpose() =
    (this[0].indices).map { c ->
        ((this.indices).map{ r ->
            this[r][c]
        }).toTypedArray()
    }.toTypedArray()

fun List<Pixel>.transpose() = this.map { Pixel(it.y, it.x) }
