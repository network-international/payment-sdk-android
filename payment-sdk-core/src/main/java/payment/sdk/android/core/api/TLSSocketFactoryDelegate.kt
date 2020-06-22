package payment.sdk.android.core.api

import java.net.InetAddress
import java.net.Socket

import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class TLSSocketFactoryDelegate : SSLSocketFactory() {

    private val sslSocketFactory: SSLSocketFactory = SSLContext.getInstance("TLS").run {
        init(null, null, null)
        socketFactory
    }

    override fun getSupportedCipherSuites(): Array<String> =
            sslSocketFactory.supportedCipherSuites

    override fun getDefaultCipherSuites(): Array<String> =
            sslSocketFactory.defaultCipherSuites

    override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket? {
        return enableTlsOnSocket(sslSocketFactory.createSocket(s, host, port, autoClose))
    }

    override fun createSocket(host: String, port: Int): Socket? {
        return enableTlsOnSocket(sslSocketFactory.createSocket(host, port))
    }

    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket? {
        return enableTlsOnSocket(sslSocketFactory.createSocket(host, port, localHost, localPort))
    }

    override fun createSocket(host: InetAddress, port: Int): Socket? {
        return enableTlsOnSocket(sslSocketFactory.createSocket(host, port))
    }

    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress,
                              localPort: Int): Socket? {
        return enableTlsOnSocket(sslSocketFactory.createSocket(address, port, localAddress, localPort))
    }

    private fun enableTlsOnSocket(socket: Socket): Socket {
        if (socket is SSLSocket) {
            socket.enabledProtocols = arrayOf("TLSv1.2")
        }
        return socket
    }
}
