import io.obswebsocket.community.client.OBSRemoteController

lateinit var controller: OBSRemoteController
var paused = false

fun main(args: Array<String>) {
    controller = OBSRemoteController.builder()
        .host("localhost")
        .port(4455)
        .lifecycle().onReady {
            controller.startRecord(5)
        }.and()
        .build()
    controller.connect()
    Thread.sleep(5000)
    while (true) {
        Thread.sleep(100)
        if (VMix.isGameplay()) {
            if (paused) {
                paused = false
                controller.resumeRecord(5)
            }
        }
        if (!VMix.isGameplay()) {
            if (!paused) {
                paused = true
                controller.pauseRecord(5)
            }
        }
    }
}