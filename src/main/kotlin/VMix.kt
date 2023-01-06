import org.xml.sax.InputSource
import java.io.StringReader
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

object VMix {

    fun isGameplay(): Boolean {
        val vmixxml = URL("http://172.30.4.85:8088/api").openStream().bufferedReader().readLine()

        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val inputs = builder.parse(InputSource(StringReader(vmixxml))).getElementsByTagName("vmix").item(0)
        for (i in 0 until inputs.childNodes.length) {
            val node = inputs.childNodes.item(i)
            if (node.nodeName == "active") {
                if (node.textContent == "1") {
                    return true
                }
                break
            }
        }
        return false
    }
}