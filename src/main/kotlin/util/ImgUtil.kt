package util

import org.im4java.core.IMOperation
import org.im4java.core.ImageMagickCmd
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.awt.Point
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

object ImgUtil {

    fun bufferedImage2Mat(image: BufferedImage): Mat {
        val byteArrayOutputStream = ByteArrayOutputStream()
        ImageIO.write(image, "png", byteArrayOutputStream)
        byteArrayOutputStream.flush()
        return Imgcodecs.imdecode(MatOfByte(*byteArrayOutputStream.toByteArray()), Imgcodecs.IMREAD_UNCHANGED)
    }

    fun mat2BufferedImage(matrix: Mat): BufferedImage {
        val mob = MatOfByte()
        Imgcodecs.imencode(".png", matrix, mob)
        return ImageIO.read(ByteArrayInputStream(mob.toArray()))
    }

    fun overlayText(under: String, text: String, position: Point, fontSize: Int = 100) {
        val composite = ImageMagickCmd("convert")
        val operation = IMOperation()
        operation.addImage(under)

        operation.fill("WHITE")
        operation.font("Gotham-Black.otf")
        operation.gravity("North")
        operation.pointsize(fontSize)
        operation.draw("\"text ${position.x},${position.y} '$text'\"")

        operation.addImage(under)
        composite.run(operation)
    }

    fun overlayImg(under: String, over: String, output: String, flip: Boolean = false) {
        val composite = ImageMagickCmd("convert")
        val operation = IMOperation()
        operation.compose("dstover")
        operation.addImage(over)
        if (flip) operation.flop()

        operation.openOperation()
        operation.addImage(under)

        operation.closeOperation()
        operation.composite()

        operation.addImage(output)
        composite.run(operation)
    }

    enum class Side { LEFT, RIGHT }
    class ImgResult(val value: Double, val side: Side)
    fun matchImg(img: BufferedImage, template: BufferedImage): ImgResult {
        val mat = bufferedImage2Mat(img)
        val matTemplate = bufferedImage2Mat(template)

        val resultCols = mat.cols() - matTemplate.cols() + 1
        val resultRows = mat.rows() - matTemplate.rows() + 1
        if (resultCols > 0 && resultRows > 0) {
            val result = Mat(resultRows, resultCols, CvType.CV_8UC1)

            Imgproc.matchTemplate(mat, matTemplate, result, Imgproc.TM_SQDIFF_NORMED)
            val mmr = Core.minMaxLoc(result)

            val side = if (mmr.minLoc.x > img.width / 2) Side.RIGHT else Side.LEFT
            return ImgResult(mmr.minVal, side)
        }
        return ImgResult(1.0, Side.LEFT)
    }
}